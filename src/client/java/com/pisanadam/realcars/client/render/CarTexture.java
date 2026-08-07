package com.pisanadam.realcars.client.render;

import com.pisanadam.realcars.RealCars;
import net.minecraft.resources.Identifier;

/**
 * Araç gövde atlasındaki malzeme bölgeleri.
 *
 * <p>Her bölge kendi içinde düzgün desenlidir; bu yüzden bir kutunun UV'sinin
 * bölge içinde tam olarak nereye düştüğü fark etmez, tek koşul ayak izinin
 * bölgeye sığmasıdır. Bu sayede 12 aracın gövdesi elle UV açmadan, tamamen
 * parametrik olarak üretilebiliyor.
 *
 * <p>Değerler {@code tools/gen_textures.py} içindeki ATLAS_REGIONS ile
 * birebir aynı olmalıdır.
 */
public record CarTexture(int u, int v, int width, int height) {
	public static final int ATLAS_SIZE = 512;
	public static final Identifier ATLAS = RealCars.id("textures/entity/car/base.png");

	public static final CarTexture BODY = new CarTexture(0, 0, 256, 128);
	public static final CarTexture GLASS = new CarTexture(256, 0, 256, 128);
	public static final CarTexture TRIM = new CarTexture(0, 128, 256, 128);
	public static final CarTexture INTERIOR = new CarTexture(256, 128, 256, 128);
	public static final CarTexture TIRE = new CarTexture(0, 256, 128, 128);
	public static final CarTexture GRILLE = new CarTexture(128, 256, 128, 128);
	/** Alt gövde araç boyunda olduğu için geniş bir bölge gerektirir. */
	public static final CarTexture UNDER = new CarTexture(256, 256, 256, 128);
	public static final CarTexture RIM_STREET = new CarTexture(0, 384, 64, 64);
	public static final CarTexture RIM_SPORT = new CarTexture(64, 384, 64, 64);
	public static final CarTexture RIM_OFFROAD = new CarTexture(128, 384, 64, 64);
	public static final CarTexture RIM_CHROME = new CarTexture(192, 384, 64, 64);
	public static final CarTexture LIGHT_FRONT = new CarTexture(256, 384, 64, 64);
	public static final CarTexture LIGHT_REAR = new CarTexture(320, 384, 64, 64);
	public static final CarTexture CHROME = new CarTexture(384, 384, 128, 128);

	/**
	 * Verilen ölçülerdeki bir kutunun UV ayak izi bu bölgeye sığıyor mu?
	 * Sığmazsa kutu komşu bölgeden örnekler ve yanlış malzeme görünür.
	 */
	public boolean fits(final float sizeX, final float sizeY, final float sizeZ) {
		return 2.0F * (sizeX + sizeZ) <= this.width && (sizeY + sizeZ) <= this.height;
	}

	public static CarTexture rimFor(final com.pisanadam.realcars.entity.WheelType wheel) {
		return switch (wheel) {
			case WHEEL_STREET -> RIM_STREET;
			case WHEEL_SPORT -> RIM_SPORT;
			case WHEEL_OFFROAD -> RIM_OFFROAD;
			case WHEEL_CHROME -> RIM_CHROME;
		};
	}
}
