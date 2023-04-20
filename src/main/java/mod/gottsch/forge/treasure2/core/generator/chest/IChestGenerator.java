/*
 * This file is part of  Treasure2.
 * Copyright (c) 2019 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.treasure2.core.generator.chest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import mod.gottsch.forge.gottschcore.enums.IRarity;
import mod.gottsch.forge.gottschcore.loot.LootPoolShell;
import mod.gottsch.forge.gottschcore.loot.LootTableShell;
import mod.gottsch.forge.gottschcore.random.RandomHelper;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.IWorldGenContext;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.treasure2.Treasure;
import mod.gottsch.forge.treasure2.core.block.AbstractTreasureChestBlock;
import mod.gottsch.forge.treasure2.core.block.entity.AbstractTreasureChestBlockEntity;
import mod.gottsch.forge.treasure2.core.block.entity.ITreasureChestBlockEntity;
import mod.gottsch.forge.treasure2.core.config.ChestConfiguration;
import mod.gottsch.forge.treasure2.core.config.Config;
import mod.gottsch.forge.treasure2.core.enums.Rarity;
import mod.gottsch.forge.treasure2.core.generator.ChestGeneratorData;
import mod.gottsch.forge.treasure2.core.generator.GeneratorResult;
import mod.gottsch.forge.treasure2.core.generator.GeneratorType;
import mod.gottsch.forge.treasure2.core.generator.GeneratorUtil;
import mod.gottsch.forge.treasure2.core.generator.marker.GravestoneMarkerGenerator;
import mod.gottsch.forge.treasure2.core.item.LockItem;
import mod.gottsch.forge.treasure2.core.lock.LockLayout;
import mod.gottsch.forge.treasure2.core.lock.LockState;
import mod.gottsch.forge.treasure2.core.registry.ChestRegistry;
import mod.gottsch.forge.treasure2.core.registry.DimensionalGeneratedRegistry;
import mod.gottsch.forge.treasure2.core.registry.GeneratedRegistry;
import mod.gottsch.forge.treasure2.core.registry.KeyLockRegistry;
import mod.gottsch.forge.treasure2.core.registry.TreasureLootTableRegistry;
import mod.gottsch.forge.treasure2.core.registry.support.ChestGenContext;
import mod.gottsch.forge.treasure2.core.registry.support.ChestGenContext.GenType;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.RegistryObject;


/**
 * @author Mark Gottschling on Dec 4, 2019
 *
 */
public interface IChestGenerator {

	default public GeneratorResult<ChestGeneratorData> generate(IWorldGenContext context, ICoords coords,
			final IRarity rarity, BlockState state) {

		GeneratorResult<ChestGeneratorData> result = new GeneratorResult<>(ChestGeneratorData.class);
		result.getData().setSpawnCoords(coords);

		// select a loot table
		Optional<LootTableShell> lootTableShell = selectLootTable(context.random(), rarity);
		ResourceLocation lootTableResourceLocation = null;
		if (lootTableShell.isPresent()) {
			lootTableResourceLocation = lootTableShell.get().getResourceLocation();
		}
		else {
			Treasure.LOGGER.debug("unable to select a LootTable for rarity -> {}", rarity);
			return result.fail();
		}

		// select a chest from the rarity
		AbstractTreasureChestBlock chest = selectChest(context.random(), rarity);
		if (chest == null) {
			Treasure.LOGGER.warn("unable to select a chest for rarity -> {}.", rarity);
			return result.fail();
		}
		result.getData().setRegistryName(chest.getRegistryName());

		// place the chest in the world
		BlockEntity blockEntity = null;
		if (state != null) {
			blockEntity = placeInWorld(context, coords, chest, state);
		} else {
			blockEntity = placeInWorld(context, chest, coords);
		}

		if (blockEntity == null) {
			Treasure.LOGGER.debug("Unable to locate tile entity for chest -> {}", coords);
			return result.fail();
		}

		// add the loot table
		addLootTable((ITreasureChestBlockEntity) blockEntity, lootTableResourceLocation);

		// seal the chest
		addSeal((ITreasureChestBlockEntity) blockEntity);

		// update the backing tile entity's generation contxt
		addGenerationContext((ITreasureChestBlockEntity) blockEntity, rarity);

		// add locks
		addLocks(context.random(), chest, (ITreasureChestBlockEntity) blockEntity, rarity);

		// update result
		result.getData().setSpawnCoords(coords);
		result.getData().setState(state);

		return result.success();
	}

