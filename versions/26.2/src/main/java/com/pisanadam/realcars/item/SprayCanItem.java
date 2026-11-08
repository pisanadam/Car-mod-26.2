package com.pisanadam.realcars.item;

import com.pisanadam.realcars.entity.CarColors;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Sprey boya — araca sağ tıklayarak lifte gitmeden hızlıca renk değiştirmeyi
 * sağlar. Boyama {@code CarEntity.interact} içinde yapılır.
 */
public class SprayCanItem extends Item {
	private final DyeColor dye;

	public SprayCanItem(final Item.Properties properties, final DyeColor dye) {
		super(properties);
		this.dye = dye;
	}

	public DyeColor dye() {
		return this.dye;
	}

	public int carColor() {
		return CarColors.of(this.dye);
	}

	@Override
	public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
								final TooltipDisplay display, final Consumer<Component> adder,
								final TooltipFlag flag) {
		adder.accept(Component.translatable("tooltip.realcars.color")
			.append(": ")
			.append(Component.translatable("realcars.color." + this.dye.getSerializedName()))
			.withStyle(ChatFormatting.GRAY));
	}
}
