package com.pisanadam.realcars.client.render;

import com.pisanadam.realcars.entity.SpoilerType;
import com.pisanadam.realcars.entity.WheelType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Araç modeli. Boyanan ve boyanmayan parçalar ayrı köklerde tutulur; renderer
 * ikisini farklı renklerle gönderdiği için tek dokudan istenen boya çıkar.
 */
public class CarModel3D extends EntityModel<CarRenderState> {
	private final ModelPart painted;
	private final ModelPart plain;
	private final Map<SpoilerType, ModelPart> spoilers = new EnumMap<>(SpoilerType.class);
	private final ModelPart[] wheels = new ModelPart[CarMeshFactory.WheelPosition.values().length];
	private final Map<WheelType, ModelPart>[] rims = createRimMaps();

	@SuppressWarnings("unchecked")
	private static Map<WheelType, ModelPart>[] createRimMaps() {
		return new Map[CarMeshFactory.WheelPosition.values().length];
	}

	public CarModel3D(final ModelPart root) {
		super(root, RenderTypes::entityCutoutCull);
		this.painted = root.getChild(CarMeshFactory.PAINTED);
		this.plain = root.getChild(CarMeshFactory.PLAIN);

		for (final SpoilerType spoiler : SpoilerType.values()) {
			if (spoiler.hasItem()) {
				this.spoilers.put(spoiler,
					this.painted.getChild(CarMeshFactory.SPOILER_PREFIX + spoiler.itemName()));
			}
		}

		for (final CarMeshFactory.WheelPosition position : CarMeshFactory.WheelPosition.values()) {
			final ModelPart wheel = this.plain.getChild(CarMeshFactory.WHEEL_PREFIX + position.partName());
			this.wheels[position.ordinal()] = wheel;
			final Map<WheelType, ModelPart> perWheel = new EnumMap<>(WheelType.class);
			for (final WheelType type : WheelType.values()) {
				perWheel.put(type, wheel.getChild(type.itemName()));
			}
			this.rims[position.ordinal()] = perWheel;
		}
	}

	public ModelPart painted() {
		return this.painted;
	}

	public ModelPart plain() {
		return this.plain;
	}

	@Override
	public void setupAnim(final CarRenderState state) {
		super.setupAnim(state);

		// Yalnızca takılı rüzgarlık görünür.
		for (final Map.Entry<SpoilerType, ModelPart> entry : this.spoilers.entrySet()) {
			entry.getValue().visible = entry.getKey() == state.spoiler;
		}

		final float spin = state.wheelAngle * Mth.DEG_TO_RAD;
		// Direksiyon açısı en fazla 30 derece; yalnızca ön tekerlekler döner.
		final float steer = -state.steerAngle * 30.0F * Mth.DEG_TO_RAD;
		for (final CarMeshFactory.WheelPosition position : CarMeshFactory.WheelPosition.values()) {
			final ModelPart wheel = this.wheels[position.ordinal()];
			wheel.xRot = spin;
			wheel.yRot = position.front() ? steer : 0.0F;
			for (final Map.Entry<WheelType, ModelPart> rim : this.rims[position.ordinal()].entrySet()) {
				rim.getValue().visible = rim.getKey() == state.wheel;
			}
		}
	}
}
