package com.pisanadam.realcars.registry;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.EngineType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Modun sesleri — dosyalar {@code tools/gen_sounds.py} tarafından üretilir. */
public final class ModSounds {
	public static final SoundEvent ENGINE_IDLE = register("engine_idle");
	public static final SoundEvent ENGINE_START = register("engine_start");
	public static final SoundEvent ENGINE_STOP = register("engine_stop");
	public static final SoundEvent HORN = register("horn");
	public static final SoundEvent BRAKE_SQUEAL = register("brake_squeal");
	public static final SoundEvent GRAVEL_LOOP = register("gravel_loop");
	public static final SoundEvent CRASH = register("crash");
	public static final SoundEvent DOOR_CLOSE = register("door_close");
	public static final SoundEvent WRENCH_USE = register("wrench_use");

	/** Her motor tipinin kendi döngü sesi vardır (silindir sayısına göre farklı). */
	private static final Map<EngineType, SoundEvent> ENGINE_LOOPS = new EnumMap<>(EngineType.class);

	static {
		for (final EngineType engine : EngineType.values()) {
			ENGINE_LOOPS.put(engine, register(engine.soundName()));
		}
	}

	private ModSounds() {
	}

	private static SoundEvent register(final String name) {
		final Identifier id = RealCars.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static SoundEvent engineLoop(final EngineType engine) {
		return ENGINE_LOOPS.get(engine);
	}

	public static void init() {
	}
}
