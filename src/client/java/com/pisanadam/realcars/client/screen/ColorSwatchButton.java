package com.pisanadam.realcars.client.screen;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** Modifiye ekranındaki renk kutucuğu; seçili olan çerçeveyle işaretlenir. */
public class ColorSwatchButton extends AbstractButton {
	private final int color;
	private final Runnable action;
	private final BooleanSupplier selected;

	public ColorSwatchButton(final int x, final int y, final int width, final int height,
							 final int color, final Runnable action, final BooleanSupplier selected) {
		super(x, y, width, height, Component.empty());
		this.color = color;
		this.action = action;
		this.selected = selected;
	}

	@Override
	public void onPress(final InputWithModifiers input) {
		this.action.run();
	}

	@Override
	protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
								   final float partialTick) {
		final int x0 = this.getX();
		final int y0 = this.getY();
		final int x1 = x0 + this.getWidth();
		final int y1 = y0 + this.getHeight();

		graphics.fill(x0, y0, x1, y1, 0xFF2A2E34);
		graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFF000000 | this.color);

		// Takılı renk sarı, üzerine gelinen renk beyaz çerçeveyle gösterilir.
		final int border = this.selected.getAsBoolean() ? 0xFFF2C13A
			: (this.isHovered() ? 0xFFFFFFFF : 0);
		if (border != 0) {
			graphics.outline(x0, y0, this.getWidth(), this.getHeight(), border);
		}
	}

	@Override
	protected void updateWidgetNarration(final NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
