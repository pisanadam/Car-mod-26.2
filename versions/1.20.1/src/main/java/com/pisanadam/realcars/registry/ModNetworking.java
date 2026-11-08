package com.pisanadam.realcars.registry;

import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.net.CarActionPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** İstemci -> sunucu paketlerinin kaydı ve işlenmesi. */
public final class ModNetworking {
	private ModNetworking() {
	}

	public static void init() {
		ServerPlayNetworking.registerGlobalReceiver(CarActionPayload.ID, (server, player, handler, buf, sender) -> {
			final CarActionPayload payload = CarActionPayload.read(buf);
			server.execute(() -> {
				// Eylemler yalnızca aracı gerçekten süren oyuncudan kabul edilir.
				if (!(player.getVehicle() instanceof CarEntity car)) {
					return;
				}
				if (car.getControllingPassenger() != player) {
					return;
				}
				switch (payload.action()) {
					case TOGGLE_ENGINE -> car.setEngineOn(!car.engineOn());
					case HORN -> car.honk();
				}
			});
		});
	}
}
