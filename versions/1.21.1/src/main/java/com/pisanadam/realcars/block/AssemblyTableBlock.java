package com.pisanadam.realcars.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import com.pisanadam.realcars.menu.AssemblyMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Montaj tezgahı — araç parçalarını ve arabaları birleştirmek için 3x3'lük bir
 * tezgah açar. Tarifler normal tariflerdir, yani sıradan bir çalışma tezgahında
 * da yapılabilirler; bu blok garajın çalışma noktasıdır.
 *
 * <p>Menü olarak vanilla tezgah menüsü değil {@link AssemblyMenu} kullanılır;
 * sebebi orada anlatılıyor.
 */
public class AssemblyTableBlock extends Block {
	public static final MapCodec<AssemblyTableBlock> CODEC = simpleCodec(AssemblyTableBlock::new);
	private static final Component TITLE = Component.translatable("gui.realcars.assembly");

	public AssemblyTableBlock(final BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends AssemblyTableBlock> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
											   final Player player, final BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.openMenu(this.getMenuProvider(state, level, pos));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected @Nullable MenuProvider getMenuProvider(final BlockState state, final Level level, final BlockPos pos) {
		return new SimpleMenuProvider(
			(containerId, inventory, player) ->
				new AssemblyMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
			TITLE);
	}
}
