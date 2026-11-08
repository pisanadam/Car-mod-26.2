package com.pisanadam.realcars.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pisanadam.realcars.client.render.RiderVisibility;
import com.pisanadam.realcars.entity.CarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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
 * anlaşılmaz.
 *
 * <p>Bu yüzden çizim tamamen iptal edilemez: isim etiketini üst sınıf
 * {@code EntityRenderer.submit} çiziyor. Model ve katmanların (zırh, elindeki
 * eşya) çizimi atlanıp isim etiketi elle çizilir.
 *
 * <p>Bilgi çizim durumuna, durum çıkarılırken yazılır; çünkü çizim anında
 * elde yalnızca durum vardır, varlığın kendisi yoktur.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState,
		M extends EntityModel<? super S>> extends EntityRenderer<T, S> {

	protected LivingEntityRendererMixin(final EntityRendererProvider.Context context) {
		super(context);
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void realcars$markCarPassenger(final T entity, final S state, final float partialTicks,
										   final CallbackInfo info) {
		((RiderVisibility) state).realcars$setInCar(
			entity instanceof Player && entity.getVehicle() instanceof CarEntity);
	}

	@Inject(method = "submit", at = @At("HEAD"), cancellable = true)
	private void realcars$hideCarPassenger(final S state, final PoseStack poseStack,
										   final SubmitNodeCollector collector,
										   final CameraRenderState camera, final CallbackInfo info) {
		if (((RiderVisibility) state).realcars$isInCar()) {
			this.submitNameDisplay(state, poseStack, collector, camera);
			info.cancel();
		}
	}
}
