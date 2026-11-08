package com.pisanadam.realcars.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * Rüzgarlık (spoiler) tipleri. {@code downforce} viraj tutuşuna eklenen bonustur.
 */
public enum SpoilerType implements StringRepresentable {
	SPOILER_NONE("spoiler_none", 0.00F),
	SPOILER_LIP("spoiler_lip", 0.04F),
	SPOILER_DUCKTAIL("spoiler_ducktail", 0.07F),
	SPOILER_GT("spoiler_gt", 0.12F);

	public static final Codec<SpoilerType> CODEC = StringRepresentable.fromEnum(SpoilerType::values);
	private static final IntFunction<SpoilerType> BY_ID =
		ByIdMap.continuous(SpoilerType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, SpoilerType> STREAM_CODEC =
		ByteBufCodecs.idMapper(BY_ID, SpoilerType::ordinal);

	private final String name;
	private final float downforce;

	SpoilerType(final String name, final float downforce) {
		this.name = name;
		this.downforce = downforce;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public String itemName() {
		return this.name;
	}

	/** Rüzgarlıksız seçenek bir item değildir, sadece "takılı değil" durumudur. */
	public boolean hasItem() {
		return this != SPOILER_NONE;
	}

	public float downforce() {
		return this.downforce;
	}
}
