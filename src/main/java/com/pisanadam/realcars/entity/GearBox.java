package com.pisanadam.realcars.entity;

import net.minecraft.util.Mth;

/**
 * Vites ve devir hesapları.
 *
 * <p>Her vitesin kapsadığı bir hız bandı vardır; araç o bandın neresindeyse
 * devir de rölanti ile kırmızı bölge arasında oraya düşer. Tork eğrisi orta
 * devirlerde tepe yaptığı için kırmızı bölgede ısrar etmek ivmeyi düşürür —
 * doğru anda vites değiştirmek gerçekten hızlandırır.
 *
 * <p>Vites 0 boşu, -1 geri vitesi temsil eder.
 */
public final class GearBox {
	public static final int REVERSE = -1;
	public static final int NEUTRAL = 0;

	public static final int IDLE_RPM = 800;
	/** Geri viteste ulaşılabilecek azami hız (km/s). */
	public static final float REVERSE_TOP_SPEED = 28.0F;

	private GearBox() {
	}

	/** {@code gear} vitesinin üst hız sınırı (km/s). */
	public static float gearTopSpeed(final int gear, final int gearCount, final float topSpeedKmh) {
		if (gear <= NEUTRAL) {
			return gear == REVERSE ? REVERSE_TOP_SPEED : 0.0F;
		}
		final int g = Math.min(gear, gearCount);
		// Alt vitesler birbirine yakın, üst vitesler geniş aralıklı olsun diye
		// üstel bir dağılım kullanılır.
		return topSpeedKmh * (float) Math.pow((double) g / gearCount, 0.8);
	}

	/** {@code gear} vitesinin alt hız sınırı — bir önceki vitesle bindirmelidir. */
	public static float gearBottomSpeed(final int gear, final int gearCount, final float topSpeedKmh) {
		if (gear <= 1) {
			return 0.0F;
		}
		return gearTopSpeed(gear - 1, gearCount, topSpeedKmh) * 0.78F;
	}

	/** Verilen hız ve viteste motor devri. */
	public static int rpm(final float speedKmh, final int gear, final int gearCount,
						  final float topSpeedKmh, final int redlineRpm) {
		if (gear == NEUTRAL) {
			return IDLE_RPM;
		}
		if (gear == REVERSE) {
			final float f = Mth.clamp(Math.abs(speedKmh) / REVERSE_TOP_SPEED, 0.0F, 1.0F);
			return (int) Mth.lerp(f, IDLE_RPM, redlineRpm * 0.8F);
		}
		final float bottom = gearBottomSpeed(gear, gearCount, topSpeedKmh);
		final float top = gearTopSpeed(gear, gearCount, topSpeedKmh);
		final float f = Mth.clamp((Math.abs(speedKmh) - bottom) / Math.max(1.0F, top - bottom), 0.0F, 1.15F);
		return (int) Mth.clamp(Mth.lerp(f, IDLE_RPM, redlineRpm), IDLE_RPM, redlineRpm * 1.05F);
	}

	/**
	 * Motorun o devirdeki bağıl torku (0.35 - 1.0). Tepe nokta kırmızı bölgenin
	 * biraz altındadır; hem çok düşük hem çok yüksek devirde güç düşer.
	 */
	public static float torqueFactor(final int rpm, final int redlineRpm) {
		final float f = (float) rpm / redlineRpm;
		return Mth.clamp(1.0F - 1.7F * (f - 0.72F) * (f - 0.72F), 0.35F, 1.0F);
	}

	/**
	 * Vites oranından gelen çekiş katsayısı — birinci vites en çok, son vites
	 * en az kuvvet uygular.
	 */
	public static float pullFactor(final int gear, final int gearCount) {
		if (gear == REVERSE) {
			return 1.35F;
		}
		if (gear == NEUTRAL) {
			return 0.0F;
		}
		return (float) Math.pow((double) gearCount / Math.min(gear, gearCount), 0.45);
	}

	/** Otomatik şanzımanın bu devirde seçeceği vites. */
	public static int autoShift(final int currentGear, final int rpm, final int gearCount,
								final int redlineRpm, final boolean throttle) {
		if (currentGear <= NEUTRAL) {
			return currentGear;
		}
		if (throttle && rpm > redlineRpm * 0.90F && currentGear < gearCount) {
			return currentGear + 1;
		}
		if (rpm < redlineRpm * 0.32F && currentGear > 1) {
			return currentGear - 1;
		}
		return currentGear;
	}

	/**
	 * Manuel şanzımanda yanlış vitesin cezası: devir bandın çok dışındaysa
	 * motor boğulur ve çekiş düşer.
	 */
	public static float mismatchPenalty(final int rpm, final int redlineRpm) {
		if (rpm < IDLE_RPM * 1.4F) {
			return 0.45F;
		}
		if (rpm > redlineRpm) {
			return 0.30F;
		}
		return 1.0F;
	}
}
