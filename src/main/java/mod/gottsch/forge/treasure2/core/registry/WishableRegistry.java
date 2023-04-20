/*
 * This file is part of  Treasure2.
 * Copyright (c) 2022 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.treasure2.core.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import mod.gottsch.forge.gottschcore.enums.IRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * @author Mark Gottschling on Nov 25, 2022
 *
 */
public class WishableRegistry {
	/*
	 * a Table registry for rarity/key lookups for keys
	 */
	private static final Multimap<IRarity, RegistryObject<Item>> BY_RARITY;
	private static final Map<ResourceLocation, RegistryObject<Item>> BY_NAME;
	private static final Map<ResourceLocation, IRarity> RARITY_BY_NAME;

	static {
		BY_RARITY = ArrayListMultimap.create();
		BY_NAME = Maps.newHashMap();
		RARITY_BY_NAME = Maps.newHashMap();
	}
	
	public static void register(RegistryObject<Item> chest) {		
		if (!BY_NAME.containsKey(chest.get().getRegistryName())) {
			BY_NAME.put(chest.getId(), chest);
		}
	}
	
	public static void registerByRarity(IRarity rarity, RegistryObject<Item> chest) {
		if (!BY_RARITY.containsKey(rarity)) {
			BY_RARITY.put(rarity, chest);
			RARITY_BY_NAME.put(chest.getId(), rarity);
		}
	}
	
	public static List<RegistryObject<Item>> getAll() {
		return new ArrayList<>(BY_NAME.values());
	}
	
	public static void clearByRarity() {
		BY_RARITY.clear();		
		RARITY_BY_NAME.clear();
	}
	
	public static IRarity getRarity(ResourceLocation name) {
		if (RARITY_BY_NAME.containsKey(name)) {
			return RARITY_BY_NAME.get(name);
		}
		return null;
	}
	
	public static IRarity getRarity(Item wishable) {
		return getRarity(wishable.getRegistryName());
	}
}