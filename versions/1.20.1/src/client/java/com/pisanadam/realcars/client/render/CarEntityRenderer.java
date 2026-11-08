package com.pisanadam.realcars.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.CarModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Araçları çizer.
 *
 * <p>İki gövde türü vardır. Varsayılan olarak her araç, gerçek bir araç
 * modelinden dönüştürülmüş yüksek poligonlu gövdesiyle ({@link HdCarMesh})
 * çizilir. O dosya kaynak paketinde yoksa ya da okunamazsa eski parametrik,
 * kutulardan kurulan gövdeye ({@link CarModel3D}) dönülür.
 *
 * <p>Model ayrı çağrılarda gönderilir, çünkü her parçanın rengi ve çizim tipi
 * farklıdır: gövde aracın boyasıyla, cam yarı saydam, lambalar ışık yayan
 * (emissive) bir geçişte.
 *
 * <p>Süspansiyon burada canlandırılır: gövde virajda yatar, frende burnunu
 * daldırır; tekerlekler ise ayrı parçalar olduğu için yerde kalır. Yatma ve
 * dalma, aracın yaklaşık yalpa merkezi olan gövde ortası hizasında döndürülür,
 * yoksa gövde yana savrulmuş gibi görünürdü.
 */
public class CarEntityRenderer extends EntityRenderer<CarEntity> {
	private static final int NO_TINT = 0xFFFFFFFF;
	/** Virajda yatmanın ve frende dalmanın üst sınırı (derece). */
	private static final float MAX_ROLL = 4.0F;
	private static final float MAX_PITCH = 3.0F;
	/** Direksiyonun çevirebileceği en büyük açı (derece), parametrik modelle aynı. */
	private static final float MAX_STEER_DEGREES = 30.0F;
	/** Bu mesafeden (blok) ötede düşük detaylı gövde çizilir. */
	private static final double LOD_DISTANCE = 40.0D;

	/** Modifiye ekranındaki önizleme bu açıyı verir; null ise aracın kendi yönü kullanılır. */
	public static Float previewYaw;

	private final CarModel3D model;
	private final RenderType renderType;
	private final RenderType glowType;
	/** Yalpa merkezinin yerden yüksekliği (blok). */
	private final float rollCentre;

	private final HdCarMesh hd;
	private final RenderType hdSolid;
	private final RenderType hdGlass;
	private final RenderType hdGlow;
	/** Parametrik rüzgarlığı HD gövdenin gerçek bagaj yüksekliğine taşıyan fark (blok). */
	private final float spoilerLift;

	public CarEntityRenderer(final EntityRendererProvider.Context context, final ModelLayerLocation layer,
							 final CarModel carModel) {
		super(context);
		this.model = new CarModel3D(context.bakeLayer(layer));
		this.renderType = RenderType.entityCutout(CarTexture.ATLAS);
		this.glowType = RenderType.entityTranslucentEmissive(CarTexture.ATLAS);
		this.shadowRadius = Math.max(carModel.body().width(), carModel.body().length()) / 32.0F;

		final String carId = carModel.itemName().substring("car_".length());
		this.hd = HdCarMesh.load(context.getResourceManager(), carId);
		if (this.hd != null) {
			final ResourceLocation texture = HdCarMesh.textureId(carId);
			// Modellerin çoğu tek yüzlü paneller içerir; arka yüz ayıklaması
			// kapalı olmalı, yoksa içten bakınca delik görünür.
			this.hdSolid = RenderType.entityCutoutNoCull(texture);
			this.hdGlass = RenderType.entityTranslucent(texture);
			this.hdGlow = RenderType.entityTranslucentEmissive(texture);
			this.rollCentre = this.hd.height() * 0.4F;
			final CarModel.Body body = carModel.body();
			final float halfL = body.halfLength() / 16.0F;
			final float deckTop = (body.hasBed() ? body.beltHeight() + 6.5F : body.tailHeight()) / 16.0F;
			this.spoilerLift = this.hd.maxHeight(halfL - 10.0F / 16.0F, halfL - 2.0F / 16.0F) - deckTop;
			RealCars.LOGGER.debug("{}: yüksek poligonlu gövde, {} dörtgen", carId, this.hd.quadCount());
		} else {
			this.hdSolid = null;
			this.hdGlass = null;
			this.hdGlow = null;
			this.rollCentre = (carModel.body().clearance() + carModel.body().beltHeight()) / 32.0F;
			this.spoilerLift = 0.0F;
		}
	}

