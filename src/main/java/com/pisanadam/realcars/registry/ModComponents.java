package com.pisanadam.realcars.registry;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Item üzerinde taşınan veri bileşenleri. */
public final class ModComponents {
	/**
	 * Araç item'ının taşıdığı yapılandırma. Aracı toplayıp tekrar koyduğunda
	 * boyası, tekerleği, rüzgarlığı ve yakıtı korunur.
	 */
	public static final DataComponentType<CarConfig> CAR_CONFIG =
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, RealCars.id("car_config"),
			DataComponentType.<CarConfig>builder()
				.persistent(CarConfig.CODEC)
				.networkSynchronized(CarConfig.STREAM_CODEC)
				.build());

	private ModComponents() {
	}

	public static void init() {
	}
}
