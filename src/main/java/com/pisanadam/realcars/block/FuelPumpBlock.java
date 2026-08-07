package com.pisanadam.realcars.block;

import com.mojang.serialization.MapCodec;
import com.pisanadam.realcars.entity.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.sounds.SoundEvents;

/**
 * Yakıt pompası. Yakınındaki araca sağ tıklayarak depoyu doldurur; her 20 litre
 * için envanterden bir kömür harcanır.
 */
public class FuelPumpBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<FuelPumpBlock> CODEC = simpleCodec(FuelPumpBlock::new);

	/** Bir kömürün doldurduğu yakıt miktarı (litre). */
	private static final float FUEL_PER_COAL = 20.0F;

	public FuelPumpBlock(final BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<? extends FuelPumpBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(final BlockState state, final Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
											   final Player player, final net.minecraft.world.phys.BlockHitResult hitResult) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		final CarEntity car = CarLiftBlock.findNearestCar(level, pos);
		if (car == null) {
			return InteractionResult.CONSUME;
		}

		final float capacity = car.model().fuelCapacity();
		final float missing = capacity - car.fuel();
		if (missing < 0.5F) {
			player.sendOverlayMessage(Component.translatable("message.realcars.tank_full"));
			return InteractionResult.CONSUME;
		}

		final int coalNeeded = (int) Math.ceil(missing / FUEL_PER_COAL);
		final int coalAvailable = player.getAbilities().instabuild ? coalNeeded : countCoal(player);
		if (coalAvailable <= 0) {
			return InteractionResult.CONSUME;
		}

		final int coalUsed = Math.min(coalNeeded, coalAvailable);
		if (!player.getAbilities().instabuild) {
			consumeCoal(player, coalUsed);
		}
		car.setFuel(car.fuel() + coalUsed * FUEL_PER_COAL);
		level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7F, 1.4F);
		player.sendOverlayMessage(Component.translatable("message.realcars.refueled"));
		return InteractionResult.SUCCESS;
	}

	private static int countCoal(final Player player) {
		int total = 0;
		for (final ItemStack stack : player.getInventory()) {
			if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static void consumeCoal(final Player player, final int amount) {
		int remaining = amount;
		for (final ItemStack stack : player.getInventory()) {
			if (remaining <= 0) {
				return;
			}
			if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
				final int take = Math.min(remaining, stack.getCount());
				stack.shrink(take);
				remaining -= take;
			}
		}
	}
}
