package com.pisanadam.realcars.block;

import com.mojang.serialization.MapCodec;
import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.registry.ModMenus;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Araç lifti. Yanına park edilmiş bir arabaya sağ tıklanınca modifiye ekranını
 * açar; İngiliz anahtarı olmadan modifiye yapmanın kalıcı yoludur.
 */
public class CarLiftBlock extends Block {
	public static final MapCodec<CarLiftBlock> CODEC = simpleCodec(CarLiftBlock::new);
	/** Liftin araç arayacağı yarıçap (blok). */
	private static final double SEARCH_RADIUS = 4.0D;

	public CarLiftBlock(final BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends CarLiftBlock> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
											   final Player player, final BlockHitResult hitResult) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		final CarEntity car = findNearestCar(level, pos);
		if (car == null) {
			return InteractionResult.CONSUME;
		}
		ModMenus.openModification(player, car);
		return InteractionResult.SUCCESS;
	}

	/** Lifte en yakın aracı bulur. */
	public static @Nullable CarEntity findNearestCar(final Level level, final BlockPos pos) {
		final AABB area = new AABB(pos).inflate(SEARCH_RADIUS);
		final List<CarEntity> cars = level.getEntitiesOfClass(CarEntity.class, area, CarEntity::isAlive);
		CarEntity nearest = null;
		double bestDistance = Double.MAX_VALUE;
		for (final CarEntity car : cars) {
			final double distance = car.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
			if (distance < bestDistance) {
				bestDistance = distance;
				nearest = car;
			}
		}
		return nearest;
	}
}
