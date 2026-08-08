package com.pisanadam.realcars.client.sound;

import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.EngineType;
import com.pisanadam.realcars.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Görünür araçların motor ve sürüş seslerini yönetir.
 *
 * <p>Her araç için en fazla bir motor döngüsü çalar; motor kapanınca ya da araç
 * yok olunca döngü kendini durdurur ve buradan silinir. Motor tipi modifiye ile
 * değişirse ses de değişmelidir, bu yüzden çalan döngünün hangi motora ait
 * olduğu saklanır.
 */
public final class CarSoundManager {
	private static final Int2ObjectMap<Playing> ACTIVE = new Int2ObjectOpenHashMap<>();
	/** Çakıl sesinin duyulmaya başladığı hız (km/s). */
	private static final float GRAVEL_SPEED = 12.0F;
	/** Fren gıcırtısının duyulmaya başladığı hız (km/s). */
	private static final float BRAKE_SPEED = 20.0F;
	/** Aynı araçtan iki gıcırtı arasındaki en kısa süre (tick). */
	private static final int BRAKE_COOLDOWN = 16;
	/** Araç id -> son gıcırtının oyun zamanı; üst üste binmeyi engeller. */
	private static final Int2LongMap LAST_BRAKE = new Int2LongOpenHashMap();
	private static int brakeSounds;

	private record Playing(CarEngineSoundInstance instance, EngineType engine) {
	}

	private CarSoundManager() {
	}

	public static void tick(final Minecraft client) {
		if (client.level == null) {
			ACTIVE.clear();
			return;
		}

		final Iterator<Int2ObjectMap.Entry<Playing>> iterator =
			ACTIVE.int2ObjectEntrySet().iterator();
		while (iterator.hasNext()) {
			final Int2ObjectMap.Entry<Playing> entry = iterator.next();
			final Playing playing = entry.getValue();
			final Entity entity = client.level.getEntity(entry.getIntKey());
			final boolean stale = playing.instance().isStopped()
				|| !(entity instanceof CarEntity car)
				|| !car.engineOn()
				|| car.engineType() != playing.engine();
			if (stale) {
				playing.instance().stopEngine();
				iterator.remove();
			}
		}

		for (final Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof CarEntity car)) {
				continue;
			}
			if (car.engineOn() && !ACTIVE.containsKey(car.getId())) {
				final CarEngineSoundInstance instance = new CarEngineSoundInstance(car);
				client.getSoundManager().play(instance);
				ACTIVE.put(car.getId(), new Playing(instance, car.engineType()));
			}
			// Fren, motorun açık olmasına bağlı değil ve yalnızca sürücünün
			// değil çevredekilerin de duyması gerekir; bu yüzden görünen her
			// araç için ayrı ayrı bakılır.
			playBrakeSqueal(client, car);
		}

		forgetOldBrakeTimes(client);
		playRideEffects(client);
	}

	/**
	 * Sert frende ve patinajda lastik gıcırtısı çalar.
	 *
	 * <p>Fren her tick tetiklendiği için ses de her tick çalınırsa üst üste
	 * binip uğultuya dönüşür; bu yüzden araç başına bir bekleme süresi tutulur.
	 * Ses yüksekliği ve tizliği hızla artar — yavaşta hafif bir cızırtı, hızda
	 * belirgin bir gıcırtı duyulur.
	 */
	private static void playBrakeSqueal(final Minecraft client, final CarEntity car) {
		final float speed = Math.abs(car.speedKmh());
		if (speed < BRAKE_SPEED || !car.onGround()) {
			return;
		}
		if (!car.braking() && !car.wheelSlipping()) {
			return;
		}
		final long now = client.level.getGameTime();
		if (now - LAST_BRAKE.get(car.getId()) < BRAKE_COOLDOWN) {
			return;
		}
		LAST_BRAKE.put(car.getId(), now);
		brakeSounds++;

		final float volume = Mth.clamp(speed / 110.0F, 0.25F, 0.9F);
		final float pitch = Mth.clamp(0.82F + speed / 260.0F, 0.82F, 1.25F);
		client.getSoundManager().play(new EntityBoundSoundInstance(
			ModSounds.BRAKE_SQUEAL, SoundSource.NEUTRAL, volume, pitch, car,
			client.level.getRandom().nextLong()));
	}

	/** Çoktan yok olmuş araçların bekleme kayıtları birikmesin. */
	private static void forgetOldBrakeTimes(final Minecraft client) {
		final long now = client.level.getGameTime();
		if (LAST_BRAKE.isEmpty() || now % 200L != 0L) {
			return;
		}
		LAST_BRAKE.values().removeIf(time -> now - time > 400L);
	}

	/**
	 * Şimdiye kadar çalınan fren sesi sayısı. Sesin gerçekten çalındığı
	 * ekran görüntüsünden anlaşılamadığı için istemci testi bunu okur.
	 */
	public static int brakeSoundCount() {
		return brakeSounds;
	}

	/** Sürücünün duyduğu, motordan bağımsız zemin sesi. */
	private static void playRideEffects(final Minecraft client) {
		if (client.player == null || !(client.player.getVehicle() instanceof CarEntity car)) {
			return;
		}
		final float speed = Math.abs(car.speedKmh());
		if (speed < GRAVEL_SPEED || !car.onGround()) {
			return;
		}
		if (car.groundState().is(com.pisanadam.realcars.registry.ModTags.DUSTY)
			&& client.level != null && client.level.getGameTime() % 24L == 0L) {
			client.getSoundManager().play(new EntityBoundSoundInstance(
				ModSounds.GRAVEL_LOOP, SoundSource.NEUTRAL,
				Math.min(0.7F, speed / 90.0F), 0.9F + speed / 400.0F, car,
				client.level.getRandom().nextLong()));
		}
	}

	public static void clear() {
		ACTIVE.values().forEach(playing -> playing.instance().stopEngine());
		ACTIVE.clear();
		LAST_BRAKE.clear();
	}
}
