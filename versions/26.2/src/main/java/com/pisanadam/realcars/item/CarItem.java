package com.pisanadam.realcars.item;

import com.pisanadam.realcars.entity.CarConfig;
import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.CarModel;
import com.pisanadam.realcars.registry.ModEntities;
import com.pisanadam.realcars.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Yerdeki bir bloğa sağ tıklanınca aracı çıkaran item. */
public class CarItem extends Item {
	private final CarModel model;

	public CarItem(final Item.Properties properties, final CarModel model) {
		super(properties);
		this.model = model;
	}

	public CarModel model() {
		return this.model;
	}

	@Override
	public InteractionResult useOn(final UseOnContext context) {
		final Level level = context.getLevel();
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (context.getClickedFace() != Direction.UP) {
			return InteractionResult.PASS;
		}

		final BlockPos above = context.getClickedPos().above();
		final CarEntity car = ModEntities.create(this.model, level);
		if (car == null) {
			return InteractionResult.FAIL;
		}
		car.setPos(above.getX() + 0.5D, above.getY(), above.getZ() + 0.5D);
		// Araç, koyan oyuncunun baktığı yöne dönük çıksın.
		car.setYRot(context.getHorizontalDirection().toYRot());
		car.applyConfig(ModItems.readConfig(context.getItemInHand(), this.model));

		if (!level.noCollision(car, car.getBoundingBox())) {
			return InteractionResult.FAIL;
		}
		level.addFreshEntity(car);
		context.getItemInHand().consume(1, context.getPlayer());
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
								final TooltipDisplay display, final java.util.function.Consumer<Component> adder,
								final TooltipFlag flag) {
		final CarConfig config = ModItems.readConfig(stack, this.model);
		adder.accept(Component.translatable("tooltip.realcars.top_speed", this.model.topSpeedKmh())
			.withStyle(ChatFormatting.GRAY));
		adder.accept(Component.translatable("tooltip.realcars.engine",
			Component.translatable("item.realcars." + config.engine().itemName())).withStyle(ChatFormatting.GRAY));
		adder.accept(Component.translatable("tooltip.realcars.transmission",
			Component.translatable("item.realcars." + config.transmission().itemName())).withStyle(ChatFormatting.GRAY));
		adder.accept(Component.translatable("tooltip.realcars.wheels",
			Component.translatable("item.realcars." + config.wheel().itemName())).withStyle(ChatFormatting.GRAY));
		adder.accept(Component.translatable("tooltip.realcars.fuel",
			String.format("%.0f", config.fuel()), this.model.fuelCapacity()).withStyle(ChatFormatting.DARK_GRAY));
	}
}