	// TODO this should be a generic call that passes in ManagedTableType
	default public List<LootTableShell> buildLootTableList(IRarity rarity) {
		return TreasureLootTableRegistry.getLootTableByRarity(TreasureLootTableRegistry.ManagedTableType.CHEST, rarity);
	}

	default public Optional<List<LootTableShell>> buildInjectedLootTableList(String key, IRarity rarity) {
		return Optional.ofNullable(TreasureLootTableRegistry.getLootTableByKeyRarity(TreasureLootTableRegistry.ManagedTableType.INJECT, key, rarity));
	}

	/**
	 * 
	 * @param rarity
	 * @return
	 */
	// TODO how to prevent special chests from ending up in the rarity tag lists?
	// TODO move to the ChestRegistry ?
	// why did I deprecate this?? - should use ChestRegistry
	@Deprecated
	default public AbstractTreasureChestBlock selectChest(final Random random, final IRarity rarity) {
		Treasure.LOGGER.debug("attempting to get chest list for rarity -> {}", rarity);
		List<RegistryObject<Block>> chestList = (List<RegistryObject<Block>>) ChestRegistry.getChest(rarity);
		Treasure.LOGGER.debug("size of chests lists -> {}", chestList.size());
		RegistryObject<Block> chest = null;
		if (!chestList.isEmpty()) {
			chest = chestList.get(RandomHelper.randomInt(random, 0, chestList.size() - 1));
		}
		return chest == null ? null : (AbstractTreasureChestBlock) chest.get();
	}

