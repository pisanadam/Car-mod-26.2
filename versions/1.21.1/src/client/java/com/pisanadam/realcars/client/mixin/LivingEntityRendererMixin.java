package com.pisanadam.realcars.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pisanadam.realcars.entity.CarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arabaya binen oyuncunun modelini gizler; isim etiketi görünmeye devam eder.
 *
 * <p>Oyuncu araca binince gövdesi çizilmez — böylece koltuğa oturmuş gibi
 * durmak yerine gerçekten aracın içindeymiş gibi görünür. İsim etiketi
 * kalmalıdır, yoksa çok oyunculu sunucuda arabanın içinde kimin olduğu
 * anlaşılmaz. Bu yüzden çizim tamamen iptal edilmez: model ve katmanlar
 * atlanıp isim etiketi elle çizilir.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>>
		extends EntityRenderer<T> {

	protected LivingEntityRendererMixin(final EntityRendererProvider.Context context) {
		super(context);
	}

	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At("HEAD"), cancellable = true)
	private void realcars$hideCarPassenger(final T entity, final float yaw, final float partialTicks,
										   final PoseStack poseStack, final MultiBufferSource buffers,
										   final int light, final CallbackInfo info) {
		if (entity instanceof Player && entity.getVehicle() instanceof CarEntity) {
			if (this.shouldShowName(entity)) {
				this.renderNameTag(entity, entity.getDisplayName(), poseStack, buffers, light, partialTicks);
			}
			info.cancel();
		}
	}
}
