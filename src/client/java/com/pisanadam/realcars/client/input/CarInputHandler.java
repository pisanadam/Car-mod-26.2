package com.pisanadam.realcars.client.input;

import com.pisanadam.realcars.entity.CarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;

/**
 * Yerel oyuncunun tuş durumunu sürdüğü araca aktarır.
 *
 * <p>Sunucu tarafında araç bu bilgiyi {@code ServerPlayer#getLastClientInput()}
 * üzerinden alabiliyor; istemcide böyle bir alan olmadığı için köprüyü bu sınıf
 * kurar. Fizik, dünya tick'inden önce doğru girdiyi görsün diye tick'in
 * başında çağrılır.
 */
public final class CarInputHandler {
	private CarInputHandler() {
	}

	public static void tick(final Minecraft client) {
		if (client.player == null) {
			return;
		}
		if (client.player.getVehicle() instanceof CarEntity car) {
			// Ekran açıkken (örneğin modifiye menüsü) gaz basılı kalmasın.
			final Input input = client.gui.screen() == null ? client.player.input.keyPresses : Input.EMPTY;
			car.setClientInput(input);
		}
	}
}