	/**
	 * 
	 * @param level
	 * @param random
	 * @param blockEntity
	 * @param lootRarity
	 */
	default public void fillChest(final Level level, Random random, final BlockEntity blockEntity, final IRarity rarity, Player player) {
		Optional<LootTableShell> lootTableShell = null;
		ResourceLocation lootTableResourceLocation = ((ITreasureChestBlockEntity)blockEntity).getLootTable();
		Treasure.LOGGER.debug("chest has loot table property of -> {}", lootTableResourceLocation);

		if (!(blockEntity instanceof AbstractTreasureChestBlockEntity)) {
			return;
		}
		AbstractTreasureChestBlockEntity chestBlockEntity = (AbstractTreasureChestBlockEntity)blockEntity;

		if (lootTableResourceLocation == null) {
			lootTableShell = selectLootTable(random, rarity);
		}
		else {
			lootTableShell = TreasureLootTableRegistry.getLootTableByResourceLocation(lootTableResourceLocation);
		}	
		// is valid loot table shell
		if (lootTableShell.isPresent()) {
			Treasure.LOGGER.debug("using loot table shell -> {}, {}", lootTableShell.get().getCategory(), lootTableShell.get().getRarity());
			lootTableResourceLocation = lootTableShell.get().getResourceLocation();
		}
		else {
			Treasure.LOGGER.debug("Unable to select a LootTable for rarity -> {}", rarity);
			return;
		}
		Treasure.LOGGER.debug("loot table resource -> {}", lootTableResourceLocation); 

		LootTable lootTable = level.getServer().getLootTables().get(lootTableResourceLocation);
		if (lootTable == null) {
			Treasure.LOGGER.warn("Unable to select a lootTable.");
			return;
		}		
		Treasure.LOGGER.debug("selected loot table -> {} from resource -> {}", lootTable, lootTableResourceLocation);

		// update rarity from lootTableShell		
		IRarity effectiveRarity = TreasureLootTableRegistry.getEffectiveRarity(lootTableShell.get(), rarity);		
		Treasure.LOGGER.debug("generating loot from loot table for effective rarity {}", effectiveRarity);

		// setup lists of items
		List<ItemStack> treasureStacks = new ArrayList<>();
		List<ItemStack> itemStacks = new ArrayList<>();

		/*
		 * Using per loot table file - category strategy (instead of per pool strategy)
		 */
		// get a list of loot pools
		List<LootPoolShell> lootPoolShells = lootTableShell.get().getPools();
		if (lootPoolShells != null && lootPoolShells.size() > 0) {
			Treasure.LOGGER.debug("# of pools -> {}", lootPoolShells.size());
		}

		// setup context
		LootContext lootContext = null;
		lootContext = new LootContext.Builder((ServerLevel) level)
				.withLuck(player.getLuck())
				.withParameter(LootContextParams.THIS_ENTITY, player)
				.withParameter(LootContextParams.ORIGIN, 
						new Vec3(blockEntity.getBlockPos().getX(), 
								blockEntity.getBlockPos().getY(), 
								blockEntity.getBlockPos().getZ()))
				.create(LootContextParamSets.CHEST);


		//		Treasure.LOGGER.debug("loot context -> {}", lootContext);

		for (LootPoolShell pool : lootPoolShells) {
			Treasure.LOGGER.debug("processing pool (from poolShell) -> {}", pool.getName());
			// go get the vanilla managed pool
			LootPool lootPool = lootTable.getPool(pool.getName());

			if (lootPool != null) {
				// geneate loot from pools
				if (pool.getName().equalsIgnoreCase("treasure") ||
						pool.getName().equalsIgnoreCase("charms")) {
					Treasure.LOGGER.debug("generating loot from treasure/charm pool -> {}", pool.getName());
					lootPool.addRandomItems(treasureStacks::add, lootContext);
				}
				else {
					Treasure.LOGGER.debug("generating loot from loot pool -> {}", pool.getName());
					lootPool.addRandomItems(itemStacks::add, lootContext);
				}
			}
		}
		Treasure.LOGGER.debug("size of treasure stacks -> {}", treasureStacks.size());
		Treasure.LOGGER.debug("size of item stacks -> {}", itemStacks.size());

		// record original item size (max number of items to pull from final list)
		int treasureLootItemSize = treasureStacks.size();
		int lootItemSize = itemStacks.size();

		// TODO move to separate method
		// fetch all injected loot tables by category/rarity
		Treasure.LOGGER.debug("searching for injectable tables for category ->{}, rarity -> {}", lootTableShell.get().getCategory(), effectiveRarity);
		Optional<List<LootTableShell>> injectLootTableShells = buildInjectedLootTableList(lootTableShell.get().getCategory(), effectiveRarity);
		if (injectLootTableShells.isPresent()) {
			Treasure.LOGGER.debug("found injectable tables for category ->{}, rarity -> {}", lootTableShell.get().getCategory(), effectiveRarity);
			Treasure.LOGGER.debug("size of injectable tables -> {}", injectLootTableShells.get().size());

			// add predicate
			treasureStacks.addAll(getInjectedLootItems(level, random, injectLootTableShells.get(), lootContext, p -> {
				return p.getName().equalsIgnoreCase("treasure") || p.getName().equalsIgnoreCase("charms");
			}));
			itemStacks.addAll(getInjectedLootItems(level, random, injectLootTableShells.get(), lootContext, p -> {
				return !p.getName().equalsIgnoreCase("treasure") && !p.getName().equalsIgnoreCase("charms");
			}));
			//			itemStacks.addAll(TreasureLootTableRegistry.getLootTableMaster().getInjectedLootItems(world, random, injectLootTableShells.get(), lootContext));
		}

		// check the inventory
		IItemHandler itemHandler = chestBlockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElse(null);
		if (itemHandler != null) {
			ItemStackHandler inventory = (ItemStackHandler)itemHandler;

			// add the treasure items to the chest
			Collections.shuffle(treasureStacks, random);
			fillInventory(inventory, random, treasureStacks.stream().limit(treasureLootItemSize).collect(Collectors.toList()));

			// add a treasure map if there is still space
			addTreasureMap(level, random, inventory, new Coords(blockEntity.getBlockPos()), rarity);

			// shuffle the items list
			Collections.shuffle(itemStacks, random);		
			// fill the chest with items
			fillInventory(inventory, random, itemStacks.stream().limit(lootItemSize).collect(Collectors.toList()));
		}
	}

