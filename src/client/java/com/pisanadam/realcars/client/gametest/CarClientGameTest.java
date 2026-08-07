package com.pisanadam.realcars.client.gametest;

import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.CarModel;
import com.pisanadam.realcars.entity.SpoilerType;
import com.pisanadam.realcars.entity.WheelType;
import com.pisanadam.realcars.registry.ModEntities;
import com.pisanadam.realcars.registry.ModMenus;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;

/**
 * Oyunu gerçekten açıp modun görsel tarafını sınayan istemci testi.
 *
 * <p>Bir dünya kurar, asfalt bir alan döşer, her aracı yan yana çıkarır ve
 * ekran görüntüleri alır; ardından bir araca binip göstergeyi ve modifiye
 * ekranını da fotoğraflar. Görüntüler {@code build/run/clientGametest/screenshots}
 * altına düşer.
 *
 * <p>Çalıştırmak için: {@code ./gradlew runClientGametest} (başsız bir makinede
 * {@code xvfb-run} ile). Bu giriş noktası yalnızca
 * fabric-client-gametest-api-v1 yüklüyken istendiği için normal oyunda hiç
 * yüklenmez.
 */
public class CarClientGameTest implements FabricClientGameTest {
	private static final int GROUND_Y = 64;

	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			final var server = singleplayer.getServer();
			final var connection = singleplayer.getConnection();

			server.runCommand("time set noon");
			server.runCommand("weather clear");
			// Konsoldan çalıştığı için komutun hedefi açıkça verilmeli.
			server.runCommand("gamemode creative @a");
			// Araçların üzerinde duracağı düz asfalt alan
			server.runCommand("fill -20 " + (GROUND_Y - 1) + " -20 20 " + (GROUND_Y - 1)
				+ " 20 realcars:asphalt");
			server.runCommand("fill -20 " + GROUND_Y + " -20 20 " + (GROUND_Y + 12) + " 20 air");
			server.runCommand("tp @p 0 " + (GROUND_Y + 2) + " -14 0 10");
			context.waitTicks(20);

			spawnEveryCar(context, singleplayer);
			connection.waitForChunksRender();
			context.waitTicks(40);
			context.takeScreenshot("01-tum-araclar");

			// Yakından tek araç: tekerlek, jant ve rüzgarlık ayrıntıları görünsün
			server.runCommand("kill @e[type=!player]");
			server.runCommand("tp @a 7 " + (GROUND_Y + 2) + " -6 90 5");
			context.waitTicks(10);
			spawnShowcaseCar(context, singleplayer);
			connection.waitForChunksRender();
			context.waitTicks(40);
			context.takeScreenshot("02-yakin-plan");

			rideAndScreenshot(context, singleplayer);
		}
	}

	/** Araçların bulunduğu bölgeyi kapsayan arama kutusu. */
	private static AABB searchArea() {
		return new AABB(-64.0D, GROUND_Y - 8.0D, -64.0D, 64.0D, GROUND_Y + 24.0D, 64.0D);
	}

	/** On iki aracı bir sıra hâlinde çıkarır. */
	private static void spawnEveryCar(final ClientGameTestContext context,
									  final TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> {
			final var level = singleplayer.getConnection().getServerLevel();
			int x = -18;
			for (final CarModel model : CarModel.values()) {
				final CarEntity car = ModEntities.type(model)
					.create(level, EntitySpawnReason.MOB_SUMMONED);
				if (car == null) {
					throw new AssertionError(model + " için araç yaratılamadı");
				}
				car.setPos(x, GROUND_Y, 0.0D);
				car.setYRot(0.0F);
				level.addFreshEntity(car);
				x += 3;
			}
		});
		context.waitTicks(10);

		// Hepsi gerçekten dünyada mı?
		final int alive = singleplayer.getServer().computeOnServer(server -> {
			final var level = singleplayer.getConnection().getServerLevel();
			final List<CarEntity> cars = level.getEntitiesOfClass(CarEntity.class,
				searchArea());
			return cars.size();
		});
		if (alive != CarModel.values().length) {
			throw new AssertionError("Beklenen " + CarModel.values().length
				+ " araç, dünyada bulunan " + alive);
		}
	}

	/** Modifiye edilmiş tek bir aracı oyuncunun önüne koyar. */
	private static void spawnShowcaseCar(final ClientGameTestContext context,
										 final TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> {
			final var level = singleplayer.getConnection().getServerLevel();
			final CarEntity car = ModEntities.type(CarModel.CAR_MUSTANG_GT)
				.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (car == null) {
				throw new AssertionError("Gösterim aracı yaratılamadı");
			}
			car.setPos(0.0D, GROUND_Y, -6.0D);
			car.setYRot(35.0F);
			car.applyConfig(car.config()
				.withColor(0xE8C62E)
				.withWheel(WheelType.WHEEL_CHROME)
				.withSpoiler(SpoilerType.SPOILER_GT));
			level.addFreshEntity(car);
		});
		context.waitTicks(10);
	}

	/** Araca binip hız göstergesini ve modifiye ekranını fotoğraflar. */
	private static void rideAndScreenshot(final ClientGameTestContext context,
										  final TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> {
			final var level = singleplayer.getConnection().getServerLevel();
			final var player = singleplayer.getConnection().getServerPlayer();
			final List<CarEntity> cars = level.getEntitiesOfClass(CarEntity.class,
				searchArea());
			if (cars.isEmpty()) {
				throw new AssertionError("Binilecek araç yok");
			}
			final CarEntity car = cars.getFirst();
			car.setEngineOn(true);
			player.startRiding(car, true, true);
		});
		context.waitTicks(30);
		context.takeScreenshot("03-gosterge");

		// Gaza bas: araç gerçekten hızlanıyor mu, gösterge ibresi kalkıyor mu?
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(60);
		final float speed = context.computeOnClient(client ->
			client.player != null && client.player.getVehicle() instanceof CarEntity car
				? Math.abs(car.speedKmh()) : 0.0F);
		context.takeScreenshot("04-surus");
		context.getInput().releaseKey(options -> options.keyUp);
		if (speed < 5.0F) {
			throw new AssertionError("Araç gaza rağmen hızlanmadı: " + speed + " km/s");
		}
		context.waitTicks(40);

		// Modifiye ekranı
		singleplayer.getServer().runOnServer(server -> {
			final var player = singleplayer.getConnection().getServerPlayer();
			if (player.getVehicle() instanceof CarEntity car) {
				ModMenus.openModification(player, car);
			}
		});
		context.waitTicks(30);
		context.takeScreenshot("05-modifiye-ekrani");
	}
}
