package com.pisanadam.realcars.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Yakıt bidonu — bir araca sağ tıklandığında depoya {@link #FUEL_AMOUNT} litre
 * ekler ve tükenir. Doldurma mantığı {@code CarEntity.interact} içindedir.
 */
public class FuelCanisterItem extends Item {
	/** Bir bidonun taşıdığı yakıt (litre). */
	public static final float FUEL_AMOUNT = 25.0F;

	public FuelCanisterItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
								final java.util.List<Component> tooltip,
								final TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.realcars.canister_hint").withStyle(ChatFormatting.GRAY));
	}
}
