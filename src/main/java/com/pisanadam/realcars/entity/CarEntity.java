package com.pisanadam.realcars.entity;

import com.pisanadam.realcars.item.FuelCanisterItem;
import com.pisanadam.realcars.item.SprayCanItem;
import com.pisanadam.realcars.item.WrenchItem;
import com.pisanadam.realcars.registry.ModItems;
import com.pisanadam.realcars.registry.ModMenus;
import com.pisanadam.realcars.registry.ModSounds;
import com.pisanadam.realcars.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sürülebilir araba.
 *
 * <p>Fizik, aracı süren oyuncunun tarafında (yani {@code isLocalInstanceAuthoritative()}
 * doğru olan tarafta) işletilir; bu vanilla'nın tekne/at davranışıyla aynıdır ve
 * direksiyonun gecikmesiz hissedilmesini sağlar. Sunucu, sürücünün girdisini
 * {@link ServerPlayer#getLastClientInput()} üzerinden okuyabildiği için hareket
 * için ayrı bir paket gerekmez; yalnızca motor/korna/vites gibi anlık eylemler
 * küçük bir paketle bildirilir.
 *
 * <p>Hız gerçek ölçekte tutulur: 1 blok = 1 metre kabul edildiğinden
 * {@code 1 km/s = 1/72 blok/tick}. Yani göstergedeki sayı oyundaki gerçek hızdır.
 */
public class CarEntity extends VehicleEntity {
	/** 1 km/s kaç blok/tick eder (1 blok = 1 m, 20 tick = 1 s). */
	public static final double KMH_TO_BLOCKS_PER_TICK = 1.0 / 72.0;
	/** Blokların içinden geçmeyi önlemek için tek tickte alınabilecek azami yol. */
	private static final double MAX_BLOCKS_PER_TICK = 4.5;

	private static final EntityDataAccessor<Integer> DATA_COLOR =
		SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_WHEEL =
		SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_SPOILER =
		SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ENGINE =
		SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_TRANSMISSION =
		SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_FUEL =
		SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> DATA_ENGINE_ON =
		SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BOOLEAN);
	/** Sürücü dışındaki oyuncuların da doğru sesi/ibreyi görmesi için yayınlanır. */
	private static final EntityDataAccessor<Float> DATA_SPEED =
		SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_RPM =
		SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_GEAR =
		SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_STEER =
		SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

	private final CarModel model;
	private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

	/** İşaretli hız: eksi değer geri gidildiği anlamına gelir (km/s). */
	private float speedKmh;
	private int gear = GearBox.NEUTRAL;
	private int rpm = GearBox.IDLE_RPM;
	private float steerAngle;
	/** Tekerleklerin toplam dönme açısı (derece) — render bunu kullanır. */
	private float wheelAngle;
	private float wheelAngleO;
	private int shiftCooldown;
	private boolean wheelSlip;
	/** Süspansiyon hareketini besleyen yükler — her iki tarafta da hesaplanır. */
	private float prevSpeedKmh;
	private float longAccel;
	private float lateralLoad;
	/** İstemcide yerel oyuncunun tuş durumu; sunucuda kullanılmaz. */
	private Input clientInput = Input.EMPTY;

	public CarEntity(final EntityType<? extends CarEntity> type, final Level level, final CarModel model) {
		super(type, level);
		this.model = model;
		this.blocksBuilding = true;
		// defineSynchedData() üst sınıfın kurucusundan, yani `model` daha
		// atanmadan çağrılır; bu yüzden modele bağlı varsayılanlar ancak burada
		// yazılabilir. Kaydedilmiş bir araç yükleniyorsa readAdditionalSaveData
		// bunları hemen sonra kendi değerleriyle değiştirir.
		this.applyConfig(CarConfig.factory(model));
	}

	// ----------------------------------------------------------------------
	// Durum
	// ----------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 *
	 * <p>Dikkat: bu metot {@code Entity} kurucusundan, yani {@link #model} alanı
	 * daha atanmadan çağrılır. Bu yüzden burada yalnızca nötr varsayılanlar
	 * tanımlanır; modele özgü değerler kurucunun sonunda yazılır.
	 */
	@Override
	protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_COLOR, 0xFFFFFF);
		entityData.define(DATA_WHEEL, WheelType.WHEEL_STREET.ordinal());
		entityData.define(DATA_SPOILER, SpoilerType.SPOILER_NONE.ordinal());
		entityData.define(DATA_ENGINE, EngineType.ENGINE_I4.ordinal());
		entityData.define(DATA_TRANSMISSION, TransmissionType.TRANSMISSION_MANUAL.ordinal());
		entityData.define(DATA_FUEL, 0.0F);
		entityData.define(DATA_ENGINE_ON, false);
		entityData.define(DATA_SPEED, 0.0F);
		entityData.define(DATA_RPM, GearBox.IDLE_RPM);
		entityData.define(DATA_GEAR, GearBox.NEUTRAL);
		entityData.define(DATA_STEER, 0.0F);
	}

	public CarModel model() {
		return this.model;
	}

	public CarConfig config() {
		return new CarConfig(this.color(), this.wheelType(), this.spoilerType(),
			this.engineType(), this.transmissionType(), this.fuel());
	}

	public void applyConfig(final CarConfig config) {
		this.entityData.set(DATA_COLOR, config.color());
		this.entityData.set(DATA_WHEEL, config.wheel().ordinal());
		this.entityData.set(DATA_SPOILER, config.spoiler().ordinal());
		this.entityData.set(DATA_ENGINE, config.engine().ordinal());
		this.entityData.set(DATA_TRANSMISSION, config.transmission().ordinal());
		this.entityData.set(DATA_FUEL, config.fuel());
	}

	public int color() {
		return this.entityData.get(DATA_COLOR);
	}

	public WheelType wheelType() {
		return WheelType.values()[Mth.clamp(this.entityData.get(DATA_WHEEL), 0, WheelType.values().length - 1)];
	}

	public SpoilerType spoilerType() {
		return SpoilerType.values()[Mth.clamp(this.entityData.get(DATA_SPOILER), 0, SpoilerType.values().length - 1)];
	}

	public EngineType engineType() {
		return EngineType.values()[Mth.clamp(this.entityData.get(DATA_ENGINE), 0, EngineType.values().length - 1)];
	}

	public TransmissionType transmissionType() {
		return TransmissionType.values()[
			Mth.clamp(this.entityData.get(DATA_TRANSMISSION), 0, TransmissionType.values().length - 1)];
	}

	public float fuel() {
		return this.entityData.get(DATA_FUEL);
	}

	public void setFuel(final float value) {
		this.entityData.set(DATA_FUEL, Mth.clamp(value, 0.0F, this.model.fuelCapacity()));
	}

	public boolean engineOn() {
		return this.entityData.get(DATA_ENGINE_ON);
	}

	/** Sürücünün kendi tarafında hesapladığı hız; diğer istemcilerde yayınlanan değer. */
	public float speedKmh() {
		return this.isLocalInstanceAuthoritative() ? this.speedKmh : this.entityData.get(DATA_SPEED);
	}

	public int rpm() {
		return this.isLocalInstanceAuthoritative() ? this.rpm : this.entityData.get(DATA_RPM);
	}

	public int gear() {
		return this.isLocalInstanceAuthoritative() ? this.gear : this.entityData.get(DATA_GEAR);
	}

	public float steerAngle() {
		return this.isLocalInstanceAuthoritative() ? this.steerAngle : this.entityData.get(DATA_STEER);
	}

	public boolean wheelSlipping() {
		return this.wheelSlip;
	}

	/** Takılı motora göre bu aracın ulaşabileceği azami hız. */
	public float topSpeedKmh() {
		final float stock = this.model.topSpeedKmh();
		final float ratio = this.engineType().powerFactor() / this.model.defaultEngine().powerFactor();
		return stock * Mth.lerp(0.55F, 1.0F, ratio);
	}

	public int gearCount() {
		return this.transmissionType().gearCount();
	}

	/** Render için tekerlek dönüş açısı (derece), kareler arası yumuşatılmış. */
	public float wheelAngle(final float partialTicks) {
		return Mth.lerp(partialTicks, this.wheelAngleO, this.wheelAngle);
	}

	// ----------------------------------------------------------------------
	// Sürüş
	// ----------------------------------------------------------------------

	@Override
	public void tick() {
		if (this.getHurtTime() > 0) {
			this.setHurtTime(this.getHurtTime() - 1);
		}
		if (this.getDamage() > 0.0F) {
			this.setDamage(this.getDamage() - 1.0F);
		}
		if (this.shiftCooldown > 0) {
			this.shiftCooldown--;
		}

		super.tick();
		this.interpolation.interpolate();

		if (this.isLocalInstanceAuthoritative()) {
			this.driveTick();
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.publishState();
		} else if (!this.level().isClientSide()) {
			// Araç sürülürken fiziği sürücünün istemcisi işletir, dolayısıyla
			// sunucu otoriter değildir. Toz efektleri ve diğer oyuncuların
			// gördüğü gösterge yine de doğru olsun diye hız, vites ve
			// direksiyon gerçek hareketten türetilip yayınlanır.
			this.deriveStateFromMovement();
			this.publishState();
		}

		this.updateWheelSpin();
		this.updateChassisLoads();
		this.spawnGroundEffects();
	}

	/**
	 * Gövdenin viraj/fren hareketini besleyen yükleri günceller.
	 *
	 * <p>Yalnızca zaten yayınlanan hız ve direksiyon açısından türetildiği için
	 * fiziği kimin işlettiğinden bağımsız olarak her tarafta doğru çalışır;
	 * ek bir senkron alanı gerekmez.
	 */
	private void updateChassisLoads() {
		final float speed = this.speedKmh();
		this.longAccel = Mth.lerp(0.25F, this.longAccel, speed - this.prevSpeedKmh);
		this.prevSpeedKmh = speed;
		// Yanal yük hem direksiyon açısına hem hıza bağlıdır: dururken
		// direksiyon çevirmek aracı yatırmaz.
		final float lateral = this.steerAngle() * Mth.clamp(Math.abs(speed) / 90.0F, 0.0F, 1.0F);
		this.lateralLoad = Mth.lerp(0.2F, this.lateralLoad, lateral);
	}

	/** Boyuna yük: -1 sert fren (burun daldı), +1 tam gaz (arka çöktü). */
	public float longitudinalLoad() {
		return Mth.clamp(this.longAccel / 2.2F, -1.0F, 1.0F);
	}

	/** Yanal yük: -1 sola, +1 sağa yatma. */
	public float lateralLoad() {
		return Mth.clamp(this.lateralLoad, -1.0F, 1.0F);
	}

	/** Stop lambaları yansın mı? */
	public boolean braking() {
		return this.longAccel < -0.35F && Math.abs(this.speedKmh()) > 1.0F;
	}

	/** Bir tick'lik sürüş simülasyonu: girdi, vites, çekiş, direksiyon, yakıt. */
	private void driveTick() {
		final Input input = this.driverInput();
		final boolean throttle = input.forward();
		final boolean brakeOrReverse = input.backward();
		final boolean handbrake = input.shift();

		final float grip = this.groundGrip();
		final boolean airborne = !this.onGround();

		// --- yakıt ---
		if (this.engineOn() && this.fuel() <= 0.0F) {
			this.setEngineOn(false);
		}
		final boolean running = this.engineOn() && this.fuel() > 0.0F;

		// --- vites seçimi ---
		this.updateGear(throttle, brakeOrReverse, running);

		// --- boyuna kuvvetler ---
		final float topSpeed = this.topSpeedKmh();
		this.rpm = GearBox.rpm(this.speedKmh, this.gear, this.gearCount(), topSpeed,
			this.engineType().redlineRpm());

		float accel = 0.0F;
		if (running && !airborne) {
			// 0-100 süresinden türetilen temel ivme (km/s / tick)
			final float baseAccel = 100.0F / (this.model.accelSeconds() * 20.0F);
			final float power = GearBox.torqueFactor(this.rpm, this.engineType().redlineRpm())
				* GearBox.pullFactor(this.gear, this.gearCount())
				* this.engineType().powerFactor() / this.model.defaultEngine().powerFactor();
			final float penalty = this.transmissionType().automatic()
				? 1.0F : GearBox.mismatchPenalty(this.rpm, this.engineType().redlineRpm());

			if (throttle && this.gear > GearBox.NEUTRAL) {
				accel = baseAccel * power * penalty;
			} else if (brakeOrReverse && this.gear == GearBox.REVERSE) {
				accel = -baseAccel * 0.6F * power;
			}
		}

		// Tutuş, aktarılabilecek kuvveti sınırlar; fazlası patinaja gider. Sınır
		// aracın kendi ivmesine göre ölçeklenir: asfaltta uygun lastikle
		// (grip ~1) tavan ivmenin üstünde kalır, yani araç kataloğundaki 0-100
		// süresini gerçekten tutturur. Toprakta ve buzda ise sınır ivmenin
		// altına düşer ve tekerlek boşa döner.
		final float baseAccelFor = 100.0F / (this.model.accelSeconds() * 20.0F);
		final float maxTraction = baseAccelFor * (0.55F + 0.75F * grip);
		this.wheelSlip = Math.abs(accel) > maxTraction && Math.abs(this.speedKmh) < topSpeed * 0.5F;
		accel = Mth.clamp(accel, -maxTraction, maxTraction);

		// --- frenleme ve sürtünme ---
		if (brakeOrReverse && this.gear > GearBox.NEUTRAL && this.speedKmh > 0.5F) {
			this.speedKmh -= 2.4F * grip;
		}
		if (handbrake) {
			this.speedKmh *= 0.88F;
		}
		this.speedKmh += accel;

		// Yuvarlanma direnci + hava direnci (yüksek hızda baskın olan ikincisi)
		final float rolling = airborne ? 0.02F : 0.06F;
		final float drag = 0.000045F * this.speedKmh * this.speedKmh * Math.signum(this.speedKmh);
		this.speedKmh -= Math.signum(this.speedKmh) * rolling + drag;
		if (Math.abs(this.speedKmh) < 0.25F && !throttle && !brakeOrReverse) {
			this.speedKmh = 0.0F;
		}
		this.speedKmh = Mth.clamp(this.speedKmh, -GearBox.REVERSE_TOP_SPEED, topSpeed);

		// --- direksiyon ---
		this.updateSteering(input, grip);

		// --- hız vektörüne çevir ---
		final double blocksPerTick = Mth.clamp(this.speedKmh * KMH_TO_BLOCKS_PER_TICK,
			-MAX_BLOCKS_PER_TICK, MAX_BLOCKS_PER_TICK);
		final float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		double vy = this.getDeltaMovement().y;
		// Yerdeyken de küçük bir aşağı hız gerekir: dikey hız tam sıfır olursa
		// move() zemine bastırmaz ve bir sonraki tick onGround yanlış çıkar.
		// Araç bir tick yerde bir tick havada sayılır, ivme ve tutuş yarıya iner.
		vy = airborne ? vy - 0.08D : -0.08D;
		this.setDeltaMovement(-Mth.sin(yawRad) * blocksPerTick, vy, Mth.cos(yawRad) * blocksPerTick);

		// --- yakıt tüketimi ---
		if (running) {
			final float load = throttle ? 1.0F : 0.25F;
			final float burn = (0.00016F + 0.00060F * (this.rpm / (float) this.engineType().redlineRpm()))
				* load * this.model.massFactor();
			this.setFuel(this.fuel() - burn);
		}
	}

	private void updateGear(final boolean throttle, final boolean backward, final boolean running) {
		if (!running) {
			return;
		}
		if (this.speedKmh <= 0.2F && backward && this.gear >= GearBox.NEUTRAL) {
			this.gear = GearBox.REVERSE;
		} else if (this.gear == GearBox.REVERSE && throttle && this.speedKmh >= -0.2F) {
			this.gear = 1;
		} else if (this.gear == GearBox.NEUTRAL && throttle) {
			this.gear = 1;
		}

		if (this.transmissionType().automatic() && this.gear > GearBox.NEUTRAL) {
			final int next = GearBox.autoShift(this.gear, this.rpm, this.gearCount(),
				this.engineType().redlineRpm(), throttle);
			if (next != this.gear && this.shiftCooldown == 0) {
				this.gear = next;
				this.shiftCooldown = (int) (10 / this.transmissionType().shiftFactor());
			}
		}
	}

	/** Oyuncunun elle vites değiştirmesi (manuel şanzıman). */
	public void shift(final int delta) {
		if (this.transmissionType().automatic() || this.shiftCooldown > 0) {
			return;
		}
		final int next = Mth.clamp(this.gear + delta, GearBox.REVERSE, this.gearCount());
		if (next == GearBox.REVERSE && this.speedKmh > 1.0F) {
			return;
		}
		this.gear = next;
		this.shiftCooldown = (int) (6 / this.transmissionType().shiftFactor());
	}

	private void updateSteering(final Input input, final float grip) {
		final float speedFraction = Mth.clamp(Math.abs(this.speedKmh) / Math.max(1.0F, this.topSpeedKmh()), 0.0F, 1.0F);
		// Yüksek hızda direksiyon sertleşir; rüzgarlık bastırma kuvvetiyle bunu telafi eder.
		final float agility = (1.0F - 0.72F * speedFraction) + this.spoilerType().downforce();
		final float maxTurn = 3.4F * agility * Mth.clamp(grip, 0.3F, 1.4F);

		float target = 0.0F;
		if (input.left()) {
			target -= 1.0F;
		}
		if (input.right()) {
			target += 1.0F;
		}
		// Duran araç direksiyonla dönmez.
		if (Math.abs(this.speedKmh) < 0.6F) {
			target = 0.0F;
		}
		this.steerAngle = Mth.lerp(0.35F, this.steerAngle, target);

		final float direction = this.speedKmh < 0.0F ? -1.0F : 1.0F;
		final float turn = this.steerAngle * maxTurn * direction
			* Mth.clamp(Math.abs(this.speedKmh) / 12.0F, 0.0F, 1.0F);
		this.setYRot(this.getYRot() + turn);
		this.yRotO = this.getYRot() - turn;
	}

	/**
	 * Gerçekleşen konum ve yön değişiminden hızı, vitesi ve direksiyon açısını
	 * geri hesaplar. Fiziği işletmeyen taraf için bir tahmindir, ama gösterge ve
	 * partikül eşikleri için yeterince doğrudur.
	 */
	private void deriveStateFromMovement() {
		final double dx = this.getX() - this.xo;
		final double dz = this.getZ() - this.zo;
		final double travelled = Math.sqrt(dx * dx + dz * dz);

		// İlerleme yönü aracın burnuyla aynı mı, yoksa geri mi gidiyor?
		final float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		final double forwardX = -Mth.sin(yawRad);
		final double forwardZ = Mth.cos(yawRad);
		final double alignment = dx * forwardX + dz * forwardZ;
		final float sign = alignment < 0.0D ? -1.0F : 1.0F;

		this.speedKmh = (float) (travelled / KMH_TO_BLOCKS_PER_TICK) * sign;

		// Hız hangi vitesin bandına düşüyorsa o vites varsayılır.
		final float topSpeed = this.topSpeedKmh();
		if (this.speedKmh < -0.5F) {
			this.gear = GearBox.REVERSE;
		} else if (this.speedKmh < 0.5F) {
			this.gear = GearBox.NEUTRAL;
		} else {
			this.gear = 1;
			for (int g = this.gearCount(); g >= 1; g--) {
				if (this.speedKmh >= GearBox.gearBottomSpeed(g, this.gearCount(), topSpeed)) {
					this.gear = g;
					break;
				}
			}
		}
		this.rpm = GearBox.rpm(this.speedKmh, this.gear, this.gearCount(), topSpeed,
			this.engineType().redlineRpm());

		// Direksiyon açısı, bu tick'teki dönüş miktarından tahmin edilir.
		final float turn = Mth.wrapDegrees(this.getYRot() - this.yRotO);
		this.steerAngle = Mth.clamp(turn / 3.0F, -1.0F, 1.0F);
	}

	private void publishState() {
		if (!this.level().isClientSide()) {
			this.entityData.set(DATA_SPEED, this.speedKmh);
			this.entityData.set(DATA_RPM, this.rpm);
			this.entityData.set(DATA_GEAR, this.gear);
			this.entityData.set(DATA_STEER, this.steerAngle);
		}
	}

	/**
	 * Sürücünün girdisi.
	 *
	 * <p>Sunucuda oyuncunun en son bildirdiği tuş durumu okunur. İstemcide böyle
	 * bir alan yoktur; oradaki yerel tuşlar her tick {@link #setClientInput}
	 * ile aktarılır (bkz. {@code CarInputHandler}).
	 */
	private Input driverInput() {
		if (this.getControllingPassenger() instanceof ServerPlayer serverPlayer) {
			return serverPlayer.getLastClientInput();
		}
		return this.clientInput;
	}

	/** İstemci tarafında yerel oyuncunun tuş durumunu araca aktarır. */
	public void setClientInput(final Input input) {
		this.clientInput = input;
	}

	// ----------------------------------------------------------------------
	// Zemin
	// ----------------------------------------------------------------------

	private BlockPos groundPos() {
		return BlockPos.containing(this.getX(), this.getBoundingBox().minY - 0.15D, this.getZ());
	}

	public BlockState groundState() {
		return this.level().getBlockState(this.groundPos());
	}

	/**
	 * Altındaki bloğun ve takılı tekerleklerin belirlediği tutuş katsayısı.
	 * Asfaltta sokak/spor lastikler, toprakta arazi lastikleri kazanır.
	 */
	public float groundGrip() {
		if (!this.onGround()) {
			return 0.15F;
		}
		final BlockState state = this.groundState();
		final WheelType wheel = this.wheelType();
		float grip;
		if (state.is(ModTags.HIGH_GRIP)) {
			grip = wheel.roadGrip();
		} else if (state.is(ModTags.LOW_GRIP)) {
			grip = 0.22F * wheel.offroadGrip() + 0.10F;
		} else if (state.is(ModTags.DUSTY)) {
			grip = 0.30F + 0.55F * wheel.offroadGrip();
		} else {
			grip = 0.55F * wheel.roadGrip() + 0.30F * wheel.offroadGrip();
		}
		if (this.isInWater()) {
			grip *= 0.45F;
		}
		return grip + this.spoilerType().downforce();
	}

	/** Kum/toprakta toz, patinajda lastik dumanı, suda serpinti. */
	private void spawnGroundEffects() {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		final float speed = Math.abs(this.speedKmh());
		if (speed < 8.0F || !this.onGround()) {
			return;
		}
		final BlockState ground = this.groundState();
		final CarModel.Body body = this.model.body();
		final float halfWidth = body.width() / 32.0F;
		final float halfBase = body.wheelbase() / 32.0F;
		final float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		final double sin = Mth.sin(yawRad);
		final double cos = Mth.cos(yawRad);

		final boolean dusty = ground.is(ModTags.DUSTY);
		final boolean slipping = this.wheelSlip || (speed > 30.0F && Math.abs(this.steerAngle()) > 0.55F);
		if (!dusty && !slipping) {
			return;
		}

		// Dört tekerleğin yere değdiği noktalar
		for (int i = 0; i < 4; i++) {
			final double lx = (i % 2 == 0 ? -halfWidth : halfWidth);
			final double lz = (i < 2 ? -halfBase : halfBase);
			final double px = this.getX() + lx * cos - lz * sin;
			final double pz = this.getZ() + lx * sin + lz * cos;
			final double py = this.getBoundingBox().minY + 0.05D;

			if (dusty) {
				final int count = (int) Mth.clamp(speed / 22.0F, 1.0F, 4.0F);
				serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
					px, py, pz, count, 0.14D, 0.06D, 0.14D, speed * 0.0035D);
			}
			if (slipping) {
				serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
					px, py, pz, 1, 0.08D, 0.02D, 0.08D, 0.008D);
			}
		}
	}

	private void updateWheelSpin() {
		this.wheelAngleO = this.wheelAngle;
		final float radiusBlocks = this.wheelType().radius() / 16.0F;
		final double travelled = Math.abs(this.speedKmh()) * KMH_TO_BLOCKS_PER_TICK;
		// Patinajda tekerlek yoldan hızlı döner.
		final double factor = this.wheelSlip ? 2.2D : 1.0D;
		final float degrees = (float) (travelled * factor / (2.0D * Math.PI * radiusBlocks) * 360.0D);
		this.wheelAngle += this.speedKmh() < 0.0F ? -degrees : degrees;
		if (this.wheelAngle > 3600.0F || this.wheelAngle < -3600.0F) {
			// Kayan nokta hassasiyetini korumak için sarmala; interpolasyonun
			// bozulmaması için önceki değeri de aynı miktarda kaydır.
			final float wrap = this.wheelAngle > 0.0F ? -3600.0F : 3600.0F;
			this.wheelAngle += wrap;
			this.wheelAngleO += wrap;
		}
	}

	// ----------------------------------------------------------------------
	// Motor kontrolü
	// ----------------------------------------------------------------------

	public void setEngineOn(final boolean on) {
		if (on == this.engineOn()) {
			return;
		}
		if (on && this.fuel() <= 0.0F) {
			return;
		}
		this.entityData.set(DATA_ENGINE_ON, on);
		if (!on) {
			this.gear = GearBox.NEUTRAL;
		}
		if (!this.level().isClientSide()) {
			this.level().playSound(null, this, on ? ModSounds.ENGINE_START : ModSounds.ENGINE_STOP,
				SoundSource.NEUTRAL, 0.9F, 1.0F);
		}
	}

	public void honk() {
		if (!this.level().isClientSide()) {
			this.level().playSound(null, this, ModSounds.HORN, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
	}

	// ----------------------------------------------------------------------
	// Yolcu / etkileşim
	// ----------------------------------------------------------------------

	@Override
	public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
		final InteractionResult superResult = super.interact(player, hand, location);
		if (superResult != InteractionResult.PASS) {
			return superResult;
		}

		final ItemStack held = player.getItemInHand(hand);

		// İngiliz anahtarı: modifiye ekranını aç
		if (held.getItem() instanceof WrenchItem) {
			if (!this.level().isClientSide()) {
				ModMenus.openModification(player, this);
				this.level().playSound(null, this, ModSounds.WRENCH_USE, SoundSource.NEUTRAL, 0.7F, 1.0F);
			}
			return InteractionResult.SUCCESS;
		}

		// Yakıt bidonu: depoyu doldur
		if (held.getItem() instanceof FuelCanisterItem) {
			if (this.fuel() >= this.model.fuelCapacity() - 0.5F) {
				if (!this.level().isClientSide()) {
					player.sendOverlayMessage(Component.translatable("message.realcars.tank_full"));
				}
				return InteractionResult.CONSUME;
			}
			if (!this.level().isClientSide()) {
				this.setFuel(this.fuel() + FuelCanisterItem.FUEL_AMOUNT);
				held.consume(1, player);
				player.sendOverlayMessage(Component.translatable("message.realcars.refueled"));
			}
			return InteractionResult.SUCCESS;
		}

		// Sprey boya: lifte gitmeden hızlı renk değişimi
		if (held.getItem() instanceof SprayCanItem spray) {
			if (spray.carColor() == this.color()) {
				return InteractionResult.CONSUME;
			}
			if (!this.level().isClientSide()) {
				this.entityData.set(DATA_COLOR, spray.carColor());
				held.consume(1, player);
				this.level().playSound(null, this, ModSounds.WRENCH_USE, SoundSource.NEUTRAL, 0.6F, 1.4F);
			}
			return InteractionResult.SUCCESS;
		}

		if (player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!this.level().isClientSide() && !player.startRiding(this)) {
			return InteractionResult.PASS;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return this.getFirstPassenger() instanceof LivingEntity passenger ? passenger : super.getControllingPassenger();
	}

	@Override
	protected boolean canAddPassenger(final Entity passenger) {
		return this.getPassengers().size() < this.maxPassengers();
	}

	@Override
	protected void addPassenger(final Entity passenger) {
		super.addPassenger(passenger);
		this.playDoorSound(passenger);
	}

	@Override
	protected void removePassenger(final Entity passenger) {
		super.removePassenger(passenger);
		this.playDoorSound(passenger);
	}

	/**
	 * Binip inerken kapı sesi. Sunucudan çalınır ki yalnızca binen değil
	 * çevredeki oyuncular da duysun; perde biraz rastgeledir, aynı ses üst üste
	 * duyulduğunda mekanik gelmesin diye.
	 */
	private void playDoorSound(final Entity passenger) {
		if (this.level().isClientSide() || !(passenger instanceof Player)) {
			return;
		}
		this.level().playSound(null, this, ModSounds.DOOR_CLOSE, SoundSource.NEUTRAL,
			0.8F, 0.95F + this.random.nextFloat() * 0.1F);
	}

	private int maxPassengers() {
		return switch (this.model.carClass()) {
			case SPORT -> 2;
			case VAN, SUV -> 4;
			default -> 3;
		};
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(final Entity passenger, final net.minecraft.world.entity.EntityDimensions dimensions,
											   final float scale) {
		final CarModel.Body body = this.model.body();
		final int index = Math.max(0, this.getPassengers().indexOf(passenger));
		// Sürücü sol ön, sonraki yolcular sağ ön ve arka koltuklar.
		final float x = (index == 1 || index == 3) ? body.width() / 56.0F : -body.width() / 56.0F;
		final float z = index < 2 ? (body.cabinCenterZ() - 4.0F) / 16.0F : (body.cabinCenterZ() + 8.0F) / 16.0F;
		final float y = body.seatHeight() / 16.0F;
		return new Vec3(x, y, z);
	}

	@Override
	protected void positionRider(final Entity passenger, final Entity.MoveFunction moveFunction) {
		super.positionRider(passenger, moveFunction);
		this.clampRotation(passenger);
	}

	private void clampRotation(final Entity passenger) {
		passenger.setYBodyRot(this.getYRot());
		final float delta = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
		final float clamped = Mth.clamp(delta, -110.0F, 110.0F);
		passenger.yRotO += clamped - delta;
		passenger.setYRot(passenger.getYRot() + clamped - delta);
		passenger.setYHeadRot(passenger.getYRot());
	}

	@Override
	public void onPassengerTurned(final Entity passenger) {
		this.clampRotation(passenger);
	}

	@Override
	public boolean canBeCollidedWith(final @Nullable Entity other) {
		return true;
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	public boolean canCollideWith(final Entity entity) {
		return (entity.canBeCollidedWith(this) || entity.isPushable()) && !this.isPassengerOfSameVehicle(entity);
	}

	@Override
	public @Nullable InterpolationHandler getInterpolation() {
		return this.interpolation;
	}

	// ----------------------------------------------------------------------
	// Kayıt / düşürme
	// ----------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		output.store("Config", CarConfig.CODEC, this.config());
		output.putInt("Gear", this.gear);
		output.putFloat("Speed", this.speedKmh);
		output.putBoolean("EngineOn", this.engineOn());
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		input.read("Config", CarConfig.CODEC).ifPresent(this::applyConfig);
		this.gear = input.getIntOr("Gear", GearBox.NEUTRAL);
		this.speedKmh = input.getFloatOr("Speed", 0.0F);
		this.entityData.set(DATA_ENGINE_ON, input.getBooleanOr("EngineOn", false));
	}

	@Override
	protected Item getDropItem() {
		return ModItems.car(this.model);
	}

	@Override
	public ItemStack getPickResult() {
		final ItemStack stack = new ItemStack(this.getDropItem());
		ModItems.writeConfig(stack, this.config());
		return stack;
	}

	@Override
	public void destroy(final ServerLevel level, final Item dropItem) {
		this.kill(level);
		if (level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.ENTITY_DROPS)) {
			final ItemStack stack = new ItemStack(dropItem);
			ModItems.writeConfig(stack, this.config());
			stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, this.getCustomName());
			this.spawnAtLocation(level, stack);
		}
		level.playSound(null, this, ModSounds.CRASH, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}
}
