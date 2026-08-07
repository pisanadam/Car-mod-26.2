package com.pisanadam.realcars.client.sound;

import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.GearBox;
import com.pisanadam.realcars.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Devire bağlı motor sesi.
 *
 * <p>Ses dosyası tek bir referans devirde ({@link #REFERENCE_RPM}) üretilmiştir;
 * gerçek devir bunun katı olduğunda perde de aynı oranda kaydırılır. Oyun
 * motorunun perde aralığı 0.5-2.0 ile sınırlı olduğu için çok düşük ve çok
 * yüksek devirler bu aralığa sıkıştırılır, ama duyulan etki gerçek bir motorun
 * yükselip alçalması gibidir.
 *
 * <p>Perde ve ses seviyesi anlık değil, yumuşatılarak izlenir; böylece vites
 * değişiminde ses zıplamaz, düşüp tekrar yükselir.
 */
public class CarEngineSoundInstance extends AbstractTickableSoundInstance {
	/** {@code tools/gen_sounds.py} içindeki REF_RPM ile aynı olmalıdır. */
	private static final float REFERENCE_RPM = 3000.0F;
	private static final float MIN_PITCH = 0.5F;
	private static final float MAX_PITCH = 2.0F;

	private final CarEntity car;
	private float smoothedPitch = 1.0F;
	private float smoothedVolume;

	public CarEngineSoundInstance(final CarEntity car) {
		super(ModSounds.engineLoop(car.engineType()), SoundSource.NEUTRAL, RandomSource.create());
		this.car = car;
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
		final float targetPitch = Mth.clamp(rpm / REFERENCE_RPM, MIN_PITCH, MAX_PITCH);
		// Yüksek devirde motor daha gür duyulur, rölantide sadece mırıldanır.
		final float load = Mth.clamp((rpm - GearBox.IDLE_RPM)
			/ (float) Math.max(1, this.car.engineType().redlineRpm() - GearBox.IDLE_RPM), 0.0F, 1.0F);
		final float targetVolume = 0.32F + 0.58F * load;

		this.smoothedPitch = Mth.lerp(0.22F, this.smoothedPitch, targetPitch);
		this.smoothedVolume = Mth.lerp(0.18F, this.smoothedVolume, targetVolume);
		this.pitch = this.smoothedPitch;
		this.volume = this.smoothedVolume;
	}
}
