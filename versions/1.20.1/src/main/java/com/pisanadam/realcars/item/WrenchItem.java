package com.pisanadam.realcars.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * İngiliz anahtarı. Asıl işi {@code CarEntity.interact} içinde yapılır: elinde
 * bu varken bir araca sağ tıklamak modifiye ekranını açar.
 */
public class WrenchItem extends Item {
	public WrenchItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(final ItemStack stack, final net.minecraft.world.level.Level level,
								final java.util.List<Component> tooltip,
								final TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.realcars.wrench_hint").withStyle(ChatFormatting.GRAY));
	}
}