	/** Bu araç yüksek poligonlu gövdeyle mi çiziliyor? (Testler için.) */
	public boolean usesHdMesh() {
		return this.hd != null;
	}

	public HdCarMesh hdMesh() {
		return this.hd;
	}

	@Override
	public ResourceLocation getTextureLocation(final CarEntity car) {
		return CarTexture.ATLAS;
	}

	private CarRenderState extract(final CarEntity car, final float partialTicks, final int light) {
		final CarRenderState state = new CarRenderState();
		state.lightCoords = light;
		state.distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr(car);
		state.yRot = Mth.rotLerp(partialTicks, car.yRotO, car.getYRot());
		state.color = car.color();
		state.wheel = car.wheelType();
		state.spoiler = car.spoilerType();
		state.wheelAngle = car.wheelAngle(partialTicks);
		state.steerAngle = car.steerAngle();
		if (previewYaw != null) {
			state.yRot = previewYaw;
			state.steerAngle = 0.0F;
		}
		state.engineOn = car.engineOn();
		state.braking = car.braking();
		state.bodyRoll = car.lateralLoad() * MAX_ROLL;
		state.bodyPitch = car.longitudinalLoad() * MAX_PITCH;
		state.hurtTime = car.getHurtTime() - partialTicks;
		state.hurtDir = car.getHurtDir();
		state.damageTime = Math.max(car.getDamage() - partialTicks, 0.0F);
		return state;
	}

	@Override
	public void render(final CarEntity car, final float entityYaw, final float partialTicks,
					   final PoseStack poseStack, final MultiBufferSource buffers, final int light) {
		final CarRenderState state = this.extract(car, partialTicks, light);
		poseStack.pushPose();
		// Aracın burnu -Z'de modellenir; entity'nin baktığı yöne çevriliyor.
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
		if (state.hurtTime > 0.0F) {
			// Hasar alınca araç hafifçe sarsılsın.
			poseStack.mulPose(Axis.ZP.rotationDegrees(
				Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 12.0F * state.hurtDir));
		}

		this.model.setupAnim(state);
		HdCarMesh.faceCamera = previewYaw == null;
		if (this.hd != null) {
			this.submitHd(state, poseStack, buffers);
		} else {
			this.submitParametric(state, poseStack, buffers);
		}

		poseStack.popPose();
		super.render(car, entityYaw, partialTicks, poseStack, buffers, light);
	}

	// ----------------------------------------------------------------------
	// Yüksek poligonlu gövde
	// ----------------------------------------------------------------------

