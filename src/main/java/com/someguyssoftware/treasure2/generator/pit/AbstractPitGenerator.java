package com.someguyssoftware.treasure2.generator.pit;

import java.util.Random;

import com.someguyssoftware.gottschcore.block.BlockContext;
import com.someguyssoftware.gottschcore.measurement.Quantity;
import com.someguyssoftware.gottschcore.random.RandomWeightedCollection;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.gottschcore.tileentity.ProximitySpawnerTileEntity;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.block.TreasureBlocks;
import com.someguyssoftware.treasure2.generator.ChestGeneratorData;
import com.someguyssoftware.treasure2.generator.GenUtil;
import com.someguyssoftware.treasure2.generator.GeneratorResult;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LogBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.DungeonHooks;

/**
 * 
 * @author Mark Gottschling on Mar 7, 2018
 *
 */
public abstract class AbstractPitGenerator implements IPitGenerator<GeneratorResult<ChestGeneratorData>> {

	protected static final int OFFSET_Y = 5;
	protected static final int SURFACE_OFFSET_Y = 6;
	protected static final Block DEFAULT_LOG = Blocks.OAK_LOG;
	
	private RandomWeightedCollection<Block> blockLayers = new RandomWeightedCollection<>();
	private int offsetY = OFFSET_Y;

	/**
	 * 
	 */
	public AbstractPitGenerator() {
		super();
		// standard set of block layers
		getBlockLayers().add(50, Blocks.AIR);
		getBlockLayers().add(25,  Blocks.SAND);
		getBlockLayers().add(15, Blocks.GRAVEL);
		getBlockLayers().add(10, DEFAULT_LOG);
	}

	/**
	 * 
	 */
	@Override
	public boolean generateBase(IWorld world, Random random, ICoords surfaceCorods, ICoords spawnCoords) {
		Treasure.LOGGER.debug("generating base ...");
		// at chest level
		buildLayer(world, spawnCoords, Blocks.AIR);
		
		// above the chest
		buildAboveChestLayers(world, random, spawnCoords);
		
		return true;
	}
	
	@Override
	public boolean generatePit(IWorld world, Random random, ICoords surfaceCoords, ICoords spawnCoords) {
		buildPit(world, random, spawnCoords, surfaceCoords, getBlockLayers());
		return true;
	}
	
	@Override
	public boolean generateEntrance(IWorld world, Random random, ICoords surfaceCoords, ICoords spawnCoords) {
		Treasure.LOGGER.debug("generating entrance ...");
		// pit enterance
		buildLogLayer(world, random, surfaceCoords.add(0, -3, 0), DEFAULT_LOG);
		buildLayer(world, surfaceCoords.add(0, -4, 0), Blocks.SAND);
		buildLogLayer(world, random, surfaceCoords.add(0, -5, 0), DEFAULT_LOG);
		return true;
	}
	
	/**
	 * 
	 * @param world
	 * @param random
	 * @param surfaceCoords
	 * @param spawnCoords
	 * @return
	 */
	@Override
	public GeneratorResult<ChestGeneratorData> generate(IWorld world, Random random, ICoords surfaceCoords, ICoords spawnCoords) {
		GeneratorResult<ChestGeneratorData> result = new GeneratorResult<>(ChestGeneratorData.class);		
		result.getData().setSpawnCoords(spawnCoords);
		result.getData().getChestContext().setCoords(spawnCoords);
		
		// is the chest placed in a cavern
		boolean inCavern = false;
		
		// check above if there is a free space - chest may have spawned in underground cavern, ravine, dungeon etc
		BlockState blockState = world.getBlockState(spawnCoords.add(0, 1, 0).toPos());
		
		// if there is air above the origin, then in cavern. (pos in isAir() doesn't matter)
		if (blockState == null || blockState.getMaterial() == Material.AIR) {
			Treasure.LOGGER.debug("Spawn coords is in cavern.");
			inCavern = true;
		}

		if (inCavern) {
			Treasure.LOGGER.debug("Shaft is in cavern... finding ceiling.");
			spawnCoords = GenUtil.findUndergroundCeiling(world, spawnCoords.add(0, 1, 0));
			if (spawnCoords == null) {
				Treasure.LOGGER.warn("Exiting: Unable to locate cavern ceiling.");
				return result.fail();
			}
			result.getData().setSpawnCoords(spawnCoords);
			// update the chest coords in the result
			result.getData().getChestContext().setCoords(spawnCoords);
		}
	
		// generate shaft
		int yDist = (surfaceCoords.getY() - spawnCoords.getY()) - 2;
		Treasure.LOGGER.debug("Distance to ySurface =" + yDist);
	
		if (yDist > getMinSurfaceToSpawnDistance()) {
			Treasure.LOGGER.debug("Generating shaft @ " + spawnCoords.toShortString());

			generateBase(world, random, surfaceCoords, spawnCoords);
			
			// pit enterance
			generateEntrance(world, random, surfaceCoords, spawnCoords);

			// build the pit
			generatePit(world, random, surfaceCoords, spawnCoords);
		}			
		// shaft is only 2-6 blocks long - can only support small covering
		else if (yDist >= 2) {
			Treasure.LOGGER.debug("less than 2, generate simple short pit gen");
			// simple short pit
			result = new SimpleShortPitGenerator().generate(world, random, surfaceCoords, spawnCoords);
		}
		return result.success();
	}	
	
