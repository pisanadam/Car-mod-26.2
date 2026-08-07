package com.pisanadam.realcars.registry;

import com.pisanadam.realcars.RealCars;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Sürüş yüzeyini belirleyen blok etiketleri (veriler {@code data/realcars/tags/block}). */
public final class ModTags {
	/** Toz kaldıran yüzeyler: kum, toprak, çakıl, çamur... */
	public static final TagKey<Block> DUSTY = blockTag("dusty");
	/** Asfalt, taş, beton gibi tutuşu yüksek yüzeyler. */
	public static final TagKey<Block> HIGH_GRIP = blockTag("high_grip");
	/** Buz, kar, balçık gibi kaygan yüzeyler. */
	public static final TagKey<Block> LOW_GRIP = blockTag("low_grip");

	private ModTags() {
	}

	private static TagKey<Block> blockTag(final String path) {
		return TagKey.create(Registries.BLOCK, RealCars.id(path));
	}
}
