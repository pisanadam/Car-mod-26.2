package com.pisanadam.realcars.registry;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.menu.CarModificationMenu;
import com.pisanadam.realcars.menu.CarWorkbenchMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** Modun menü tipleri. */
public final class ModMenus {
	private static final StreamCodec<RegistryFriendlyByteBuf, Integer> ENTITY_ID_CODEC =
		ByteBufCodecs.VAR_INT.cast();

	/**
	 * Modifiye menüsü. Açılış verisi olarak aracın entity kimliği taşınır;
	 * istemci menüyü kurarken aracı bu kimlikten bulur.
	 */
	public static final MenuType<CarModificationMenu> CAR_MODIFICATION =
		Registry.register(BuiltInRegistries.MENU, RealCars.id("car_modification"),
			new ExtendedMenuType<CarModificationMenu, Integer>(CarModificationMenu::new, ENTITY_ID_CODEC));

	/** Araba yapma masasının menüsü — açılış verisi taşımaz. */
	public static final MenuType<CarWorkbenchMenu> CAR_WORKBENCH =
		Registry.register(BuiltInRegistries.MENU, RealCars.id("car_workbench"),
			new MenuType<>(CarWorkbenchMenu::new, FeatureFlags.VANILLA_SET));

	private ModMenus() {
	}

	/** Verilen araç için modifiye ekranını açar. */
	public static void openModification(final Player player, final CarEntity car) {
		player.openMenu(new CarModificationProvider(car));
	}

	private record CarModificationProvider(CarEntity car) implements ExtendedMenuProvider<Integer> {
		@Override
		public Integer getScreenOpeningData(final ServerPlayer serverPlayer) {
			return this.car.getId();
		}

		@Override
		public Component getDisplayName() {
			return Component.translatable("gui.realcars.modification");
		}

		@Override
		public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player opener) {
			return new CarModificationMenu(containerId, inventory, this.car.getId());
		}
	}

	public static void init() {
	}
}
