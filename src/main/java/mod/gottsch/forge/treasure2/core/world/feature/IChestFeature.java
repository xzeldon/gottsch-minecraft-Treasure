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
package mod.gottsch.forge.treasure2.core.world.feature;

import mod.gottsch.forge.gottschcore.enums.IRarity;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.treasure2.Treasure;
import mod.gottsch.forge.treasure2.core.generator.ChestGeneratorData;
import mod.gottsch.forge.treasure2.core.generator.GeneratorResult;
import mod.gottsch.forge.treasure2.core.persistence.TreasureSavedData;
import mod.gottsch.forge.treasure2.core.random.RarityLevelWeightedCollection;
import mod.gottsch.forge.treasure2.core.registry.GeneratedCache;
import mod.gottsch.forge.treasure2.core.registry.RarityLevelWeightedChestGeneratorRegistry;
import mod.gottsch.forge.treasure2.core.registry.support.GeneratedChestContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author Mark Gottschling on Sep 21, 2022
 *
 */
public interface IChestFeature extends ITreasureFeature {
	public boolean meetsProximityCriteria(ServerLevelAccessor world, ResourceLocation dimension, IFeatureType key, ICoords spawnCoords, int minDistance);

	/**
	 *
	 * @param genLevel
	 * @param cache
	 * @param rarity
	 * @param coords
	 * @param featureType
	 * @return
	 */
	default public boolean failAndPlaceholdChest(WorldGenLevel genLevel, GeneratedCache<GeneratedChestContext> cache, IRarity rarity, ICoords coords, IFeatureType featureType) {
		// add placeholder
		GeneratedChestContext generatedChestContext = new GeneratedChestContext(rarity, coords, GeneratedChestContext.GeneratedType.PLACEHOLDER).withFeatureType(featureType);
		cache.cache(rarity, coords, generatedChestContext);
		// need to save on fail
		TreasureSavedData savedData = TreasureSavedData.get(genLevel.getLevel());
		if (savedData != null) {
			savedData.setDirty();
		}
		return false;
	}

	/**
	 *
	 * @param world
	 * @param rarity
	 * @param featureType
	 * @param cache
	 * @param data
	 */
	default public void cacheGeneratedChest(ServerLevelAccessor world, IRarity rarity, IFeatureType featureType, GeneratedCache<GeneratedChestContext> cache, GeneratorResult<ChestGeneratorData> data) {
		Treasure.LOGGER.debug("feature gen result -> {}", data);
		// GeneratedChestContext is used to cache data about the chest in the Dimension Generated Chest cache.
		GeneratedChestContext context = new GeneratedChestContext(
				data.getData().getRarity(), data.getData().getCoords())
				.withMarkerCoords(data.getData().getSpawnCoords())
				.withFeatureType(featureType)
				.withName(data.getData().getRegistryName());

		Treasure.LOGGER.debug("chestGenContext -> {}", context);
		// cache the chest at its exact location
		cache.cache(rarity, context.getCoords(), context);
	}

	/**
	 *
	 * @param dimension
	 * @param rarity
	 * @param featureType
	 */
	default public void updateChestGeneratorRegistry(ResourceLocation dimension, IRarity rarity, IFeatureType featureType) {
		// update the adjusted weight collection
		RarityLevelWeightedChestGeneratorRegistry.adjustAllWeightsExcept(dimension, featureType, 1, rarity);
		Map<IFeatureType, RarityLevelWeightedCollection> map = RarityLevelWeightedChestGeneratorRegistry.RARITY_SELECTOR.get(dimension);
		RarityLevelWeightedCollection dumpCol = map.get(featureType);
		List<String> dump = dumpCol.dump();
		Treasure.LOGGER.debug("weighted collection dump -> {}", dump);
	}
}