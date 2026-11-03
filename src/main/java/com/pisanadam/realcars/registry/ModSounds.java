package com.pisanadam.realcars.registry;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.EngineVoice;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Modun sesleri. Motor sesleri gerçek araç kayıtlarından
 * ({@code tools/import_engine_sounds.py}), diğerleri sentezle
 * ({@code tools/gen_sounds.py}) üretilir.
 */
public final class ModSounds {
	public static final SoundEvent ENGINE_START = register("engine_start");
	public static final SoundEvent ENGINE_STOP = register("engine_stop");
	public static final SoundEvent HORN = register("horn");
	public static final SoundEvent BRAKE_SQUEAL = register("brake_squeal");
	public static final SoundEvent GRAVEL_LOOP = register("gravel_loop");
	public static final SoundEvent CRASH = register("crash");
	public static final SoundEvent DOOR_CLOSE = register("door_close");
	public static final SoundEvent WRENCH_USE = register("wrench_use");

	/** Her motor sesinin (araç kaydı) üç katmanı: rölanti, orta ve yüksek devir. */
	private static final Map<EngineVoice, Map<EngineVoice.Layer, SoundEvent>> ENGINE_LAYERS =
		new EnumMap<>(EngineVoice.class);

	static {
		for (final EngineVoice voice : EngineVoice.values()) {
			final Map<EngineVoice.Layer, SoundEvent> layers = new EnumMap<>(EngineVoice.Layer.class);
			for (final EngineVoice.Layer layer : EngineVoice.Layer.values()) {
				layers.put(layer, register(voice.soundName(layer)));
			}
			ENGINE_LAYERS.put(voice, layers);
		}
	}

	private ModSounds() {
	}

	private static SoundEvent register(final String name) {
		final Identifier id = RealCars.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static SoundEvent engineLayer(final EngineVoice voice, final EngineVoice.Layer layer) {
		return ENGINE_LAYERS.get(voice).get(layer);
	}

	public static void init() {
	}
}
