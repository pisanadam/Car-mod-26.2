package com.pisanadam.realcars.net;

import com.pisanadam.realcars.RealCars;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Sürücünün anlık eylemleri (istemci -> sunucu).
 *
 * <p>Hareket için pakete gerek yoktur: aracı süren istemci fiziği kendisi
 * işletir ve konumu vanilla'nın araç paketiyle bildirir. Burada yalnızca
 * sunucunun karar vermesi gereken eylemler taşınır.
 */
public record CarActionPayload(Action action) {
	public static final ResourceLocation ID = RealCars.id("car_action");

	public enum Action {
		TOGGLE_ENGINE,
		HORN;

		private static final Action[] VALUES = values();

		static Action byId(final int id) {
			return VALUES[Math.floorMod(id, VALUES.length)];
		}
	}

	public void write(final FriendlyByteBuf buf) {
		buf.writeVarInt(this.action.ordinal());
	}

	public static CarActionPayload read(final FriendlyByteBuf buf) {
		return new CarActionPayload(Action.byId(buf.readVarInt()));
	}
}