	//////////////////
	// TODO add predicate to signature and use it instead of "treasure" filter
	default public List<ItemStack> getInjectedLootItems(Level world, Random random, List<LootTableShell> lootTableShells,
			LootContext lootContext, Predicate<LootPoolShell> predicate) {

		List<ItemStack> itemStacks = new ArrayList<>();		

		for (LootTableShell injectLootTableShell : lootTableShells) {			
			Treasure.LOGGER.debug("injectable resource -> {}", injectLootTableShell.getResourceLocation());

			// get the vanilla managed loot table
			LootTable injectLootTable = world.getServer().getLootTables().get(injectLootTableShell.getResourceLocation());

			if (injectLootTable != null) {
				// filter the pool
				List<LootPoolShell> lootPoolShells = injectLootTableShell.getPools().stream()
						.filter(pool -> predicate.test(pool) )
						.collect(Collectors.toList());

				lootPoolShells.forEach(poolShell -> {
					// get the vanilla managed loot pool
					LootPool lootPool = injectLootTable.getPool(poolShell.getName());					
					if (lootPool != null) {
						// add loot from tables to itemStacks
						lootPool.addRandomItems(itemStacks::add, lootContext);
					}
				});
				Treasure.LOGGER.debug("size of item stacks after inject -> {}", itemStacks.size());
			}
		}
		return itemStacks;
	}
	/////////////////

	/**
	 * 
	 * @param world
	 * @param random
	 * @param inventory
	 * @param chestCoords
	 * @param rarity
	 */
	default public void addTreasureMap(Level world, Random random, ItemStackHandler inventory, ICoords chestCoords, IRarity rarity) {
		ResourceLocation dimension = WorldInfo.getDimension(world);
		ChestConfiguration config = Config.chestConfigMap.get(dimension);
		//check for open slots first
		List<Integer> emptySlots = getEmptySlotsRandomized(inventory, random);
		if (!emptySlots.isEmpty() && config != null && RandomHelper.checkProbability(random, config.getTreasureMapProbability())) { 
			// determine what level of rarity map to generate
			IRarity mapRarity = getBoostedRarity(rarity, getRarityBoostAmount());
			Treasure.LOGGER.debug("get rarity chests for dimension -> {}", dimension.toString());
			// TODO how to merge surface and submerged
			GeneratedRegistry<ChestGenContext> generatedRegistry = DimensionalGeneratedRegistry.getChestGeneratedRegistry(dimension, GeneratorType.TERRESTRIAL);
			Optional<List<ChestGenContext>> chestGenContexts = Optional.empty();
			if (generatedRegistry != null) {
				chestGenContexts = generatedRegistry.getByIRarity(mapRarity);
			}

			if (chestGenContexts.isPresent()) {
				Treasure.LOGGER.debug("got chestInfos by rarity -> {}", mapRarity);
				List<ChestGenContext> validChestInfos = chestGenContexts.get().stream()
						.filter(c -> c.getGenType() != GenType.NONE && !c.isDiscovered() && !c.isCharted())
						.collect(Collectors.toList());
				if (!validChestInfos.isEmpty()) {
					Treasure.LOGGER.debug("got valid chestInfos; size -> {}", validChestInfos.size());
					ChestGenContext chestInfo = validChestInfos.get(random.nextInt(validChestInfos.size()));
					Treasure.LOGGER.debug("using chestInfo -> {}", chestInfo);
					// build a map
					ItemStack mapStack = createMap(world, chestInfo.getCoords(), mapRarity, (byte)2);

					// add map to chest
					inventory.setStackInSlot(((Integer) emptySlots.remove(emptySlots.size() - 1)).intValue(), mapStack);
					// update the chest info in the registry that the map is referring to with this chest's coords
					chestInfo.setChartedFrom(chestCoords);

					// update the current chest gen context
					Optional<ChestGenContext> thisChestInfo = generatedRegistry.get(rarity, chestCoords.toShortString());
					if (thisChestInfo.isPresent()) {
						thisChestInfo.get().setDiscovered(true);
					}
				}
			}			
		}	
	}

