package com.pisanadam.realcars.client.screen;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarColors;
import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.EngineType;
import com.pisanadam.realcars.entity.SpoilerType;
import com.pisanadam.realcars.entity.TransmissionType;
import com.pisanadam.realcars.entity.WheelType;
import com.pisanadam.realcars.menu.CarModificationMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.Lighting;
import com.pisanadam.realcars.client.render.CarEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Araç modifiye ekranı.
 *
 * <p>Solda araç canlı olarak döner; sağdaki sekmelerden yapılan her değişiklik
 * sunucuya bir düğme tıklaması olarak gider ve araç güncellenince önizleme de
 * anında değişir — ayrı bir "uygula" adımına gerek yoktur.
 */
public class CarModificationScreen extends AbstractContainerScreen<CarModificationMenu> {
	private static final ResourceLocation BACKGROUND = RealCars.id("textures/gui/modification.png");
	private static final int PANEL_WIDTH = 248;
	private static final int PANEL_HEIGHT = 235;

	private static final int PREVIEW_X = 7;
	private static final int PREVIEW_Y = 36;
	private static final int PREVIEW_W = 94;
	private static final int PREVIEW_H = 100;

	private static final int OPTIONS_X = 106;
	private static final int OPTIONS_Y = 36;
	private static final int OPTIONS_W = 135;

	private static final int TAB_Y = 18;
	private static final int TAB_W = 60;
	private static final int TAB_H = 15;

	private enum Tab {
		COLOR("gui.realcars.tab.color"),
		WHEEL("gui.realcars.tab.wheel"),
		SPOILER("gui.realcars.tab.spoiler"),
		ENGINE("gui.realcars.tab.engine");

		private final String key;

		Tab(final String key) {
			this.key = key;
		}

		Component label() {
			return Component.translatable(this.key);
		}
	}

	private final List<AbstractWidget> tabWidgets = new ArrayList<>();
	private Tab activeTab = Tab.COLOR;
	private float previewSpin;

