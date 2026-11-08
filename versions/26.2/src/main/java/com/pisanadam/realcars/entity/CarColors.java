package com.pisanadam.realcars.entity;

import net.minecraft.world.item.DyeColor;

/**
 * Sprey boyaların araç üzerinde verdiği renkler.
 *
 * <p>Dizinin sırası {@link DyeColor} sırasıyla aynıdır ve değerler
 * {@code tools/spec.py} içindeki DYE_COLORS tablosuyla birebir eşleşir —
 * böylece sprey kutusunun ikonu ile arabanın boyası aynı tonu verir.
 */
public final class CarColors {
	private static final int[] BY_DYE = {
		0xF0F2F5, // white
		0xE3741E, // orange
		0xC04AC4, // magenta
		0x54A5DA, // light_blue
		0xE8C62E, // yellow
		0x76C72B, // lime
		0xE98DB0, // pink
		0x505459, // gray
		0x9CA1A7, // light_gray
		0x229BA8, // cyan
		0x7B35B5, // purple
		0x2C44B0, // blue
		0x7A5130, // brown
		0x467722, // green
		0xB52724, // red
		0x1A1C1F, // black
	};

	private CarColors() {
	}

	public static int of(final DyeColor dye) {
		return BY_DYE[dye.ordinal()];
	}

	public static int count() {
		return BY_DYE.length;
	}

	public static int byIndex(final int index) {
		return BY_DYE[Math.floorMod(index, BY_DYE.length)];
	}
}