	/**
	 * 
	 * @return
	 */
	@Override
	public int getMinSurfaceToSpawnDistance() {
		return 6;
	}

	/**
	 * 
	 * @param world
	 * @param spawnCoords
	 */
	public void buildAboveChestLayers(IWorld world, Random random, ICoords spawnCoords) {
		buildLayer(world, spawnCoords.add(0, 1, 0), Blocks.AIR);
		buildLayer(world, spawnCoords.add(0, 2, 0), Blocks.AIR);
		buildLogLayer(world, random, spawnCoords.add(0, 3, 0), DEFAULT_LOG);
		buildLayer(world, spawnCoords.add(0, 4, 0), Blocks.SAND);
	}

	/**
	 * 
	 * @param world
	 * @param random
	 * @param spawnCoords
	 * @param surfaceCoords
	 * @return
	 */
	public ICoords buildPit(IWorld world, Random random, ICoords coords, ICoords surfaceCoords, RandomWeightedCollection<Block> col) {
		Treasure.LOGGER.debug("generating pit ...");
		ICoords nextCoords = null;
		ICoords expectedCoords = null;
		
		// randomly fill shaft
		for (int yIndex = coords.getY() + getOffsetY(); yIndex <= surfaceCoords.getY() - SURFACE_OFFSET_Y; yIndex++) {
			
			// if the block to be replaced is air block then skip to the next pos
			BlockContext cube = new BlockContext(world, new Coords(coords.getX(), yIndex, coords.getZ()));
			if (cube.isAir()) {
				continue;
			}
			
			// get the next type of block layer to build
			Block block = col.next();
			if (block == DEFAULT_LOG) {
				// special log build layer
				nextCoords = buildLogLayer(world, random, cube.getCoords(), block); // could have difference classes and implement buildLayer differently
				// ie. LayerBuilder.build(world, coords, block)
			}
			else {
				nextCoords = buildLayer(world, cube.getCoords(), block);
			}
			expectedCoords = cube.getCoords().add(0, 1, 0);
			
			// check if the return coords is different than the anticipated coords and resolve
			yIndex = autoCorrectIndex(yIndex, nextCoords, expectedCoords);
		}		
		return nextCoords;
	}
	
	/**
	 * 
	 * @param index
	 * @param coords
	 * @param expectedCoords
	 * @return
	 */
	protected int autoCorrectIndex(final int index, final ICoords coords, final ICoords expectedCoords) {
		int newIndex = index;
		if (!coords.equals(expectedCoords)) {
			// find the difference in y int and add to yIndex;
			Treasure.LOGGER.debug("Next coords does not equal expected coords. next: {}; expected: {}", coords.toShortString(), expectedCoords.toShortString());
			// NOTE the difference should = 1, there remove 1 from the diff to find unexpected difference
			int diff = coords.getY() - expectedCoords.getY() - 1;
			if (diff > 0) {
				newIndex = coords.getY();
				Treasure.LOGGER.debug("Difference of: {}. Updating yIndex to {}", diff, newIndex);
			}
		}
		return newIndex;
	}
	
	/**
	 * 
	 * @param world
	 * @param coords
	 * @param block
	 * @return
	 */
	public ICoords buildLayer(IWorld world, ICoords coords, Block block) {
		Treasure.LOGGER.debug("Building layer from {} @ {} ", block.getRegistryName(), coords.toShortString());
		GenUtil.replaceWithBlock(world, coords, block);
		GenUtil.replaceWithBlock(world, coords.add(1, 0, 0), block);
		GenUtil.replaceWithBlock(world, coords.add(0, 0, 1), block);
		GenUtil.replaceWithBlock(world, coords.add(1, 0, 1), block);
		
		return coords.add(0, 1, 0);
	}

