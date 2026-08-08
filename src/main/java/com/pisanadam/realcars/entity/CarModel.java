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
 * üretecinin kullandığı yan profilini ({@link Body}) taşır. Böylece yeni bir
 * araç eklemek yalnızca buraya bir satır ve spec.py'ye bir kayıt eklemek
 * demektir.
 */
public enum CarModel implements StringRepresentable {
	// --- Hatchback / Sedan -------------------------------------------------
	CAR_GOLF_GTI("car_golf_gti", CarClass.HATCHBACK, 250, 6.2F, 1400, 50, 0xC71F24,
		EngineType.ENGINE_I4, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_SPORT,
		Body.of(Silhouette.HATCHBACK, 68.0F, 28.0F)
			.stance(5.5F, 42.0F, 0.0F)
			.nose(13.0F).cowl(-13.0F, 16.0F)
			.roof(-1.0F, 23.5F, 25.0F)
			.tail(31.0F, 17.0F, 16.0F)
			.cabin(0.80F, 1.2F)
			.build()),
	CAR_COROLLA("car_corolla", CarClass.SEDAN, 190, 10.4F, 1320, 50, 0xD8DCE2,
		EngineType.ENGINE_I4, TransmissionType.TRANSMISSION_AUTOMATIC, WheelType.WHEEL_STREET,
		Body.of(Silhouette.NOTCHBACK, 74.0F, 28.5F)
			.stance(5.5F, 43.0F, 1.0F)
			.nose(13.5F).cowl(-13.0F, 16.0F)
			.roof(-1.0F, 23.0F, 15.0F)
			.tail(26.0F, 16.5F, 16.0F)
			.cabin(0.80F, 1.1F)
			.build()),
	CAR_BMW_M3("car_bmw_m3", CarClass.SEDAN, 290, 4.1F, 1730, 59, 0x1B54A8,
		EngineType.ENGINE_I6, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_SPORT,
		Body.of(Silhouette.NOTCHBACK, 77.0F, 30.5F)
			.stance(5.0F, 46.0F, 0.0F)
			.nose(14.0F).cowl(-11.0F, 16.5F)
			.roof(1.0F, 23.0F, 16.0F)
			.tail(27.0F, 17.0F, 16.5F)
			.cabin(0.78F, 1.6F)
			.build()),

	// --- Spor --------------------------------------------------------------
	CAR_MUSTANG_GT("car_mustang_gt", CarClass.SPORT, 250, 4.3F, 1740, 61, 0x161A1F,
		EngineType.ENGINE_V8, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_SPORT,
		Body.of(Silhouette.FASTBACK, 77.0F, 31.0F)
			.stance(5.0F, 44.0F, 2.0F)
			.nose(14.0F).cowl(-8.0F, 16.0F)
			.roof(4.0F, 22.0F, 13.0F)
			.tail(28.0F, 16.5F, 16.0F)
			.cabin(0.74F, 2.0F)
			.build()),
	CAR_GTR("car_gtr", CarClass.SPORT, 315, 2.9F, 1750, 74, 0x6E7682,
		EngineType.ENGINE_V6, TransmissionType.TRANSMISSION_SPORT, WheelType.WHEEL_SPORT,
		Body.of(Silhouette.FASTBACK, 75.0F, 30.5F)
			.stance(4.5F, 44.5F, 1.0F)
			.nose(13.0F).cowl(-9.0F, 15.5F)
			.roof(3.0F, 22.0F, 11.0F)
			.tail(27.0F, 16.0F, 15.5F)
			.cabin(0.74F, 2.0F)
			.build()),
	CAR_PORSCHE_911("car_porsche_911", CarClass.SPORT, 300, 3.4F, 1520, 64, 0xE4E7EC,
		EngineType.ENGINE_FLAT6, TransmissionType.TRANSMISSION_SPORT, WheelType.WHEEL_SPORT,
		// Arkada motor: burun alçak, tavan öne kaymış, sırt uzun ve kesintisiz.
		Body.of(Silhouette.FASTBACK, 72.0F, 29.5F)
			.stance(4.5F, 39.0F, 3.0F)
			.nose(11.5F).cowl(-10.0F, 14.5F)
			.roof(0.0F, 21.0F, 6.0F)
			.tail(26.0F, 17.0F, 15.5F)
			.cabin(0.72F, 2.2F)
			.build()),

