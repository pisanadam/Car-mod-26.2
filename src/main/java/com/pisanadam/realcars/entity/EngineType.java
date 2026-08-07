package com.pisanadam.realcars.entity;

import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;

/**
 * Motor tipleri. Sabitler {@code tools/spec.py} içindeki ENGINES tablosuyla
 * birebir eşleşir; {@code tools/validate_assets.py} bunu doğrular.
 */
public enum EngineType implements StringRepresentable {
	ENGINE_I4("engine_i4", 4, 1.00F, 6800),
	ENGINE_I6("engine_i6", 6, 1.30F, 7200),
	ENGINE_V6("engine_v6", 6, 1.25F, 7000),
	ENGINE_V8("engine_v8", 8, 1.55F, 6600),
	ENGINE_FLAT6("engine_flat6", 6, 1.40F, 7600);

	public static final Codec<EngineType> CODEC = StringRepresentable.fromEnum(EngineType::values);
	private static final IntFunction<EngineType> BY_ID =
		ByIdMap.continuous(EngineType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, EngineType> STREAM_CODEC =
		ByteBufCodecs.idMapper(BY_ID, EngineType::ordinal);

	private final String name;
	private final int cylinders;
	private final float powerFactor;
	private final int redlineRpm;

	EngineType(final String name, final int cylinders, final float powerFactor, final int redlineRpm) {
		this.name = name;
		this.cylinders = cylinders;
		this.powerFactor = powerFactor;
		this.redlineRpm = redlineRpm;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	/** Kayıtlı item / ses adı ile aynı olan kısa ad. */
	public String itemName() {
		return this.name;
	}

	public int cylinders() {
		return this.cylinders;
	}

	/** Motorun güç çarpanı — ivmeyi ve azami hızı ölçekler. */
	public float powerFactor() {
		return this.powerFactor;
	}

	/** Kırmızı bölgenin başladığı devir. */
	public int redlineRpm() {
		return this.redlineRpm;
	}

	/** Bu motorun ses döngüsünün adı ({@code sounds.json} ile eşleşir). */
	public String soundName() {
		return "engine_loop_" + switch (this) {
			case ENGINE_I4 -> "i4";
			case ENGINE_I6 -> "i6";
			case ENGINE_V6 -> "v6";
			case ENGINE_V8 -> "v8";
			case ENGINE_FLAT6 -> "flat6";
		};
	}
}
