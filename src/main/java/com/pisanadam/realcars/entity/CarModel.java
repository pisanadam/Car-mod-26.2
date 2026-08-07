package com.pisanadam.realcars.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * Modun içerdiği araçlar. Sabitler {@code tools/spec.py} CARS tablosuyla
 * birebir eşleşir ({@code car_<id>} adının büyük harflisi).
 *
 * <p>Her araç, hem sürüş fiziğini besleyen gerçekçi değerleri (azami hız,
 * 0-100 süresi, kütle, depo) hem de istemci tarafındaki parametrik gövde
 * üretecinin kullandığı ölçüleri ({@link Body}) taşır. Böylece yeni bir araç
 * eklemek yalnızca buraya bir satır ve spec.py'ye bir kayıt eklemek demektir.
 */
public enum CarModel implements StringRepresentable {
	// --- Hatchback / Sedan -------------------------------------------------
	CAR_GOLF_GTI("car_golf_gti", CarClass.HATCHBACK, 250, 6.2F, 1400, 50, 0xC71F24,
		EngineType.ENGINE_I4, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_SPORT,
		new Body(62, 28, 11, 6.0F, 26, 9, 4, 40, 0)),
	CAR_COROLLA("car_corolla", CarClass.SEDAN, 190, 10.4F, 1320, 50, 0xD8DCE2,
		EngineType.ENGINE_I4, TransmissionType.TRANSMISSION_AUTOMATIC, WheelType.WHEEL_STREET,
		new Body(70, 28, 11, 6.0F, 26, 8, 3, 44, 0)),
	CAR_BMW_M3("car_bmw_m3", CarClass.SEDAN, 290, 4.1F, 1730, 59, 0x1B54A8,
		EngineType.ENGINE_I6, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_SPORT,
		new Body(74, 30, 11, 5.0F, 26, 8, 4, 46, 0)),

	// --- Spor --------------------------------------------------------------
	CAR_MUSTANG_GT("car_mustang_gt", CarClass.SPORT, 250, 4.3F, 1740, 61, 0x161A1F,
		EngineType.ENGINE_V8, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_SPORT,
		new Body(76, 31, 11, 5.0F, 24, 7, 6, 46, 0)),
	CAR_GTR("car_gtr", CarClass.SPORT, 315, 2.9F, 1750, 74, 0x6E7682,
		EngineType.ENGINE_V6, TransmissionType.TRANSMISSION_SPORT, WheelType.WHEEL_SPORT,
		new Body(74, 31, 11, 4.5F, 24, 7, 5, 46, 0)),
	CAR_PORSCHE_911("car_porsche_911", CarClass.SPORT, 300, 3.4F, 1520, 64, 0xE4E7EC,
		EngineType.ENGINE_FLAT6, TransmissionType.TRANSMISSION_SPORT, WheelType.WHEEL_SPORT,
		new Body(68, 30, 12, 5.0F, 22, 7, 7, 42, 0)),

	// --- Off-road / SUV / Pickup -------------------------------------------
	CAR_WRANGLER("car_wrangler", CarClass.OFFROAD, 180, 7.6F, 1900, 70, 0x3E6B35,
		EngineType.ENGINE_V6, TransmissionType.TRANSMISSION_AUTOMATIC, WheelType.WHEEL_OFFROAD,
		new Body(62, 30, 14, 10.0F, 30, 12, 2, 38, 0)),
	CAR_HILUX("car_hilux", CarClass.PICKUP, 175, 11.0F, 2050, 80, 0xE8EBEF,
		EngineType.ENGINE_I4, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_OFFROAD,
		new Body(82, 29, 13, 9.0F, 24, 11, -6, 50, 26)),
	CAR_DEFENDER("car_defender", CarClass.SUV, 190, 6.6F, 2320, 90, 0x8B8F7A,
		EngineType.ENGINE_I6, TransmissionType.TRANSMISSION_AUTOMATIC, WheelType.WHEEL_OFFROAD,
		new Body(74, 30, 14, 9.0F, 36, 13, 2, 46, 0)),

	// --- Klasik / Kamyonet --------------------------------------------------
	CAR_BEETLE("car_beetle", CarClass.CLASSIC, 130, 17.5F, 820, 40, 0x62A8D8,
		EngineType.ENGINE_I4, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_STREET,
		new Body(56, 26, 13, 6.0F, 24, 10, 0, 36, 0)),
	CAR_C10("car_c10", CarClass.PICKUP, 160, 9.0F, 1680, 76, 0x2E6B52,
		EngineType.ENGINE_V8, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_STREET,
		new Body(76, 29, 12, 7.0F, 22, 10, -8, 46, 28)),
	CAR_TRANSPORTER("car_transporter", CarClass.VAN, 105, 22.0F, 1180, 42, 0xC45A3A,
		EngineType.ENGINE_I4, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_STREET,
		new Body(70, 30, 20, 6.0F, 40, 14, 4, 44, 0));

