package com.pisanadam.realcars.client.sound;

import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.EngineVoice;
import com.pisanadam.realcars.entity.GearBox;
import com.pisanadam.realcars.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Motor sesinin tek bir katmanı (rölanti, orta ya da yüksek devir).
 *
 * <p>Her araç için üç katman birlikte çalar. Katmanlar gerçek bir araç
 * kaydının farklı devirlerinden kesilmiş döngülerdir; her biri kendi kayıt
 * devrine göre perdelendirilir ve devir iki katmanın arasındayken ikisi
 * eşit güçle karıştırılır. Tek bir döngüyü 0.5-2 kat perdelemek motoru
 * "sincap sesi"ne çevirirdi; katmanlar sayesinde her devirde kendine yakın
 * bir kaydın tınısı duyulur — yarış oyunlarındaki motor sesleri de böyle
 * kurulur.
 *
 * <p>Perde ve ses seviyesi anlık değil, yumuşatılarak izlenir; vites
 * değişiminde ses zıplamaz, düşüp tekrar yükselir.
 */
public class CarEngineSoundInstance extends AbstractTickableSoundInstance {
	private static final float MIN_PITCH = 0.5F;
	private static final float MAX_PITCH = 2.0F;
	/** Kırmızı bölgenin, yüksek katmanın kayıt devrinin kaç katına denk geleceği. */
	private static final float REDLINE_OVER_HIGH = 1.9F;

	private final CarEntity car;
	private final EngineVoice.Layer layer;
	private final float[] layerRpms;
	private float smoothedRpm = GearBox.IDLE_RPM;
	private float smoothedVolume;

	public CarEngineSoundInstance(final CarEntity car, final EngineVoice voice, final EngineVoice.Layer layer) {
		super(ModSounds.engineLayer(voice, layer), SoundSource.NEUTRAL, RandomSource.create());
		this.car = car;
		this.layer = layer;
		this.layerRpms = EngineVoiceProfiles.layerRpms(voice);
		this.looping = true;
		this.delay = 0;
		this.volume = 0.0F;
		this.pitch = 1.0F;
		this.x = car.getX();
		this.y = car.getY();
		this.z = car.getZ();
	}

	public CarEntity car() {
		return this.car;
	}

	/** {@code stop()} korumalı olduğu için ses yöneticisine açık bir kapı. */
	public void stopEngine() {
		this.stop();
	}

	@Override
	public boolean canPlaySound() {
		return !this.car.isSilent();
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	/**
	 * Katmanın verilen devirdeki ağırlığı (0-1). Komşu iki katman, devrin
	 * logaritmik konumuna göre sinüs/kosinüs eğrisiyle karışır; toplam güç
	 * sabit kalır, geçişte ses ne çöker ne patlar.
	 */
	static float weight(final float[] rpms, final int layer, final float rpm) {
		final float position = position(rpms, rpm);
		final float distance = Math.abs(position - layer);
		if (distance >= 1.0F) {
			return 0.0F;
		}
		return Mth.cos(distance * Mth.HALF_PI);
	}

	/** Devrin katmanlar arasındaki konumu: 0 rölanti, 1 orta, 2 yüksek. */
	static float position(final float[] rpms, final float rpm) {
		if (rpm <= rpms[0]) {
			return 0.0F;
		}
		for (int i = 0; i < rpms.length - 1; i++) {
			if (rpm <= rpms[i + 1]) {
				return i + (float) (Math.log(rpm / rpms[i]) / Math.log(rpms[i + 1] / rpms[i]));
			}
		}
		return rpms.length - 1;
	}

	/**
	 * Oyundaki devri kayıtların devir ölçeğine taşır.
	 *
	 * <p>Kayıtlarda motorlar rölantinin ancak 2-3 katına kadar sabit tutulmuş;
	 * oyunda ise devir rölantiden kırmızı bölgeye 8-9 kat çıkar. Oyun motoru
	 * perdeyi en fazla 2 kat yükseltebildiği için doğrudan eşleme sesi
	 * yarı yolda tavana vurdururdu. Bunun yerine devir üssel olarak eşlenir:
	 * rölanti rölanti kalır, kırmızı bölge yüksek katmanın ~1.9 katına düşer;
	 * aradaki her devirde ses sürekli yükselir.
	 */
	float soundRpm(final float rpm, final int redline) {
		final float idle = GearBox.IDLE_RPM;
		final float top = this.layerRpms[this.layerRpms.length - 1] * REDLINE_OVER_HIGH;
		if (redline <= idle * 1.01F || top <= idle) {
			return rpm;
		}
		final double gamma = Math.log(top / idle) / Math.log(redline / idle);
		return (float) (idle * Math.pow(rpm / idle, Math.min(1.0, gamma)));
	}

	/**
	 * Gerçek bir motor sabit devirde bile kusursuz dönmez: rölanti hafifçe
	 * dalgalanır, yanma her çevrimde biraz farklıdır. Döngü kaydı ise birebir
	 * tekrar eder ve kulağa makine gibi gelir. Bu yüzden perdeye binde birkaç
	 * oranında, birbirinin katı olmayan iki yavaş salınım eklenir. Üç katman
	 * aynı salınımı paylaşır (araca bağlı); yoksa birbirlerinden kayıp
	 * yankılanırlardı.
	 */
	static float wobble(final CarEntity car) {
		final float t = (car.tickCount + (car.getId() % 97)) / 20.0F;
		return 1.0F + 0.0045F * Mth.sin(t * 0.83F) + 0.003F * Mth.sin(t * 2.31F + 1.3F);
	}

	@Override
	public void tick() {
		if (this.car.isRemoved() || !this.car.engineOn()) {
			this.stop();
			return;
		}

		this.x = this.car.getX();
		this.y = this.car.getY();
		this.z = this.car.getZ();

		final int rpm = Math.max(GearBox.IDLE_RPM, this.car.rpm());
		this.smoothedRpm = Mth.lerp(0.22F, this.smoothedRpm, soundRpm(rpm, this.car.engineType().redlineRpm()));
		final int index = this.layer.ordinal();
		this.pitch = Mth.clamp(this.smoothedRpm * wobble(this.car) / this.layerRpms[index], MIN_PITCH, MAX_PITCH);

		// Yüksek devirde motor daha gür duyulur, rölantide sadece mırıldanır.
		final float load = Mth.clamp((rpm - GearBox.IDLE_RPM)
			/ (float) Math.max(1, this.car.engineType().redlineRpm() - GearBox.IDLE_RPM), 0.0F, 1.0F);
		final float master = 0.38F + 0.55F * load;
		final float target = master * weight(this.layerRpms, index, this.smoothedRpm);
		this.smoothedVolume = Mth.lerp(0.25F, this.smoothedVolume, target);
		this.volume = this.smoothedVolume;
	}
}
