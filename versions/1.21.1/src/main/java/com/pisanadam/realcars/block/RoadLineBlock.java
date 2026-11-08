package com.pisanadam.realcars.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Yol çizgisi bloğu. Çizginin yönü oyuncunun baktığı yöne göre belirlenir, bu
 * yüzden blok yalnızca yatay eksende döner (kuzey-güney ya da doğu-batı).
 */
public class RoadLineBlock extends Block {
	public static final MapCodec<RoadLineBlock> CODEC = simpleCodec(RoadLineBlock::new);
	public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	public RoadLineBlock(final BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(HORIZONTAL_AXIS, Direction.Axis.Z));
	}

	@Override
	public MapCodec<? extends RoadLineBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_AXIS);
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		// Çizgi, oyuncunun yürüdüğü yön boyunca uzansın.
		return this.defaultBlockState().setValue(HORIZONTAL_AXIS,
			context.getHorizontalDirection().getAxis());
	}

	@Override
	protected BlockState rotate(final BlockState state, final Rotation rotation) {
		return switch (rotation) {
			case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> state.setValue(HORIZONTAL_AXIS,
				state.getValue(HORIZONTAL_AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
			default -> state;
		};
	}
}
