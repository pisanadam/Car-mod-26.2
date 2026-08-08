package com.pisanadam.realcars.client.render;

import com.pisanadam.realcars.entity.SpoilerType;
import com.pisanadam.realcars.entity.WheelType;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Araç çizilirken gereken, entity'den çıkarılmış durum. */
public class CarRenderState extends EntityRenderState {
	public float yRot;
	public int color = 0xFFFFFF;
	public WheelType wheel = WheelType.WHEEL_STREET;
	public SpoilerType spoiler = SpoilerType.SPOILER_NONE;
	/** Tekerleklerin toplam dönme açısı (derece). */
	public float wheelAngle;
	/** Direksiyon açısı, -1 ile 1 arasında. */
	public float steerAngle;
	public float hurtTime;
	public float damageTime;
	public int hurtDir = 1;
	public boolean engineOn;
	/** Fren yapılıyor mu — stop lambaları buna bakar. */
	public boolean braking;
	/** Virajda yana yatma açısı (derece). */
	public float bodyRoll;
	/** Frende burun dalması / gazda arka çökmesi (derece). */
	public float bodyPitch;
}