	// --- Off-road / SUV / Pickup -------------------------------------------
	CAR_WRANGLER("car_wrangler", CarClass.OFFROAD, 180, 7.6F, 1900, 70, 0x3E6B35,
		EngineType.ENGINE_V6, TransmissionType.TRANSMISSION_AUTOMATIC, WheelType.WHEEL_OFFROAD,
		Body.of(Silhouette.BOXY, 69.0F, 30.0F)
			.stance(9.0F, 39.0F, 0.0F)
			.nose(20.0F).cowl(-9.0F, 21.0F)
			.roof(-4.0F, 29.5F, 28.0F)
			.tail(32.0F, 21.5F, 21.0F)
			.cabin(0.86F, 2.4F)
			.build()),
	CAR_HILUX("car_hilux", CarClass.PICKUP, 175, 11.0F, 2050, 80, 0xE8EBEF,
		EngineType.ENGINE_I4, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_OFFROAD,
		Body.of(Silhouette.PICKUP, 85.0F, 30.0F)
			.stance(9.0F, 49.5F, 0.0F)
			.nose(19.0F).cowl(-11.0F, 22.0F)
			.roof(-4.0F, 29.5F, 9.0F)
			.tail(14.0F, 22.0F, 22.0F)
			.cabin(0.86F, 2.2F)
			.build()),
	CAR_DEFENDER("car_defender", CarClass.SUV, 190, 6.6F, 2320, 90, 0x8B8F7A,
		EngineType.ENGINE_I6, TransmissionType.TRANSMISSION_AUTOMATIC, WheelType.WHEEL_OFFROAD,
		Body.of(Silhouette.BOXY, 80.0F, 32.0F)
			.stance(9.0F, 48.0F, 0.0F)
			.nose(21.0F).cowl(-13.0F, 23.0F)
			.roof(-8.0F, 31.5F, 34.0F)
			.tail(38.0F, 24.0F, 23.5F)
			.cabin(0.88F, 2.4F)
			.build()),

	// --- Klasik / Kamyonet --------------------------------------------------
	CAR_BEETLE("car_beetle", CarClass.CLASSIC, 130, 17.5F, 820, 40, 0x62A8D8,
		EngineType.ENGINE_I4, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_STREET,
		Body.of(Silhouette.ROUNDED, 65.0F, 25.0F)
			.stance(6.0F, 38.0F, 0.0F)
			.nose(11.0F).cowl(-11.0F, 15.0F)
			.roof(-4.0F, 24.0F, 4.0F)
			.tail(20.0F, 15.5F, 11.0F)
			.cabin(0.72F, 2.6F)
			.chrome()
			.build()),
	CAR_C10("car_c10", CarClass.PICKUP, 160, 9.0F, 1680, 76, 0x2E6B52,
		EngineType.ENGINE_V8, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_STREET,
		Body.of(Silhouette.PICKUP, 85.0F, 32.0F)
			.stance(7.5F, 51.0F, 0.0F)
			.nose(18.0F).cowl(-12.0F, 21.0F)
			.roof(-6.0F, 28.0F, 6.0F)
			.tail(11.0F, 21.0F, 21.0F)
			.cabin(0.88F, 1.6F)
			.chrome()
			.build()),
	CAR_TRANSPORTER("car_transporter", CarClass.VAN, 105, 22.0F, 1180, 42, 0xC45A3A,
		EngineType.ENGINE_I4, TransmissionType.TRANSMISSION_MANUAL, WheelType.WHEEL_STREET,
		// Burunsuz: ön cam neredeyse ön aksın üstünde, yan duvar dimdik.
		Body.of(Silhouette.FORWARD_CONTROL, 68.0F, 27.5F)
			.stance(6.5F, 38.0F, -2.0F)
			.nose(14.0F).cowl(-31.0F, 17.0F)
			.roof(-27.0F, 31.0F, 28.0F)
			.tail(31.0F, 17.5F, 16.5F)
			.cabin(0.90F, 1.4F)
			.chrome()
			.build());