	/**
	 * 
	 * @param world
	 * @param coords
	 * @param rarity
	 * @param zoom
	 * @return
	 */
	default public ItemStack createMap(Level world, ICoords coords, IRarity rarity, byte zoom) {
		ItemStack itemStack = MapItem.create(world, coords.getX(), coords.getZ(), zoom, true, true);
		MapItem.renderBiomePreviewMap((ServerLevel) world, itemStack);
		MapItemSavedData.addTargetDecoration(itemStack, coords.toPos(), "+", MapDecoration.Type.RED_X);
		itemStack.setHoverName(new TranslatableComponent("display.treasure_map." + rarity.getValue()));
		return itemStack;
	}

	/**
	 * 
	 * @param rarity
	 * @param amount
	 * @return
	 */
	default public IRarity getBoostedRarity(IRarity rarity, int amount) {
		// TODO needs to change
		// TODO get all rarities from registry and sort by code
		// get the highest and the closest to the boosted amount
		return Rarity.getByCode(Math.min(rarity.getCode() + amount, Rarity.EPIC.getCode()));
	}

	/**
	 * 
	 * @return
	 */
	default public int getRarityBoostAmount() {
		int rarityBoost = 1;
		double mapProbability = RandomHelper.randomDouble(0, 100);
		if (mapProbability < 5.0) {
			rarityBoost = 3;
		}
		else if (mapProbability < 15.0) {
			rarityBoost = 2;
		}
		else if (mapProbability < 25.0) {
			rarityBoost = 1;
		}
		return rarityBoost;
	}

	/**
	 * 
	 * @param inventory
	 * @param random
	 * @param context
	 */
	default public void fillInventory(ItemStackHandler inventory, Random random, List<ItemStack> list) {
		List<Integer> emptySlots = getEmptySlotsRandomized(inventory, random);
		Treasure.LOGGER.debug("empty slots size -> {}", emptySlots.size());
		this.shuffleItems(list, emptySlots.size(), random);

		for (ItemStack itemstack : list) {
			// if no more empty slots are available
			if (emptySlots.isEmpty()) {
				return;
			}

			if (itemstack.isEmpty()) {
				inventory.setStackInSlot(((Integer) emptySlots.remove(emptySlots.size() - 1)).intValue(), ItemStack.EMPTY);
			} 
			else {
				inventory.setStackInSlot(((Integer) emptySlots.remove(emptySlots.size() - 1)).intValue(), itemstack);
			}
		}
	}

	/**
	 * 
	 * @param random
	 * @param rarity
	 * @return
	 */
	default public Optional<LootTableShell> selectLootTable(Random random, final IRarity rarity) {
		LootTableShell lootTableShell = null;

		// select the loot table by rarity
		List<LootTableShell> tables = buildLootTableList(rarity);
		if (tables !=null) { 
			Treasure.LOGGER.debug("tables size -> {}", tables.size());
		}

		// select a random table from the list
		if (tables != null && !tables.isEmpty()) {
			int index = 0;
			if (tables.size() == 1) {
				lootTableShell = tables.get(0);
			} else {
				index = RandomHelper.randomInt(random, 0, tables.size() - 1);
				lootTableShell = tables.get(index);
			}
			Treasure.LOGGER.debug("Selected loot table shell index --> {}, shell -> {}", index, lootTableShell.getCategories());
		}
		return Optional.ofNullable(lootTableShell);
	}

