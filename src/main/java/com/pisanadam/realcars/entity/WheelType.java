package com.pisanadam.realcars.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * Tekerlek tipleri. {@code tools/spec.py} WHEELS tablosuyla eşleşir.
 *
 * <p>{@code roadGrip} sert zeminde, {@code offroadGrip} toprak/kum/karda geçerli
 * tutuş çarpanıdır; ikisi arasındaki fark modifiyenin sürüşe etkisini belirler.
 */
public enum WheelType implements StringRepresentable {
	WHEEL_STREET("wheel_street", 1.00F, 0.55F, 7.0F),
	WHEEL_SPORT("wheel_sport", 1.18F, 0.42F, 7.0F),
	WHEEL_OFFROAD("wheel_offroad", 0.82F, 1.00F, 8.5F),
	WHEEL_CHROME("wheel_chrome", 1.05F, 0.50F, 7.5F);

	public static final Codec<WheelType> CODEC = StringRepresentable.fromEnum(WheelType::values);
	private static final IntFunction<WheelType> BY_ID =
		ByIdMap.continuous(WheelType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, WheelType> STREAM_CODEC =
		ByteBufCodecs.idMapper(BY_ID, WheelType::ordinal);

	private final String name;
	private final float roadGrip;
	private final float offroadGrip;
	private final float radius;

	WheelType(final String name, final float roadGrip, final float offroadGrip, final float radius) {
		this.name = name;
		this.roadGrip = roadGrip;
		this.offroadGrip = offroadGrip;
		this.radius = radius;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public String itemName() {
		return this.name;
	}

	public float roadGrip() {
		return this.roadGrip;
	}

	public float offroadGrip() {
		return this.offroadGrip;
	}

	/** Model birimi cinsinden tekerlek yarıçapı (16 birim = 1 blok). */
	public float radius() {
		return this.radius;
	}

	/** Gövde atlasındaki jant bölgesinin adı. */
	public String rimRegion() {
		return switch (this) {
			case WHEEL_STREET -> "rim_street";
			case WHEEL_SPORT -> "rim_sport";
			case WHEEL_OFFROAD -> "rim_offroad";
			case WHEEL_CHROME -> "rim_chrome";
		};
	}
}
