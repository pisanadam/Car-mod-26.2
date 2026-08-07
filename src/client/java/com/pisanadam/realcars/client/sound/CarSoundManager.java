package com.pisanadam.realcars.client.sound;

import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.EngineType;
import com.pisanadam.realcars.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundSource;
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
			if (entity instanceof CarEntity car && car.engineOn() && !ACTIVE.containsKey(car.getId())) {
				final CarEngineSoundInstance instance = new CarEngineSoundInstance(car);
				client.getSoundManager().play(instance);
				ACTIVE.put(car.getId(), new Playing(instance, car.engineType()));
			}
		}

		playRideEffects(client);
	}

	/** Sürücünün duyduğu, motordan bağımsız sesler: çakıl ve fren. */
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
	}
}
