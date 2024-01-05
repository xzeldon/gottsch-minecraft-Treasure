/**
 * 
 */
package mod.gottsch.forge.treasure2.core.world.gen.feature;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.someguyssoftware.gottschcore.random.RandomHelper;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.gottschcore.world.gen.structure.IDecayRuleSet;

import mod.gottsch.forge.treasure2.core.Treasure;
import mod.gottsch.forge.treasure2.core.chest.ChestEnvironment;
import mod.gottsch.forge.treasure2.core.chest.ChestInfo;
import mod.gottsch.forge.treasure2.core.chest.ChestInfo.GenType;
import mod.gottsch.forge.treasure2.core.config.IChestConfig;
import mod.gottsch.forge.treasure2.core.config.TreasureConfig;
import mod.gottsch.forge.treasure2.core.data.TreasureData;
import mod.gottsch.forge.treasure2.core.enums.PitTypes;
import mod.gottsch.forge.treasure2.core.enums.Rarity;
import mod.gottsch.forge.treasure2.core.enums.WorldGenerators;
import mod.gottsch.forge.treasure2.core.generator.ChestGeneratorData;
import mod.gottsch.forge.treasure2.core.generator.GeneratorResult;
import mod.gottsch.forge.treasure2.core.generator.chest.IChestGenerator;
import mod.gottsch.forge.treasure2.core.generator.pit.IPitGenerator;
import mod.gottsch.forge.treasure2.core.generator.ruins.SurfaceRuinGenerator;
import mod.gottsch.forge.treasure2.core.persistence.TreasureGenerationSavedData;
import mod.gottsch.forge.treasure2.core.registry.ChestRegistry;
import mod.gottsch.forge.treasure2.core.registry.RegistryType;
import mod.gottsch.forge.treasure2.core.world.gen.structure.TemplateHolder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.server.ServerWorld;

/**
 * NOTE: Feature is the equivalent to 1.12 WorldGenerator
 * @author Mark Gottschling on Jan 4, 2021
 *
 */
public class SurfaceChestFeature extends Feature<NoFeatureConfig> implements IChestFeature {
	/*
	 * The minimum depth from surface for a chest spawn
	 */
	protected static int UNDERGROUND_OFFSET = 5;

	private int waitChunksCount = 0;
	
	/**
	 * 
	 * @param configFactory
	 */
	public SurfaceChestFeature(Codec<NoFeatureConfig> configFactory) {
		super(configFactory);
		// NOTE ensure to set the registry name
		this.setRegistryName(Treasure.MODID, "surface_chest");
	}