	/**
	 * 
	 * @param factory
	 * @param rarity
	 * @return
	 */
	default public Optional<LootTableShell> selectLootTable(Supplier<Random> factory, IRarity rarity) {
		LootTableShell lootTableShell = null;

		// select the loot table by rarity
		List<LootTableShell> tables = buildLootTableList(rarity);
		if (tables !=null) {
			Treasure.LOGGER.debug("tables size -> {}", tables.size());
		}

		// select a random table from the list
		if (tables != null && !tables.isEmpty()) {
			int index = 0;
			if (tables.size() == 1) {
				lootTableShell = tables.get(0);
			} else {
				index = RandomHelper.randomInt(factory.get(), 0, tables.size() - 1);
				lootTableShell = tables.get(index);
			}
			Treasure.LOGGER.debug("Selected loot table shell index --> {}", index);
		}
		return Optional.ofNullable(lootTableShell);	
	}

	/**
	 * 
	 * @param tileEntity
	 * @param rarity
	 */
	public void addGenerationContext(ITreasureChestBlockEntity blockEntity, IRarity rarity);

	/**
	 * 
	 * @param blockEntity
	 * @param location
	 */
	default public void addLootTable(ITreasureChestBlockEntity blockEntity, ResourceLocation location) {
		blockEntity.setLootTable(location);
	}

	/**
	 * 
	 * @param tileEntity
	 */
	default public void addSeal(ITreasureChestBlockEntity tileEntity) {
		tileEntity.setSealed(true);
	}

	/**
	 * Default implementation. Select locks only from with the same Rarity.
	 * 
	 * @param chest
	 */
	default public void addLocks(Random random, AbstractTreasureChestBlock chest, 
			ITreasureChestBlockEntity blockEntity, IRarity rarity) {

		List<LockItem> locks = new ArrayList<>();
		locks.addAll(KeyLockRegistry.getLocks(rarity).stream().map(lock -> lock.get()).collect(Collectors.toList()));
		addLocks(random, chest, blockEntity, locks);
		locks.clear();
	}

	/**
	 * 
	 * @param random
	 * @param chest
	 * @param blockEntity
	 * @param locks
	 */
	default public void addLocks(Random random, AbstractTreasureChestBlock chest, 
			ITreasureChestBlockEntity blockEntity, 	List<LockItem> locks) {

		int numLocks = randomizedNumberOfLocksByChestType(random, chest.getLockLayout());

		// get the lock states
		List<LockState> lockStates = blockEntity.getLockStates();

		for (int i = 0; i < numLocks; i++) {
			LockItem lock = locks.get(RandomHelper.randomInt(random, 0, locks.size() - 1));
			Treasure.LOGGER.debug("adding lock: {}", lock);
			// add the lock to the chest
			lockStates.get(i).setLock(lock);
		}
	}

	/**
	 * 
	 * @param random
	 * @param lockLayout
	 * @return
	 */
	default public int randomizedNumberOfLocksByChestType(Random random, LockLayout lockLayout) {
		// determine the number of locks to add
		int numLocks = RandomHelper.randomInt(random, 0, lockLayout.getMaxLocks());
		Treasure.LOGGER.debug("# of locks to use: {})", numLocks);
		return numLocks;
	}


	/**
	 * 
	 * @param inventory
	 * @param rand
	 * @return
	 */
	default public List<Integer> getEmptySlotsRandomized(ItemStackHandler inventory, Random rand) {
		List<Integer> list = Lists.<Integer>newArrayList();

		for (int i = 0; i < inventory.getSlots(); ++i) {
			if (inventory.getStackInSlot(i).isEmpty()) {
				list.add(Integer.valueOf(i));
			}
		}

		Collections.shuffle(list, rand);
		return list;
	}

	/**
	 * shuffles items by changing their order (no stack splitting)
	 */
	default public void shuffleItems(List<ItemStack> stacks, int emptySlotsSize, Random rand) {
		Collections.shuffle(stacks, rand);
	}

