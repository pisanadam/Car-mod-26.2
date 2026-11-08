package com.pisanadam.realcars.net;

import com.pisanadam.realcars.RealCars;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sürücünün anlık eylemleri (istemci -> sunucu).
 *
 * <p>Hareket için pakete gerek yoktur: aracı süren istemci fiziği kendisi
 * işletir ve konumu vanilla'nın araç paketiyle bildirir. Burada yalnızca
 * sunucunun karar vermesi gereken eylemler taşınır.
 */
public record CarActionPayload(Action action) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CarActionPayload> TYPE =
		new CustomPacketPayload.Type<>(RealCars.id("car_action"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CarActionPayload> STREAM_CODEC =
		StreamCodec.composite(
			ByteBufCodecs.idMapper(Action::byId, Action::ordinal).cast(), CarActionPayload::action,
			CarActionPayload::new);

	public enum Action {
		TOGGLE_ENGINE,
		HORN;

		private static final Action[] VALUES = values();

		static Action byId(final int id) {
			return VALUES[Math.floorMod(id, VALUES.length)];
		}
	}

	@Override
	public CustomPacketPayload.Type<CarActionPayload> type() {
		return TYPE;
	}
}
