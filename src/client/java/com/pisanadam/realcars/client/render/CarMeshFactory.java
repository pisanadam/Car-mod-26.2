package com.pisanadam.realcars.client.render;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarModel;
import com.pisanadam.realcars.entity.SpoilerType;
import com.pisanadam.realcars.entity.WheelType;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Araç gövdelerini {@link CarModel.Body} ölçülerinden parametrik olarak üretir.
 *
 * <p>12 aracın her biri için elle küp dizmek yerine tek bir şablon
 * parametrelendirilir; yeni bir araç eklemek yalnızca {@code CarModel}'e bir
 * satır yazmak demektir.
 *
 * <p>Model uzayı Minecraft'ın standardıdır: 16 birim = 1 blok, Y aşağı doğru
 * büyür (yani yerden yükseklik eksi Y'dir). Z ekseninde eksi yön aracın burnudur.
 *
 * <p>Parçalar iki köke ayrılır:
 * <ul>
 *   <li>{@code painted} — araç rengiyle boyanan paneller (ve rüzgarlık)</li>
 *   <li>{@code plain}   — cam, tampon, far, jant, lastik gibi boyanmayanlar</li>
 * </ul>
 * Renk {@code submitModelPart} çağrısında verildiği için bu ayrım tek dokudan
 * 16 farklı boya çıkarmayı sağlar.
 */
public final class CarMeshFactory {
	public static final String PAINTED = "painted";
	public static final String PLAIN = "plain";
	public static final String WHEEL_PREFIX = "wheel_";
	public static final String TIRE = "tire";
	public static final String SPOILER_PREFIX = "spoiler_";

	/** Tekerlek genişliği (birim). */
	private static final float WHEEL_WIDTH = 5.0F;

	private CarMeshFactory() {
	}

	public static LayerDefinition create(final CarModel model) {
		final MeshDefinition mesh = new MeshDefinition();
		final PartDefinition root = mesh.getRoot();
		final PartDefinition painted = root.addOrReplaceChild(PAINTED,
			CubeListBuilder.create(), PartPose.ZERO);
		final PartDefinition plain = root.addOrReplaceChild(PLAIN,
			CubeListBuilder.create(), PartPose.ZERO);

		final CarModel.Body body = model.body();
		buildPaintedShell(painted, model, body);
		buildSpoilers(painted, body);
		buildTrimAndGlass(plain, body);
		buildWheels(plain, body);

		return LayerDefinition.create(mesh, CarTexture.ATLAS_SIZE, CarTexture.ATLAS_SIZE);
	}

	// ----------------------------------------------------------------------
	// Boyanan paneller
	// ----------------------------------------------------------------------

	private static void buildPaintedShell(final PartDefinition painted, final CarModel model,
										  final CarModel.Body body) {
		final float halfWidth = body.width() / 2.0F;
		final float halfLength = body.length() / 2.0F;
		// Kamyonetlerde ana gövde yalnızca kabin+motor bölümünü kaplar,
		// arkasına ayrı bir kasa eklenir.
		final float shellRear = body.hasBed() ? halfLength - body.bedLength() : halfLength;

		addBox(painted, "hull", CarTexture.BODY,
			-halfWidth, top(body.clearance() + body.bodyHeight()), -halfLength,
			body.width(), body.bodyHeight(), shellRear + halfLength);

		// Kaput: gövdenin ön kısmında hafif alçak bir katman — burun profilini verir.
		final float noseLength = Math.min(20.0F, body.length() * 0.22F);
		addBox(painted, "nose", CarTexture.BODY,
			-halfWidth + 1.0F, top(body.clearance() + body.bodyHeight() + 2.0F), -halfLength + 0.5F,
			body.width() - 2.0F, 2.0F, noseLength);

		// Kabin (tavan)
		final float cabinHalf = body.cabinLength() / 2.0F;
		addBox(painted, "cabin", CarTexture.BODY,
			-halfWidth + 1.5F, top(body.clearance() + body.bodyHeight() + body.cabinHeight()),
			body.cabinZ() - cabinHalf,
			body.width() - 3.0F, body.cabinHeight(), body.cabinLength());

		if (body.hasBed()) {
			// Kasa: üç duvar (yanlar + arka kapak); ortası boş kalsın diye
			// tabanı ayrı, alçak bir kutu olarak eklenir.
			final float bedFront = shellRear;
			addBox(painted, "bed_floor", CarTexture.BODY,
				-halfWidth, top(body.clearance() + 4.0F), bedFront,
				body.width(), 3.0F, body.bedLength());
			addBox(painted, "bed_left", CarTexture.BODY,
				-halfWidth, top(body.clearance() + body.bodyHeight()), bedFront,
				3.0F, body.bodyHeight() - 4.0F, body.bedLength());
			addBox(painted, "bed_right", CarTexture.BODY,
				halfWidth - 3.0F, top(body.clearance() + body.bodyHeight()), bedFront,
				3.0F, body.bodyHeight() - 4.0F, body.bedLength());
			addBox(painted, "bed_tail", CarTexture.BODY,
				-halfWidth, top(body.clearance() + body.bodyHeight()), halfLength - 3.0F,
				body.width(), body.bodyHeight() - 4.0F, 3.0F);
		}
	}

	private static void buildSpoilers(final PartDefinition painted, final CarModel.Body body) {
		final float halfWidth = body.width() / 2.0F;
		final float halfLength = body.length() / 2.0F;
		final float deckTop = body.clearance() + body.bodyHeight();

		// Üçü de baştan üretilir; hangisinin görüneceğine render sırasında
		// karar verilir (parça görünürlüğü açılıp kapanır).
		addBox(painted, SPOILER_PREFIX + SpoilerType.SPOILER_LIP.itemName(), CarTexture.BODY,
			-halfWidth + 2.0F, top(deckTop + 2.0F), halfLength - 4.0F,
			body.width() - 4.0F, 2.0F, 4.0F);

		addBox(painted, SPOILER_PREFIX + SpoilerType.SPOILER_DUCKTAIL.itemName(), CarTexture.BODY,
			-halfWidth + 2.0F, top(deckTop + 5.0F), halfLength - 8.0F,
			body.width() - 4.0F, 5.0F, 8.0F);

		// GT kanadı tek parçada üç kutudan oluşur: kanat + iki ayak.
		checkFits("spoiler_gt", CarTexture.BODY, body.width(), 12.0F, 7.0F);
		painted.addOrReplaceChild(
			SPOILER_PREFIX + SpoilerType.SPOILER_GT.itemName(),
			CubeListBuilder.create()
				.texOffs(CarTexture.BODY.u(), CarTexture.BODY.v())
				.addBox(-halfWidth + 1.0F, top(deckTop + 14.0F), halfLength - 8.0F,
					body.width() - 2.0F, 3.0F, 7.0F)
				.addBox(-halfWidth + 4.0F, top(deckTop + 12.0F), halfLength - 6.0F,
					3.0F, 12.0F, 3.0F)
				.addBox(halfWidth - 7.0F, top(deckTop + 12.0F), halfLength - 6.0F,
					3.0F, 12.0F, 3.0F),
			PartPose.ZERO);
	}

	// ----------------------------------------------------------------------
	// Boyanmayan parçalar
	// ----------------------------------------------------------------------

	private static void buildTrimAndGlass(final PartDefinition plain, final CarModel.Body body) {
		final float halfWidth = body.width() / 2.0F;
		final float halfLength = body.length() / 2.0F;
		final float cabinHalf = body.cabinLength() / 2.0F;
		final float glassBottom = body.clearance() + body.bodyHeight();
		final float glassHeight = Math.max(3.0F, body.cabinHeight() - 3.0F);

		// Camlar: ön, arka ve iki yan. Kabinin biraz dışına taşarlar ki
		// gövdenin içinde kaybolmasınlar (z-fighting olmasın).
		addBox(plain, "glass_front", CarTexture.GLASS,
			-halfWidth + 2.0F, top(glassBottom + glassHeight), body.cabinZ() - cabinHalf - 0.2F,
			body.width() - 4.0F, glassHeight, 1.0F);
		addBox(plain, "glass_rear", CarTexture.GLASS,
			-halfWidth + 2.0F, top(glassBottom + glassHeight), body.cabinZ() + cabinHalf - 0.8F,
			body.width() - 4.0F, glassHeight, 1.0F);
		addBox(plain, "glass_left", CarTexture.GLASS,
			-halfWidth + 1.2F, top(glassBottom + glassHeight), body.cabinZ() - cabinHalf + 1.0F,
			1.0F, glassHeight, body.cabinLength() - 2.0F);
		addBox(plain, "glass_right", CarTexture.GLASS,
			halfWidth - 2.2F, top(glassBottom + glassHeight), body.cabinZ() - cabinHalf + 1.0F,
			1.0F, glassHeight, body.cabinLength() - 2.0F);

		// Tamponlar
		addBox(plain, "bumper_front", CarTexture.TRIM,
			-halfWidth, top(body.clearance() + 5.0F), -halfLength - 1.5F,
			body.width(), 5.0F, 2.5F);
		addBox(plain, "bumper_rear", CarTexture.TRIM,
			-halfWidth, top(body.clearance() + 5.0F), halfLength - 1.0F,
			body.width(), 5.0F, 2.5F);

		// Ön ızgara
		addBox(plain, "grille", CarTexture.GRILLE,
			-halfWidth + 4.0F, top(body.clearance() + body.bodyHeight() - 1.0F), -halfLength - 0.6F,
			body.width() - 8.0F, body.bodyHeight() - 6.0F, 1.0F);

		// Farlar ve stoplar
		final float lightY = top(body.clearance() + body.bodyHeight() - 1.5F);
		addBox(plain, "light_front_left", CarTexture.LIGHT_FRONT,
			-halfWidth + 1.5F, lightY, -halfLength - 0.8F, 6.0F, 3.0F, 1.0F);
		addBox(plain, "light_front_right", CarTexture.LIGHT_FRONT,
			halfWidth - 7.5F, lightY, -halfLength - 0.8F, 6.0F, 3.0F, 1.0F);
		addBox(plain, "light_rear_left", CarTexture.LIGHT_REAR,
			-halfWidth + 1.5F, lightY, halfLength - 0.2F, 6.0F, 3.0F, 1.0F);
		addBox(plain, "light_rear_right", CarTexture.LIGHT_REAR,
			halfWidth - 7.5F, lightY, halfLength - 0.2F, 6.0F, 3.0F, 1.0F);

		// Alt gövde — araç yandan bakıldığında havada durmasın
		addBox(plain, "underbody", CarTexture.UNDER,
			-halfWidth + 2.0F, top(body.clearance()), -halfLength + 2.0F,
			body.width() - 4.0F, 2.0F, body.length() - 4.0F);

		// Koltuklar (kabinin içinden görünür)
		addBox(plain, "seats", CarTexture.INTERIOR,
			-halfWidth + 4.0F, top(glassBottom + 3.0F), body.cabinZ() - cabinHalf + 2.0F,
			body.width() - 8.0F, 3.0F, body.cabinLength() - 4.0F);
	}

	private static void buildWheels(final PartDefinition plain, final CarModel.Body body) {
		final float halfBase = body.wheelbase() / 2.0F;
		// Tekerlek merkezi gövde altının biraz üstünde durur.
		final float axleY = top(body.clearance() * 0.55F + WheelType.WHEEL_STREET.radius() * 0.45F);
		final float xOffset = body.width() / 2.0F - WHEEL_WIDTH * 0.35F;

		for (final WheelPosition position : WheelPosition.values()) {
			final float x = position.right() ? xOffset : -xOffset - WHEEL_WIDTH * 0.3F;
			final float z = position.front() ? -halfBase : halfBase;
			final PartDefinition wheel = plain.addOrReplaceChild(WHEEL_PREFIX + position.partName(),
				CubeListBuilder.create(), PartPose.offset(x, axleY, z));

			// Lastik her tekerlekte aynıdır; jant tipi değişince yalnızca
			// görünür olan jant parçası değişir.
			final float tireRadius = WheelType.WHEEL_OFFROAD.radius();
			addBox(wheel, TIRE, CarTexture.TIRE,
				-WHEEL_WIDTH / 2.0F, -tireRadius, -tireRadius,
				WHEEL_WIDTH, tireRadius * 2.0F, tireRadius * 2.0F);

			for (final WheelType type : WheelType.values()) {
				final float rimRadius = type.radius() * 0.62F;
				addBox(wheel, type.itemName(), CarTexture.rimFor(type),
					-WHEEL_WIDTH / 2.0F - 0.4F, -rimRadius, -rimRadius,
					WHEEL_WIDTH + 0.8F, rimRadius * 2.0F, rimRadius * 2.0F);
			}
		}
	}

	/** Dört tekerlek konumu. */
	public enum WheelPosition {
		FRONT_LEFT(true, false),
		FRONT_RIGHT(true, true),
		REAR_LEFT(false, false),
		REAR_RIGHT(false, true);

		private final boolean front;
		private final boolean right;

		WheelPosition(final boolean front, final boolean right) {
			this.front = front;
			this.right = right;
		}

		public boolean front() {
			return this.front;
		}

		public boolean right() {
			return this.right;
		}

		public String partName() {
			return this.name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	// ----------------------------------------------------------------------
	// Yardımcılar
	// ----------------------------------------------------------------------

	/** Yerden {@code heightUnits} yükseklikteki bir kutunun model-uzayı Y'si. */
	private static float top(final float heightUnits) {
		return -heightUnits;
	}

	private static void addBox(final PartDefinition parent, final String name, final CarTexture region,
							   final float x, final float y, final float z,
							   final float sizeX, final float sizeY, final float sizeZ) {
		checkFits(name, region, sizeX, sizeY, sizeZ);
		parent.addOrReplaceChild(name,
			CubeListBuilder.create()
				.texOffs(region.u(), region.v())
				.addBox(x, y, z, sizeX, sizeY, sizeZ),
			PartPose.ZERO);
	}

	/**
	 * Kutunun UV ayak izi malzeme bölgesine sığmazsa komşu bölgeden örneklenir
	 * ve yanlış doku görünür. Sessizce bozulmaması için uyarı basılır.
	 */
	private static void checkFits(final String name, final CarTexture region,
								  final float sizeX, final float sizeY, final float sizeZ) {
		if (!region.fits(sizeX, sizeY, sizeZ)) {
			RealCars.LOGGER.warn(
				"Araç parçası '{}' doku bölgesine sığmıyor: gerekli {}x{}, bölge {}x{}",
				name, 2.0F * (sizeX + sizeZ), sizeY + sizeZ, region.width(), region.height());
		}
	}
}
