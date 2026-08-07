package com.pisanadam.realcars;

import com.pisanadam.realcars.registry.ModBlocks;
import com.pisanadam.realcars.registry.ModComponents;
import com.pisanadam.realcars.registry.ModEntities;
import com.pisanadam.realcars.registry.ModItems;
import com.pisanadam.realcars.registry.ModMenus;
import com.pisanadam.realcars.registry.ModNetworking;
import com.pisanadam.realcars.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealCars implements ModInitializer {
	public static final String MOD_ID = "realcars";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Sıra önemlidir: item'lar blokların item biçimlerini ve araç
		// entity tiplerini yaratıcı sekmesine eklerken kullanır.
		ModComponents.init();
		ModSounds.init();
		ModBlocks.init();
		ModEntities.init();
		ModItems.init();
		ModMenus.init();
		ModNetworking.init();
		LOGGER.info("RealCars yüklendi.");
	}
}
