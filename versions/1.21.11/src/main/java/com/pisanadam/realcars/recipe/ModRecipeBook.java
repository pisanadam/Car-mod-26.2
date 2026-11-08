package com.pisanadam.realcars.recipe;

import com.pisanadam.realcars.RealCars;
import java.util.Collection;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Modun tariflerini oyunculara peşinen açar.
 *
 * <p>Vanilla tarif kitabı bir tarifi ancak malzemesi eline geçince gösterir.
 * Araba parçaları için bu ters işliyor: oyuncu neyin nasıl yapıldığını
 * <em>görmeden</em> malzemeyi toplamaya başlamıyor. Bu yüzden {@code realcars}
 * ad alanındaki her tarif, oyuncu dünyaya girer girmez kitabına eklenir;
 * elinde tek bir parça olmasa bile tarif kitabında yapılışıyla birlikte durur.
 *
 * <p>Kanca {@code placeNewPlayer} içinde, sunucunun tarif kitabını istemciye
 * ilk kez yolladığı yerden <em>önce</em> çalışır (Fabric olayı
 * {@code ClientboundPlayerAbilitiesPacket} gönderiminde tetikleniyor,
 * {@code sendInitialRecipeBook} ise birkaç satır sonra). Yani eklediğimiz
 * tarifler zaten ilk gönderimin içinde gider; ayrıca paket gerekmez.
 */
public final class ModRecipeBook {
	private ModRecipeBook() {
	}

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> award(handler.player, server));
		// /reload sonrası tarif kitabı sıfırdan yollanır; açtıklarımızı geri koyalım.
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
			if (success) {
				for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
					award(player, server);
				}
			}
		});
	}

	/** Oyuncuya modun bütün tariflerini verir; kaçının yeni açıldığını döndürür. */
	public static int award(final ServerPlayer player, final MinecraftServer server) {
		final Collection<RecipeHolder<?>> recipes = modRecipes(server);
		return recipes.isEmpty() ? 0 : player.awardRecipes(recipes);
	}

	/** Sunucuda yüklü tariflerden yalnızca {@code realcars} ad alanında olanlar. */
	public static List<RecipeHolder<?>> modRecipes(final MinecraftServer server) {
		return server.getRecipeManager().getRecipes().stream()
			.filter(holder -> holder.id().identifier().getNamespace().equals(RealCars.MOD_ID))
			.toList();
	}
}
