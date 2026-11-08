package com.pisanadam.realcars.menu;

import com.pisanadam.realcars.registry.ModBlocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

/**
 * Montaj tezgahının 3x3 menüsü.
 *
 * <p>Vanilla {@link CraftingMenu} kullanılamaz: {@code stillValid} açılan
 * konumda <em>çalışma tezgahı bloğu</em> arar, montaj tezgahı da o blok
 * olmadığı için menü açıldığı tick kapanır. Dışarıdan bakınca blok hiç
 * açılmıyormuş gibi görünür. Bu sınıf yalnızca o kontrolü kendi bloğuna
 * çevirir; geri kalan her şey vanilla tezgahıyla aynıdır.
 */
public class AssemblyMenu extends CraftingMenu {
	private final ContainerLevelAccess access;

	public AssemblyMenu(final int containerId, final Inventory inventory, final ContainerLevelAccess access) {
		super(containerId, inventory, access);
		this.access = access;
	}

	@Override
	public boolean stillValid(final Player player) {
		return stillValid(this.access, player, ModBlocks.ASSEMBLY_TABLE);
	}
}
