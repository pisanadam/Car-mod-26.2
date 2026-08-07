package com.pisanadam.realcars.registry;

import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.net.CarActionPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** İstemci -> sunucu paketlerinin kaydı ve işlenmesi. */
public final class ModNetworking {
	private ModNetworking() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(CarActionPayload.TYPE, CarActionPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(CarActionPayload.TYPE, (payload, context) -> {
			// Eylemler yalnızca aracı gerçekten süren oyuncudan kabul edilir.
			if (!(context.player().getVehicle() instanceof CarEntity car)) {
				return;
			}
			if (car.getControllingPassenger() != context.player()) {
				return;
			}
			switch (payload.action()) {
				case TOGGLE_ENGINE -> car.setEngineOn(!car.engineOn());
				case HORN -> car.honk();
				case SHIFT_UP -> car.shift(1);
				case SHIFT_DOWN -> car.shift(-1);
			}
		});
	}
}
