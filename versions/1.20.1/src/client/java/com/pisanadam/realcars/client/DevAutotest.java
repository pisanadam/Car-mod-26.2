package com.pisanadam.realcars.client;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.CarModel;
import com.pisanadam.realcars.registry.ModEntities;
import com.pisanadam.realcars.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Geliştirme sırasında istemciyi uçtan uca deneyen küçük otomatik test
 * (bu Minecraft sürümünde Fabric'in istemci test API'si yok).
 *
 * <p>Yalnızca {@code -Drealcars.autotest=true} ile açılır: dünyaya girince bütün
 * araçları dizer, fotoğraflar, birine biner (gösterge), modifiye ekranını açar
 * ve her adımda ekran görüntüsü alıp oyunu kapatır. Görüntüler oyun
 * klasörünün {@code screenshots} dizinine düşer.
 */
public final class DevAutotest {
	private static int ticks = -1;
	private static final List<CarEntity> CARS = new ArrayList<>();

	private DevAutotest() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(DevAutotest::tick);
	}

	private static void tick(final Minecraft client) {
		if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
			return;
		}
		ticks++;
		final MinecraftServer server = client.getSingleplayerServer();
		switch (ticks) {
			case 40 -> server.execute(() -> spawnRow(server, client));
			case 240 -> shot(client, "autotest_1_cars.png");
			case 260 -> server.execute(() -> {
				final ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				player.startRiding(CARS.get(1), true);
			});
			case 360 -> shot(client, "autotest_2_driving.png");
			case 380 -> server.execute(() -> {
				final ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				player.stopRiding();
				ModMenus.openModification(player, CARS.get(1));
			});
			case 460 -> shot(client, "autotest_3_tuning.png");
			case 480 -> {
				client.setScreen(null);
				server.execute(() -> {
					final ServerPlayer player = server.getPlayerList().getPlayers().get(0);
					final CarEntity a = CARS.get(7);
					final CarEntity b = CARS.get(8);
					player.teleportTo(player.serverLevel(), (a.getX() + b.getX()) / 2.0, a.getY() + 2.5,
						(a.getZ() + b.getZ()) / 2.0 - 7.0, 0.0F, 15.0F);
				});
			}
			case 560 -> shot(client, "autotest_4_closeup.png");
			case 580 -> {
				RealCars.LOGGER.info("AUTOTEST OK: {} araç", CARS.size());
				client.stop();
			}
			default -> {
			}
		}
	}

	private static void spawnRow(final MinecraftServer server, final Minecraft client) {
		final ServerPlayer player = server.getPlayerList().getPlayers().get(0);
		final ServerLevel level = player.serverLevel();
		level.setDayTime(6000);
		final double baseX = Math.floor(player.getX());
		final double baseZ = Math.floor(player.getZ());
		final double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
			(int) baseX, (int) baseZ + 9);
		int index = 0;
		for (final CarModel model : CarModel.values()) {
			final CarEntity car = ModEntities.create(model, level);
			if (car == null) {
				continue;
			}
			final int column = index % 6;
			final int row = index / 6;
			car.moveTo(baseX - 12.5 + column * 5.0, y, baseZ + 9.0 + row * 7.0, 45.0F, 0.0F);
			level.addFreshEntity(car);
			CARS.add(car);
			index++;
		}
		player.teleportTo(level, baseX + 0.5, y + 6.0, baseZ - 5.0, 0.0F, 25.0F);
	}

	private static void shot(final Minecraft client, final String name) {
		Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(),
			message -> RealCars.LOGGER.info("AUTOTEST ekran görüntüsü: {}", name));
	}
}