	/**
	 * 
	 * @param world
	 * @param coords
	 * @param block
	 * @return
	 */
	public ICoords buildLogLayer(final IWorld world, final Random random, final ICoords coords, final Block block) {
		Treasure.LOGGER.debug("building log layer from {} @ {} ", block.getRegistryName(), coords.toShortString());
		// ensure that block is of type LOG/LOG2
		if (!(block instanceof LogBlock)) {
			Treasure.LOGGER.debug("block is not a log");
            return coords;
        }
		
		 // determine the direction the logs are facing - north/south (8) or east/west (4)
//		int meta = random.nextInt() % 2 == 0 ? 8 : 4;
//		@SuppressWarnings("deprecation")	
//		BlockState blockState = block.getStateFromMeta(meta);
		
		// randomly select the axis the logs are facing (0 = Z, 1 = X);
		int axis = random.nextInt(2);
		BlockState blockState = block.getDefaultState();
		if (axis == 0) {
			blockState = blockState.with(LogBlock.AXIS, Direction.Axis.Z);
		}
		else {
			blockState = blockState.with(LogBlock.AXIS,  Direction.Axis.X);
		}
				
//		Treasure.LOGGER.debug("block state -> {}", blockState.getBlock().getRegistryName());
		
		// core 4-square
		GenUtil.replaceWithBlockState(world, coords, blockState);
		GenUtil.replaceWithBlockState(world, coords.add(1, 0, 0), blockState);
		GenUtil.replaceWithBlockState(world, coords.add(0, 0, 1), blockState);
		GenUtil.replaceWithBlockState(world, coords.add(1, 0, 1), blockState);
		
		if (axis == 0) {			
			// north of
			GenUtil.replaceWithBlockState(world, coords.add(0, 0, -1), blockState);
			GenUtil.replaceWithBlockState(world, coords.add(1, 0, -1), blockState);
			
			// south of
			GenUtil.replaceWithBlockState(world, coords.add(0, 0, 2), blockState);
			GenUtil.replaceWithBlockState(world, coords.add(1, 0, 2), blockState);
		}
		else {
			// west of
			GenUtil.replaceWithBlockState(world, coords.add(-1, 0, 0), blockState);
			GenUtil.replaceWithBlockState(world, coords.add(-1, 0, 1), blockState);
			// east of 
			GenUtil.replaceWithBlockState(world, coords.add(2, 0, 0), blockState);
			GenUtil.replaceWithBlockState(world, coords.add(2, 0, 1), blockState);
		}
		Treasure.LOGGER.debug("log level complete");
		return coords.add(0, 1, 0);
	}
	
	/**
	 * 
	 * @param world
	 * @param spawnCoords
	 * @param mob
	 */
	public void spawnMob(IWorld world, ICoords spawnCoords, String mobName) {
		MobEntity mob = null;
		switch (mobName) {
		case "zombie":
			mob = new ZombieEntity(world.getWorld());
			break;
		case "skeleton":
			mob = new SkeletonEntity(EntityType.SKELETON, world.getWorld());
			break;
		}
    	mob.setLocationAndAngles((double)spawnCoords.getX() + 0.5D,  (double)spawnCoords.getY(), (double)spawnCoords.getZ() + 0.5D, 0.0F, 0.0F);
    	world.addEntity(mob);
	}
	
	/**
	 * 
	 * @param world
	 * @param random
	 * @param spawnCoords
	 */
	public void spawnRandomMob(IWorld world, Random random, ICoords spawnCoords) {
    	world.setBlockState(spawnCoords.toPos(), TreasureBlocks.PROXIMITY_SPAWNER.getDefaultState(), 3);
    	ProximitySpawnerTileEntity te = (ProximitySpawnerTileEntity) world.getTileEntity(spawnCoords.toPos());
    	EntityType<?> mobEntityType = DungeonHooks.getRandomDungeonMob(random);
    	te.setMobName(mobEntityType.getRegistryName());
    	te.setMobNum(new Quantity(1, 1));
    	te.setProximity(3D);
	}
	
	/**
	 * @return the blockLayers
	 */
	public RandomWeightedCollection<Block> getBlockLayers() {
		return blockLayers;
	}

	/**
	 * @param blockLayers the blockLayers to set
	 */
	public void setBlockLayers(RandomWeightedCollection<Block> blockLayers) {
		this.blockLayers = blockLayers;
	}

	@Override
	public int getOffsetY() {
		return offsetY;
	}

	@Override
	public void setOffsetY(int offsetY) {
		this.offsetY = offsetY;
	}
}