	private void submitHd(final CarRenderState state, final PoseStack poseStack, final MultiBufferSource buffers) {
		final int level = state.distanceToCameraSq > LOD_DISTANCE * LOD_DISTANCE ? 1 : 0;
		final int light = state.lightCoords;
		final int overlay = OverlayTexture.NO_OVERLAY;

		// --- tekerlekler: süspansiyondan etkilenmez, yerde kalır ---
		// Burun -Z'de olduğundan ileri yuvarlanma X ekseni etrafında eksi
		// yönde, sola direksiyon Y ekseni etrafında eksi yöndedir (parametrik
		// modeldeki aynalanmış eksenlerle aynı sonuç).
		final float spin = -state.wheelAngle * Mth.DEG_TO_RAD;
		final float steer = -state.steerAngle * MAX_STEER_DEGREES * Mth.DEG_TO_RAD;
		for (final HdCarMesh.Part part : this.hd.parts()) {
			if (part.kind() != HdCarMesh.Kind.WHEEL) {
				continue;
			}
			poseStack.pushPose();
			poseStack.translate(part.pivotX(), part.pivotY(), part.pivotZ());
			if (part.front()) {
				poseStack.mulPose(Axis.YP.rotation(steer));
			}
			poseStack.mulPose(Axis.XP.rotation(spin));
			final HdCarMesh.Lod lod = part.lod(level);
			lod.emit(poseStack.last(), buffers.getBuffer(this.hdSolid), NO_TINT, overlay, light);
			poseStack.popPose();
		}

		// --- gövde: yalpa merkezi etrafında yatar ve dalar ---
		poseStack.translate(0.0F, this.rollCentre, 0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(state.bodyRoll));
		poseStack.mulPose(Axis.XP.rotationDegrees(state.bodyPitch));
		poseStack.translate(0.0F, -this.rollCentre, 0.0F);

		final int paint = 0xFF000000 | state.color;
		for (final HdCarMesh.Part part : this.hd.parts()) {
			final HdCarMesh.Lod lod = part.lod(level);
			switch (part.kind()) {
				case PAINT -> lod.emit(poseStack.last(), buffers.getBuffer(this.hdSolid), paint, overlay, light);
				case PLAIN -> lod.emit(poseStack.last(), buffers.getBuffer(this.hdSolid), NO_TINT, overlay, light);
				case GLASS -> lod.emit(poseStack.last(), buffers.getBuffer(this.hdGlass), NO_TINT, overlay, light);
				case LIGHT_FRONT, LIGHT_REAR -> {
					lod.emit(poseStack.last(), buffers.getBuffer(this.hdSolid), NO_TINT, overlay, light);
					// Işık yayan geçiş: farlar karanlıkta gerçekten parlar.
					final boolean lit = part.kind() == HdCarMesh.Kind.LIGHT_FRONT ? state.engineOn : state.braking;
					if (lit) {
						lod.emit(poseStack.last(), buffers.getBuffer(this.hdGlow), NO_TINT, overlay, light);
					}
				}
				case WHEEL -> {
				}
			}
		}

		// Rüzgarlık parametrik kalır (modifiye parçası); HD gövdenin gerçek
		// bagaj yüksekliğine kaydırılıp aynı boyayla çizilir.
		final ModelPart spoiler = this.model.spoiler(state.spoiler);
		if (spoiler != null) {
			poseStack.pushPose();
			poseStack.translate(0.0F, this.spoilerLift, 0.0F);
			poseStack.scale(-1.0F, -1.0F, 1.0F);
			spoiler.visible = true;
			renderPart(spoiler, poseStack, buffers.getBuffer(this.renderType), light, overlay, paint);
			poseStack.popPose();
		}
	}

	// ----------------------------------------------------------------------
	// Parametrik (kutulardan kurulan) gövde
	// ----------------------------------------------------------------------

	private void submitParametric(final CarRenderState state, final PoseStack poseStack,
								  final MultiBufferSource buffers) {
		// Model uzayında Y aşağı doğru büyür; vanilla varlık çizimindeki gibi
		// X ve Y'de aynalanır.

		// --- tekerlekler: süspansiyondan etkilenmez, yerde kalır ---
		poseStack.pushPose();
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		renderPart(this.model.wheelRoot(), poseStack, buffers.getBuffer(this.renderType), state.lightCoords, OverlayTexture.NO_OVERLAY, NO_TINT);
		poseStack.popPose();

		// --- gövde: yalpa merkezi etrafında yatar ve dalar ---
		poseStack.translate(0.0F, this.rollCentre, 0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(state.bodyRoll));
		poseStack.mulPose(Axis.XP.rotationDegrees(state.bodyPitch));
		poseStack.translate(0.0F, -this.rollCentre, 0.0F);
		poseStack.scale(-1.0F, -1.0F, 1.0F);

		// Boyanan paneller araç rengiyle, geri kalan parçalar kendi renginde.
		final int tint = 0xFF000000 | state.color;
		renderPart(this.model.painted(), poseStack, buffers.getBuffer(this.renderType), state.lightCoords, OverlayTexture.NO_OVERLAY, tint);
		renderPart(this.model.plain(), poseStack, buffers.getBuffer(this.renderType), state.lightCoords, OverlayTexture.NO_OVERLAY, NO_TINT);
		if (this.model.anyLightOn()) {
			// Işık yayan geçiş: farlar karanlıkta gerçekten parlar.
			renderPart(this.model.lights(), poseStack, buffers.getBuffer(this.glowType), state.lightCoords, OverlayTexture.NO_OVERLAY, NO_TINT);
		}
	}

	/** Parçayı ARGB renk çarpanıyla çizer (bu sürümde renk kanalları ayrı verilir). */
	private static void renderPart(final ModelPart part, final PoseStack poseStack,
								   final com.mojang.blaze3d.vertex.VertexConsumer out, final int light,
								   final int overlay, final int color) {
		part.render(poseStack, out, light, overlay, (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F,
			(color & 0xFF) / 255.0F, (color >>> 24) / 255.0F);
	}
}
