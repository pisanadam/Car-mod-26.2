package com.pisanadam.realcars.client.input;

import com.pisanadam.realcars.entity.CarEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Araca binildiği anda yapılacak istemci işleri: kamerayı üçüncü şahsa almak
 * ve tuşların ne işe yaradığını yazmak.
 *
 * <p>Kamera araçtayken birinci şahsa düşürülmez. Sebebi görsel: araca binen
 * oyuncunun modeli gizlendiği için birinci şahısta ekranda hiçbir şey kalmaz,
 * üstelik aracın nereye gittiğini görmek yukarıdan bakınca çok daha kolaydır.
 * Oyuncunun araca binmeden önceki bakış açısı saklanır ve inince geri verilir,
 * yani mod kimsenin ayarını kalıcı olarak değiştirmez.
 *
 * <p>Yardım metni sunucudan değil buradan yazılır; çünkü tuşlar yeniden
 * atanabilir ve gerçekte hangi tuşun bağlı olduğunu yalnızca istemci bilir.
 */
public final class CarRideHandler {
	/** Araca binmeden önceki bakış açısı; araçta değilken null. */
	private static @Nullable CameraType cameraBeforeRide;

	private CarRideHandler() {
	}

	public static void tick(final Minecraft client) {
		if (client.player == null) {
			cameraBeforeRide = null;
			return;
		}

		final boolean inCar = client.player.getVehicle() instanceof CarEntity;
		if (!inCar) {
			if (cameraBeforeRide != null) {
				client.options.setCameraType(cameraBeforeRide);
				cameraBeforeRide = null;
			}
			return;
		}

		if (cameraBeforeRide == null) {
			// Bu tick araca binildi.
			cameraBeforeRide = client.options.getCameraType();
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			printControls(client);
			return;
		}
		// Araçtayken birinci şahsa dönülmesin (F5 arkadan/önden geçebilir).
		if (client.options.getCameraType() == CameraType.FIRST_PERSON) {
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
	}

	/** Tuşları, gerçekten atanmış oldukları hâliyle sohbete yazar. */
	private static void printControls(final Minecraft client) {
		if (client.player == null) {
			return;
		}
		final boolean driver = client.player.getVehicle() != null
			&& client.player.getVehicle().getControllingPassenger() == client.player;

		say(client, Component.translatable("message.realcars.help.title"));
		if (!driver) {
			say(client, Component.translatable("message.realcars.help.passenger"));
			say(client, Component.translatable("message.realcars.help.dismount",
				key(client.options.keyShift)));
			return;
		}

		say(client, Component.translatable("message.realcars.help.engine",
			key(CarKeyBindings.ENGINE_TOGGLE)));
		say(client, Component.translatable("message.realcars.help.drive",
			key(client.options.keyUp), key(client.options.keyDown)));
		say(client, Component.translatable("message.realcars.help.steer",
			key(client.options.keyLeft), key(client.options.keyRight)));
		say(client, Component.translatable("message.realcars.help.handbrake",
			key(client.options.keyJump)));
		say(client, Component.translatable("message.realcars.help.horn",
			key(CarKeyBindings.HORN)));
		say(client, Component.translatable("message.realcars.help.dismount",
			key(client.options.keyShift)));
	}

	private static Component key(final KeyMapping mapping) {
		return mapping.getTranslatedKeyMessage();
	}

	private static void say(final Minecraft client, final Component line) {
		// İstemci kaynaklı sistem mesajı: sunucuya gitmez, yalnızca bu oyuncu görür.
		client.gui.hud.getChat().addClientSystemMessage(line);
	}
}
