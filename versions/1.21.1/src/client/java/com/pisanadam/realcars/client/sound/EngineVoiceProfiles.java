package com.pisanadam.realcars.client.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.EngineVoice;
import com.pisanadam.realcars.entity.GearBox;
import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Motor seslerinin katmanlarının hangi devirde kaydedildiği.
 *
 * <p>Dönüştürücü her katmanı, kayıttaki rölanti devrine oranla yazar
 * ({@code assets/realcars/engine_voices.json}: {@code {"golf_gti": {"mid": 2.4,
 * "high": 5.1}, ...}}). Rölanti katmanı oyunun rölanti devrine
 * ({@link GearBox#IDLE_RPM}) oturtulur; böylece rölantideki araç kayıttaki
 * gibi duyulur ve diğer katmanlar aynı oranla yerleşir.
 *
 * <p>Dosya kaynak paketinden okunur; bir kaynak paketi hem sesleri hem bu
 * oranları değiştirebilir.
 */
public final class EngineVoiceProfiles {
	private static final ResourceLocation FILE = RealCars.id("engine_voices.json");
	/** Dosya yoksa ya da bir ses eksikse kullanılan makul oranlar. */
	private static final float DEFAULT_MID = 2.4F;
	private static final float DEFAULT_HIGH = 5.2F;

	private static Map<EngineVoice, float[]> cache;

	private EngineVoiceProfiles() {
	}

	/** Katmanların devirleri: [rölanti, orta, yüksek]. */
	public static float[] layerRpms(final EngineVoice voice) {
		if (cache == null) {
			cache = load();
		}
		return cache.computeIfAbsent(voice, v -> rpms(DEFAULT_MID, DEFAULT_HIGH));
	}

	/** Kaynaklar yeniden yüklenince oranlar da yeniden okunsun. */
	public static void invalidate() {
		cache = null;
	}

	private static float[] rpms(final float mid, final float high) {
		final float idle = GearBox.IDLE_RPM;
		return new float[] {idle, idle * mid, idle * high};
	}

	private static Map<EngineVoice, float[]> load() {
		final Map<EngineVoice, float[]> out = new EnumMap<>(EngineVoice.class);
		try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(FILE)) {
			final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (final EngineVoice voice : EngineVoice.values()) {
				final JsonElement entry = root.get(voice.id());
				if (entry == null || !entry.isJsonObject()) {
					continue;
				}
				final JsonObject o = entry.getAsJsonObject();
				final float mid = o.has("mid") ? o.get("mid").getAsFloat() : DEFAULT_MID;
				final float high = o.has("high") ? o.get("high").getAsFloat() : DEFAULT_HIGH;
				if (mid > 1.0F && high > mid) {
					out.put(voice, rpms(mid, high));
				}
			}
		} catch (final Exception e) {
			RealCars.LOGGER.warn("engine_voices.json okunamadı, varsayılan devir oranları kullanılıyor", e);
		}
		return out;
	}
}