	public static final Codec<CarModel> CODEC = StringRepresentable.fromEnum(CarModel::values);
	private static final IntFunction<CarModel> BY_ID =
		ByIdMap.continuous(CarModel::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, CarModel> STREAM_CODEC =
		ByteBufCodecs.idMapper(BY_ID, CarModel::ordinal);

	/**
	 * Gövde ölçüleri, model birimi cinsinden (16 birim = 1 blok).
	 * Z ekseninde eksi yön aracın burnudur.
	 *
	 * @param length      toplam uzunluk
	 * @param width       gövde genişliği
	 * @param bodyHeight  ana gövde kutusunun yüksekliği
	 * @param clearance   gövde altının yerden yüksekliği
	 * @param cabinLength kabin (tavan) uzunluğu
	 * @param cabinHeight kabin yüksekliği
	 * @param cabinZ      kabin merkezinin araç merkezine göre kayıklığı
	 * @param wheelbase   ön ve arka aks arası mesafe
	 * @param bedLength   kamyonet kasası uzunluğu (0 = kasa yok)
	 */
	public record Body(float length, float width, float bodyHeight, float clearance,
					   float cabinLength, float cabinHeight, float cabinZ,
					   float wheelbase, float bedLength) {
		public boolean hasBed() {
			return this.bedLength > 0.0F;
		}
	}

	public enum CarClass {
		HATCHBACK, SEDAN, SPORT, OFFROAD, SUV, PICKUP, CLASSIC, VAN
	}

	private final String name;
	private final CarClass carClass;
	private final int topSpeedKmh;
	private final float accelSeconds;
	private final int massKg;
	private final int fuelCapacity;
	private final int defaultColor;
	private final EngineType defaultEngine;
	private final TransmissionType defaultTransmission;
	private final WheelType defaultWheel;
	private final Body body;

	CarModel(final String name, final CarClass carClass, final int topSpeedKmh, final float accelSeconds,
			 final int massKg, final int fuelCapacity, final int defaultColor,
			 final EngineType defaultEngine, final TransmissionType defaultTransmission,
			 final WheelType defaultWheel, final Body body) {
		this.name = name;
		this.carClass = carClass;
		this.topSpeedKmh = topSpeedKmh;
		this.accelSeconds = accelSeconds;
		this.massKg = massKg;
		this.fuelCapacity = fuelCapacity;
		this.defaultColor = defaultColor;
		this.defaultEngine = defaultEngine;
		this.defaultTransmission = defaultTransmission;
		this.defaultWheel = defaultWheel;
		this.body = body;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	/** Hem item hem entity kaydında kullanılan ad ({@code car_bmw_m3} gibi). */
	public String itemName() {
		return this.name;
	}

	public CarClass carClass() {
		return this.carClass;
	}

	/** Fabrika çıkışı azami hız — takılan motora göre ayrıca ölçeklenir. */
	public int topSpeedKmh() {
		return this.topSpeedKmh;
	}

	/** 0-100 km/s süresi (saniye). */
	public float accelSeconds() {
		return this.accelSeconds;
	}

	public int massKg() {
		return this.massKg;
	}

	public int fuelCapacity() {
		return this.fuelCapacity;
	}

	public int defaultColor() {
		return this.defaultColor;
	}

	public EngineType defaultEngine() {
		return this.defaultEngine;
	}

	public TransmissionType defaultTransmission() {
		return this.defaultTransmission;
	}

	public WheelType defaultWheel() {
		return this.defaultWheel;
	}

	public Body body() {
		return this.body;
	}

	/** Çarpışma kutusu genişliği (blok). */
	public float hitboxWidth() {
		return Math.max(this.body.width, this.body.length * 0.55F) / 16.0F;
	}

	/** Çarpışma kutusu yüksekliği (blok). */
	public float hitboxHeight() {
		return (this.body.clearance + this.body.bodyHeight + this.body.cabinHeight) / 16.0F;
	}

	/** Ağır araçlar yokuşta daha yavaş ama savrulmaya daha dirençlidir. */
	public float massFactor() {
		return this.massKg / 1500.0F;
	}
}
