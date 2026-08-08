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
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;

/**
 * Oyunu gerçekten açıp modun görsel tarafını sınayan istemci testi.
 *
 * <p>Bir dünya kurar, asfalt bir alan döşer ve <b>her aracı tek tek</b> hem
 * yandan hem 3/4 açıdan fotoğraflar; böylece her siluetin gerçek arabasına
 * benzeyip benzemediği gözle doğrulanabilir. Ardından bir araca binip
 * göstergeyi, süspansiyon hareketini (frende burun dalması) ve modifiye
 * ekranını da kaydeder. Görüntüler {@code build/run/clientGametest/screenshots}
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
			server.runCommand("fill -24 " + (GROUND_Y - 1) + " -24 24 " + (GROUND_Y - 1)
				+ " 24 realcars:asphalt");
			server.runCommand("fill -24 " + GROUND_Y + " -24 24 " + (GROUND_Y + 14) + " 24 air");
			server.runCommand("tp @p 0 " + (GROUND_Y + 2) + " -14 0 10");
			context.waitTicks(20);

			spawnEveryCar(context, singleplayer);
			connection.waitForChunksRender();
			context.waitTicks(40);
			context.takeScreenshot("01-tum-araclar");

			portraitOfEveryCar(context, singleplayer);
			showcase(context, singleplayer);
			rideAndScreenshot(context, singleplayer);
		}
	}

	/** Araçların bulunduğu bölgeyi kapsayan arama kutusu. */
	private static AABB searchArea() {
		return new AABB(-80.0D, GROUND_Y - 8.0D, -80.0D, 80.0D, GROUND_Y + 24.0D, 80.0D);
	}

	/** On iki aracı bir sıra hâlinde çıkarır. */
	private static void spawnEveryCar(final ClientGameTestContext context,
									  final TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> {
			int x = -22;
			for (final CarModel model : CarModel.values()) {
				spawn(singleplayer, model, x, 0.0D, 0.0F);
				x += 4;
			}
		});
		context.waitTicks(10);

		// Hepsi gerçekten dünyada mı?
		final int alive = singleplayer.getServer().computeOnServer(server -> {
			final var level = singleplayer.getConnection().getServerLevel();
			final List<CarEntity> cars = level.getEntitiesOfClass(CarEntity.class, searchArea());
			return cars.size();
		});
		if (alive != CarModel.values().length) {
			throw new AssertionError("Beklenen " + CarModel.values().length
				+ " araç, dünyada bulunan " + alive);
		}
	}

	/**
	 * Her aracı tek başına çıkarıp yandan ve 3/4 açıdan fotoğraflar.
	 *
	 * <p>Yan profil siluetin doğruluğunu (kaput eğimi, cam açısı, fastback
	 * sırtı), 3/4 açı ise genişlik daralmasını ve çamurluk kabartmalarını
	 * gösterir.
	 */
	private static void portraitOfEveryCar(final ClientGameTestContext context,
										   final TestSingleplayerContext singleplayer) {
		final var server = singleplayer.getServer();
		int index = 1;
		for (final CarModel model : CarModel.values()) {
			server.runCommand("kill @e[type=!player]");
			server.runOnServer(unused -> spawn(singleplayer, model, 0.0D, 0.0D, 90.0F));
			context.waitTicks(6);

			final String label = String.format("%02d-%s", index++, model.itemName());

			// Yandan: araç Z eksenine dik durur, oyuncu -Z'den bakar
			server.runCommand("tp @a 0 " + (GROUND_Y + 1) + " -9 0 6");
			context.waitTicks(6);
			context.takeScreenshot(label + "-yan");

			// 3/4: köşeden bakış. (-6.5, -6.5)'tan merkeze bakmak için yön
			// güneydoğu, yani yaw -45 olmalı.
			server.runCommand("tp @a -6.5 " + (GROUND_Y + 1.8) + " -6.5 -45 6");
			context.waitTicks(6);
			context.takeScreenshot(label + "-uc-ceyrek");
		}
	}

	/** Modifiye edilmiş bir aracı yakın plandan gösterir. */
	private static void showcase(final ClientGameTestContext context,
								 final TestSingleplayerContext singleplayer) {
		final var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!player]");
		server.runOnServer(unused -> {
			final CarEntity car = spawn(singleplayer, CarModel.CAR_MUSTANG_GT, 0.0D, 0.0D, 115.0F);
			car.applyConfig(car.config()
				.withColor(0xE8C62E)
				.withWheel(WheelType.WHEEL_CHROME)
				.withSpoiler(SpoilerType.SPOILER_GT));
		});
		server.runCommand("tp @a -4.2 " + (GROUND_Y + 1.4) + " -4.2 -45 8");
		context.waitTicks(20);
		context.takeScreenshot("90-yakin-plan");

		// Gece: farlar yanıyor mu?
		server.runOnServer(unused -> {
			final var level = singleplayer.getConnection().getServerLevel();
			level.getEntitiesOfClass(CarEntity.class, searchArea())
				.forEach(car -> car.setEngineOn(true));
		});
		server.runCommand("time set midnight");
		context.waitTicks(20);
		context.takeScreenshot("91-gece-farlar");
		server.runCommand("time set noon");
		context.waitTicks(10);
	}

	/** Araca binip hız göstergesini, süspansiyonu ve modifiye ekranını fotoğraflar. */
	private static void rideAndScreenshot(final ClientGameTestContext context,
										  final TestSingleplayerContext singleplayer) {
		final var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!player]");
		// Hızlanmaya yer açan bir pist. Otomatik şanzımanlı bir araç seçildi:
		// manuelde vites elle yükseltilmediği için araç birinci vitesde takılı
		// kalır ve süspansiyon hareketini gösterecek hıza çıkamaz.
		server.runCommand("forceload add -96 -16 32 16");
		server.runCommand("fill -96 " + (GROUND_Y - 1) + " -8 32 " + (GROUND_Y - 1)
			+ " 8 realcars:asphalt");
		server.runCommand("fill -96 " + GROUND_Y + " -8 32 " + (GROUND_Y + 6) + " 8 air");
		server.runOnServer(unused -> spawn(singleplayer, CarModel.CAR_DEFENDER, -60.0D, 0.0D, -90.0F));
		context.waitTicks(6);

		server.runOnServer(unused -> {
			final var level = singleplayer.getConnection().getServerLevel();
			final var player = singleplayer.getConnection().getServerPlayer();
			final List<CarEntity> cars = level.getEntitiesOfClass(CarEntity.class, searchArea());
			if (cars.isEmpty()) {
				throw new AssertionError("Binilecek araç yok");
			}
			final CarEntity car = cars.getFirst();
			car.setEngineOn(true);
			player.startRiding(car, true, true);
		});
		context.waitTicks(30);
		context.takeScreenshot("92-gosterge");

		// Süspansiyonu görmek için kamerayı arkaya al: birinci şahısta gövde
		// zaten görünmez.
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(5);

		// Gaza bas: araç gerçekten hızlanıyor mu, gösterge ibresi kalkıyor mu?
		context.getInput().holdKey(options -> options.keyUp);
		final StringBuilder trace = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			context.waitTicks(20);
			trace.append(String.format(" %.0fkm/s(v%d @%drpm)",
				load(context, CarEntity::speedKmh),
				(int) load(context, car -> (float) car.gear()),
				(int) load(context, car -> (float) car.rpm())));
		}
		System.out.println("[RealCars] hızlanma izi:" + trace);
		final float speed = Math.abs(load(context, CarEntity::speedKmh));
		context.takeScreenshot("93-surus");

		// Direksiyon kırılıyken gövde yana yatıyor mu?
		context.getInput().holdKey(options -> options.keyLeft);
		context.waitTicks(14);
		context.takeScreenshot("94-virajda-yatma");
		final float roll = load(context, CarEntity::lateralLoad);
		context.getInput().releaseKey(options -> options.keyLeft);
		context.getInput().releaseKey(options -> options.keyUp);

		// Sert fren: burun dalıyor, stop lambaları yanıyor mu?
		context.getInput().holdKey(options -> options.keyDown);
		context.waitTicks(4);
		context.takeScreenshot("95-frende-burun-dalmasi");
		final float dive = load(context, CarEntity::longitudinalLoad);
		final boolean braking = load(context, car -> car.braking() ? 1.0F : 0.0F) > 0.5F;
		context.getInput().releaseKey(options -> options.keyDown);
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

		// Ekran görüntüsü gözle bakmak için; asıl doğrulama sayılarla yapılır,
		// çünkü "gövde yattı mı" sorusu piksellerden güvenilir okunmaz.
		if (speed < 40.0F) {
			throw new AssertionError("Otomatik şanzımanlı araç 5 saniyede yeterince "
				+ "hızlanmadı: " + speed + " km/s");
		}
		if (Math.abs(roll) < 0.05F) {
			throw new AssertionError("Virajda gövdeye yanal yük binmedi: " + roll);
		}
		if (dive > -0.05F || !braking) {
			throw new AssertionError("Frende burun dalmadı: " + dive + ", fren=" + braking);
		}
		context.waitTicks(40);

		// Modifiye ekranı
		server.runOnServer(unused -> {
			final var player = singleplayer.getConnection().getServerPlayer();
			if (player.getVehicle() instanceof CarEntity car) {
				ModMenus.openModification(player, car);
			}
		});
		context.waitTicks(30);
		context.takeScreenshot("96-modifiye-ekrani");
	}

	/** Binilen araçtan istemci tarafında bir değer okur. */
	private static float load(final ClientGameTestContext context,
							  final java.util.function.Function<CarEntity, Float> reader) {
		return context.computeOnClient(client ->
			client.player != null && client.player.getVehicle() instanceof CarEntity car
				? reader.apply(car) : 0.0F);
	}

	private static CarEntity spawn(final TestSingleplayerContext singleplayer, final CarModel model,
								   final double x, final double z, final float yRot) {
		final var level = singleplayer.getConnection().getServerLevel();
		final CarEntity car = ModEntities.type(model).create(level, EntitySpawnReason.MOB_SUMMONED);
		if (car == null) {
			throw new AssertionError(model + " için araç yaratılamadı");
		}
		car.setPos(x, GROUND_Y, z);
		car.setYRot(yRot);
		car.setYHeadRot(yRot);
		level.addFreshEntity(car);
		return car;
	}
}