	/**
	 * NOTE equivalent to 1.12 generate()
	 * NOTE only use seedReader.setblockState() and that only allows you to access the 3x3 chunk.
	 *  chest/pit spawn IS doable as long as you keep it within the 3x3 chunk area, else would have to use a Structures setup
	 */
	@Override
	public boolean place(ISeedReader seedReader, ChunkGenerator generator, Random random, BlockPos pos, NoFeatureConfig config) {
		ServerWorld world = seedReader.getLevel();
		ResourceLocation dimension = WorldInfo.getDimension(world);
		
		if (!meetsDimensionCriteria(dimension)) { 
			return false;
		}
		ChestRegistry registry = TreasureData.CHEST_REGISTRIES2.get(dimension.toString()).get(RegistryType.SURFACE);

		if (!meetsWorldAgeCriteria(world, registry)) {
			return false;
		}
		
		// the get first surface y (could be leaves, trunk, water, etc)
		ICoords spawnCoords = WorldInfo.getDryLandSurfaceCoords(world, generator, new Coords(pos.offset(WorldInfo.CHUNK_RADIUS - 1, 0, WorldInfo.CHUNK_RADIUS - 1)));
		if (spawnCoords == WorldInfo.EMPTY_COORDS) {
			return false;
		}

		// determine what type to generate
		Rarity rarity = (Rarity) TreasureData.RARITIES_MAP.get(WorldGenerators.SURFACE_CHEST).next();
//		Treasure.LOGGER.debug("rarity -> {}", rarity);
		IChestConfig chestConfig = TreasureConfig.CHESTS.surfaceChests.configMap.get(rarity);
		if (chestConfig == null) {
			Treasure.LOGGER.warn("Unable to locate a chest for rarity {}.", rarity);
			return false;
		}

		// 2. test if the override (global) biome is allowed
		if (!meetsBiomeCriteria(world, spawnCoords, chestConfig.getBiomeWhitelist(), chestConfig.getBiomeBlacklist())) {
			return false;
		}

		// 3. check against all registered chests
		if (!meetsProximityCriteria(world, dimension, RegistryType.SURFACE, spawnCoords)) {
			return false;
		}			

		// 4. check if meets the probability criteria
		if (!meetsProbabilityCriteria(random)) {
			// place a placeholder chest in the registry
			ChestInfo chestInfo = new ChestInfo(rarity, spawnCoords, GenType.NONE);
			registry.register(rarity, spawnCoords, chestInfo);
			return false;
		}
		
		// generate the chest/pit/chambers
		Treasure.LOGGER.debug("Attempting to generate pit/chest.");
		Treasure.LOGGER.debug("rarity -> {}", rarity);
		Treasure.LOGGER.debug("randcollection -> {}", TreasureData.CHEST_GENS.get(rarity, WorldGenerators.SURFACE_CHEST).getClass().getSimpleName());
		Treasure.LOGGER.debug("gen -> {}", TreasureData.CHEST_GENS.get(rarity, WorldGenerators.SURFACE_CHEST).next().getClass().getSimpleName());
		Treasure.LOGGER.debug("configmap -> {}", TreasureConfig.CHESTS.surfaceChests.configMap.get(rarity));

		GeneratorResult<ChestGeneratorData> result = null;
		result = generateChest(seedReader, generator, random, spawnCoords, rarity, TreasureData.CHEST_GENS.get(rarity, WorldGenerators.SURFACE_CHEST).next(), TreasureConfig.CHESTS.surfaceChests.configMap.get(rarity));

		if (result.isSuccess()) {
			// add to registry
			ChestInfo chestInfo = ChestInfo.from(result.getData());					
			registry.register(rarity, spawnCoords, chestInfo);

			// update the adjusted weight collection
			TreasureData.RARITIES_MAP.put(WorldGenerators.SURFACE_CHEST, 
					TreasureData.RARITIES_MAP.get(WorldGenerators.SURFACE_CHEST).adjustExcept(1, rarity));
//			List<String> rarityMapDump = TreasureData.RARITIES_MAP.get(WorldGenerators.SURFACE_CHEST).dump();
//			rarityMapDump.forEach(s -> {
//				Treasure.LOGGER.info(s);
//			});
		}

		// save world data
		TreasureGenerationSavedData savedData = TreasureGenerationSavedData.get(world);
		if (savedData != null) {
			savedData.setDirty();
		}
		return true;

	}

