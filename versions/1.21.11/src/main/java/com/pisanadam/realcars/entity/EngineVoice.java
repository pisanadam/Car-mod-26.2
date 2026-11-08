package com.pisanadam.realcars.entity;

import java.util.Locale;

/**
 * Bir aracın motor sesi: gerçek bir araç kaydından çıkarılmış döngüler.
 *
 * <p>Her aracın kendi kaydı vardır (Golf GTI'ın sesi bir Golf GTI'dan, Beetle'ın
 * sesi bir Beetle'dan). Modifiyede başka bir motor takılırsa araç o motor
 * tipinin temsilci sesine geçer: Golf'e V8 takılırsa Mustang gibi gürler.
 *
 * <p>Her ses üç katmandan oluşur ({@link Layer}); katmanların hangi devirde
 * kaydedildiği {@code assets/realcars/engine_voices.json} içinde yazar ve
 * istemci devre göre katmanlar arasında geçiş yapar. Dosyalar
 * {@code tools/import_engine_sounds.py} tarafından üretilir.
 */
public enum EngineVoice {
	GOLF_GTI, COROLLA, BMW_M3, MUSTANG_GT, GTR, PORSCHE_911, WRANGLER, HILUX, DEFENDER, BEETLE, C10,
	TRANSPORTER,
	/** Yalnızca modifiyeyle V6 takılan araçlar için (Wrangler'ın kendi sesi bir Jeep'ten). */
	V6;

	public enum Layer {
		IDLE, MID, HIGH;

		public String id() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}

	public String id() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	/** Ses olayının adı, ör. {@code engine_golf_gti_idle}. */
	public String soundName(final Layer layer) {
		return "engine_" + this.id() + "_" + layer.id();
	}

	/** Aracın kendi motoru takılıysa kendi sesi, değilse takılı motorun temsilci sesi. */
	public static EngineVoice of(final CarModel model, final EngineType installed) {
		if (installed == model.defaultEngine()) {
			return valueOf(model.itemName().substring("car_".length()).toUpperCase(Locale.ROOT));
		}
		return forEngine(installed);
	}

	public static EngineVoice forEngine(final EngineType engine) {
		return switch (engine) {
			case ENGINE_I4 -> GOLF_GTI;
			case ENGINE_I6 -> GTR;
			case ENGINE_V6 -> V6;
			case ENGINE_V8 -> MUSTANG_GT;
			case ENGINE_FLAT6 -> PORSCHE_911;
			case ENGINE_FLAT4 -> BEETLE;
		};
	}
}
