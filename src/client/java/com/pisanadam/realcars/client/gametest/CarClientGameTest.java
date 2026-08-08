package com.pisanadam.realcars.client.gametest;

import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.CarModel;
import com.pisanadam.realcars.entity.SpoilerType;
import com.pisanadam.realcars.entity.WheelType;
import com.pisanadam.realcars.registry.ModEntities;
import com.pisanadam.realcars.client.input.CarKeyBindings;
import com.pisanadam.realcars.client.sound.CarSoundManager;
import com.pisanadam.realcars.registry.ModMenus;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

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

	/** Araca binip sürüşü, göstergeyi, süspansiyonu ve modifiye ekranını sınar. */
	private static void rideAndScreenshot(final ClientGameTestContext context,
										  final TestSingleplayerContext singleplayer) {
		final var server = singleplayer.getServer();
		server.runCommand("kill @e[type=!player]");
		// Hızlanmaya yer açan uzun bir pist.
		server.runCommand("forceload add -96 -16 96 16");
		server.runCommand("fill -96 " + (GROUND_Y - 1) + " -8 96 " + (GROUND_Y - 1)
			+ " 8 realcars:asphalt");
		server.runCommand("fill -96 " + GROUND_Y + " -8 96 " + (GROUND_Y + 6) + " 8 air");
		// Elle vites değiştirmenin kalkmış olması burada sınanır: bu araç
		// manuel şanzımanla geliyordu ve eskiden birinci vitesde takılı kalırdı.
		server.runOnServer(unused -> spawn(singleplayer, CarModel.CAR_BMW_M3, -88.0D, 0.0D, -90.0F));
		context.waitTicks(6);

		mountByRightClick(context, singleplayer, -88);
		checkCameraLockedToThirdPerson(context);

		// Motoru da oyuncunun bastığı tuşla çalıştır; sunucudan doğrudan
		// setEngineOn çağırmak tuş yolunu sınamadan bırakırdı.
		context.getInput().pressKey(CarKeyBindings.ENGINE_TOGGLE);
		context.waitTicks(10);
		if (load(context, car -> car.engineOn() ? 1.0F : 0.0F) < 0.5F) {
			throw new AssertionError("Motor tuşuyla motor çalışmadı");
		}
		context.waitTicks(15);
		context.takeScreenshot("92-gosterge");

		checkReverse(context);
		final float speed = accelerate(context);
		context.takeScreenshot("93-surus");
		checkWheelsRoll(context);
		checkSuspension(context);
		checkGaugeStopsAtWall(context, singleplayer);

		if (speed < 60.0F) {
			throw new AssertionError("Otomatik şanzımanla araç 4 saniyede yeterince "
				+ "hızlanmadı: " + speed + " km/s");
		}

		checkSecondPassenger(context, singleplayer);

		// Modifiye ekranı
		server.runOnServer(unused -> {
			final var player = singleplayer.getConnection().getServerPlayer();
			if (player.getVehicle() instanceof CarEntity car) {
				ModMenus.openModification(player, car);
			}
		});
		context.waitTicks(30);
		context.takeScreenshot("96-modifiye-ekrani");

		// İniş: kapı sesi hem binerken hem inerken çalınır, iki yol da gerçek
		// oyunda yürütülsün. Kamera da binmeden önceki hâline dönmeli.
		context.setScreen(() -> null);
		server.runOnServer(unused -> singleplayer.getConnection().getServerPlayer().stopRiding());
		context.waitTicks(10);
		if (!context.computeOnClient(client ->
			client.player == null || client.player.getVehicle() == null)) {
			throw new AssertionError("Oyuncu araçtan inemedi");
		}
		if (context.computeOnClient(client -> client.options.getCameraType()) != CameraType.FIRST_PERSON) {
			throw new AssertionError("İnince kamera eski hâline dönmedi");
		}
	}

	/**
	 * Araca binince kameranın üçüncü şahsa geçtiğini ve araçtayken birinci
	 * şahsa düşürülemediğini sınar.
	 */
	private static void checkCameraLockedToThirdPerson(final ClientGameTestContext context) {
		if (context.computeOnClient(client -> client.options.getCameraType()) != CameraType.THIRD_PERSON_BACK) {
			throw new AssertionError("Araca binince kamera üçüncü şahsa geçmedi");
		}
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		context.waitTicks(3);
		if (context.computeOnClient(client -> client.options.getCameraType()) == CameraType.FIRST_PERSON) {
			throw new AssertionError("Araçtayken birinci şahsa geçilebiliyor");
		}
	}

	/** Geri tuşunun dururken aracı gerçekten geri götürdüğünü sınar. */
	private static void checkReverse(final ClientGameTestContext context) {
		context.getInput().holdKey(options -> options.keyDown);
		context.waitTicks(25);
		final float reverse = load(context, CarEntity::speedKmh);
		context.getInput().releaseKey(options -> options.keyDown);
		context.waitTicks(20);
		if (reverse > -2.0F) {
			throw new AssertionError("Geri tuşuna basılınca araç geri gitmedi: " + reverse + " km/s");
		}
	}

	/** Gaza basıp hızlanma izini yazar ve ulaşılan hızı döndürür. */
	private static float accelerate(final ClientGameTestContext context) {
		context.getInput().holdKey(options -> options.keyUp);
		final StringBuilder trace = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			context.waitTicks(20);
			trace.append(String.format(" %.0fkm/s(v%d)",
				load(context, CarEntity::speedKmh),
				(int) load(context, car -> (float) car.gear())));
		}
		System.out.println("[RealCars] hızlanma izi:" + trace);
		return Math.abs(load(context, CarEntity::speedKmh));
	}

	/**
	 * Göstergenin, aracın gerçekten gitmediği bir hızı yazmadığını sınar.
	 *
	 * <p>Aracın önüne duvar örülür ve gaz basılı tutulur. Araç duvara dayanıp
	 * ilerleyemediğine göre gösterge de sıfıra yakın olmalıdır; eskiden
	 * istenen hızı yazdığı için duvara dayalı araç doksan km/s gösteriyordu.
	 */
	private static void checkGaugeStopsAtWall(final ClientGameTestContext context,
											  final TestSingleplayerContext singleplayer) {
		// Önce el freniyle durulur. Hızlı giden araca duvar örmek yarış durumu
		// yaratır: duvar örülene kadar araç oradan çoktan geçmiş olur.
		context.getInput().releaseKey(options -> options.keyUp);
		context.getInput().holdKey(options -> options.keyJump);
		context.waitTicks(45);
		context.getInput().releaseKey(options -> options.keyJump);
		context.waitTicks(10);

		// Viraj testinden sonra araç dümdüz gitmiyor olabilir; duvarı önüne
		// örebilmek için burnu tekrar +X yönüne çevrilir. Yön istemcide
		// belirlendiği için düzeltme de istemcide yapılmalı.
		context.runOnClient(client -> {
			if (client.player != null && client.player.getVehicle() instanceof CarEntity car) {
				car.setYRot(-90.0F);
				car.setYHeadRot(-90.0F);
			}
		});
		context.waitTicks(5);

		final int wallX = Math.round(load(context, car -> (float) car.getX())) + 8;
		final int wallZ = Math.round(load(context, car -> (float) car.getZ()));
		singleplayer.getServer().runCommand("fill " + wallX + " " + GROUND_Y + " " + (wallZ - 10)
			+ " " + (wallX + 2) + " " + (GROUND_Y + 4) + " " + (wallZ + 10) + " stone");
		context.waitTicks(5);

		// Duvar gerçekten örüldü mü? Örülmediyse testin geri kalanı yanıltıcı olur.
		final boolean wallBuilt = singleplayer.getServer().computeOnServer(server ->
			!singleplayer.getConnection().getServerLevel()
				.getBlockState(new BlockPos(wallX, GROUND_Y + 1, wallZ)).isAir());
		if (!wallBuilt) {
			throw new AssertionError("Duvar örülemedi: " + wallX + "," + wallZ);
		}

		// Sekiz blokluk mesafeyi kapatıp duvara dayanana kadar gaz basılı.
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(60);
		final float travelled = travelInTicks(context, 20);
		final float gauge = Math.abs(load(context, CarEntity::speedKmh));
		context.getInput().releaseKey(options -> options.keyUp);
		System.out.printf("[RealCars] duvara dayalı araç: gösterge %.1f km/s, "
			+ "20 tickte gidilen %.2f blok%n", gauge, travelled);

		if (travelled > 0.8F) {
			throw new AssertionError("Araç duvarda durmadı: " + travelled + " blok");
		}
		if (gauge > 8.0F) {
			throw new AssertionError("Araç duvara dayalıyken gösterge " + gauge
				+ " km/s yazıyor — gitmediği bir hızı gösteriyor");
		}
	}

	/** Verilen tick sayısında aracın gerçekten aldığı yatay yol (blok). */
	private static float travelInTicks(final ClientGameTestContext context, final int ticks) {
		final float x0 = load(context, car -> (float) car.getX());
		final float z0 = load(context, car -> (float) car.getZ());
		context.waitTicks(ticks);
		final float dx = load(context, car -> (float) car.getX()) - x0;
		final float dz = load(context, car -> (float) car.getZ()) - z0;
		return (float) Math.sqrt(dx * dx + dz * dz);
	}

	/** Virajda yana yatma ve frende burun dalmasını sınar, kare de alır. */
	private static void checkSuspension(final ClientGameTestContext context) {
		context.getInput().holdKey(options -> options.keyLeft);
		context.waitTicks(14);
		context.takeScreenshot("94-virajda-yatma");
		final float roll = load(context, CarEntity::lateralLoad);
		context.getInput().releaseKey(options -> options.keyLeft);

		final int squealsBefore = context.computeOnClient(client -> CarSoundManager.brakeSoundCount());
		context.getInput().releaseKey(options -> options.keyUp);
		context.getInput().holdKey(options -> options.keyDown);
		context.waitTicks(4);
		context.takeScreenshot("95-frende-burun-dalmasi");
		final float dive = load(context, CarEntity::longitudinalLoad);
		final boolean braking = load(context, car -> car.braking() ? 1.0F : 0.0F) > 0.5F;
		context.waitTicks(20);
		final int squeals = context.computeOnClient(client -> CarSoundManager.brakeSoundCount())
			- squealsBefore;
		context.getInput().releaseKey(options -> options.keyDown);
		context.getInput().holdKey(options -> options.keyUp);

		if (Math.abs(roll) < 0.05F) {
			throw new AssertionError("Virajda gövdeye yanal yük binmedi: " + roll);
		}
		if (dive > -0.05F || !braking) {
			throw new AssertionError("Frende burun dalmadı: " + dive + ", fren=" + braking);
		}
		if (squeals == 0) {
			throw new AssertionError("Fren sesi hiç çalınmadı");
		}
	}

	/**
	 * İkinci bir yolcunun araca binebildiğini, ama aracı süremediğini sınar.
	 *
	 * <p>Sunucuda ikinci bir canlı bindirilir; direksiyonun ilk binende kalması
	 * gerekir, yoksa iki kişi aynı anda aracı sürmeye çalışırdı.
	 */
	private static void checkSecondPassenger(final ClientGameTestContext context,
											 final TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(unused -> {
			final var level = singleplayer.getConnection().getServerLevel();
			final var player = singleplayer.getConnection().getServerPlayer();
			if (!(player.getVehicle() instanceof CarEntity car)) {
				throw new AssertionError("İkinci yolcu sınanamadı: oyuncu araçta değil");
			}
			final var passenger = net.minecraft.world.entity.EntityTypes.VILLAGER
				.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (passenger == null) {
				throw new AssertionError("Yolcu yaratılamadı");
			}
			passenger.setPos(car.getX(), car.getY() + 1.0D, car.getZ());
			level.addFreshEntity(passenger);
			if (!passenger.startRiding(car)) {
				throw new AssertionError("İkinci yolcu araca binemedi");
			}
			if (car.getPassengers().size() != 2) {
				throw new AssertionError("Araçta beklenen 2 yolcu, bulunan "
					+ car.getPassengers().size());
			}
			if (car.getControllingPassenger() != player) {
				throw new AssertionError("Direksiyon ilk binende kalmadı");
			}
		});
		context.waitTicks(10);
	}

	/**
	 * Oyuncunun araca <em>gerçekten oyundaki gibi</em> binmesini sınar: araca
	 * nişan alıp sağ tıklar.
	 *
	 * <p>Bu yol sunucudan {@code startRiding} çağırmakla aynı şey değildir.
	 * Sağ tıklama önce nişan ışınının araca değmesini gerektirir; varlık
	 * "seçilebilir" değilse ışın onu görmez ve araca hiç tıklanamaz. Bu yüzden
	 * önce ışının araca değdiği ayrıca doğrulanır — hata orada olursa mesaj
	 * doğrudan sebebi söyler.
	 */
	private static void mountByRightClick(final ClientGameTestContext context,
										  final TestSingleplayerContext singleplayer, final int carX) {
		singleplayer.getServer().runCommand("tp @a " + carX + " " + (GROUND_Y + 1) + " -3 0 20");
		context.waitTicks(10);
		context.getInput().lookAt(new BlockPos(carX, GROUND_Y, 0));
		context.waitTicks(5);

		final boolean aimed = context.computeOnClient(client ->
			client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof CarEntity);
		if (!aimed) {
			throw new AssertionError("Nişan ışını araca değmiyor: araç seçilebilir "
				+ "(isPickable) değilse ona sağ tıklamak da vurmak da mümkün olmaz");
		}

		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(10);
		final boolean riding = context.computeOnClient(client ->
			client.player != null && client.player.getVehicle() instanceof CarEntity);
		if (!riding) {
			throw new AssertionError("Araca sağ tıklandı ama binilemedi");
		}
	}

	/**
	 * Tekerleğin dönme açısının kat edilen yola uyduğunu doğrular.
	 *
	 * <p>Bir tur atan tekerlek {@code 2*pi*r} kadar yol alır; dolayısıyla
	 * {@code N} tick'te ilerleyen açı, o sürede alınan yolun tekerlek çevresine
	 * bölümünün 360 katı olmalıdır. Ekran görüntüsünden "dönüyor mu" sorusu
	 * güvenilir okunmadığı için doğrulama sayıyla yapılır.
	 */
	private static void checkWheelsRoll(final ClientGameTestContext context) {
		// Gaz kesilir: patinajda lastik yoldan hızlı döner, o yüzden ölçüm
		// aracın serbest yuvarlandığı bir pencerede yapılır.
		context.getInput().releaseKey(options -> options.keyUp);
		context.waitTicks(10);

		final float angleBefore = load(context, car -> car.wheelAngle(1.0F));
		final float x0 = load(context, car -> (float) car.getX());
		final float z0 = load(context, car -> (float) car.getZ());
		final int ticks = 20;
		context.waitTicks(ticks);
		final boolean slipping = load(context, car -> car.wheelSlipping() ? 1.0F : 0.0F) > 0.5F;
		context.getInput().holdKey(options -> options.keyUp);
		// Açı, kayan nokta hassasiyeti için +-3600 derecede sarmalanır. Araç
		// ileri gittiğinden fark pozitif olmalı; eksi çıktıysa ölçüm penceresi
		// bir sarmalın üstüne denk gelmiştir. Pencere 3600 dereceden kısa
		// tutulduğu için tek bir sarmal eklemek yeterlidir.
		float turned = load(context, car -> car.wheelAngle(1.0F)) - angleBefore;
		if (turned < 0.0F) {
			turned += 3600.0F;
		}

		final float radiusBlocks = load(context, car -> car.wheelType().radius()) / 16.0F;
		final double dx = load(context, car -> (float) car.getX()) - x0;
		final double dz = load(context, car -> (float) car.getZ()) - z0;
		final double travelled = Math.sqrt(dx * dx + dz * dz);
		final float expected = (float) (travelled / (2.0D * Math.PI * radiusBlocks) * 360.0D);
		System.out.printf("[RealCars] araç %.1f blok gitti, tekerlek %.0f derece döndü, "
			+ "kaymadan yuvarlansa %.0f derece dönerdi%n", travelled, turned, expected);

		if (turned < 90.0F) {
			throw new AssertionError("Tekerlek dönmüyor: " + turned + " derece");
		}
		if (slipping) {
			throw new AssertionError("Gaz kesikken araç hâlâ patinajda sayılıyor");
		}
		// Hızlanma sürdüğü için birebir tutması beklenmez; kabaca uyması yeter.
		if (turned < expected * 0.9F || turned > expected * 1.1F) {
			throw new AssertionError("Tekerlek dönüşü kat edilen yolla uyuşmuyor: " + turned
				+ " derece, beklenen ~" + expected + " (" + travelled + " blok)");
		}
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
