package com.pisanadam.realcars.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Vurulunca sarsılan, yeterince hasar alınca kırılıp eşya düşüren araç tabanı.
 *
 * <p>Minecraft 1.20.3'ten itibaren oyunda bunun karşılığı ({@code VehicleEntity})
 * vardır; bu sürümde olmadığı için aynı davranış burada kurulur, böylece araç
 * kodu sürümler arasında aynı kalır.
 */
public abstract class VehicleBase extends Entity {
	private static final EntityDataAccessor<Integer> DATA_ID_HURT =
		SynchedEntityData.defineId(VehicleBase.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ID_HURTDIR =
		SynchedEntityData.defineId(VehicleBase.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_ID_DAMAGE =
		SynchedEntityData.defineId(VehicleBase.class, EntityDataSerializers.FLOAT);

	protected VehicleBase(final EntityType<?> type, final Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(DATA_ID_HURT, 0);
		this.entityData.define(DATA_ID_HURTDIR, 1);
		this.entityData.define(DATA_ID_DAMAGE, 0.0F);
	}

	@Override
	public boolean hurt(final DamageSource source, final float amount) {
		if (this.level().isClientSide() || this.isRemoved()) {
			return true;
		}
		if (this.isInvulnerableTo(source)) {
			return false;
		}
		this.setHurtDir(-this.getHurtDir());
		this.setHurtTime(10);
		this.markHurt();
		this.setDamage(this.getDamage() + amount * 10.0F);
		this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
		final boolean creative = source.getEntity() instanceof Player player && player.getAbilities().instabuild;
		if (creative || this.getDamage() > 40.0F) {
			if (creative) {
				this.discard();
			} else {
				this.destroy(this.getDropItem());
			}
		}
		return true;
	}

	public void destroy(final Item dropItem) {
		this.kill();
		if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
			final ItemStack stack = new ItemStack(dropItem);
			if (this.hasCustomName()) {
				stack.setHoverName(this.getCustomName());
			}
			this.spawnAtLocation(stack);
		}
	}

	public abstract Item getDropItem();

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	public void setHurtTime(final int time) {
		this.entityData.set(DATA_ID_HURT, time);
	}

	public void setHurtDir(final int dir) {
		this.entityData.set(DATA_ID_HURTDIR, dir);
	}

	public void setDamage(final float damage) {
		this.entityData.set(DATA_ID_DAMAGE, damage);
	}

	public float getDamage() {
		return this.entityData.get(DATA_ID_DAMAGE);
	}

	public int getHurtTime() {
		return this.entityData.get(DATA_ID_HURT);
	}

	public int getHurtDir() {
		return this.entityData.get(DATA_ID_HURTDIR);
	}
}