	/**
	 * 
	 * @param world
	 * @param registry
	 * @return
	 */
	private boolean meetsWorldAgeCriteria(ServerWorld world, ChestRegistry registry) {
		// wait count check		
		if (registry.getValues().isEmpty() && waitChunksCount < TreasureConfig.CHESTS.surfaceChestGen.waitChunks.get()) {
			Treasure.LOGGER.debug("World is too young");
			this.waitChunksCount++;
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param random
	 * @return
	 */
	private boolean meetsProbabilityCriteria(Random random) {
		if (!RandomHelper.checkProbability(random, TreasureConfig.CHESTS.surfaceChestGen.probability.get())) {
			Treasure.LOGGER.debug("ChestConfig does not meet generate probability.");
			return false;
		}
		return true;
	}


	/////////////////////////////////
	/**
	 * 
	 * @param world
	 * @param random
	 * @param coords
	 * @param rarity
	 * @param next
	 * @param iChestConfig
	 * @return
	 */
	private GeneratorResult<ChestGeneratorData> generateChest(IServerWorld world, ChunkGenerator generator, Random random, ICoords coords, Rarity rarity,
			IChestGenerator chestGenerator, IChestConfig config) {

		// result to return to the caller
		GeneratorResult<ChestGeneratorData> generationResult = new GeneratorResult<>(ChestGeneratorData.class);
		// result from environment (pit | ruins) generation
		GeneratorResult<ChestGeneratorData> environmentGenerationResult = new GeneratorResult<>(ChestGeneratorData.class);

		ICoords chestCoords = null;
		boolean isSurfaceChest = false;
		boolean isStructure = false;

		// 1. collect location data points
		ICoords surfaceCoords = coords;
		//		Treasure.LOGGER.debug("surface coords -> {}", surfaceCoords.toShortString());
		if (!WorldInfo.isValidY(surfaceCoords)) {
			Treasure.LOGGER.debug("surface coords are invalid @ {}", surfaceCoords.toShortString());
			return generationResult.fail();
		}


		// 2. determine if above ground or below ground
		if (config.isSurfaceAllowed() && RandomHelper.checkProbability(random, TreasureConfig.CHESTS.surfaceChests.surfaceChestProbability.get())) {
			isSurfaceChest = true;

			if (RandomHelper.checkProbability(random, TreasureConfig.GENERAL.surfaceStructureProbability.get())) {
				isStructure = true;

				environmentGenerationResult = generateSurfaceRuins(world, generator, random, surfaceCoords, config);
				Treasure.LOGGER.debug("surface result -> {}", environmentGenerationResult.toString());
				if (!environmentGenerationResult.isSuccess()) {
					return environmentGenerationResult.fail();
				}
				// update generation meta data
				generationResult.getData().setStructure(true);

				// set the chest coords to the surface pos
				chestCoords = environmentGenerationResult.getData().getChestContext().getCoords();
			}
			else {
				// set the chest coords to the surface pos
				chestCoords = new Coords(surfaceCoords);
				Treasure.LOGGER.debug("surface chest coords -> {}", chestCoords);
			}
			generationResult.getData().setEnvironment(ChestEnvironment.SURFACE);
		}
		else if (config.isSubterraneanAllowed()) {
			Treasure.LOGGER.debug("else generate pit");
			environmentGenerationResult = generatePit(world, random, rarity, surfaceCoords, config);
			Treasure.LOGGER.debug("result -> {}",environmentGenerationResult.toString());
			if (!environmentGenerationResult.isSuccess()) {
				return environmentGenerationResult.fail();
			}
			chestCoords = environmentGenerationResult.getData().getChestContext().getCoords();
			generationResult.getData().setEnvironment(ChestEnvironment.SUBTERRANEAN);
			generationResult.getData().setPit(true);
		}

		// if chest isn't generated, then fail
		if (chestCoords == null) {
			Treasure.LOGGER.debug("chest coords were not provided in result -> {}", environmentGenerationResult.toString());
			return generationResult.fail();
		}

		GeneratorResult<ChestGeneratorData> chestResult = chestGenerator.generate(world, random, chestCoords, rarity, environmentGenerationResult.getData().getChestContext().getState());
		if (!chestResult.isSuccess()) {
			return generationResult.fail();
		}

		// add markers (above chest or shaft)
		if (!isStructure) {
			chestGenerator.addMarkers(world, generator, random, surfaceCoords, isSurfaceChest);
			generationResult.getData().setMarkers(true);
		}

		Treasure.LOGGER.info("CHEATER! {} chest at coords: {}", rarity, surfaceCoords.toShortString());
		generationResult.getData().setChestContext(chestResult.getData().getChestContext());
		generationResult.getData().setRegistryName(chestResult.getData().getRegistryName());
		generationResult.getData().setRarity(rarity);
		return generationResult.success();
	}

	/**
	 * Land Only
	 * @param world
	 * @param random
	 * @param chestRarity
	 * @param markerCoords
	 * @param config
	 * @return
	 */
	public GeneratorResult<ChestGeneratorData> generatePit(IServerWorld world, Random random, Rarity chestRarity, ICoords markerCoords, IChestConfig config) {
		GeneratorResult<ChestGeneratorData> result = new GeneratorResult<ChestGeneratorData>(ChestGeneratorData.class);
		GeneratorResult<ChestGeneratorData> pitResult = new GeneratorResult<ChestGeneratorData>(ChestGeneratorData.class);

		// check if it has 50% land
		if (!WorldInfo.isSolidBase(world, markerCoords, 2, 2, 50)) {
			Treasure.LOGGER.debug("Coords [{}] does not meet solid base requires for {} x {}", markerCoords.toShortString(), 3, 3);
			return result.fail();
		}

		// determine spawn coords below ground
		ICoords spawnCoords = getUndergroundSpawnPos(world, random, markerCoords, config.getMinDepth(), config.getMaxDepth());

		if (spawnCoords == null || spawnCoords == WorldInfo.EMPTY_COORDS) {
			Treasure.LOGGER.debug("Unable to spawn underground @ {}", markerCoords);
			return result.fail();
		}
		Treasure.LOGGER.debug("Below ground @ {}", spawnCoords.toShortString());
		result.getData().setSpawnCoords(markerCoords);

		// select a pit generator
		IPitGenerator<GeneratorResult<ChestGeneratorData>> pitGenerator = selectPitGenerator(random);
		Treasure.LOGGER.debug("Using pit generator -> {}", pitGenerator.getClass().getSimpleName());

		// 3. build the pit
		pitResult = pitGenerator.generate(world, random, markerCoords, spawnCoords);

		if (!pitResult.isSuccess()) return result.fail();

		result.setData(pitResult.getData());
		Treasure.LOGGER.debug("Is pit generated: {}", pitResult.isSuccess());
		return result.success();
	}

	/**
	 * 
	 * @param world
	 * @param random
	 * @param spawnCoords
	 * @param config
	 * @return
	 */
	public GeneratorResult<ChestGeneratorData> generateSurfaceRuins(IServerWorld world, ChunkGenerator generator, Random random, ICoords spawnCoords,
			IChestConfig config) {
		return generateSurfaceRuins(world, generator, random, spawnCoords, null, null, config);
	}

	/**
	 * 
	 * @param world
	 * @param random
	 * @param spawnCoords
	 * @param decayProcessor
	 * @param config
	 * @return
	 */
	public GeneratorResult<ChestGeneratorData> generateSurfaceRuins(IServerWorld world, ChunkGenerator chunkGenerator, Random random, ICoords spawnCoords,
			TemplateHolder holder, IDecayRuleSet decayRuleSet, IChestConfig config) {

		GeneratorResult<ChestGeneratorData> result = new GeneratorResult<>(ChestGeneratorData.class);		
		result.getData().setSpawnCoords(spawnCoords);

		SurfaceRuinGenerator generator = new SurfaceRuinGenerator();

		// build the structure
		GeneratorResult<ChestGeneratorData> genResult = generator.generate(world, chunkGenerator, random, spawnCoords, holder, decayRuleSet);
		Treasure.LOGGER.debug("surface struct result -> {}", genResult);
		if (!genResult.isSuccess()) return result.fail();

		result.setData(genResult.getData());
		return result.success();
	}

	/**
	 * 
	 * @param world
	 * @param random
	 * @param pos
	 * @param minDepth
	 * @param maxDepth
	 * @return
	 */
	public static ICoords getUndergroundSpawnPos(IServerWorld world, Random random, ICoords pos, int minDepth, int maxDepth) {
		ICoords spawnPos = null;

		int depth = RandomHelper.randomInt(minDepth, maxDepth);
		int ySpawn = Math.max(UNDERGROUND_OFFSET, pos.getY() - depth);
		Treasure.LOGGER.debug("ySpawn -> {}", ySpawn);
		spawnPos = new Coords(pos.getX(), ySpawn, pos.getZ());
		// get floor pos (if in a cavern or tunnel etc)
		spawnPos = WorldInfo.getDryLandSurfaceCoords(world, spawnPos);

		return spawnPos;
	}

	/**
	 * Land Only
	 * @param random
	 * @return
	 */
	public static IPitGenerator<GeneratorResult<ChestGeneratorData>> selectPitGenerator(Random random) {
		PitTypes pitType = RandomHelper.checkProbability(random, TreasureConfig.PITS.pitStructureProbability.get()) ? PitTypes.STRUCTURE : PitTypes.STANDARD;
		Treasure.LOGGER.debug("using pit type -> {}", pitType);
		List<IPitGenerator<GeneratorResult<ChestGeneratorData>>> pitGenerators = TreasureData.PIT_GENS.row(pitType).values().stream()
				.collect(Collectors.toList());
		IPitGenerator<GeneratorResult<ChestGeneratorData>> pitGenerator = pitGenerators.get(random.nextInt(pitGenerators.size()));
		Treasure.LOGGER.debug("Using PitType: {}, Gen: {}", pitType, pitGenerator.getClass().getSimpleName());

		return pitGenerator;
	}
}