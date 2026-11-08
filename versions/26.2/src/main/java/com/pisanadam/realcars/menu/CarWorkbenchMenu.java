package com.pisanadam.realcars.menu;

import com.pisanadam.realcars.entity.CarModel;
import com.pisanadam.realcars.registry.ModBlocks;
import com.pisanadam.realcars.registry.ModItems;
import com.pisanadam.realcars.registry.ModMenus;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Araba yapma masasının menüsü.
 *
 * <p>Sıradan bir 3x3 tezgahta arabayı yapmak için parçaların hangi gözlere
 * gideceğini ezberlemek gerekir. Burada her parçanın kendi adlandırılmış yuvası
 * var: şasi, motor, şanzıman, tekerlek (dört adet), koltuk ve ön cam. Doğru
 * bileşim konunca çıkan araba sağdaki gözde görünür.
 *
 * <p>Hangi arabanın çıkacağına bu sınıf karar vermez: parçalar aracın gerçek
 * 3x3 kalıbına dizilip sunucunun tarif yöneticisine sorulur. Böylece masa,
 * tariflerle her zaman tutarlı kalır — yeni bir araba eklendiğinde burada
 * değiştirilecek hiçbir şey yoktur.
 */
public class CarWorkbenchMenu extends AbstractContainerMenu {
	public static final int SLOT_CHASSIS = 0;
	public static final int SLOT_ENGINE = 1;
	public static final int SLOT_TRANSMISSION = 2;
	public static final int SLOT_WHEELS = 3;
	public static final int SLOT_SEAT = 4;
	public static final int SLOT_WINDSHIELD = 5;
	/** Girdi yuvası sayısı. */
	public static final int INPUT_SLOTS = 6;
	/** Bir araba için gereken tekerlek sayısı. */
	public static final int WHEELS_NEEDED = 4;
	/** Sonuç gözünün menü içindeki sırası. */
	public static final int RESULT_SLOT = INPUT_SLOTS;

	private static final int SLOT_X = 8;
	private static final int SLOT_Y = 35;
	private static final int RESULT_X = 145;

