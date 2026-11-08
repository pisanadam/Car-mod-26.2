package com.pisanadam.realcars.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.net.CarActionPayload;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Araç kullanırken geçerli tuşlar. Yön tuşları vanilla hareket tuşlarıdır;
 * burada yalnızca araca özgü eylemler tanımlanır.
 */
public final class CarKeyBindings {
	/**
	 * Tuş ayarları ekranında modun kendi başlığı altında toplansınlar diye
	 * ayrı bir kategori kaydedilir. Başlık {@code key.category.realcars.controls}
	 * anahtarından okunur.
	 */
	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(RealCars.id("controls"));

	public static final KeyMapping ENGINE_TOGGLE = register("key.realcars.engine_toggle", GLFW.GLFW_KEY_G);
	public static final KeyMapping HORN = register("key.realcars.horn", GLFW.GLFW_KEY_H);

	private CarKeyBindings() {
	}

	/**
	 * Sınıfı istemci başlatılırken yükler.
	 *
	 * <p>Tuş atamaları statik alanlarda kaydedildiği için sınıfın ne zaman
	 * yüklendiği önemlidir: Fabric, oyun ayarları hazırlandıktan sonra yapılan
	 * kayıtları reddeder. Sınıf ilk kullanımına (tick) bırakılırsa oyun
	 * "GameOptions has already been initialised" diyip çöker; bu yüzden
	 * onInitializeClient içinden açıkça çağrılır.
	 */
	public static void init() {
	}

	private static KeyMapping register(final String translationKey, final int key) {
		return KeyMappingHelper.registerKeyMapping(
			new KeyMapping(translationKey, InputConstants.Type.KEYSYM, key, CATEGORY));
	}

	/** Basılan tuşları sunucuya bildirir; yalnızca araçtayken çalışır. */
	public static void tick(final Minecraft client) {
		if (client.player == null || !(client.player.getVehicle() instanceof CarEntity)) {
			// Araçtan inince tuş kuyruğu birikmesin.
			drain(ENGINE_TOGGLE);
			drain(HORN);
			return;
		}
		if (consume(ENGINE_TOGGLE)) {
			send(CarActionPayload.Action.TOGGLE_ENGINE);
		}
		if (consume(HORN)) {
			send(CarActionPayload.Action.HORN);
		}
	}

	private static boolean consume(final KeyMapping mapping) {
		boolean pressed = false;
		while (mapping.consumeClick()) {
			pressed = true;
		}
		return pressed;
	}

	private static void drain(final KeyMapping mapping) {
		while (mapping.consumeClick()) {
			// yalnızca kuyruğu boşalt
		}
	}

	private static void send(final CarActionPayload.Action action) {
		ClientPlayNetworking.send(new CarActionPayload(action));
	}
}
