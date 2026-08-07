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
	private static final int WHEEL_SLOTS = CarMeshFactory.WheelPosition.values().length;
	/** Direksiyonun çevirebileceği en büyük açı (derece). */
	private static final float MAX_STEER_DEGREES = 30.0F;

	private final ModelPart painted;
	private final ModelPart plain;
	private final Map<SpoilerType, ModelPart> spoilers = new EnumMap<>(SpoilerType.class);
	/** Her tekerlek yuvası için tip -> tekerlek parçası. */
	private final Map<WheelType, ModelPart>[] wheels = newWheelSlots();

	@SuppressWarnings("unchecked")
	private static Map<WheelType, ModelPart>[] newWheelSlots() {
		return new Map[WHEEL_SLOTS];
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
			final ModelPart slot = this.plain.getChild(CarMeshFactory.WHEEL_PREFIX + position.partName());
			final Map<WheelType, ModelPart> perType = new EnumMap<>(WheelType.class);
			for (final WheelType type : WheelType.values()) {
				perType.put(type, slot.getChild(type.itemName()));
			}
			this.wheels[position.ordinal()] = perType;
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
		final float steer = -state.steerAngle * MAX_STEER_DEGREES * Mth.DEG_TO_RAD;
		for (final CarMeshFactory.WheelPosition position : CarMeshFactory.WheelPosition.values()) {
			for (final Map.Entry<WheelType, ModelPart> entry : this.wheels[position.ordinal()].entrySet()) {
				final ModelPart wheel = entry.getValue();
				wheel.visible = entry.getKey() == state.wheel;
				if (!wheel.visible) {
					continue;
				}
				// ModelPart dönüşleri Z-Y-X sırasıyla birleşir; böylece
				// direksiyon (Y) dıştan, tekerlek dönüşü (X) içten uygulanır.
				wheel.xRot = spin;
				wheel.yRot = position.front() ? steer : 0.0F;
			}
		}
	}
}
