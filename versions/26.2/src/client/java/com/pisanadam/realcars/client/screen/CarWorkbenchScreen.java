package com.pisanadam.realcars.client.screen;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarModel;
import com.pisanadam.realcars.menu.CarWorkbenchMenu;
import com.pisanadam.realcars.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
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
	private static final int PANEL_WIDTH = 256;
	private static final int PANEL_HEIGHT = 202;
	/** Katalog ızgarasının sol üst köşesi ve ölçüleri. */
	private static final int CATALOGUE_X = 180;
	private static final int CATALOGUE_Y = 20;
	private static final int CATALOGUE_COLUMNS = 4;
	private static final int CELL = 18;
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
		this.drawCatalogue(graphics, mouseX, mouseY);
		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040);
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040);

		final Component hint = this.hoveredSlotLabel();
		if (hint != null) {
			graphics.centeredText(this.font, hint, this.imageWidth / 2, HINT_Y, 0xFF404040);
		}
	}

	/**
	 * On iki arabanın katalogu: her hücrede araba ikonu, üstüne gelince adı ve
	 * hangi parçaları istediği yazar.
	 *
	 * <p>Etiketler çizim aşamasında, yani ekran koordinatlarında çiziliyor;
	 * {@code leftPos}/{@code topPos} zaten uygulanmış olduğu için panel içi
	 * koordinatlar doğrudan kullanılır.
	 */
	private void drawCatalogue(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
		final CarModel[] models = CarModel.values();
		for (int index = 0; index < models.length; index++) {
			final int x = CATALOGUE_X + (index % CATALOGUE_COLUMNS) * CELL;
			final int y = CATALOGUE_Y + (index / CATALOGUE_COLUMNS) * CELL;
			graphics.item(new ItemStack(ModItems.car(models[index])), x, y);
			if (this.isOver(mouseX, mouseY, x, y)) {
				graphics.fill(x, y, x + 16, y + 16, 0x60FFFFFF);
				graphics.setTooltipForNextFrame(this.recipeLines(models[index]),
					mouseX - this.leftPos, mouseY - this.topPos);
			}
		}
	}

	/** Bir arabanın adı ve gerektirdiği parçalar — ipucu balonunun içeriği. */
	private List<FormattedCharSequence> recipeLines(final CarModel model) {
		final List<FormattedCharSequence> lines = new ArrayList<>();
		lines.add(Component.translatable("item.realcars." + model.itemName())
			.withStyle(ChatFormatting.WHITE).getVisualOrderText());
		lines.add(Component.translatable("gui.realcars.needs")
			.withStyle(ChatFormatting.GRAY).getVisualOrderText());
		for (final CarWorkbenchMenu.Requirement need : CarWorkbenchMenu.requirements(model)) {
			final Component name = new ItemStack(need.item()).getHoverName();
			final Component line = need.count() > 1
				? Component.literal("  " + need.count() + "x ").append(name)
				: Component.literal("  ").append(name);
			lines.add(line.copy().withStyle(ChatFormatting.GRAY).getVisualOrderText());
		}
		return lines;
	}

	private boolean isOver(final int mouseX, final int mouseY, final int x, final int y) {
		final int localX = mouseX - this.leftPos;
		final int localY = mouseY - this.topPos;
		return localX >= x && localX < x + 16 && localY >= y && localY < y + 16;
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		final CarModel[] models = CarModel.values();
		for (int index = 0; index < models.length; index++) {
			final int x = CATALOGUE_X + (index % CATALOGUE_COLUMNS) * CELL;
			final int y = CATALOGUE_Y + (index / CATALOGUE_COLUMNS) * CELL;
			if (this.isOver((int) event.x(), (int) event.y(), x, y) && this.minecraft != null) {
				// Parçaları sunucu dizer: envanteri yalnızca o görebilir.
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
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
