package com.pisanadam.realcars.client.mixin;

import com.pisanadam.realcars.client.render.RiderVisibility;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Çizim durumuna {@link RiderVisibility} bayrağını ekler. */
@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements RiderVisibility {
	@Unique
	private boolean realcars$inCar;

	@Override
	public boolean realcars$isInCar() {
		return this.realcars$inCar;
	}

	@Override
	public void realcars$setInCar(final boolean inCar) {
		this.realcars$inCar = inCar;
	}
}
