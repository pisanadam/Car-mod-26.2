package com.pisanadam.realcars.client.render;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarModel;
import com.pisanadam.realcars.entity.Silhouette;
import com.pisanadam.realcars.entity.SpoilerType;
import com.pisanadam.realcars.entity.WheelType;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Araç gövdelerini {@link CarModel.Body} yan profilinden parametrik olarak
 * üretir.
 *
 * <p><b>Eğimli yüzeyler nasıl oluyor?</b> Minecraft yalnızca eksen hizalı kutu
 * üretebilir, ama {@link PartPose#offsetAndRotation} bir parçanın tamamını
 * döndürür. Bu yüzden her eğim = uygun açıyla döndürülmüş ince bir panel
 * ("slab"). Vanilla teknenin eğimli bordası da tam olarak böyle yapılır.
 * Panelin altında kalan boşluk, dışarıdan görünmeyecek kadar ince basamaklarla
 * ({@link #wedge}) doldurulur; basamak yüksekliği panel kalınlığının yarısını
 * geçmediği için merdiven dışarı taşmaz.
 *
 * <p>Gövde, aracın yan profilini veren noktalardan kurulur: burun → kaput →
 * ön cam → tavan → arka cam → bagaj → kuyruk. {@link Silhouette} her bölümün
 * kaç panele bölüneceğini ve ne kadar kavis yapacağını söyler; böylece aynı
 * kod hem Beetle'ın kesintisiz kavisini hem Defender'ın dik duvarını üretir.
 *
 * <p>Model uzayı Minecraft'ın standardıdır: 16 birim = 1 blok, Y aşağı doğru
 * büyür (yani yerden yükseklik eksi Y'dir). Z ekseninde eksi yön aracın burnudur.
 *
 * <p>Parçalar dört köke ayrılır:
 * <ul>
 *   <li>{@code painted} — araç rengiyle boyanan paneller (ve rüzgarlık)</li>
 *   <li>{@code plain}   — cam, tampon, ızgara, iç mekan gibi boyanmayanlar</li>
 *   <li>{@code wheels}  — süspansiyon hareketinden etkilenmeyen tekerlekler</li>
 *   <li>{@code lights}  — gece parlayan farlar ve stop lambaları</li>
 * </ul>
 */
public final class CarMeshFactory {
	public static final String PAINTED = "painted";
	public static final String PLAIN = "plain";
	public static final String WHEELS = "wheels";
	public static final String LIGHTS = "lights";
	public static final String WHEEL_PREFIX = "wheel_";
	public static final String SPOILER_PREFIX = "spoiler_";
	public static final String HEADLIGHTS = "headlights";
	public static final String BRAKELIGHTS = "brakelights";

	/** Tekerlek genişliği (birim). */
	private static final float WHEEL_WIDTH = 5.0F;
	/** Eğimli panellerin kalınlığı. */
	private static final float PANEL = 1.6F;
	/** Cam kalınlığı. */
	private static final float GLASS = 1.0F;
	/**
	 * Panel altını dolduran basamakların azami yüksekliği. Panel kalınlığının
	 * yarısından küçük olduğu sürece merdiven panelin dışına taşmaz.
	 */
	private static final float MAX_STEP = PANEL * 0.30F;
	/** Basamak tepeleri panelin orta yüzeyinin bu kadar altına gömülür. */
	private static final float STEP_SINK = PANEL * 0.30F;
	/**
	 * Tekerleği oluşturan şerit sayısı; sonuç {@code 2 x} kenarlı bir çokgendir.
	 * Altı şerit 12 kenar demek: yarıçap en fazla %3 dalgalanır, yani tekerlek
	 * dönerken gözle yuvarlak görünür.
	 */
	private static final int WHEEL_FACETS = 6;
	/** Şeridin uzun yarı ölçüsü: köşeleri tam çemberin üstüne düşürür. */
	private static final float FACET_LONG = Mth.cos(Mth.PI / (2.0F * WHEEL_FACETS));
	/** Şeridin ince yarı ölçüsü. */
	private static final float FACET_THIN = Mth.sin(Mth.PI / (2.0F * WHEEL_FACETS));
	/**
	 * Çamurluk boşluğunda gövdenin her yandan içeri çekildiği miktar. Tekerlek
	 * bu boşluğa oturur; yoksa gövde tam genişlikte kalır ve lastiğin yalnızca
	 * ince bir dilimi dışarı taşar.
	 */
	private static final float ARCH_INSET = 3.8F;

	private CarMeshFactory() {
	}

	public static LayerDefinition create(final CarModel model) {
		final MeshDefinition mesh = new MeshDefinition();
		final PartDefinition root = mesh.getRoot();
		final PartDefinition painted = root.addOrReplaceChild(PAINTED, CubeListBuilder.create(), PartPose.ZERO);
		final PartDefinition plain = root.addOrReplaceChild(PLAIN, CubeListBuilder.create(), PartPose.ZERO);
		final PartDefinition wheels = root.addOrReplaceChild(WHEELS, CubeListBuilder.create(), PartPose.ZERO);
		final PartDefinition lights = root.addOrReplaceChild(LIGHTS, CubeListBuilder.create(), PartPose.ZERO);

		final CarModel.Body body = model.body();
		buildLowerBody(painted, model, body);
		buildUpperPanels(painted, body);
		buildGreenhouse(painted, plain, body);
		buildFendersAndSills(painted, model, body);
		if (body.hasBed()) {
			buildBed(painted, plain, body);
		}
		buildSpoilers(painted, body);
		buildTrim(plain, body);
		buildLamps(plain, lights, body);
		buildWheels(wheels, body);

		return LayerDefinition.create(mesh, CarTexture.ATLAS_SIZE, CarTexture.ATLAS_SIZE);
	}

	// ----------------------------------------------------------------------
	// Alt gövde: cam altı hattına kadar olan, tam genişlikteki kütle
	// ----------------------------------------------------------------------

	/**
	 * Gövdeyi tabandan cam altı hattına kadar doldurur. Tavanı, yan profilin
	 * cam altı hattıyla sınırlanmış hâlidir: burunda kaputun eğimini izler,
	 * kabin boyunca düzdür, kuyrukta yeniden alçalır.
	 *
	 * <p>Gövde yatay olarak da ikiye ayrılır. Çamurluk kemerinin tepesinden
	 * yukarısı her yerde tam genişliktir; aşağısı ise tekerleklerin hizasında
	 * {@link #ARCH_INSET} kadar içeri çekilir. Minecraft kutudan malzeme
	 * çıkaramadığı için tekerlek boşluğu ancak böyle, o bölgeyi hiç doldurmayarak
	 * açılabilir. Aksi hâlde gövde tekerleğin önünü kapatır ve lastiğin yalnızca
	 * ince bir dilimi görünür.
	 */
	private static void buildLowerBody(final PartDefinition painted, final CarModel model,
									   final CarModel.Body body) {
		final float belt = body.beltHeight();
		final float base = body.clearance();
		final float archTop = archTop(model);
		final float archHalf = archHalf(model);
		final CubeListBuilder hull = CubeListBuilder.create();

		// --- kemer tepesinin üstü: her yerde tam genişlik ---
		fillBand(hull, "hull_upper", body, -body.halfLength(), body.halfLength(),
			archTop, belt, body.width());

		// --- kemer tepesinin altı: tekerlek hizasında içeri çekilir ---
		float z = -body.halfLength();
		for (final float[] window : archWindows(body, archHalf)) {
			fillBand(hull, "hull_lower", body, z, window[0], base, archTop, body.width());
			fillBand(hull, "hull_arch", body, window[0], window[1], base, archTop,
				body.width() - 2.0F * ARCH_INSET);
			z = window[1];
		}
		fillBand(hull, "hull_lower", body, z, body.halfLength(), base, archTop, body.width());

		painted.addOrReplaceChild("hull", hull, PartPose.ZERO);
	}

	/** Çamurluk kemerinin tepe yüksekliği — fabrika tekerleğinin çapından çıkar. */
	private static float archTop(final CarModel model) {
		return 2.0F * model.defaultWheel().radius() + 1.2F;
	}

	/** Kemer boşluğunun aks etrafında Z yönünde yarı uzunluğu. */
	private static float archHalf(final CarModel model) {
		return model.defaultWheel().radius() + 3.0F;
	}

	/** İki aksın çevresindeki, gövdenin içeri çekildiği Z aralıkları. */
	private static java.util.List<float[]> archWindows(final CarModel.Body body, final float archHalf) {
		final float halfL = body.halfLength();
		final java.util.List<float[]> windows = new java.util.ArrayList<>(2);
		for (final float axle : new float[] {body.frontAxleZ(), body.rearAxleZ()}) {
			final float from = Mth.clamp(axle - archHalf, -halfL, halfL);
			final float to = Mth.clamp(axle + archHalf, -halfL, halfL);
			if (to - from > 0.1F) {
				windows.add(new float[] {from, to});
			}
		}
		return windows;
	}

	/**
	 * {@code z0..z1} arasını, tabandan yan profile (en çok {@code capHeight}'a)
	 * kadar doldurur. Profil parçalı doğrusal olduğu için önce kırılma
	 * noktalarından bölünür, sonra her düz parça basamaklı bir kamaya çevrilir.
	 */
	private static void fillBand(final CubeListBuilder out, final String name, final CarModel.Body body,
								 final float z0, final float z1, final float baseHeight,
								 final float capHeight, final float width) {
		if (z1 - z0 < 0.05F || width <= 0.0F) {
			return;
		}
		float from = z0;
		for (final float breakZ : profileBreaks(body)) {
			if (breakZ <= from + 0.05F || breakZ >= z1 - 0.05F) {
				continue;
			}
			fillPiece(out, name, body, from, breakZ, baseHeight, capHeight, width);
			from = breakZ;
		}
		fillPiece(out, name, body, from, z1, baseHeight, capHeight, width);
	}

	private static void fillPiece(final CubeListBuilder out, final String name, final CarModel.Body body,
								  final float z0, final float z1, final float baseHeight,
								  final float capHeight, final float width) {
		final float h0 = Math.min(body.profileHeight(z0), capHeight);
		final float h1 = Math.min(body.profileHeight(z1), capHeight);
		// Basamaklar yalnızca eğimli bir panelin (kaput ya da bagaj kapağı)
		// altındayken gömülür; bir üst bandın dibinde gömülürlerse arada
		// görünür bir boşluk açılırdı.
		final boolean underSlab = z1 <= body.cowlZ() + 0.05F || z0 >= body.deckZ() - 0.05F;
		final float sink = underSlab ? STEP_SINK : 0.0F;
		wedge(out, name, CarTexture.BODY, z0, h0, z1, h1, baseHeight, width, sink);
	}

	/** Yan profilin kırılma noktaları. */
	private static float[] profileBreaks(final CarModel.Body body) {
		return new float[] {body.cowlZ(), body.roofFrontZ(), body.roofRearZ(), body.deckZ()};
	}

	// ----------------------------------------------------------------------
	// Eğimli üst paneller: kaput ve bagaj kapağı
	// ----------------------------------------------------------------------

	private static void buildUpperPanels(final PartDefinition painted, final CarModel.Body body) {
		// Kaput — burnun üstünü kapatan eğimli panel(ler)
		int index = 0;
		for (final float[] seg : chain(body, Section.NOSE)) {
			slab(painted, "bonnet_" + index++, CarTexture.BODY,
				seg[0], seg[1], seg[2], seg[3], body.width() - 1.0F, PANEL);
		}

		// Bagaj kapağı / kuyruk paneli
		index = 0;
		final CubeListBuilder deckFill = CubeListBuilder.create()
			.texOffs(CarTexture.BODY.u(), CarTexture.BODY.v());
		for (final float[] seg : chain(body, Section.DECK)) {
			slab(painted, "deck_" + index++, CarTexture.BODY,
				seg[0], seg[1], seg[2], seg[3], body.width() - 1.0F, PANEL);
			// Bagaj kapağı cam altı hattının üstündeyse altını doldur
			wedge(deckFill, "deck_fill", CarTexture.BODY, seg[0], seg[1], seg[2], seg[3],
				body.beltHeight() - 0.5F, body.width() - 1.5F, STEP_SINK);
		}
		painted.addOrReplaceChild("deck_fill", deckFill, PartPose.ZERO);
	}

	// ----------------------------------------------------------------------
	// Kabin: ön cam, tavan, arka cam, direkler, yan camlar
	// ----------------------------------------------------------------------

	private static void buildGreenhouse(final PartDefinition painted, final PartDefinition plain,
										final CarModel.Body body) {
		final float roofW = body.roofWidth();
		final float pillarX = roofW / 2.0F - 1.1F;
		final float belt = body.beltHeight();

		// --- iç mekan: camdan bakınca karanlık görünsün, öbür yan görünmesin ---
		final CubeListBuilder core = CubeListBuilder.create()
			.texOffs(CarTexture.INTERIOR.u(), CarTexture.INTERIOR.v());
		final float coreW = Math.max(4.0F, roofW - 3.4F);
		for (final float[] seg : chain(body, Section.WINDSCREEN)) {
			wedge(core, "cabin_core", CarTexture.INTERIOR, seg[0], seg[1] - 1.4F, seg[2], seg[3] - 1.4F,
				belt - 2.0F, coreW, STEP_SINK);
		}
		box(core, "cabin_core", CarTexture.INTERIOR, -coreW / 2.0F, body.roofHeight() - 1.4F,
			body.roofFrontZ(), coreW, body.roofHeight() - 1.4F - (belt - 2.0F),
			body.roofRearZ() - body.roofFrontZ());
		for (final float[] seg : chain(body, Section.BACKLIGHT)) {
			wedge(core, "cabin_core", CarTexture.INTERIOR, seg[0], seg[1] - 1.4F, seg[2], seg[3] - 1.4F,
				belt - 2.0F, coreW, STEP_SINK);
		}
		plain.addOrReplaceChild("cabin_core", core, PartPose.ZERO);

		// --- ön cam + A direkleri ---
		int index = 0;
		for (final float[] seg : chain(body, Section.WINDSCREEN)) {
			slab(plain, "glass_front_" + index, CarTexture.GLASS,
				seg[0], seg[1], seg[2], seg[3], roofW - 2.6F, GLASS);
			slabPair(painted, "pillar_a_" + index, CarTexture.BODY,
				seg[0], seg[1], seg[2], seg[3], pillarX, 2.4F, PANEL);
			index++;
		}

		// --- tavan (hafif kubbeli) ---
		index = 0;
		for (final float[] seg : chain(body, Section.ROOF)) {
			slab(painted, "roof_" + index++, CarTexture.BODY,
				seg[0], seg[1], seg[2], seg[3], roofW, PANEL);
		}

		// --- arka cam + C direkleri ---
		index = 0;
		for (final float[] seg : chain(body, Section.BACKLIGHT)) {
			slab(plain, "glass_rear_" + index, CarTexture.GLASS,
				seg[0], seg[1], seg[2], seg[3], roofW - 2.6F, GLASS);
			slabPair(painted, "pillar_c_" + index, CarTexture.BODY,
				seg[0], seg[1], seg[2], seg[3], pillarX, 2.4F, PANEL);
			index++;
		}

		// --- yan camlar ve B direği ---
		final float glassTop = body.roofHeight() - 1.6F;
		final float glassBottom = belt + 0.4F;
		final float glassX = roofW / 2.0F - 0.8F;
		final float cabinFront = body.roofFrontZ() + 0.8F;
		final float cabinRear = body.roofRearZ() - 0.8F;
		final float pillarB = Mth.lerp(0.46F, cabinFront, cabinRear);

		if (cabinRear - cabinFront > 6.0F && glassTop > glassBottom) {
			sideBoxPair(plain, "glass_side_front", CarTexture.GLASS, glassX, GLASS,
				glassBottom, glassTop, cabinFront, pillarB - 1.1F);
			sideBoxPair(plain, "glass_side_rear", CarTexture.GLASS, glassX, GLASS,
				glassBottom, glassTop, pillarB + 1.1F, cabinRear);
			sideBoxPair(painted, "pillar_b", CarTexture.BODY, pillarX, 2.4F,
				belt, body.roofHeight(), pillarB - 1.1F, pillarB + 1.1F);
		} else if (glassTop > glassBottom) {
			sideBoxPair(plain, "glass_side_front", CarTexture.GLASS, glassX, GLASS,
				glassBottom, glassTop, cabinFront, cabinRear);
		}
	}

	// ----------------------------------------------------------------------
	// Çamurluk kabartmaları, marşpiyel, kapı ayrım çizgileri
	// ----------------------------------------------------------------------

	private static void buildFendersAndSills(final PartDefinition painted, final CarModel model,
											 final CarModel.Body body) {
		final float halfW = body.halfWidth();
		final float flare = body.archFlare();
		// Gövdedeki boşlukla aynı ölçüler: dudak tam tekerleğin üstüne oturur.
		final float archTop = archTop(model);
		final float archHalf = archHalf(model);

		final CubeListBuilder fenders = CubeListBuilder.create()
			.texOffs(CarTexture.BODY.u(), CarTexture.BODY.v());
		for (final float side : new float[] {-1.0F, 1.0F}) {
			final float x0 = side > 0.0F ? halfW - 1.0F : -halfW + 1.0F - flare;
			for (final float axle : new float[] {body.frontAxleZ(), body.rearAxleZ()}) {
				// Kemerin tepesi: tekerleğin üstünden geçen yatay dudak
				box(fenders, "fender", CarTexture.BODY, x0, archTop + 2.2F, axle - archHalf,
					flare, 2.2F, archHalf * 2.0F);
				// Kemerin iki ucu: dudağı gövdeye bağlayan kısa dikmeler
				box(fenders, "fender", CarTexture.BODY, x0, archTop, axle - archHalf - 1.8F,
					flare, 3.6F, 1.8F);
				box(fenders, "fender", CarTexture.BODY, x0, archTop, axle + archHalf,
					flare, 3.6F, 1.8F);
			}
			// Marşpiyel — kapı altı eşiği, iki aks arasını bağlar
			box(fenders, "sill", CarTexture.BODY, x0, body.clearance() + 2.4F,
				body.frontAxleZ() + archHalf, flare * 0.7F, 3.2F,
				body.rearAxleZ() - body.frontAxleZ() - archHalf * 2.0F);
		}
		painted.addOrReplaceChild("fenders", fenders, PartPose.ZERO);

		// Kapı ayrım çizgileri — gövdeden hafifçe taşan koyu oyuklar
		final CubeListBuilder seams = CubeListBuilder.create()
			.texOffs(CarTexture.BODY_DARK.u(), CarTexture.BODY_DARK.v());
		final float seamTop = body.beltHeight();
		final float seamBottom = body.clearance() + 2.0F;
		for (final float side : new float[] {-1.0F, 1.0F}) {
			final float x = side > 0.0F ? halfW - 0.15F : -halfW - 0.15F;
			for (final float z : doorSeams(body)) {
				box(seams, "seam", CarTexture.BODY_DARK, x, seamTop, z - 0.5F,
					0.3F, seamTop - seamBottom, 1.0F);
			}
		}
		painted.addOrReplaceChild("seams", seams, PartPose.ZERO);
	}

	/** Kapı ayrım çizgilerinin Z konumları (kabinin uzunluğuna göre 2 ya da 3 tane). */
	private static float[] doorSeams(final CarModel.Body body) {
		final float front = body.cowlZ();
		final float rear = Math.min(body.deckZ(), body.halfLength() - 3.0F);
		if (rear - front < 22.0F) {
			return new float[] {front, rear};
		}
		return new float[] {front, Mth.lerp(0.5F, front, rear), rear};
	}

	// ----------------------------------------------------------------------
	// Kamyonet kasası
	// ----------------------------------------------------------------------

	private static void buildBed(final PartDefinition painted, final PartDefinition plain,
								 final CarModel.Body body) {
		final float halfW = body.halfWidth();
		final float front = body.deckZ();
		final float rear = body.halfLength();
		final float floor = body.beltHeight();
		final float wall = 6.5F;

		final CubeListBuilder bed = CubeListBuilder.create()
			.texOffs(CarTexture.BODY.u(), CarTexture.BODY.v());
		box(bed, "bed_left", CarTexture.BODY, -halfW, floor + wall, front, 3.0F, wall, rear - front);
		box(bed, "bed_right", CarTexture.BODY, halfW - 3.0F, floor + wall, front, 3.0F, wall, rear - front);
		box(bed, "bed_tail", CarTexture.BODY, -halfW, floor + wall, rear - 2.5F, body.width(), wall, 2.5F);
		box(bed, "bed_head", CarTexture.BODY, -halfW, floor + wall + 1.5F, front,
			body.width(), wall + 1.5F, 2.5F);
		painted.addOrReplaceChild("bed", bed, PartPose.ZERO);

		// Kasa tabanı — oluklu sac
		addBox(plain, "bed_floor", CarTexture.UNDER, -halfW + 3.0F, floor + 0.6F, front + 2.5F,
			body.width() - 6.0F, 0.8F, rear - front - 5.0F);
	}

	// ----------------------------------------------------------------------
	// Rüzgarlıklar
	// ----------------------------------------------------------------------

	private static void buildSpoilers(final PartDefinition painted, final CarModel.Body body) {
		final float halfW = body.halfWidth();
		final float halfL = body.halfLength();
		// Kanat, bagaj kapağının kuyruk ucuna oturur.
		final float deckTop = body.hasBed() ? body.beltHeight() + 6.5F : body.tailHeight();
		final float z = halfL - 8.0F;

		// Üçü de baştan üretilir; hangisinin görüneceğine render sırasında
		// karar verilir (parça görünürlüğü açılıp kapanır).
		addBox(painted, SPOILER_PREFIX + SpoilerType.SPOILER_LIP.itemName(), CarTexture.BODY,
			-halfW + 2.0F, deckTop + 2.0F, halfL - 4.5F, body.width() - 4.0F, 2.0F, 4.0F);

		addBox(painted, SPOILER_PREFIX + SpoilerType.SPOILER_DUCKTAIL.itemName(), CarTexture.BODY,
			-halfW + 2.0F, deckTop + 5.0F, halfL - 9.0F, body.width() - 4.0F, 5.0F, 9.0F);

		// GT kanadı tek parçada üç kutudan oluşur: kanat + iki ayak.
		final CubeListBuilder gt = CubeListBuilder.create()
			.texOffs(CarTexture.BODY.u(), CarTexture.BODY.v());
		box(gt, "spoiler_gt", CarTexture.BODY, -halfW + 1.0F, deckTop + 14.0F, z, body.width() - 2.0F, 3.0F, 7.0F);
		box(gt, "spoiler_gt", CarTexture.BODY, -halfW + 4.0F, deckTop + 12.0F, z + 2.0F, 3.0F, 12.0F, 3.0F);
		box(gt, "spoiler_gt", CarTexture.BODY, halfW - 7.0F, deckTop + 12.0F, z + 2.0F, 3.0F, 12.0F, 3.0F);
		painted.addOrReplaceChild(SPOILER_PREFIX + SpoilerType.SPOILER_GT.itemName(), gt, PartPose.ZERO);
	}

	// ----------------------------------------------------------------------
	// Tampon, ızgara, ayna, egzoz, alt gövde
	// ----------------------------------------------------------------------

	private static void buildTrim(final PartDefinition plain, final CarModel.Body body) {
		final float halfW = body.halfWidth();
		final float halfL = body.halfLength();
		final CarTexture bumperTex = body.chrome() ? CarTexture.CHROME : CarTexture.TRIM;
		final float bumperTop = body.clearance() + 3.5F;

		// Tamponlar — koyu renkli bant gövdenin önünü kaplamasın diye alçak tutulur
		addBox(plain, "bumper_front", bumperTex, -halfW + 1.5F, bumperTop, -halfL - 1.6F,
			body.width() - 3.0F, 4.5F, 2.6F);
		addBox(plain, "bumper_rear", bumperTex, -halfW + 1.5F, bumperTop, halfL - 1.0F,
			body.width() - 3.0F, 4.5F, 2.6F);

		// Ön ızgara — tamponla kaputun arasını doldurur
		addBox(plain, "grille", CarTexture.GRILLE, -halfW + 4.0F, body.noseHeight() - 0.8F,
			-halfL - 0.7F, body.width() - 8.0F, body.noseHeight() - 0.8F - bumperTop, 1.2F);

		// Yan aynalar — ön camın dibinde, gövdeden dışarı
		final float mirrorZ = body.cowlZ() + 1.0F;
		final float mirrorY = body.beltHeight() + 2.4F;
		final CubeListBuilder mirrors = CubeListBuilder.create()
			.texOffs(CarTexture.TRIM.u(), CarTexture.TRIM.v());
		for (final float side : new float[] {-1.0F, 1.0F}) {
			final float x = side > 0.0F ? halfW - 0.5F : -halfW - 2.6F;
			box(mirrors, "mirror", CarTexture.TRIM, x, mirrorY, mirrorZ, 3.1F, 2.4F, 1.2F);
		}
		plain.addOrReplaceChild("mirrors", mirrors, PartPose.ZERO);

		// Egzoz
		addBox(plain, "exhaust", CarTexture.TRIM, halfW - 8.0F, body.clearance() + 1.4F,
			halfL - 1.0F, 3.0F, 1.4F, 2.4F);

		// Alt gövde — araç yandan bakıldığında havada durmasın
		addBox(plain, "underbody", CarTexture.UNDER, -halfW + ARCH_INSET + 0.5F, body.clearance(),
			-halfL + 2.0F, body.width() - 2.0F * ARCH_INSET - 1.0F, 2.0F, body.length() - 4.0F);
	}

	// ----------------------------------------------------------------------
	// Farlar ve stop lambaları
	// ----------------------------------------------------------------------

	private static void buildLamps(final PartDefinition plain, final PartDefinition lights,
								   final CarModel.Body body) {
		final float halfW = body.halfWidth();
		final float halfL = body.halfLength();
		final float frontY = body.noseHeight() - 0.8F;
		final float rearY = Math.min(body.tailHeight() - 0.8F, body.beltHeight() + 1.5F);
		final float lampW = Math.min(6.0F, body.width() * 0.20F);
		// Lamba, tamponla kaput arasında kalan boşluğa sığdırılır.
		final float lampH = Mth.clamp(frontY - (body.clearance() + 4.5F), 1.6F, 3.4F);

		// Mercekler: her zaman çizilir
		final CubeListBuilder lenses = CubeListBuilder.create();
		for (final float side : new float[] {-1.0F, 1.0F}) {
			final float x = side > 0.0F ? halfW - 1.2F - lampW : -halfW + 1.2F;
			box(lenses, "lens_front", CarTexture.LIGHT_FRONT, x, frontY, -halfL - 0.8F, lampW, lampH, 1.2F);
			box(lenses, "lens_rear", CarTexture.LIGHT_REAR, x, rearY, halfL - 0.4F, lampW, lampH, 1.2F);
		}
		plain.addOrReplaceChild("lenses", lenses, PartPose.ZERO);

		// Parlayan kopyalar: mercekten biraz dışarıda dururlar ki üst üste
		// binip titremesinler.
		final CubeListBuilder head = CubeListBuilder.create();
		final CubeListBuilder brake = CubeListBuilder.create();
		for (final float side : new float[] {-1.0F, 1.0F}) {
			final float x = side > 0.0F ? halfW - 1.2F - lampW : -halfW + 1.2F;
			box(head, "headlight", CarTexture.LIGHT_FRONT, x, frontY, -halfL - 1.3F,
				lampW, lampH, 1.2F);
			box(brake, "brakelight", CarTexture.LIGHT_REAR, x, rearY, halfL + 0.1F,
				lampW, lampH, 1.2F);
		}
		lights.addOrReplaceChild(HEADLIGHTS, head, PartPose.ZERO);
		lights.addOrReplaceChild(BRAKELIGHTS, brake, PartPose.ZERO);
	}

	// ----------------------------------------------------------------------
	// Tekerlekler
	// ----------------------------------------------------------------------

	/**
	 * Dört tekerlek yuvası kurar. Her yuvada dört tekerlek tipi baştan üretilir;
	 * takılı olan görünür kılınır.
	 *
	 * <p>Lastik kare görünmesin diye her tekerlek, 45 derece aralıklarla
	 * döndürülmüş dört ince şeridin birleşimidir. Şerit yarı ölçüleri
	 * {@code (r·cos 22.5°, r·sin 22.5°)} seçilirse her şeridin köşeleri tam olarak
	 * {@code r} yarıçaplı çember üzerine düşer; birleşim de düzgün bir sekizgen
	 * olur. (Tam kare kullanmak köşeleri {@code r√2}'ye taşırdı ve tekerlek
	 * sekiz köşeli bir yıldıza dönerdi.) Şeritler tekerleğin kendi parçasının
	 * altında durduğu için dönüş ve direksiyon açısı hepsine birden uygulanır.
	 */
	private static void buildWheels(final PartDefinition wheels, final CarModel.Body body) {
		// Lastiğin iç yüzü kemer boşluğunun duvarının hemen içinde, dış yüzü
		// gövdeden biraz dışarıda kalır; böylece tekerlek boşlukta durur.
		final float xOffset = body.halfWidth() - ARCH_INSET + WHEEL_WIDTH / 2.0F - 0.4F;

		for (final WheelPosition position : WheelPosition.values()) {
			final float x = position.right() ? xOffset : -xOffset;
			final float z = position.front() ? body.frontAxleZ() : body.rearAxleZ();
			// Yuvanın kendisi yer hizasındadır; yükseklik tip grubundan gelir.
			final PartDefinition slot = wheels.addOrReplaceChild(WHEEL_PREFIX + position.partName(),
				CubeListBuilder.create(), PartPose.offset(x, 0.0F, z));

			for (final WheelType type : WheelType.values()) {
				final float tire = type.radius();
				final float rim = tire * 0.62F;
				final PartDefinition wheel = slot.addOrReplaceChild(type.itemName(),
					CubeListBuilder.create(), PartPose.offset(0.0F, top(tire), 0.0F));
				for (int facet = 0; facet < WHEEL_FACETS; facet++) {
					final CubeListBuilder builder = CubeListBuilder.create();
					box(builder, "tire", CarTexture.TIRE,
						-WHEEL_WIDTH / 2.0F, tire * FACET_THIN, -tire * FACET_LONG,
						WHEEL_WIDTH, tire * FACET_THIN * 2.0F, tire * FACET_LONG * 2.0F);
					// Jant lastiğin biraz dışına taşar ki yandan görünsün.
					box(builder, "rim", CarTexture.rimFor(type),
						-WHEEL_WIDTH / 2.0F - 0.4F, rim * FACET_THIN, -rim * FACET_LONG,
						WHEEL_WIDTH + 0.8F, rim * FACET_THIN * 2.0F, rim * FACET_LONG * 2.0F);
					wheel.addOrReplaceChild("facet_" + facet, builder,
						PartPose.rotation(facet * Mth.PI / WHEEL_FACETS, 0.0F, 0.0F));
				}
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
	// Profil zincirleri
	// ----------------------------------------------------------------------

	/** Yan profilin bölümleri. */
	private enum Section {
		NOSE, WINDSCREEN, ROOF, BACKLIGHT, DECK
	}

	/**
	 * Bir bölümü, siluetin istediği kadar panele bölünmüş ve kavis verilmiş
	 * hâlde döndürür. Her eleman {@code {z0, h0, z1, h1}} biçiminde bir panelin
	 * iki ucudur.
	 *
	 * <p>Kavis, kirişin orta noktası dik yönde {@code bow × kiriş uzunluğu}
	 * kadar dışarı itilerek elde edilen ikinci derece Bézier eğrisidir; bu
	 * yüzden Beetle'ın sırtı basamak basamak değil, sürekli bir yay gibi
	 * görünür.
	 */
	private static java.util.List<float[]> chain(final CarModel.Body body, final Section section) {
		final Silhouette form = body.silhouette();
		final float halfL = body.halfLength();
		return switch (section) {
			case NOSE -> arc(-halfL, body.noseHeight(), body.cowlZ(), body.beltHeight(),
				form.noseBow(), form.noseSegments());
			case WINDSCREEN -> arc(body.cowlZ(), body.beltHeight(), body.roofFrontZ(), body.roofHeight(),
				0.0F, 1);
			case ROOF -> arc(body.roofFrontZ(), body.roofHeight(), body.roofRearZ(), body.roofHeight(),
				form.roofCrown(), form.roofCrown() > 0.02F ? 2 : 1);
			case BACKLIGHT -> arc(body.roofRearZ(), body.roofHeight(), body.deckZ(), body.deckHeight(),
				form.tailBow(), form.tailSegments());
			case DECK -> arc(body.deckZ(), body.deckHeight(), halfL, body.tailHeight(), 0.0F, 1);
		};
	}

	private static java.util.List<float[]> arc(final float z0, final float h0, final float z1, final float h1,
											   final float bow, final int segments) {
		final java.util.List<float[]> result = new java.util.ArrayList<>(segments);
		if (z1 - z0 < 0.05F) {
			return result;
		}
		final int count = Math.max(1, segments);
		final float dz = z1 - z0;
		final float dh = h1 - h0;
		final float len = Mth.sqrt(dz * dz + dh * dh);
		// Kirişe dik, yukarı bakan birim vektör
		final float nz = len < 1.0E-4F ? 0.0F : -dh / len;
		final float nh = len < 1.0E-4F ? 1.0F : dz / len;
		final float cz = (z0 + z1) / 2.0F + nz * bow * len;
		final float ch = (h0 + h1) / 2.0F + nh * bow * len;

		float prevZ = z0;
		float prevH = h0;
		for (int i = 1; i <= count; i++) {
			final float t = (float) i / count;
			final float u = 1.0F - t;
			final float z = u * u * z0 + 2.0F * t * u * cz + t * t * z1;
			final float h = u * u * h0 + 2.0F * t * u * ch + t * t * h1;
			result.add(new float[] {prevZ, prevH, z, h});
			prevZ = z;
			prevH = h;
		}
		return result;
	}

	// ----------------------------------------------------------------------
	// Geometri yardımcıları
	// ----------------------------------------------------------------------

	/** Yerden {@code heightUnits} yükseklikteki bir noktanın model-uzayı Y'si. */
	private static float top(final float heightUnits) {
		return -heightUnits;
	}

	/**
	 * {@code (z0,h0)} ile {@code (z1,h1)} arasına eğimli ince bir panel koyar.
	 *
	 * <p>Panel yerel eksende Z boyunca uzanır ve X ekseni etrafında döndürülür.
	 * X ekseni etrafında {@code a} kadar dönme yerel {@code +Z}'yi model
	 * uzayında {@code (0, -sin a, cos a)} yönüne taşıdığından, istenen
	 * {@code (0, -(h1-h0), z1-z0)} yönü için {@code a = atan2(h1-h0, z1-z0)}
	 * gerekir.
	 */
	private static void slab(final PartDefinition parent, final String name, final CarTexture region,
							 final float z0, final float h0, final float z1, final float h1,
							 final float width, final float thickness) {
		final float dz = z1 - z0;
		final float dh = h1 - h0;
		final float len = Mth.sqrt(dz * dz + dh * dh);
		if (len < 0.05F || width <= 0.0F) {
			return;
		}
		checkFits(name, region, width, thickness, len);
		parent.addOrReplaceChild(name,
			CubeListBuilder.create()
				.texOffs(region.u(), region.v())
				.addBox(-width / 2.0F, -thickness / 2.0F, -len / 2.0F, width, thickness, len),
			PartPose.offsetAndRotation(0.0F, top((h0 + h1) / 2.0F), (z0 + z1) / 2.0F,
				(float) Math.atan2(dh, dz), 0.0F, 0.0F));
	}

	/** Aynı eğimde, aracın iki yanına simetrik yerleştirilen bir panel çifti (direkler). */
	private static void slabPair(final PartDefinition parent, final String name, final CarTexture region,
								 final float z0, final float h0, final float z1, final float h1,
								 final float offsetX, final float width, final float thickness) {
		final float dz = z1 - z0;
		final float dh = h1 - h0;
		final float len = Mth.sqrt(dz * dz + dh * dh);
		if (len < 0.05F) {
			return;
		}
		checkFits(name, region, width, thickness, len);
		final float angle = (float) Math.atan2(dh, dz);
		for (final float side : new float[] {-1.0F, 1.0F}) {
			parent.addOrReplaceChild(name + (side > 0.0F ? "_right" : "_left"),
				CubeListBuilder.create()
					.texOffs(region.u(), region.v())
					.addBox(side * offsetX - width / 2.0F, -thickness / 2.0F, -len / 2.0F,
						width, thickness, len),
				PartPose.offsetAndRotation(0.0F, top((h0 + h1) / 2.0F), (z0 + z1) / 2.0F,
					angle, 0.0F, 0.0F));
		}
	}

	/** İki yana simetrik, eksen hizalı ince kutu çifti (yan camlar, B direği). */
	private static void sideBoxPair(final PartDefinition parent, final String name, final CarTexture region,
									final float offsetX, final float thickness,
									final float bottomHeight, final float topHeight,
									final float z0, final float z1) {
		if (z1 - z0 < 0.2F || topHeight - bottomHeight < 0.2F) {
			return;
		}
		final CubeListBuilder builder = CubeListBuilder.create().texOffs(region.u(), region.v());
		for (final float side : new float[] {-1.0F, 1.0F}) {
			final float x = side > 0.0F ? offsetX - thickness / 2.0F : -offsetX - thickness / 2.0F;
			box(builder, name, region, x, topHeight, z0, thickness, topHeight - bottomHeight, z1 - z0);
		}
		parent.addOrReplaceChild(name, builder, PartPose.ZERO);
	}

	/**
	 * Eğimli bir panelin altını, tabandan panele kadar basamaklı kutularla
	 * doldurur. Basamak yüksekliği {@link #MAX_STEP} ile sınırlı olduğundan
	 * merdiven panelin kalınlığı içinde kalır ve dışarıdan görünmez.
	 */
	private static void wedge(final CubeListBuilder out, final String name, final CarTexture region,
							  final float z0, final float h0, final float z1, final float h1,
							  final float baseHeight, final float width, final float sink) {
		if (z1 - z0 < 0.05F) {
			return;
		}
		final int steps = Math.max(1, Mth.ceil(Math.abs(h1 - h0) / MAX_STEP));
		final float dz = (z1 - z0) / steps;
		for (int i = 0; i < steps; i++) {
			final float zA = z0 + i * dz;
			final float h = Mth.lerp((i + 0.5F) / steps, h0, h1) - sink;
			if (h - baseHeight <= 0.05F) {
				continue;
			}
			box(out, name, region, -width / 2.0F, h, zA, width, h - baseHeight, dz);
		}
	}

	/**
	 * {@code CubeListBuilder}'a kutu ekler. Y burada <em>yerden yükseklik</em>
	 * olarak verilir; model uzayına çevirmeyi bu metot yapar. Doku uzaklığı da
	 * burada kurulur, böylece aynı parçada farklı malzemeler karışmaz.
	 */
	private static void box(final CubeListBuilder out, final String name, final CarTexture region,
							final float x, final float heightTop, final float z,
							final float sizeX, final float sizeY, final float sizeZ) {
		if (sizeX <= 0.0F || sizeY <= 0.0F || sizeZ <= 0.0F) {
			return;
		}
		checkFits(name, region, sizeX, sizeY, sizeZ);
		out.texOffs(region.u(), region.v()).addBox(x, top(heightTop), z, sizeX, sizeY, sizeZ);
	}

	/** Tek kutuluk bağımsız bir parça. */
	private static void addBox(final PartDefinition parent, final String name, final CarTexture region,
							   final float x, final float heightTop, final float z,
							   final float sizeX, final float sizeY, final float sizeZ) {
		if (sizeX <= 0.0F || sizeY <= 0.0F || sizeZ <= 0.0F) {
			return;
		}
		checkFits(name, region, sizeX, sizeY, sizeZ);
		parent.addOrReplaceChild(name,
			CubeListBuilder.create()
				.texOffs(region.u(), region.v())
				.addBox(x, top(heightTop), z, sizeX, sizeY, sizeZ),
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
