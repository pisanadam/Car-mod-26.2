package com.pisanadam.realcars.registry;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.CarModel;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Her araç modeli kendi entity tipiyle kaydedilir; böylece {@code /summon
 * realcars:car_bmw_m3} çalışır ve her modelin çarpışma kutusu kendi ölçüsünde olur.
 */
public final class ModEntities {
	private static final Map<CarModel, EntityType<CarEntity>> TYPES = new EnumMap<>(CarModel.class);

	static {
		for (final CarModel model : CarModel.values()) {
			TYPES.put(model, registerCar(model));
		}
	}

	private ModEntities() {
	}

	private static EntityType<CarEntity> registerCar(final CarModel model) {
		final ResourceKey<EntityType<?>> key =
			ResourceKey.create(Registries.ENTITY_TYPE, RealCars.id(model.itemName()));
		final EntityType<CarEntity> type = EntityType.Builder
			.<CarEntity>of((entityType, level) -> new CarEntity(entityType, level, model), MobCategory.MISC)
			.sized(model.hitboxWidth(), model.hitboxHeight())
			.eyeHeight(model.hitboxHeight() * 0.8F)
			// Araçlar hızlı gittiği için uzaktan da izlenmeleri ve sık
			// güncellenmeleri gerekir, yoksa diğer oyuncularda zıplarlar.
			.clientTrackingRange(12)
			.updateInterval(2)
			.build(key);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
	}

	public static EntityType<CarEntity> type(final CarModel model) {
		return TYPES.get(model);
	}

	public static @Nullable CarEntity create(final CarModel model, final Level level) {
		return TYPES.get(model).create(level, EntitySpawnReason.MOB_SUMMONED);
	}

	public static void init() {
	}
}
