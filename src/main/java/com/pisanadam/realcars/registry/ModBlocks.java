package com.pisanadam.realcars.registry;

import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.block.AssemblyTableBlock;
import com.pisanadam.realcars.block.CarLiftBlock;
import com.pisanadam.realcars.block.CarWorkbenchBlock;
import com.pisanadam.realcars.block.FuelPumpBlock;
import com.pisanadam.realcars.block.RoadLineBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Modun blokları: garaj ekipmanları ve yol yapı blokları. */
public final class ModBlocks {
	private static final List<Item> TAB_ITEMS = new ArrayList<>();

	public static final Block ASPHALT = register("asphalt", Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
			.strength(1.6F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));

	public static final Block ASPHALT_SLAB = register("asphalt_slab", SlabBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
			.strength(1.6F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));

	public static final Block ROAD_LINE_WHITE = register("road_line_white", RoadLineBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
			.strength(1.6F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));

	public static final Block ROAD_LINE_YELLOW = register("road_line_yellow", RoadLineBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
			.strength(1.6F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));

	public static final Block ASSEMBLY_TABLE = register("assembly_table", AssemblyTableBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
			.strength(2.5F).requiresCorrectToolForDrops().sound(SoundType.WOOD));

	public static final Block CAR_WORKBENCH = register("car_workbench", CarWorkbenchBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
			.strength(3.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));

	public static final Block CAR_LIFT = register("car_lift", CarLiftBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
			.strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL));

	public static final Block FUEL_PUMP = register("fuel_pump", FuelPumpBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
			.strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL));

	private ModBlocks() {
	}

	private static Block register(final String name, final Function<BlockBehaviour.Properties, Block> factory,
								  final BlockBehaviour.Properties properties) {
		final ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, RealCars.id(name));
		final Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
			factory.apply(properties.setId(blockKey)));

		final ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, RealCars.id(name));
		final BlockItem blockItem = new BlockItem(block,
			new Item.Properties().useBlockDescriptionPrefix().setId(itemKey));
		// Blok -> item eşlemesi olmadan orta tık ("pick block") çalışmaz.
		blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
		TAB_ITEMS.add(Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem));
		return block;
	}

	static void addToCreativeTab(final CreativeModeTab.Output output) {
		TAB_ITEMS.forEach(output::accept);
	}

	public static void init() {
	}
}
