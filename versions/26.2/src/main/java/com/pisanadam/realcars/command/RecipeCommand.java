package com.pisanadam.realcars.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.pisanadam.realcars.RealCars;
import com.pisanadam.realcars.recipe.ModRecipeBook;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

/**
 * {@code /cars recipes} — modun tariflerini sohbette gösterir.
 *
 * <p>Tarif kitabı zaten her şeyi açıyor ({@link ModRecipeBook}), ama kitabı
 * açmadan "asfalt nasıl yapılıyordu" diye bakmak isteyen için ızgarayı düz
 * yazıyla basan bir komut daha faydalı. Üç yoldan da aynı yere çıkılır:
 *
 * <ul>
 *   <li>{@code /cars recipes} — bütün tariflerin tıklanabilir listesi</li>
 *   <li>{@code /cars recipes asphalt} — tek bir eşyanın yapılışı</li>
 *   <li>{@code /recipe cars ...} — aynısı, vanilla {@code /recipe} komutunun
 *       altına eklenmiş hâli (o komut yetkili oyuncuya açık olduğu için
 *       normal oyuncu {@code /cars} yolunu kullanır)</li>
 * </ul>
 */
public final class RecipeCommand {
	/** Izgaradaki dolu gözlere sırayla verilen harfler. */
	private static final String LETTERS = "ABCDEFGHI";
	/** Bir satıra kaç tarif adı sığdırılacağı. */
	private static final int INDEX_COLUMNS = 4;

	private static final SuggestionProvider<CommandSourceStack> RESULTS = (context, builder) ->
		SharedSuggestionProvider.suggest(
			resultIds(context.getSource()).stream().map(Identifier::getPath), builder);

