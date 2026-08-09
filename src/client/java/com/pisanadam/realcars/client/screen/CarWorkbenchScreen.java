package com.pisanadam.realcars.client.screen;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.menu.CarWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Araba yapma masasının ekranı.
 *
 * <p>Altı girdi gözünün her biri belli bir parçaya ayrılmıştır. Hangisinin ne
 * olduğu, üstüne gelince gözlerin hemen üstünde yazılır; böylece dar ekrana
 * altı etiketi birden sığdırmaya çalışmadan hepsi öğrenilebilir.
 */
public class CarWorkbenchScreen extends AbstractContainerScreen<CarWorkbenchMenu> {
	private static final Identifier BACKGROUND = RealCars.id("textures/gui/car_workbench.png");
	private static final int PANEL_WIDTH = 176;
	private static final int PANEL_HEIGHT = 166;
	/** Gözün üstündeki ipucu satırının Y'si. */
	private static final int HINT_Y = 22;

	private static final String[] SLOT_KEYS = {
		"gui.realcars.slot.chassis",
		"gui.realcars.slot.engine",
		"gui.realcars.slot.transmission",
		"gui.realcars.slot.wheels",
		"gui.realcars.slot.seat",
		"gui.realcars.slot.windshield",
	};

	public CarWorkbenchScreen(final CarWorkbenchMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
								  final float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos,
			0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
	}

	@Override
	protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040);
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040);

		final Component hint = this.hoveredSlotLabel();
		if (hint != null) {
			graphics.centeredText(this.font, hint, this.imageWidth / 2, HINT_Y, 0xFF404040);
		}
	}

	/** Fare hangi parça gözünün üstündeyse onun adı; başka yerdeyse null. */
	private Component hoveredSlotLabel() {
		if (this.hoveredSlot == null) {
			return null;
		}
		final int index = this.hoveredSlot.index;
		return index >= 0 && index < SLOT_KEYS.length
			? Component.translatable(SLOT_KEYS[index]) : null;
	}
}
