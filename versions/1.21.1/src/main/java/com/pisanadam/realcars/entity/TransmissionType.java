package com.pisanadam.realcars.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * Şanzıman tipleri. {@code tools/spec.py} TRANSMISSIONS tablosuyla eşleşir.
 */
public enum TransmissionType implements StringRepresentable {
	TRANSMISSION_MANUAL("transmission_manual", 6, false, 1.00F),
	TRANSMISSION_AUTOMATIC("transmission_automatic", 6, true, 0.90F),
	TRANSMISSION_SPORT("transmission_sport", 7, true, 1.15F);

	public static final Codec<TransmissionType> CODEC = StringRepresentable.fromEnum(TransmissionType::values);
	private static final IntFunction<TransmissionType> BY_ID =
		ByIdMap.continuous(TransmissionType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, TransmissionType> STREAM_CODEC =
		ByteBufCodecs.idMapper(BY_ID, TransmissionType::ordinal);

	private final String name;
	private final int gearCount;
	private final boolean automatic;
	private final float shiftFactor;

	TransmissionType(final String name, final int gearCount, final boolean automatic, final float shiftFactor) {
		this.name = name;
		this.gearCount = gearCount;
		this.automatic = automatic;
		this.shiftFactor = shiftFactor;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public String itemName() {
		return this.name;
	}

	/** İleri vites sayısı (geri vites hariç). */
	public int gearCount() {
		return this.gearCount;
	}

	/** Otomatik şanzımanlar vitesi kendileri değiştirir. */
	public boolean automatic() {
		return this.automatic;
	}

	/** Vites değişiminde kaybedilen gücü belirler — yüksek olan daha hızlı geçer. */
	public float shiftFactor() {
		return this.shiftFactor;
	}
}