	public CarModificationScreen(final CarModificationMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title);
		this.imageWidth = PANEL_WIDTH;
		this.imageHeight = PANEL_HEIGHT;
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = 140;
	}

	@Override
	protected void init() {
		super.init();
		for (final Tab tab : Tab.values()) {
			final int x = this.leftPos + 4 + tab.ordinal() * (TAB_W + 1);
			this.addRenderableWidget(Button.builder(tab.label(), button -> this.selectTab(tab))
				.bounds(x, this.topPos + TAB_Y, TAB_W, TAB_H)
				.build());
		}
		this.rebuildTabWidgets();
	}

	private void selectTab(final Tab tab) {
		this.activeTab = tab;
		this.rebuildTabWidgets();
	}

	private void rebuildTabWidgets() {
		this.tabWidgets.forEach(this::removeWidget);
		this.tabWidgets.clear();

		final int x = this.leftPos + OPTIONS_X + 3;
		final int y = this.topPos + OPTIONS_Y + 3;
		switch (this.activeTab) {
			case COLOR -> this.buildColorGrid(x, y);
			case WHEEL -> this.buildOptionList(x, y, WheelType.values().length, index -> {
				final WheelType wheel = WheelType.values()[index];
				return new Option(Component.translatable("item.realcars." + wheel.itemName()),
					CarModificationMenu.wheelButton(wheel),
					car -> car.wheelType() == wheel);
			});
			case SPOILER -> this.buildOptionList(x, y, SpoilerType.values().length, index -> {
				final SpoilerType spoiler = SpoilerType.values()[index];
				return new Option(Component.translatable("realcars.spoiler." + spoiler.itemName()),
					CarModificationMenu.spoilerButton(spoiler),
					car -> car.spoilerType() == spoiler);
			});
			case ENGINE -> this.buildEngineTab(x, y);
		}
		this.tabWidgets.forEach(this::addRenderableWidget);
	}

	private record Option(Component label, int buttonId, java.util.function.Predicate<CarEntity> installed) {
	}

	private void buildColorGrid(final int x, final int y) {
		final int perRow = 4;
		final int cell = 24;
		for (int i = 0; i < CarColors.count(); i++) {
			final int index = i;
			final int cx = x + (i % perRow) * cell;
			final int cy = y + (i / perRow) * cell;
			this.tabWidgets.add(new ColorSwatchButton(cx, cy, cell - 3, cell - 3,
				CarColors.byIndex(index),
				() -> this.click(CarModificationMenu.colorButton(index)),
				() -> {
					final CarEntity car = this.menu.car();
					return car != null && car.color() == CarColors.byIndex(index);
				}));
		}
	}

	private void buildOptionList(final int x, final int y, final int count,
								 final java.util.function.IntFunction<Option> factory) {
		for (int i = 0; i < count; i++) {
			final Option option = factory.apply(i);
			this.tabWidgets.add(Button.builder(this.decorate(option), button -> this.click(option.buttonId()))
				.bounds(x, y + i * 21, OPTIONS_W - 9, 19)
				.build());
		}
	}

	private void buildEngineTab(final int x, final int y) {
		int row = 0;
		for (final EngineType engine : EngineType.values()) {
			final Option option = new Option(Component.translatable("item.realcars." + engine.itemName()),
				CarModificationMenu.engineButton(engine), car -> car.engineType() == engine);
			this.tabWidgets.add(Button.builder(this.decorate(option), button -> this.click(option.buttonId()))
				.bounds(x, y + row * 14, OPTIONS_W - 9, 13)
				.build());
			row++;
		}
		row++;
		for (final TransmissionType transmission : TransmissionType.values()) {
			final Option option = new Option(Component.translatable("item.realcars." + transmission.itemName()),
				CarModificationMenu.transmissionButton(transmission),
				car -> car.transmissionType() == transmission);
			this.tabWidgets.add(Button.builder(this.decorate(option), button -> this.click(option.buttonId()))
				.bounds(x, y + row * 14, OPTIONS_W - 9, 13)
				.build());
			row++;
		}
	}

	/** Takılı olan seçeneğin yanına bir işaret koyar. */
	private Component decorate(final Option option) {
		final CarEntity car = this.menu.car();
		final boolean installed = car != null && option.installed().test(car);
		return installed ? Component.literal("✔ ").append(option.label()) : option.label();
	}

	private void click(final int buttonId) {
		if (this.minecraft == null || this.minecraft.gameMode == null) {
			return;
		}
		this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
		// Etiketlerdeki "takılı" işareti güncellensin.
		this.rebuildTabWidgets();
	}

	@Override
	protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX,
							final int mouseY) {
		graphics.blit(BACKGROUND, this.leftPos, this.topPos,
			0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
		this.drawPreview(graphics, partialTick);
	}

	/** Aracı önizleme çukurunda yavaşça döndürerek çizer. */
	private void drawPreview(final GuiGraphics graphics, final float partialTick) {
		final CarEntity car = this.menu.car();
		if (car == null || this.minecraft == null) {
			return;
		}
		this.previewSpin += partialTick * 0.6F;

		final int x0 = this.leftPos + PREVIEW_X + 2;
		final int y0 = this.topPos + PREVIEW_Y + 2;
		final int x1 = x0 + PREVIEW_W - 4;
		final int y1 = y0 + PREVIEW_H - 4;

		// GUI'de dünya ters durduğu için Z ekseninde 180 derece çevrilir;
		// X ekseninde hafif eğim aracı yukarıdan gösterir.
		final Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
		final Quaternionf tilt = new Quaternionf().rotateX(-22.0F * Mth.DEG_TO_RAD);
		rotation.mul(tilt);

		// body().length() model birimi cinsinden (16 birim = 1 blok); önizleme
		// kutusunda araç ~80 piksel yer kaplasın istiyoruz.
		final float scale = 80.0F * 16.0F / Math.max(24.0F, car.model().body().length());
		final Vector3f translation = new Vector3f(0.0F, car.getBbHeight() / 2.0F, 0.0F);

		graphics.enableScissor(x0, y0, x1, y1);
		graphics.pose().pushPose();
		graphics.pose().translate((x0 + x1) / 2.0F, (y0 + y1) / 2.0F, 50.0F);
		graphics.pose().scale(scale, scale, -scale);
		graphics.pose().translate(translation.x, translation.y, translation.z);
		graphics.pose().mulPose(rotation);
		Lighting.setupForEntityInInventory();
		final EntityRenderDispatcher dispatcher = this.minecraft.getEntityRenderDispatcher();
		dispatcher.overrideCameraOrientation(tilt.conjugate(new Quaternionf()).rotateY((float) Math.PI));
		dispatcher.setRenderShadow(false);
		// Önizlemede araç düz dursun ve kendi ekseninde dönsün.
		CarEntityRenderer.previewYaw = Mth.wrapDegrees(this.previewSpin);
		try {
			dispatcher.render(car, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, graphics.pose(), graphics.bufferSource(), 0xF000F0);
		} finally {
			CarEntityRenderer.previewYaw = null;
		}
		graphics.flush();
		dispatcher.setRenderShadow(true);
		graphics.pose().popPose();
		Lighting.setupFor3DItems();
		graphics.disableScissor();
	}

	@Override
	protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
		graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040);
		graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040);
	}
}