	private final ContainerLevelAccess access;
	private final Player player;
	private final Container inputs = new SimpleContainer(INPUT_SLOTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			CarWorkbenchMenu.this.slotsChanged(this);
		}
	};
	private final ResultContainer result = new ResultContainer();

	public CarWorkbenchMenu(final int containerId, final Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public CarWorkbenchMenu(final int containerId, final Inventory inventory,
							final ContainerLevelAccess access) {
		super(ModMenus.CAR_WORKBENCH, containerId);
		this.access = access;
		this.player = inventory.player;

		this.addSlot(partSlot(SLOT_CHASSIS, ModItems::isChassis));
		this.addSlot(partSlot(SLOT_ENGINE, ModItems::isEngine));
		this.addSlot(partSlot(SLOT_TRANSMISSION, ModItems::isTransmission));
		this.addSlot(partSlot(SLOT_WHEELS, ModItems::isWheel));
		this.addSlot(partSlot(SLOT_SEAT, stack -> stack.is(ModItems.CAR_SEAT)));
		this.addSlot(partSlot(SLOT_WINDSHIELD, stack -> stack.is(ModItems.WINDSHIELD)));
		this.addSlot(new CarSlot(this.result, 0, RESULT_X, SLOT_Y));

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(inventory, col, 8 + col * 18, 176));
		}
	}

	private Slot partSlot(final int index, final java.util.function.Predicate<ItemStack> accepts) {
		return new Slot(this.inputs, index, SLOT_X + index * 18, SLOT_Y) {
			@Override
			public boolean mayPlace(final ItemStack stack) {
				return accepts.test(stack);
			}
		};
	}

	@Override
	public void slotsChanged(final Container container) {
		this.access.execute((level, pos) -> {
			if (level instanceof ServerLevel serverLevel) {
				this.result.setItem(0, this.assembleCar(serverLevel));
				this.broadcastChanges();
			}
		});
	}

	/** Parçaları aracın gerçek kalıbına dizip tariflerden çıkan arabayı bulur. */
	private ItemStack assembleCar(final ServerLevel level) {
		if (this.inputs.getItem(SLOT_WHEELS).getCount() < WHEELS_NEEDED) {
			return ItemStack.EMPTY;
		}
		final CraftingInput input = this.craftingInput();
		return level.getServer().getRecipeManager()
			.getRecipeFor(RecipeType.CRAFTING, input, level)
			.map(holder -> holder.value().assemble(input))
			.orElse(ItemStack.EMPTY);
	}

	private CraftingInput craftingInput() {
		final ItemStack wheel = this.one(SLOT_WHEELS);
		return CraftingInput.of(3, 3, List.of(
			wheel, this.one(SLOT_WINDSHIELD), wheel,
			this.one(SLOT_ENGINE), this.one(SLOT_CHASSIS), this.one(SLOT_TRANSMISSION),
			wheel, this.one(SLOT_SEAT), wheel));
	}

	private ItemStack one(final int slot) {
		final ItemStack stack = this.inputs.getItem(slot);
		return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
	}

	/**
	 * Katalogdan bir arabaya tıklanınca o arabanın parçalarını oyuncunun
	 * envanterinden alıp gözlere dizer.
	 *
	 * <p>Hangi parçanın gerektiğini araç tanımının kendisi söyler, yani katalog
	 * tariflerle aynı kaynaktan beslenir. Eksik bir parça varsa dizim yine de
	 * yapılır (elde ne varsa yerine konur) ve eksiğin adı oyuncuya söylenir —
	 * böylece neyi craftlaması gerektiğini görür.
	 */
	@Override
	public boolean clickMenuButton(final Player who, final int buttonId) {
		final CarModel[] models = CarModel.values();
		if (buttonId < 0 || buttonId >= models.length || !(who instanceof ServerPlayer)) {
			return false;
		}
		final CarModel model = models[buttonId];
		ItemStack missing = ItemStack.EMPTY;
		for (final Requirement need : requirements(model)) {
			if (!this.stock(who, need.slot(), need.item(), need.count()) && missing.isEmpty()) {
				missing = new ItemStack(need.item());
			}
		}
		this.inputs.setChanged();
		who.sendOverlayMessage(missing.isEmpty()
			? Component.translatable("message.realcars.parts_loaded")
			: Component.translatable("message.realcars.missing_part", missing.getHoverName()));
		return true;
	}

	/** Bir aracın hangi gözde neye ihtiyacı olduğu. */
	public record Requirement(int slot, Item item, int count) {
	}

	/** Verilen aracın parça listesi — tarifle aynı bileşim. */
	public static List<Requirement> requirements(final CarModel model) {
		return List.of(
			new Requirement(SLOT_CHASSIS, ModItems.chassis(model.chassis()), 1),
			new Requirement(SLOT_ENGINE, ModItems.engine(model.defaultEngine()), 1),
			new Requirement(SLOT_TRANSMISSION, ModItems.transmission(model.defaultTransmission()), 1),
			new Requirement(SLOT_WHEELS, ModItems.wheel(model.defaultWheel()), WHEELS_NEEDED),
			new Requirement(SLOT_SEAT, ModItems.CAR_SEAT, 1),
			new Requirement(SLOT_WINDSHIELD, ModItems.WINDSHIELD, 1));
	}

	/**
	 * Bir gözü istenen parçayla doldurur; başaramazsa false döner.
	 * Gözde yanlış bir şey varsa oyuncuya geri verilir.
	 */
	private boolean stock(final Player who, final int slot, final Item item, final int count) {
		final ItemStack current = this.inputs.getItem(slot);
		if (current.is(item) && current.getCount() >= count) {
			return true;
		}
		if (!current.isEmpty()) {
			who.getInventory().placeItemBackInInventory(current);
			this.inputs.setItem(slot, ItemStack.EMPTY);
		}
		if (who.getAbilities().instabuild) {
			this.inputs.setItem(slot, new ItemStack(item, count));
			return true;
		}
		final int taken = takeFromInventory(who, item, count);
		if (taken > 0) {
			this.inputs.setItem(slot, new ItemStack(item, taken));
		}
		return taken >= count;
	}

	/** Envanterden en çok {@code count} adet alır, gerçekten alınanı döndürür. */
	private static int takeFromInventory(final Player who, final Item item, final int count) {
		int remaining = count;
		for (int index = 0; index < who.getInventory().getContainerSize() && remaining > 0; index++) {
			final ItemStack stack = who.getInventory().getItem(index);
			if (!stack.is(item)) {
				continue;
			}
			final int move = Math.min(remaining, stack.getCount());
			stack.shrink(move);
			remaining -= move;
		}
		return count - remaining;
	}

	/** Araba alınınca parçaları harcar. */
	private void consumeParts() {
		for (int slot = 0; slot < INPUT_SLOTS; slot++) {
			this.inputs.removeItem(slot, slot == SLOT_WHEELS ? WHEELS_NEEDED : 1);
		}
	}

	@Override
	public boolean stillValid(final Player who) {
		return stillValid(this.access, who, ModBlocks.CAR_WORKBENCH);
	}

	@Override
	public void removed(final Player who) {
		super.removed(who);
		this.result.clearContent();
		this.access.execute((level, pos) -> this.clearContainer(who, this.inputs));
	}

	@Override
	public ItemStack quickMoveStack(final Player who, final int slotIndex) {
		final Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		final ItemStack stack = slot.getItem();
		final ItemStack original = stack.copy();
		final int inventoryStart = RESULT_SLOT + 1;

		if (slotIndex <= RESULT_SLOT) {
			// Masadan envantere
			if (!this.moveItemStackTo(stack, inventoryStart, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack, original);
		} else if (!this.moveItemStackTo(stack, 0, INPUT_SLOTS, false)) {
			// Envanterden masaya; olmuyorsa envanter içinde taşı
			final int hotbarStart = this.slots.size() - 9;
			final boolean fromHotbar = slotIndex >= hotbarStart;
			final boolean moved = fromHotbar
				? this.moveItemStackTo(stack, inventoryStart, hotbarStart, false)
				: this.moveItemStackTo(stack, hotbarStart, this.slots.size(), false);
			if (!moved) {
				return ItemStack.EMPTY;
			}
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	/** Sonuç gözü: içine bir şey konamaz, alınınca parçalar harcanır. */
	private class CarSlot extends Slot {
		CarSlot(final Container container, final int index, final int x, final int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(final ItemStack stack) {
			return false;
		}

		@Override
		public void onTake(final Player taker, final ItemStack stack) {
			CarWorkbenchMenu.this.consumeParts();
			if (taker instanceof ServerPlayer) {
				CarWorkbenchMenu.this.slotsChanged(CarWorkbenchMenu.this.inputs);
			}
			super.onTake(taker, stack);
		}
	}
}
