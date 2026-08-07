package com.pisanadam.realcars.client.hud;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.GearBox;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Araca binince görünen km/saat göstergesi.
 *
 * <p>Kadran, ibre, dijital hız, vites, devir çubuğu ve yakıt göstergesinden
 * oluşur. İbre ve devir çubuğu tick başına zıplamasın diye kare arası
 * yumuşatılır: hedef değere her karede biraz yaklaşılır.
 */
public class SpeedometerHud implements HudElement {
	public static final Identifier ID = RealCars.id("speedometer");

	private static final Identifier DIAL = RealCars.id("textures/gui/speedometer_dial.png");
	private static final Identifier NEEDLE = RealCars.id("textures/gui/speedometer_needle.png");

	private static final int DIAL_SIZE = 128;
	/** Kadranın ekranda kaplayacağı kare kenar uzunluğu. */
	private static final int DIAL_DRAW = 84;
	private static final float DIAL_SCALE = (float) DIAL_DRAW / DIAL_SIZE;
	private static final int NEEDLE_W = 16;
	private static final int NEEDLE_H = 64;
	/** İbre dokusunda dönme merkezinin (göbeğin) bulunduğu satır. */
	private static final int NEEDLE_PIVOT_Y = 59;

	/** Kadranın yay başlangıcı ve süpürdüğü açı (derece) — doku ile aynı. */
	private static final float DIAL_START_DEG = 160.0F;
	private static final float DIAL_SWEEP_DEG = 220.0F;

	private static final int MARGIN_RIGHT = 8;
	/** Devir ve yakıt çubukları hotbar hizasına düşmesin diye alt boşluk geniştir. */
	private static final int MARGIN_BOTTOM = 30;

	private float shownSpeed;
	private float shownRpm;

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
		// HUD gizliyse (F1) bu metot hiç çağrılmaz, ayrıca kontrol gerekmez.
		final Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		if (!(client.player.getVehicle() instanceof CarEntity car)) {
			this.shownSpeed = 0.0F;
			this.shownRpm = GearBox.IDLE_RPM;
			return;
		}

		// Kare hızından bağımsız yumuşatma
		final float blend = Mth.clamp(deltaTracker.getGameTimeDeltaTicks() * 0.35F, 0.02F, 1.0F);
		this.shownSpeed = Mth.lerp(blend, this.shownSpeed, Math.abs(car.speedKmh()));
		this.shownRpm = Mth.lerp(blend, this.shownRpm, car.rpm());

		final int right = graphics.guiWidth() - MARGIN_RIGHT;
		final int bottom = graphics.guiHeight() - MARGIN_BOTTOM;
		final int dialX = right - DIAL_DRAW;
		final int dialY = bottom - DIAL_DRAW;

