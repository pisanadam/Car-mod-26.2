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
 * <p>Gövde ve rüzgarlık aracın boyasıyla, geri kalan her şey doğal renginde
 * gönderilir; bu yüzden model iki ayrı çağrıda çizilir. Tekerleklerin dönüşü ve
 * ön tekerleklerin direksiyon açısı {@link CarModel3D#setupAnim} içinde
 * parçalara işlenir.
 */
public class CarEntityRenderer extends EntityRenderer<CarEntity, CarRenderState> {
	private static final int NO_TINT = 0xFFFFFFFF;

	private final CarModel3D model;
	private final RenderType renderType;

	public CarEntityRenderer(final EntityRendererProvider.Context context, final ModelLayerLocation layer,
							 final CarModel carModel) {
		super(context);
		this.model = new CarModel3D(context.bakeLayer(layer));
		this.renderType = RenderTypes.entityCutoutCull(CarTexture.ATLAS);
		this.shadowRadius = Math.max(carModel.body().width(), carModel.body().length()) / 32.0F;
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
		poseStack.scale(-1.0F, -1.0F, 1.0F);

		this.model.setupAnim(state);
		// Boyanan paneller araç rengiyle, geri kalan parçalar kendi renginde.
		final int tint = 0xFF000000 | state.color;
		collector.submitModelPart(this.model.painted(), poseStack, this.renderType,
			state.lightCoords, OverlayTexture.NO_OVERLAY, null, tint, null, state.outlineColor);
		collector.submitModelPart(this.model.plain(), poseStack, this.renderType,
			state.lightCoords, OverlayTexture.NO_OVERLAY, null, NO_TINT, null, state.outlineColor);

		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
