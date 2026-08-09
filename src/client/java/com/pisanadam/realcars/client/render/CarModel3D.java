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
 * Araç modeli. Parçalar dört köke ayrılır (boyanan gövde, boyanmayan parçalar,
 * tekerlekler, parlayan lambalar); renderer her kökü kendi rengi ve çizim
 * tipiyle gönderdiği için tek dokudan hem istenen boya hem gece yanan far çıkar.
 *
 * <p>Tekerleklerin ayrı bir kökte olmasının sebebi süspansiyon: gövde virajda
 * yatıp frende burnunu daldırırken tekerlekler yerde kalmalıdır.
 */
public class CarModel3D extends EntityModel<CarRenderState> {
	private static final int WHEEL_SLOTS = CarMeshFactory.WheelPosition.values().length;
	/** Direksiyonun çevirebileceği en büyük açı (derece). */
	private static final float MAX_STEER_DEGREES = 30.0F;

	private final ModelPart painted;
	private final ModelPart plain;
	private final ModelPart wheelRoot;
	private final ModelPart lights;
	private final ModelPart headlights;
	private final ModelPart brakelights;
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
		this.wheelRoot = root.getChild(CarMeshFactory.WHEELS);
		this.lights = root.getChild(CarMeshFactory.LIGHTS);
		this.headlights = this.lights.getChild(CarMeshFactory.HEADLIGHTS);
		this.brakelights = this.lights.getChild(CarMeshFactory.BRAKELIGHTS);

		for (final SpoilerType spoiler : SpoilerType.values()) {
			if (spoiler.hasItem()) {
				this.spoilers.put(spoiler,
					this.painted.getChild(CarMeshFactory.SPOILER_PREFIX + spoiler.itemName()));
			}
		}

		for (final CarMeshFactory.WheelPosition position : CarMeshFactory.WheelPosition.values()) {
			final ModelPart slot = this.wheelRoot.getChild(CarMeshFactory.WHEEL_PREFIX + position.partName());
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

	public ModelPart wheelRoot() {
		return this.wheelRoot;
	}

	public ModelPart lights() {
		return this.lights;
	}

	/** Motor açıkken far, fren yaparken stop yanar; ikisi de kapalıysa çizim atlanabilir. */
	public boolean anyLightOn() {
		return this.headlights.visible || this.brakelights.visible;
	}

	@Override
	public void setupAnim(final CarRenderState state) {
		super.setupAnim(state);

		// Yalnızca takılı rüzgarlık görünür.
		for (final Map.Entry<SpoilerType, ModelPart> entry : this.spoilers.entrySet()) {
			entry.getValue().visible = entry.getKey() == state.spoiler;
		}

		this.headlights.visible = state.engineOn;
		this.brakelights.visible = state.braking;

		final float spin = state.wheelAngle * Mth.DEG_TO_RAD;
		// Model, çizim sırasında X ve Y'de aynalanıyor (scale(-1,-1,1)); Y ekseni
		// etrafındaki dönüş de bu aynayla işaret değiştirir. Bu yüzden direksiyon
		// açısı olduğu gibi verilir, eksiye çevrilmez — çevrilirse tekerlekler
		// aracın döndüğü yönün tersine bakar.
		final float steer = state.steerAngle * MAX_STEER_DEGREES * Mth.DEG_TO_RAD;
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
