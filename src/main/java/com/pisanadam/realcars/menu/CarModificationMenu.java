package com.pisanadam.realcars.menu;

import com.pisanadam.realcars.entity.CarColors;
import com.pisanadam.realcars.entity.CarConfig;
import com.pisanadam.realcars.entity.CarEntity;
import com.pisanadam.realcars.entity.EngineType;
import com.pisanadam.realcars.entity.SpoilerType;
import com.pisanadam.realcars.entity.TransmissionType;
import com.pisanadam.realcars.entity.WheelType;
import com.pisanadam.realcars.registry.ModItems;
import com.pisanadam.realcars.registry.ModMenus;
import com.pisanadam.realcars.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Araç modifiye menüsü.
 *
 * <p>Aracın kendisi bir sandık değildir; menü yalnızca oyuncunun envanterini
 * gösterir ve düğme tıklamalarını ({@link #clickMenuButton}) modifiye
 * komutlarına çevirir. Böylece takılacak parça envanterden alınır, sökülen
 * parça geri verilir.
 *
 * <p>Düğme kimlikleri kategoriye göre bloklara ayrılmıştır; istemci de aynı
 * şemayı kullanır ({@link #colorButton} vb.).
 */
public class CarModificationMenu extends AbstractContainerMenu {
	private static final int COLOR_BASE = 0;
	private static final int WHEEL_BASE = 100;
	private static final int SPOILER_BASE = 200;
	private static final int ENGINE_BASE = 300;
	private static final int TRANSMISSION_BASE = 400;

	/** Bir araçta dört tekerlek vardır; tekerlek değişimi dördünü birden ister. */
	private static final int WHEEL_COUNT = 4;

	private final Player player;
	private final @Nullable CarEntity car;

	public CarModificationMenu(final int containerId, final Inventory inventory, final int carEntityId) {
		super(ModMenus.CAR_MODIFICATION, containerId);
		this.player = inventory.player;
		final Entity found = inventory.player.level().getEntity(carEntityId);
		this.car = found instanceof CarEntity carEntity ? carEntity : null;

		// Oyuncu envanteri — modifiye ekranının alt yarısı
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 143 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(inventory, col, 8 + col * 18, 201));
		}
	}

	public @Nullable CarEntity car() {
		return this.car;
	}

	public static int colorButton(final int index) {
		return COLOR_BASE + index;
	}

	public static int wheelButton(final WheelType wheel) {
		return WHEEL_BASE + wheel.ordinal();
	}

	public static int spoilerButton(final SpoilerType spoiler) {
		return SPOILER_BASE + spoiler.ordinal();
	}

	public static int engineButton(final EngineType engine) {
		return ENGINE_BASE + engine.ordinal();
	}

	public static int transmissionButton(final TransmissionType transmission) {
		return TRANSMISSION_BASE + transmission.ordinal();
	}

	@Override
	public boolean clickMenuButton(final Player clicker, final int buttonId) {
		if (this.car == null || !this.car.isAlive() || !this.stillValid(clicker)) {
			return false;
		}
		final CarConfig current = this.car.config();
		final boolean creative = clicker.getAbilities().instabuild;

		if (buttonId >= TRANSMISSION_BASE) {
			final TransmissionType target = byOrdinal(TransmissionType.values(), buttonId - TRANSMISSION_BASE);
			if (target == null || target == current.transmission()) {
				return false;
			}
			return this.swapPart(clicker, creative,
				ModItems.transmission(target), 1,
				ModItems.transmission(current.transmission()), 1,
				current.withTransmission(target));
		}
		if (buttonId >= ENGINE_BASE) {
			final EngineType target = byOrdinal(EngineType.values(), buttonId - ENGINE_BASE);
			if (target == null || target == current.engine()) {
				return false;
			}
			return this.swapPart(clicker, creative,
				ModItems.engine(target), 1,
				ModItems.engine(current.engine()), 1,
				current.withEngine(target));
		}
		if (buttonId >= SPOILER_BASE) {
			final SpoilerType target = byOrdinal(SpoilerType.values(), buttonId - SPOILER_BASE);
			if (target == null || target == current.spoiler()) {
				return false;
			}
			// "Rüzgarlıksız" bir parça değildir: takarken bir şey istemez,
			// sökerken eski rüzgarlığı geri verir.
			final Item needed = target.hasItem() ? ModItems.spoiler(target) : null;
			final Item returned = current.spoiler().hasItem() ? ModItems.spoiler(current.spoiler()) : null;
			return this.swapPart(clicker, creative, needed, 1, returned, 1, current.withSpoiler(target));
		}
		if (buttonId >= WHEEL_BASE) {
			final WheelType target = byOrdinal(WheelType.values(), buttonId - WHEEL_BASE);
			if (target == null || target == current.wheel()) {
				return false;
			}
			return this.swapPart(clicker, creative,
				ModItems.wheel(target), WHEEL_COUNT,
				ModItems.wheel(current.wheel()), WHEEL_COUNT,
				current.withWheel(target));
		}

		final int colorIndex = buttonId - COLOR_BASE;
		if (colorIndex < 0 || colorIndex >= CarColors.count()) {
			return false;
		}
		final int newColor = CarColors.byIndex(colorIndex);
		if (newColor == current.color()) {
			return false;
		}
		final Item sprayCan = ModItems.sprayCan(DyeColor.values()[colorIndex]);
		return this.swapPart(clicker, creative, sprayCan, 1, null, 0, current.withColor(newColor));
	}

	/**
	 * Gerekli parçayı envanterden düşer, sökülen parçayı geri verir ve yeni
	 * yapılandırmayı araca uygular. Parça yoksa hiçbir şey değişmez.
	 */
	private boolean swapPart(final Player clicker, final boolean creative,
							 final @Nullable Item needed, final int neededCount,
							 final @Nullable Item returned, final int returnedCount,
							 final CarConfig newConfig) {
		if (this.car == null) {
			return false;
		}
		if (!creative && needed != null && !this.consume(clicker, needed, neededCount)) {
			return false;
		}
		if (!creative && returned != null && returnedCount > 0) {
			this.give(clicker, returned, returnedCount);
		}
		this.car.applyConfig(newConfig);
		clicker.level().playSound(null, this.car, ModSounds.WRENCH_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
		return true;
	}

	private boolean consume(final Player clicker, final Item item, final int count) {
		int remaining = count;
		for (final ItemStack stack : clicker.getInventory()) {
			if (stack.is(item)) {
				remaining -= stack.getCount();
				if (remaining <= 0) {
					break;
				}
			}
		}
		if (remaining > 0) {
			return false;
		}
		remaining = count;
		for (final ItemStack stack : clicker.getInventory()) {
			if (remaining <= 0) {
				break;
			}
			if (stack.is(item)) {
				final int take = Math.min(remaining, stack.getCount());
				stack.shrink(take);
				remaining -= take;
			}
		}
		return true;
	}

	private void give(final Player clicker, final Item item, final int count) {
		final ItemStack stack = new ItemStack(item, count);
		if (!clicker.getInventory().add(stack) && !stack.isEmpty()) {
			clicker.drop(stack, false);
		}
	}

	private static <T> @Nullable T byOrdinal(final T[] values, final int index) {
		return index >= 0 && index < values.length ? values[index] : null;
	}

	@Override
	public ItemStack quickMoveStack(final Player clicker, final int slotIndex) {
		// Menüde araç yuvası olmadığı için shift-tık envanter içinde taşır.
		final Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		final ItemStack stack = slot.getItem();
		final ItemStack copy = stack.copy();
		final boolean fromMainInventory = slotIndex < 27;
		final boolean moved = fromMainInventory
			? this.moveItemStackTo(stack, 27, 36, false)
			: this.moveItemStackTo(stack, 0, 27, false);
		if (!moved) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	@Override
	public boolean stillValid(final Player clicker) {
		return this.car != null && this.car.isAlive() && this.car.distanceToSqr(clicker) < 64.0D;
	}
}
