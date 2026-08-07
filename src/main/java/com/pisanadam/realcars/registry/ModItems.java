package com.pisanadam.realcars.registry;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.entity.CarConfig;
import com.pisanadam.realcars.entity.CarModel;
import com.pisanadam.realcars.entity.ChassisType;
import com.pisanadam.realcars.entity.EngineType;
import com.pisanadam.realcars.entity.SpoilerType;
import com.pisanadam.realcars.entity.TransmissionType;
import com.pisanadam.realcars.entity.WheelType;
import com.pisanadam.realcars.item.CarItem;
import com.pisanadam.realcars.item.FuelCanisterItem;
import com.pisanadam.realcars.item.SprayCanItem;
import com.pisanadam.realcars.item.WrenchItem;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Modun tüm item'ları ve yaratıcı mod sekmesi. */
public final class ModItems {
	private static final Map<CarModel, Item> CARS = new EnumMap<>(CarModel.class);
	private static final Map<WheelType, Item> WHEELS = new EnumMap<>(WheelType.class);
	private static final Map<SpoilerType, Item> SPOILERS = new EnumMap<>(SpoilerType.class);
	private static final Map<EngineType, Item> ENGINES = new EnumMap<>(EngineType.class);
	private static final Map<TransmissionType, Item> TRANSMISSIONS = new EnumMap<>(TransmissionType.class);
	private static final Map<ChassisType, Item> CHASSIS = new EnumMap<>(ChassisType.class);
	private static final Map<DyeColor, Item> SPRAY_CANS = new EnumMap<>(DyeColor.class);

	public static final Item STEEL_PLATE = simple("steel_plate");
	public static final Item RUBBER = simple("rubber");
	public static final Item GLASS_PANEL = simple("glass_panel");
	public static final Item CAR_SEAT = simple("car_seat");
	public static final Item WINDSHIELD = simple("windshield");
	public static final Item HEADLIGHT = simple("headlight");
	public static final Item CAR_MANUAL = simple("car_manual");
	public static final Item WRENCH = register("wrench", WrenchItem::new, new Item.Properties().stacksTo(1));
	public static final Item FUEL_CANISTER =
		register("fuel_canister", FuelCanisterItem::new, new Item.Properties().stacksTo(16));

	static {
		for (final ChassisType chassis : ChassisType.values()) {
			CHASSIS.put(chassis, simple(chassis.itemName()));
		}
		for (final EngineType engine : EngineType.values()) {
			ENGINES.put(engine, simple(engine.itemName()));
		}
		for (final TransmissionType transmission : TransmissionType.values()) {
			TRANSMISSIONS.put(transmission, simple(transmission.itemName()));
		}
		for (final WheelType wheel : WheelType.values()) {
			WHEELS.put(wheel, simple(wheel.itemName()));
		}
		for (final SpoilerType spoiler : SpoilerType.values()) {
			if (spoiler.hasItem()) {
				SPOILERS.put(spoiler, simple(spoiler.itemName()));
			}
		}
		for (final DyeColor dye : DyeColor.values()) {
			SPRAY_CANS.put(dye, register("spray_can_" + dye.getSerializedName(),
				properties -> new SprayCanItem(properties, dye), new Item.Properties().stacksTo(16)));
		}
		for (final CarModel model : CarModel.values()) {
			CARS.put(model, register(model.itemName(),
				properties -> new CarItem(properties, model), new Item.Properties().stacksTo(1)));
		}
	}

	/** Yaratıcı mod sekmesi — tüm parçalar, boyalar ve arabalar burada listelenir. */
	public static final CreativeModeTab TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB, RealCars.id("main"),
		net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.realcars.main"))
			.icon(() -> new ItemStack(CARS.get(CarModel.CAR_MUSTANG_GT)))
			.displayItems((parameters, output) -> {
				output.accept(STEEL_PLATE);
				output.accept(RUBBER);
				output.accept(GLASS_PANEL);
				output.accept(CAR_SEAT);
				output.accept(WINDSHIELD);
				output.accept(HEADLIGHT);
				CHASSIS.values().forEach(output::accept);
				ENGINES.values().forEach(output::accept);
				TRANSMISSIONS.values().forEach(output::accept);
				WHEELS.values().forEach(output::accept);
				SPOILERS.values().forEach(output::accept);
				output.accept(WRENCH);
				output.accept(FUEL_CANISTER);
				output.accept(CAR_MANUAL);
				SPRAY_CANS.values().forEach(output::accept);
				for (final CarModel model : CarModel.values()) {
					final ItemStack stack = new ItemStack(CARS.get(model));
					writeConfig(stack, CarConfig.factory(model));
					output.accept(stack);
				}
				ModBlocks.addToCreativeTab(output);
			})
			.build());

	private ModItems() {
	}

	private static Item simple(final String name) {
		return register(name, Item::new, new Item.Properties());
	}

	private static Item register(final String name, final Function<Item.Properties, Item> factory,
								 final Item.Properties properties) {
		final ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, RealCars.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	// -- arama ---------------------------------------------------------------

	public static Item car(final CarModel model) {
		return CARS.get(model);
	}

	public static Item wheel(final WheelType wheel) {
		return WHEELS.get(wheel);
	}

	public static Item spoiler(final SpoilerType spoiler) {
		return SPOILERS.get(spoiler);
	}

	public static Item engine(final EngineType engine) {
		return ENGINES.get(engine);
	}

	public static Item transmission(final TransmissionType transmission) {
		return TRANSMISSIONS.get(transmission);
	}

	public static Item chassis(final ChassisType chassis) {
		return CHASSIS.get(chassis);
	}

	public static Item sprayCan(final DyeColor dye) {
		return SPRAY_CANS.get(dye);
	}

	/** Araç item'ına yapılandırmayı yazar (boya, parçalar, yakıt). */
	public static void writeConfig(final ItemStack stack, final CarConfig config) {
		stack.set(ModComponents.CAR_CONFIG, config);
	}

	/** Araç item'ındaki yapılandırma; yoksa fabrika ayarları döner. */
	public static CarConfig readConfig(final ItemStack stack, final CarModel model) {
		final CarConfig stored = stack.get(ModComponents.CAR_CONFIG);
		return stored != null ? stored : CarConfig.factory(model);
	}

	public static void init() {
	}
}