	public static final Codec<CarModel> CODEC = StringRepresentable.fromEnum(CarModel::values);
	private static final IntFunction<CarModel> BY_ID =
		ByIdMap.continuous(CarModel::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, CarModel> STREAM_CODEC =
		ByteBufCodecs.idMapper(BY_ID, CarModel::ordinal);

	/**
	 * Aracın yan profili, model birimi cinsinden (16 birim = 1 blok, yani
	 * 1 birim = 6.25 cm). Z ekseninde eksi yön aracın burnu, yükseklikler
	 * yerden ölçülür.
	 *
	 * <p>Üst profil, burundan kuyruğa şu noktalardan geçen bir çizgidir:
	 *
	 * <pre>
	 *                        roofFrontZ ______ roofRearZ
	 *                                  /      \
	 *                       ön cam    /        \  arka cam
	 *              cowlZ ___________ /          \____ deckZ
	 *         kaput    /                              \____ kuyruk
	 *   nose ________/                                      \
	 * </pre>
	 *
	 * Aradaki eğimler bu noktalardan türetilir; {@link Silhouette} de her
	 * eğimin kaç panele bölüneceğini ve ne kadar kavis yapacağını söyler.
	 * Böylece "ön cam kaç derece" gibi bir sayı elle girilmez, gerçek araç
	 * fotoğrafından okunabilen ölçülerden çıkar.
	 *
	 * @param silhouette     gövde tipi
	 * @param length         toplam uzunluk
	 * @param width          en geniş yerinde gövde genişliği
	 * @param clearance      gövde tabanının yerden yüksekliği
	 * @param wheelbase      ön ve arka aks arası mesafe
	 * @param axleOffset     aks çiftinin gövde merkezine göre kayıklığı
	 *                       (artı = tekerlekler arkada, ön çıkma uzar)
	 * @param noseHeight     burnun (z = -length/2) tepe yüksekliği
	 * @param cowlZ          ön camın dibi — kaputun bittiği yer
	 * @param beltHeight     cam altı hattı (kapı üst kenarı)
	 * @param roofFrontZ     ön camın tepesi
	 * @param roofHeight     tavan yüksekliği
	 * @param roofRearZ      tavanın arka kenarı
	 * @param deckZ          arka camın dibi — bagaj kapağının başladığı yer
	 * @param deckHeight     bagaj kapağı yüksekliği
	 * @param tailHeight     kuyruğun (z = +length/2) yüksekliği
	 * @param roofWidthRatio tavanın gövdeye genişlik oranı (kabin daralması)
	 * @param archFlare      çamurluğun gövdeden taşma miktarı
	 * @param chrome         tampon ve süslemeler krom mu (klasik araçlar)
	 */
	public record Body(Silhouette silhouette,
					   float length, float width, float clearance, float wheelbase, float axleOffset,
					   float noseHeight, float cowlZ, float beltHeight,
					   float roofFrontZ, float roofHeight, float roofRearZ,
					   float deckZ, float deckHeight, float tailHeight,
					   float roofWidthRatio, float archFlare, boolean chrome) {

		public static Builder of(final Silhouette silhouette, final float length, final float width) {
			return new Builder(silhouette, length, width);
		}

		public float halfLength() {
			return this.length / 2.0F;
		}

		public float halfWidth() {
			return this.width / 2.0F;
		}

		/** Kabinin (tavanın) genişliği — gövdeden dardır. */
		public float roofWidth() {
			return this.width * this.roofWidthRatio;
		}

		/** Ön aksın Z konumu. */
		public float frontAxleZ() {
			return this.axleOffset - this.wheelbase / 2.0F;
		}

		public float rearAxleZ() {
			return this.axleOffset + this.wheelbase / 2.0F;
		}

		public boolean hasBed() {
			return this.silhouette.hasBed();
		}

		/** Kamyonet kasasının uzunluğu (kasa yoksa 0). */
		public float bedLength() {
			return this.hasBed() ? this.halfLength() - this.deckZ : 0.0F;
		}

		/** Kabinin orta noktası — koltuk ve yolcu konumları buradan çıkar. */
		public float cabinCenterZ() {
			return (this.cowlZ + this.roofRearZ) / 2.0F;
		}

		/** Koltuk oturma yüzeyinin yerden yüksekliği. */
		public float seatHeight() {
			return this.beltHeight - 4.0F;
		}

		/** Aracın en yüksek noktası. */
		public float overallHeight() {
			return Math.max(this.roofHeight, this.deckHeight);
		}

		/** Yan profilin verilen Z'deki yüksekliği (üst hat). */
		public float profileHeight(final float z) {
			final float half = this.halfLength();
			if (z <= -half) {
				return this.noseHeight;
			}
			if (z >= half) {
				return this.tailHeight;
			}
			if (z <= this.cowlZ) {
				return lerp(-half, this.noseHeight, this.cowlZ, this.beltHeight, z);
			}
			if (z <= this.roofFrontZ) {
				return lerp(this.cowlZ, this.beltHeight, this.roofFrontZ, this.roofHeight, z);
			}
			if (z <= this.roofRearZ) {
				return this.roofHeight;
			}
			if (z <= this.deckZ) {
				return lerp(this.roofRearZ, this.roofHeight, this.deckZ, this.deckHeight, z);
			}
			return lerp(this.deckZ, this.deckHeight, half, this.tailHeight, z);
		}

		private static float lerp(final float z0, final float h0, final float z1, final float h1, final float z) {
			return z1 - z0 < 1.0E-4F ? h1 : h0 + (h1 - h0) * (z - z0) / (z1 - z0);
		}

		/** Okunabilir kurulum: her ölçü adıyla verilir, sıra karıştırılamaz. */
		public static final class Builder {
			private final Silhouette silhouette;
			private final float length;
			private final float width;
			private float clearance = 6.0F;
			private float wheelbase = 42.0F;
			private float axleOffset;
			private float noseHeight = 14.0F;
			private float cowlZ = -12.0F;
			private float beltHeight = 16.0F;
			private float roofFrontZ = -2.0F;
			private float roofHeight = 24.0F;
			private float roofRearZ = 12.0F;
			private float deckZ = 24.0F;
			private float deckHeight = 17.0F;
			private float tailHeight = 16.0F;
			private float roofWidthRatio = 0.80F;
			private float archFlare = 1.5F;
			private boolean chrome;

			private Builder(final Silhouette silhouette, final float length, final float width) {
				this.silhouette = silhouette;
				this.length = length;
				this.width = width;
			}

			/** Yerden yükseklik, aks aralığı ve aksların öne/arkaya kaykılığı. */
			public Builder stance(final float clearance, final float wheelbase, final float axleOffset) {
				this.clearance = clearance;
				this.wheelbase = wheelbase;
				this.axleOffset = axleOffset;
				return this;
			}

			public Builder nose(final float noseHeight) {
				this.noseHeight = noseHeight;
				return this;
			}

			/** Ön camın dibi: kaputun bittiği Z ve cam altı hattının yüksekliği. */
			public Builder cowl(final float cowlZ, final float beltHeight) {
				this.cowlZ = cowlZ;
				this.beltHeight = beltHeight;
				return this;
			}

			public Builder roof(final float frontZ, final float height, final float rearZ) {
				this.roofFrontZ = frontZ;
				this.roofHeight = height;
				this.roofRearZ = rearZ;
				return this;
			}

			/** Arka camın dibi, bagaj yüksekliği ve kuyruk yüksekliği. */
			public Builder tail(final float deckZ, final float deckHeight, final float tailHeight) {
				this.deckZ = deckZ;
				this.deckHeight = deckHeight;
				this.tailHeight = tailHeight;
				return this;
			}

			public Builder cabin(final float roofWidthRatio, final float archFlare) {
				this.roofWidthRatio = roofWidthRatio;
				this.archFlare = archFlare;
				return this;
			}

			public Builder chrome() {
				this.chrome = true;
				return this;
			}

			public Body build() {
				return new Body(this.silhouette, this.length, this.width, this.clearance,
					this.wheelbase, this.axleOffset, this.noseHeight, this.cowlZ, this.beltHeight,
					this.roofFrontZ, this.roofHeight, this.roofRearZ, this.deckZ, this.deckHeight,
					this.tailHeight, this.roofWidthRatio, this.archFlare, this.chrome);
			}
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

	/**
	 * Çarpışma kutusu genişliği (blok).
	 *
	 * <p>Minecraft'ta çarpışma kutusu yatayda karedir, araç ise döner. Kutuyu
	 * aracın <em>uzunluğuna</em> göre seçmek her yönde güvenli olurdu ama 5
	 * metrelik bir araba için 3 blokluk bir kare demek: araç yolun kenarına
	 * sürtüp durur, sürüş takılmalı ve dengesiz hissedilir. Bu yüzden kutu
	 * aracın genişliğini alır; karşılığında araç uzunlamasına bloklara biraz
	 * girer.
	 */
	public float hitboxWidth() {
		return this.body.width() / 16.0F;
	}

	/** Çarpışma kutusu yüksekliği (blok). */
	public float hitboxHeight() {
		return this.body.overallHeight() / 16.0F;
	}

	/** Ağır araçlar yokuşta daha yavaş ama savrulmaya daha dirençlidir. */
	public float massFactor() {
		return this.massKg / 1500.0F;
	}
}
