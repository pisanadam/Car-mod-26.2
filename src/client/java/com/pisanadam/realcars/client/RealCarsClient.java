package com.pisanadam.realcars.client;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.client.hud.SpeedometerHud;
import com.pisanadam.realcars.client.input.CarInputHandler;
import com.pisanadam.realcars.client.input.CarKeyBindings;
import com.pisanadam.realcars.client.input.CarRideHandler;
import com.pisanadam.realcars.client.render.CarEntityRenderer;
import com.pisanadam.realcars.client.render.CarMeshFactory;
import com.pisanadam.realcars.client.screen.CarModificationScreen;
import com.pisanadam.realcars.client.sound.CarSoundManager;
import com.pisanadam.realcars.entity.CarModel;
import com.pisanadam.realcars.registry.ModEntities;
import com.pisanadam.realcars.registry.ModMenus;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class RealCarsClient implements ClientModInitializer {
	private static final Map<CarModel, ModelLayerLocation> LAYERS = new EnumMap<>(CarModel.class);

	@Override
	public void onInitializeClient() {
		registerCarRenderers();

		// Tuş atamaları oyun ayarları hazırlanmadan önce kaydedilmek zorunda,
		// yani sınıf tam burada yüklenmeli.
		CarKeyBindings.init();

		MenuScreens.register(ModMenus.CAR_MODIFICATION, CarModificationScreen::new);

		// Gösterge listenin en sonuna, yani her şeyin üstüne eklenir. Vanilla
		// bir öğeye iliştirmek cazip görünüyor ama o öğe gizlendiğinde (örneğin
		// yaratıcı modda deneyim çubuğu) gösterge de kayboluyor.
		HudElementRegistry.addLast(SpeedometerHud.ID, new SpeedometerHud());

		// Girdi köprüsü tick'in başında çalışmalı ki fizik güncel tuşları görsün.
		ClientTickEvents.START_CLIENT_TICK.register(CarInputHandler::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			CarKeyBindings.tick(client);
			CarRideHandler.tick(client);
			CarSoundManager.tick(client);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CarSoundManager.clear());

		RealCars.LOGGER.info("RealCars istemci tarafı hazır.");
	}

	private static void registerCarRenderers() {
		for (final CarModel model : CarModel.values()) {
			final ModelLayerLocation layer =
				new ModelLayerLocation(RealCars.id("car/" + model.itemName()), "main");
			LAYERS.put(model, layer);
			ModelLayerRegistry.registerModelLayer(layer, () -> CarMeshFactory.create(model));
			EntityRendererRegistry.register(ModEntities.type(model),
				context -> new CarEntityRenderer(context, layer, model));
		}
	}

	public static ModelLayerLocation layerFor(final CarModel model) {
		return LAYERS.get(model);
	}
}
