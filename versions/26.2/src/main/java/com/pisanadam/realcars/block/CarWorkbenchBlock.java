package com.pisanadam.realcars.block;

import com.mojang.serialization.MapCodec;
import com.pisanadam.realcars.menu.CarWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Araba yapma masası — arabanın parçalarını adlandırılmış gözlere koyup
 * birleştirdiğin tezgah. Sıradan tezgahtan farkı, hangi parçanın nereye
 * gideceğini ezberlemek zorunda olmaman.
 */
public class CarWorkbenchBlock extends Block {
	public static final MapCodec<CarWorkbenchBlock> CODEC = simpleCodec(CarWorkbenchBlock::new);
	private static final Component TITLE = Component.translatable("gui.realcars.workbench");

	public CarWorkbenchBlock(final BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends CarWorkbenchBlock> codec() {
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
				new CarWorkbenchMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
			TITLE);
	}
}