		this.drawDial(graphics, dialX, dialY, car);
		this.drawReadout(graphics, client.font, dialX, dialY, car);
		this.drawBars(graphics, client.font, dialX, dialY, car);
	}

	private void drawDial(final GuiGraphicsExtractor graphics, final int x, final int y, final CarEntity car) {
		// blit'in genişlik/yükseklik parametreleri hem ekrandaki hem dokudaki
		// bölgeyi belirtir, yani küçültmez — kırpar. Bu yüzden doku tam
		// boyunda çizilip matris ile ölçekleniyor.
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(DIAL_SCALE, DIAL_SCALE);
		graphics.blit(RenderPipelines.GUI_TEXTURED, DIAL, 0, 0, 0.0F, 0.0F,
			DIAL_SIZE, DIAL_SIZE, DIAL_SIZE, DIAL_SIZE);
		graphics.pose().popMatrix();

		final float fraction = Mth.clamp(this.shownSpeed / Math.max(1.0F, car.topSpeedKmh()), 0.0F, 1.0F);
		// Kadran çentikleri ekran uzayında (Y aşağı) 0 = sağ olacak şekilde
		// yerleştirildi. İbre dokusu yukarı, yani 270 dereceye bakıyor; istenen
		// yöne dönmesi için saat yönünde (açı - 270), yani (açı + 90) gerekir.
		final float angle = DIAL_START_DEG + DIAL_SWEEP_DEG * fraction + 90.0F;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x + DIAL_DRAW / 2.0F, y + DIAL_DRAW / 2.0F);
		graphics.pose().rotate(angle * Mth.DEG_TO_RAD);
		graphics.pose().scale(DIAL_SCALE, DIAL_SCALE);
		graphics.blit(RenderPipelines.GUI_TEXTURED, NEEDLE,
			-NEEDLE_W / 2, -NEEDLE_PIVOT_Y, 0.0F, 0.0F,
			NEEDLE_W, NEEDLE_H, NEEDLE_W, NEEDLE_H);
		graphics.pose().popMatrix();
	}

	private void drawReadout(final GuiGraphicsExtractor graphics, final Font font,
							 final int x, final int y, final CarEntity car) {
		final int centreX = x + DIAL_DRAW / 2;
		final String speed = String.valueOf(Math.round(this.shownSpeed));
		final int speedColor = this.shownSpeed > car.topSpeedKmh() * 0.85F ? 0xFFE05038 : 0xFFF2F4F8;

		graphics.pose().pushMatrix();
		graphics.pose().translate(centreX, y + DIAL_DRAW / 2 + 8);
		graphics.pose().scale(1.6F, 1.6F);
		graphics.centeredText(font, speed, 0, 0, speedColor);
		graphics.pose().popMatrix();

		graphics.centeredText(font, Component.translatable("hud.realcars.kmh"),
			centreX, y + DIAL_DRAW / 2 + 24, 0xFF9AA2AE);
		graphics.centeredText(font, gearLabel(car), centreX, y + DIAL_DRAW / 2 - 24, 0xFFE6C33A);
	}

	private static Component gearLabel(final CarEntity car) {
		final int gear = car.gear();
		if (gear == GearBox.REVERSE) {
			return Component.translatable("hud.realcars.gear.reverse");
		}
		if (gear == GearBox.NEUTRAL) {
			return Component.translatable("hud.realcars.gear.neutral");
		}
		return Component.literal(String.valueOf(gear));
	}

	private void drawBars(final GuiGraphicsExtractor graphics, final Font font,
						  final int x, final int y, final CarEntity car) {
		final int barX = x - 66;
		final int barW = 60;
		final int rpmY = y + DIAL_DRAW - 30;
		final int fuelY = y + DIAL_DRAW - 14;

		// Devir çubuğu — kırmızı bölgede rengi değişir
		final float redline = car.engineType().redlineRpm();
		final float rpmFraction = Mth.clamp(this.shownRpm / redline, 0.0F, 1.0F);
		graphics.fill(barX - 1, rpmY - 1, barX + barW + 1, rpmY + 7, 0xC0101317);
		graphics.fill(barX, rpmY, barX + barW, rpmY + 6, 0xFF23272E);
		final int rpmColor = rpmFraction > 0.85F ? 0xFFDD3A2C : 0xFF5BC46A;
		graphics.fill(barX, rpmY, barX + (int) (barW * rpmFraction), rpmY + 6, rpmColor);
		// Kırmızı bölgenin başladığı yeri işaretle
		final int redlineX = barX + (int) (barW * 0.85F);
		graphics.fill(redlineX, rpmY - 1, redlineX + 1, rpmY + 7, 0xFFDD3A2C);
		graphics.text(font, Component.translatable("hud.realcars.rpm"),
			barX + barW + 4, rpmY - 1, 0xFF9AA2AE);

		// Yakıt göstergesi
		final float fuelFraction = Mth.clamp(car.fuel() / car.model().fuelCapacity(), 0.0F, 1.0F);
		graphics.fill(barX - 1, fuelY - 1, barX + barW + 1, fuelY + 7, 0xC0101317);
		graphics.fill(barX, fuelY, barX + barW, fuelY + 6, 0xFF23272E);
		final int fuelColor = fuelFraction < 0.15F ? 0xFFDD3A2C : 0xFF4E9BE0;
		graphics.fill(barX, fuelY, barX + (int) (barW * fuelFraction), fuelY + 6, fuelColor);
		graphics.text(font, Component.translatable("hud.realcars.fuel"),
			barX + barW + 4, fuelY - 1, 0xFF9AA2AE);

		if (!car.engineOn()) {
			graphics.centeredText(font, Component.translatable("message.realcars.engine_off"),
				x + DIAL_DRAW / 2, y - 10, 0xFFDD3A2C);
		}
	}
}
