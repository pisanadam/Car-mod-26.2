package com.pisanadam.realcars.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.CarModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Araçları çizer.
 *
 * <p>Model dört ayrı çağrıda gönderilir, çünkü her kökün rengi ve çizim tipi
 * farklıdır: gövde aracın boyasıyla, cam ve tampon doğal renginde, lambalar
 * ışık yayan (emissive) bir geçişte.
 *
 * <p>Süspansiyon burada canlandırılır: gövde virajda yatar, frende burnunu
 * daldırır; tekerlekler ise ayrı bir kökte olduğu için yerde kalır. Yatma ve
 * dalma, aracın yaklaşık yalpa merkezi olan gövde ortası hizasında döndürülür,
 * yoksa gövde yana savrulmuş gibi görünürdü.
 */
public class CarEntityRenderer extends EntityRenderer<CarEntity, CarRenderState> {
	private static final int NO_TINT = 0xFFFFFFFF;
	/** Virajda yatmanın ve frende dalmanın üst sınırı (derece). */
	private static final float MAX_ROLL = 4.0F;
	private static final float MAX_PITCH = 3.0F;

	private final CarModel3D model;
	private final RenderType renderType;
	private final RenderType glowType;
	/** Yalpa merkezinin yerden yüksekliği (blok). */
	private final float rollCentre;

	public CarEntityRenderer(final EntityRendererProvider.Context context, final ModelLayerLocation layer,
							 final CarModel carModel) {
		super(context);
		this.model = new CarModel3D(context.bakeLayer(layer));
		this.renderType = RenderTypes.entityCutoutCull(CarTexture.ATLAS);
		this.glowType = RenderTypes.entityTranslucentEmissive(CarTexture.ATLAS);
		this.shadowRadius = Math.max(carModel.body().width(), carModel.body().length()) / 32.0F;
		this.rollCentre = (carModel.body().clearance() + carModel.body().beltHeight()) / 32.0F;
	}

	@Override
	public CarRenderState createRenderState() {
		return new CarRenderState();
	}

	@Override
	public void extractRenderState(final CarEntity car, final CarRenderState state, final float partialTicks) {
		super.extractRenderState(car, state, partialTicks);
		state.yRot = car.getYRot(partialTicks);
		state.color = car.color();
		state.wheel = car.wheelType();
		state.spoiler = car.spoilerType();
		state.wheelAngle = car.wheelAngle(partialTicks);
		state.steerAngle = car.steerAngle();
		state.engineOn = car.engineOn();
		state.braking = car.braking();
		state.bodyRoll = car.lateralLoad() * MAX_ROLL;
		state.bodyPitch = car.longitudinalLoad() * MAX_PITCH;
		state.hurtTime = car.getHurtTime() - partialTicks;
		state.hurtDir = car.getHurtDir();
		state.damageTime = Math.max(car.getDamage() - partialTicks, 0.0F);
	}

	@Override
	public void submit(final CarRenderState state, final PoseStack poseStack,
					   final SubmitNodeCollector collector, final CameraRenderState camera) {
		poseStack.pushPose();
		// Model uzayında Y aşağı doğru büyür; vanilla araç/varlık çizimindeki
		// gibi ters çevirip aracın burnunu +Z'ye bakacak şekilde döndürüyoruz.
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
		if (state.hurtTime > 0.0F) {
			// Hasar alınca araç hafifçe sarsılsın.
			poseStack.mulPose(Axis.ZP.rotationDegrees(
				Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 12.0F * state.hurtDir));
		}

		this.model.setupAnim(state);

		// --- tekerlekler: süspansiyondan etkilenmez, yerde kalır ---
		poseStack.pushPose();
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		collector.submitModelPart(this.model.wheelRoot(), poseStack, this.renderType,
			state.lightCoords, OverlayTexture.NO_OVERLAY, null, NO_TINT, null, state.outlineColor);
		poseStack.popPose();

		// --- gövde: yalpa merkezi etrafında yatar ve dalar ---
		poseStack.translate(0.0F, this.rollCentre, 0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(state.bodyRoll));
		poseStack.mulPose(Axis.XP.rotationDegrees(state.bodyPitch));
		poseStack.translate(0.0F, -this.rollCentre, 0.0F);
		poseStack.scale(-1.0F, -1.0F, 1.0F);

		// Boyanan paneller araç rengiyle, geri kalan parçalar kendi renginde.
		final int tint = 0xFF000000 | state.color;
		collector.submitModelPart(this.model.painted(), poseStack, this.renderType,
			state.lightCoords, OverlayTexture.NO_OVERLAY, null, tint, null, state.outlineColor);
		collector.submitModelPart(this.model.plain(), poseStack, this.renderType,
			state.lightCoords, OverlayTexture.NO_OVERLAY, null, NO_TINT, null, state.outlineColor);
		if (this.model.anyLightOn()) {
			// Işık yayan geçiş: farlar karanlıkta gerçekten parlar.
			collector.submitModelPart(this.model.lights(), poseStack, this.glowType,
				state.lightCoords, OverlayTexture.NO_OVERLAY, null, NO_TINT, null, state.outlineColor);
		}

		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
