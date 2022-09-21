package com.someguyssoftware.treasure2.world.gen.feature;

import java.util.List;
import java.util.Map;

import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.chest.ChestInfo;
import com.someguyssoftware.treasure2.config.TreasureConfig;
import com.someguyssoftware.treasure2.data.TreasureData;
import com.someguyssoftware.treasure2.enums.Rarity;
import com.someguyssoftware.treasure2.registry.ChestRegistry;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public interface ITreasureFeature {

    /**
     * 
     * @return
     */
//	public boolean isEnabled();
	
	public void init();
	
	public Map<String, Integer> getChunksSinceLastDimensionFeature();

	public Map<String, Map<Rarity, Integer>> getChunksSinceLastDimensionRarityFeature();
	
	/**
	 * 
	 * @param dimension
	 * @return
	 */
	default public boolean meetsDimensionCriteria(ResourceLocation dimension) {
		// test the dimension white list
		return TreasureConfig.GENERAL.dimensionsWhiteList.get().contains(dimension.toString());
	}

	/**
	 * 
	 * @param world
	 * @param spawnCoords
	 * @param chestConfig
	 * @return
	 */
	default public boolean meetsBiomeCriteria(ServerWorld world, ICoords spawnCoords, IChestConfig chestConfig) {
		Biome biome = world.getBiome(spawnCoords.toPos());
		TreasureBiomeHelper.Result biomeCheck =TreasureBiomeHelper.isBiomeAllowed(biome, chestConfig.getBiomeWhiteList(), chestConfig.getBiomeBlackList());
		if(biomeCheck == Result.BLACK_LISTED ) {
			if (WorldInfo.isClientSide(world)) {
				Treasure.LOGGER.debug("Biome {} is not a valid biome @ {}", biome.getRegistryName().toString(), spawnCoords.toShortString());
			}
			else {
				// TODO test if this crashes with the getRegistryName because in 1.12 this was a client side only
				Treasure.LOGGER.debug("Biome {} is not valid @ {}",biome.getRegistryName().toString(), spawnCoords.toShortString());
			}					
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param world
	 * @param dimension
	 * @param spawnCoords
	 * @return
	 */
	default public boolean meetsProximityCriteria(ServerWorld world, ResourceLocation dimension, ICoords spawnCoords) {
		if (ITreasureFeature.isRegisteredChestWithinDistance(world, dimension, "surface", spawnCoords, 
				TreasureConfig.CHESTS.surfaceChestGen.minBlockDistance.get())) {
			Treasure.LOGGER.debug("The distance to the nearest treasure chest is less than the minimun required.");
			return false;
		}	
		return true;
	}

	// TODO this is legecay - check if it is used
    default public boolean checkDimensionWhiteList(String dimensionName) {
        // test the dimension white list
        if (!TreasureConfig.GENERAL.dimensionsWhiteList.get().contains(dimensionName)) {
            return false;
        }
        return true;
    }
 
    /**
     * 
     * @param world
     * @param dimension
     * @param key
     * @param coords
     * @param minDistance
     * @return
     */
    public static boolean isRegisteredChestWithinDistance(IWorld world, ResourceLocation dimension, String key, ICoords coords, int minDistance) {
    	
    Map<String, ChestRegistry> registries = TreasureData.CHEST_REGISTRIES2.get(dimension.toString());
	if (registries == null || registries.size() == 0) {
		Treasure.LOGGER.debug("Unable to locate the ChestRegistry or the Registry doesn't contain any values");
		return false;
	}
	ChestRegistry registry = registries.get(key);
	if (registry == null) {
		Treasure.LOGGER.debug("Unable to locate the ChestRegistry or the Registry doesn't contain any values");
		return false;
	}
	
	// generate a box with coords as center and minDistance as radius
	ICoords startBox = new Coords(coords.getX() - minDistance, 0, coords.getZ() - minDistance);
	ICoords endBox = new Coords(coords.getX() + minDistance, 0, coords.getZ() + minDistance);
	
	// find if box overlaps anything in the registry
	if (registry.withinArea(startBox, endBox)) {
		return true;
	}
	
	return false;
}
	/**
	 * 
	 * @param world
	 * @param pos
	 * @param minDistance
	 * @return
	 */
    @Deprecated
	public static boolean isRegisteredChestWithinDistance(IWorld world, ICoords coords, int minDistance) {

		double minDistanceSq = minDistance * minDistance;

		// get a list of dungeons
		List<ChestInfo> infos = TreasureData.CHEST_REGISTRIES.get(WorldInfo.getDimension((World)world).toString()).getValues();

		if (infos == null || infos.size() == 0) {
			Treasure.LOGGER.debug("Unable to locate the ChestConfig Registry or the Registry doesn't contain any values");
			return false;
		}

		for (ChestInfo info : infos) {
			// calculate the distance to the poi
			double distance = coords.getDistanceSq(info.getCoords());
			if (distance < minDistanceSq) {
				return true;
			}
		}
		return false;
	}
}
