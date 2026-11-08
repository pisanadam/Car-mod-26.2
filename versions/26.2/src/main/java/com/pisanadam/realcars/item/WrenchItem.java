package com.pisanadam.realcars.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * İngiliz anahtarı. Asıl işi {@code CarEntity.interact} içinde yapılır: elinde
 * bu varken bir araca sağ tıklamak modifiye ekranını açar.
 */
public class WrenchItem extends Item {
	public WrenchItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
								final TooltipDisplay display, final Consumer<Component> adder,
								final TooltipFlag flag) {
		adder.accept(Component.translatable("tooltip.realcars.wrench_hint").withStyle(ChatFormatting.GRAY));
	}
}