	/**
	 * Wrapper method so that is can be overridden (as used in the Template Pattern)
	 * 
	 * @param world
	 * @param random
	 * @param coods
	 */
	default public void addMarkers(IWorldGenContext context, ICoords coords, final boolean isSurfaceChest) {
		if (!isSurfaceChest && Config.SERVER.markers.enableMarkerStructures.get() 
				&& RandomHelper.checkProbability(context.random(), Config.SERVER.markers.structureProbability.get())) {
			Treasure.LOGGER.debug("generating a random structure marker -> {}", coords.toShortString());
			//			new StructureMarkerGenerator().generate(world, random, coords);
			new GravestoneMarkerGenerator().generate(context, coords);
		} else {
			new GravestoneMarkerGenerator().generate(context, coords);
		}
	}

	/**
	 * 
	 * @param level
	 * @param random
	 * @param chest
	 * @param chestCoords
	 * @return
	 */
	default public BlockEntity placeInWorld(IWorldGenContext context, AbstractTreasureChestBlock chest, ICoords chestCoords) {
		// replace block @ coords
		boolean isPlaced = GeneratorUtil.replaceBlockWithChest(context, chest, chestCoords);

		// get the backing tile entity of the chest
		BlockEntity blockEntity = (BlockEntity) context.level().getBlockEntity(chestCoords.toPos());

		// check to ensure the chest has been generated
		if (!isPlaced || !(context.level().getBlockState(chestCoords.toPos()).getBlock() instanceof AbstractTreasureChestBlock)) {
			Treasure.LOGGER.debug("Unable to place chest @ {}", chestCoords.toShortString());
			// remove the title entity (if exists)

			// if a block entity exists, then this is on a server level
			if (blockEntity != null && (blockEntity instanceof AbstractTreasureChestBlockEntity)) {
				((ServerLevel)context.level()).removeBlockEntity(chestCoords.toPos());
			}
			return null;
		}

		// if tile entity failed to create, remove the chest
		if (blockEntity == null || !(blockEntity instanceof AbstractTreasureChestBlockEntity)) {
			// remove chest
			context.level().setBlock(chestCoords.toPos(), Blocks.AIR.defaultBlockState(), 3);
			Treasure.LOGGER.debug("Unable to create BlockEntityChest, removing BlockChest");
			return null;
		}
		return blockEntity;
	}

	/**
	 * 
	 * @param level
	 * @param random
	 * @param chestCoords
	 * @param chest
	 * @param state
	 * @return
	 */
	default public BlockEntity placeInWorld(IWorldGenContext context, ICoords chestCoords,
			AbstractTreasureChestBlock chest, BlockState state) {
		
		// replace block @ coords
		boolean isPlaced = GeneratorUtil.replaceBlockWithChest(context, chestCoords, chest, state);
		Treasure.LOGGER.debug("isPlaced -> {}", isPlaced);
		// get the backing tile entity of the chest
		BlockEntity blockEntity = (BlockEntity) context.level().getBlockEntity(chestCoords.toPos());

		// check to ensure the chest has been generated
		if (!isPlaced || !(context.level().getBlockState(chestCoords.toPos()).getBlock() instanceof AbstractTreasureChestBlock)) {
			Treasure.LOGGER.debug("Unable to place chest @ {}", chestCoords.toShortString());
			// remove the title entity (if exists)
			if (blockEntity != null && (blockEntity instanceof AbstractTreasureChestBlockEntity)) {
				((ServerLevel)context.level()).removeBlockEntity(chestCoords.toPos());
			}
			return null;
		}

		// if tile entity failed to create, remove the chest
		if (blockEntity == null || !(blockEntity instanceof AbstractTreasureChestBlockEntity)) {
			// remove chest
			context.level().setBlock(chestCoords.toPos(), Blocks.AIR.defaultBlockState(), 3);
			Treasure.LOGGER.debug("Unable to create BlockEntityChest, removing BlockChest");
			return null;
		}
		return blockEntity;
	}
}