	private RecipeCommand() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			dispatcher.register(Commands.literal("cars")
				.executes(context -> index(context.getSource()))
				.then(recipes("recipes")));
			dispatcher.register(Commands.literal("realcars")
				.executes(context -> index(context.getSource()))
				.then(recipes("recipes")));
			// Vanilla /recipe düğümüne kaynaşır: Brigadier aynı adlı kökleri
			// birleştirdiği için /recipe give ve /recipe take yerinde kalır.
			dispatcher.register(Commands.literal("recipe").then(recipes("cars")));
		});
	}

	private static LiteralArgumentBuilder<CommandSourceStack> recipes(final String name) {
		return Commands.literal(name)
			.executes(context -> index(context.getSource()))
			.then(Commands.argument("item", StringArgumentType.word())
				.suggests(RESULTS)
				.executes(context -> show(context.getSource(),
					StringArgumentType.getString(context, "item"))));
	}

	// ----------------------------------------------------------------- liste

	/** Bütün tariflerin adı; her biri kendi yapılışını açan bir bağlantı. */
	private static int index(final CommandSourceStack source) {
		final List<RecipeHolder<?>> recipes = ModRecipeBook.modRecipes(source.getServer());
		if (recipes.isEmpty()) {
			source.sendFailure(Component.translatable("command.realcars.recipes.empty"));
			return 0;
		}
		final ContextMap context = SlotDisplayContext.fromLevel(source.getLevel());

		final List<Component> entries = new ArrayList<>();
		final List<Identifier> seen = new ArrayList<>();
		for (final RecipeHolder<?> holder : recipes) {
			final ItemStack result = result(holder, context);
			final Identifier id = idOf(result);
			if (result.isEmpty() || id == null || seen.contains(id)) {
				continue;
			}
			seen.add(id);
			entries.add(link(result.getHoverName(), id));
		}

		source.sendSuccess(() -> Component.translatable("command.realcars.recipes.title", entries.size())
			.withStyle(ChatFormatting.GOLD), false);
		for (int start = 0; start < entries.size(); start += INDEX_COLUMNS) {
			final MutableComponent line = Component.empty();
			for (int i = start; i < Math.min(start + INDEX_COLUMNS, entries.size()); i++) {
				if (i > start) {
					line.append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY));
				}
				line.append(entries.get(i));
			}
			source.sendSuccess(() -> line, false);
		}
		source.sendSuccess(() -> Component.translatable("command.realcars.recipes.hint")
			.withStyle(ChatFormatting.DARK_GRAY), false);
		return entries.size();
	}

	private static Component link(final Component name, final Identifier id) {
		return name.copy().withStyle(style -> style
			.withColor(ChatFormatting.AQUA)
			.withClickEvent(new ClickEvent.RunCommand("/cars recipes " + id.getPath()))
			.withHoverEvent(new HoverEvent.ShowText(
				Component.translatable("command.realcars.recipes.click"))));
	}

	// ---------------------------------------------------------------- yapılış

	/** Adı verilen eşyayı üreten bütün tarifleri ızgara olarak yazar. */
	private static int show(final CommandSourceStack source, final String query) {
		final ContextMap context = SlotDisplayContext.fromLevel(source.getLevel());
		final String wanted = query.contains(":") ? query : RealCars.MOD_ID + ":" + query;

		int found = 0;
		for (final RecipeHolder<?> holder : ModRecipeBook.modRecipes(source.getServer())) {
			final Identifier id = idOf(result(holder, context));
			if (id == null || (!id.toString().equals(wanted) && !id.getPath().equals(query))) {
				continue;
			}
			print(source, holder, context);
			found++;
		}
		if (found == 0) {
			source.sendFailure(Component.translatable("command.realcars.recipes.unknown", query));
		}
		return found;
	}

	private static void print(final CommandSourceStack source, final RecipeHolder<?> holder,
							  final ContextMap context) {
		final List<RecipeDisplay> displays = holder.value().display();
		if (displays.isEmpty()) {
			return;
		}
		final RecipeDisplay display = displays.getFirst();
		final ItemStack result = display.result().resolveForFirstStack(context);

		source.sendSuccess(() -> Component.translatable("command.realcars.recipes.header",
			result.getHoverName(), result.getCount()).withStyle(ChatFormatting.GOLD), false);

		switch (display) {
			case ShapedCraftingRecipeDisplay shaped -> grid(source, shaped, context);
			case ShapelessCraftingRecipeDisplay shapeless -> loose(source, shapeless.ingredients(), context);
			default -> source.sendSuccess(() -> Component.translatable("command.realcars.recipes.other")
				.withStyle(ChatFormatting.GRAY), false);
		}
	}

	/**
	 * Şekilli tarif: 3x3 kutucuk çizilir, altına harf açıklaması yazılır.
	 *
	 * <p>Aynı malzemeye hep aynı harf düşsün diye harfler malzemenin
	 * <em>ilk göründüğü</em> sırada dağıtılır; böylece ızgara ile açıklama
	 * listesi baştan sona tutarlı okunur.
	 */
	private static void grid(final CommandSourceStack source, final ShapedCraftingRecipeDisplay shaped,
							 final ContextMap context) {
		final Map<String, Character> letters = new LinkedHashMap<>();
		final Map<Character, Component> legend = new LinkedHashMap<>();
		final List<SlotDisplay> slots = shaped.ingredients();

		for (int row = 0; row < shaped.height(); row++) {
			final StringBuilder line = new StringBuilder("  ");
			for (int column = 0; column < shaped.width(); column++) {
				final int index = row * shaped.width() + column;
				final SlotDisplay slot = index < slots.size() ? slots.get(index) : SlotDisplay.Empty.INSTANCE;
				final List<ItemStack> stacks = slot.resolveForStacks(context);
				if (stacks.isEmpty()) {
					line.append("[ ]");
					continue;
				}
				final String key = key(stacks);
				Character letter = letters.get(key);
				if (letter == null) {
					letter = LETTERS.charAt(Math.min(letters.size(), LETTERS.length() - 1));
					letters.put(key, letter);
					legend.put(letter, name(stacks));
				}
				line.append('[').append(letter.charValue()).append(']');
			}
			final String text = line.toString();
			source.sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.WHITE), false);
		}

		legend.forEach((letter, name) -> source.sendSuccess(() -> Component.literal("  " + letter + " = ")
			.withStyle(ChatFormatting.DARK_GRAY).append(name.copy().withStyle(ChatFormatting.GRAY)), false));
	}

	/** Şekilsiz tarif: sıra önemsiz, malzemeler alt alta sayılır. */
	private static void loose(final CommandSourceStack source, final List<SlotDisplay> ingredients,
							  final ContextMap context) {
		source.sendSuccess(() -> Component.translatable("command.realcars.recipes.shapeless")
			.withStyle(ChatFormatting.DARK_GRAY), false);

		final Map<String, Integer> counts = new LinkedHashMap<>();
		final Map<String, Component> names = new LinkedHashMap<>();
		for (final SlotDisplay slot : ingredients) {
			final List<ItemStack> stacks = slot.resolveForStacks(context);
			if (stacks.isEmpty()) {
				continue;
			}
			final String key = key(stacks);
			counts.merge(key, 1, Integer::sum);
			names.putIfAbsent(key, name(stacks));
		}
		counts.forEach((key, count) -> source.sendSuccess(() -> Component.literal("  " + count + "x ")
			.withStyle(ChatFormatting.DARK_GRAY).append(names.get(key).copy().withStyle(ChatFormatting.GRAY)), false));
	}

	// --------------------------------------------------------------- yardımcı

	/** Bir gözün kimliği: kabul ettiği eşyaların kimlikleri. Etiketler de tek anahtara iner. */
	private static String key(final List<ItemStack> stacks) {
		return stacks.stream().map(RecipeCommand::idOf).filter(Objects::nonNull)
			.map(Identifier::toString).distinct().sorted().reduce("", (a, b) -> a + "|" + b);
	}

	/** Gözün okunur adı; birden çok eşya kabul ediliyorsa ilk üçü eğik çizgiyle. */
	private static Component name(final List<ItemStack> stacks) {
		final MutableComponent out = Component.empty();
		final int shown = Math.min(stacks.size(), 3);
		for (int i = 0; i < shown; i++) {
			if (i > 0) {
				out.append(" / ");
			}
			out.append(stacks.get(i).getHoverName());
		}
		if (stacks.size() > shown) {
			out.append(" …");
		}
		return out;
	}

	private static ItemStack result(final RecipeHolder<?> holder, final ContextMap context) {
		final List<RecipeDisplay> displays = holder.value().display();
		return displays.isEmpty() ? ItemStack.EMPTY : displays.getFirst().result().resolveForFirstStack(context);
	}

	private static Identifier idOf(final ItemStack stack) {
		return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
	}

	/** Öneri listesi: modun ürettiği eşyaların kimlikleri. */
	private static List<Identifier> resultIds(final CommandSourceStack source) {
		final ContextMap context = SlotDisplayContext.fromLevel(source.getLevel());
		final List<Identifier> ids = new ArrayList<>();
		for (final RecipeHolder<?> holder : ModRecipeBook.modRecipes(source.getServer())) {
			final Identifier id = idOf(result(holder, context));
			if (id != null && !ids.contains(id)) {
				ids.add(id);
			}
		}
		return ids;
	}
}
