/*
 * This file is part of  Treasure2.
 * Copyright (c) 2021, Mark Gottschling (gottsch)
 * 
 * All rights reserved.
 *
 * Treasure2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Treasure2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Treasure2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.treasure2.core.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.someguyssoftware.gottschcore.loot.LootTableShell;
import com.someguyssoftware.gottschcore.spatial.ICoords;

import mod.gottsch.forge.treasure2.core.enums.Rarity;
import mod.gottsch.forge.treasure2.core.loot.TreasureLootTableMaster2;
import mod.gottsch.forge.treasure2.core.loot.TreasureLootTableRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/**
 * @author Mark Gottschling on Aug 14, 2021
 *
 */
public interface IWishable {
	public static final String DROPPED_BY_KEY = "droppedBy";

	/**
	 * 
	 * @param world
	 * @param random
	 * @param entityItem
	 * @param coords
	 */
	public Optional<ItemStack> generateLoot(World world, Random random, ItemStack itemStack, ICoords coords);

	/**
	 * 
	 * @param world
	 * @param random
	 * @param itemStacks
	 * @param category
	 * @param rarity
	 * @param lootContext
	 */
	default public void injectLoot(World world, Random random, List<ItemStack> itemStacks, String category, Rarity rarity, LootContext lootContext) {
		Optional<List<LootTableShell>> injectLootTableShells = buildInjectedLootTableList(category, rarity);		
		if (injectLootTableShells.isPresent()) {
			itemStacks.addAll(TreasureLootTableRegistry.getLootTableMaster().getInjectedLootItems(world, random, injectLootTableShells.get(), lootContext));
		}
	}

	/**
	 * 
	 * @param key
	 * @param rarity
	 * @return
	 */
	default public Optional<List<LootTableShell>> buildInjectedLootTableList(String key, Rarity rarity) {
		return Optional.ofNullable(TreasureLootTableRegistry.getLootTableMaster().getLootTableByKeyRarity(TreasureLootTableMaster2.ManagedTableType.INJECT, key, rarity));
	}

	/**
	 * 
	 * @return
	 */
	default public List<LootTableShell> getLootTables() {
		return TreasureLootTableRegistry.getLootTableMaster().getLootTableByRarity(Rarity.COMMON);
	}

	/**
	 * 
	 * @param random
	 * @return
	 */
	default public ItemStack getDefaultLootKey (Random random) {
		List<KeyItem> keys = new ArrayList<>(TreasureItems.keys.get(Rarity.COMMON));
		return new ItemStack(keys.get(random.nextInt(keys.size())));
	}

	/**
	 * 
	 * @param random
	 * @return
	 */
	default public Rarity getDefaultEffectiveRarity(Random random) {
		return Rarity.UNCOMMON;
	}

	/**
	 * 
	 * @param world
	 * @param player
	 * @return
	 */
	default public LootContext getLootContext(World world, PlayerEntity player, ICoords coords) {
		return new LootContext.Builder((ServerWorld) world)
				.withLuck((player != null) ? player.getLuck() : 0)
				.withOptionalParameter(LootParameters.THIS_ENTITY, player)
				.withParameter(LootParameters.ORIGIN, coords.toVec3d())
				.create(LootParameterSets.CHEST);
	}
}