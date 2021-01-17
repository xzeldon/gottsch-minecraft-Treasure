/**
 * 
 */
package com.someguyssoftware.treasure2.eventhandler;

import com.someguyssoftware.gottschcore.mod.IMod;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.data.TreasureData;
import com.someguyssoftware.treasure2.loot.TreasureLootTableMaster2;
import com.someguyssoftware.treasure2.loot.TreasureLootTableRegistry;
import com.someguyssoftware.treasure2.persistence.TreasureGenerationSavedData;
import com.someguyssoftware.treasure2.registry.TreasureDecayRegistry;
import com.someguyssoftware.treasure2.registry.TreasureMetaRegistry;
import com.someguyssoftware.treasure2.registry.TreasureTemplateRegistry;
import com.someguyssoftware.treasure2.world.gen.feature.TreasureFeatures;

import net.minecraft.data.loot.ChestLootTables;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTables;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

/**
 * @author Mark Gottschling on Jun 29, 2018
 *
 */
//@Mod.EventBusSubscriber(modid = Treasure.MODID, bus = EventBusSubscriber.Bus.MOD)
public class WorldEventHandler {

	// reference to the mod.
	private IMod mod;
	
	/**
	 * 
	 */
	public WorldEventHandler(IMod mod) {
		setMod(mod);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onWorldLoad(WorldEvent.Load event) {
		Treasure.LOGGER.info("In world load event for dimension {}", event.getWorld().getDimension().toString());
		
		/*
		 * On load of dimension 0 (overworld), initialize the loot table's context and other static loot tables
		 */
		if (WorldInfo.isServerSide((World)event.getWorld()) && event.getWorld().getDimension().isSurfaceWorld()) {
			ServerWorld world = (ServerWorld) event.getWorld();

			// called once to initiate world-level properties in the LootTableMaster
			TreasureLootTableRegistry.initialize(world);
			
			// register mod's loot tables
			TreasureLootTableRegistry.register(mod.getId());
			TreasureMetaRegistry.register(getMod().getId());
			TreasureTemplateRegistry.register(getMod().getId());
			TreasureDecayRegistry.register(getMod().getId());
					
			/*
			 * clear the current World Gens values and reload
			 */
			TreasureFeatures.FEATURES.forEach(feature -> {
				feature.init();
			});

			/*
			 * un-load the chest registry
			 */
			TreasureData.CHEST_REGISTRIES.entrySet().forEach(entry -> {
//				Treasure.logger.debug("Chest registry size BEFORE cleaning -> {}", ChestRegistry.getInstance().getValues().size());
				entry.getValue().clear();
//				Treasure.logger.debug("Chest registry size AFTER cleaning -> {}", ChestRegistry.getInstance().getValues().size());
			});		
			TreasureGenerationSavedData.get(world);
//			Treasure.logger.debug("Chest registry size after world event load -> {}", ChestRegistry.getInstance().getValues().size());
		}	
	}
	
	@SubscribeEvent
	public void lootLoad(LootTableLoadEvent event) {
		if (event.getName().equals(LootTables.CHESTS_SIMPLE_DUNGEON/*"minecraft:chests/simple_dungeon"*/)) {
			// load a loot table
			ResourceLocation location = new ResourceLocation(Treasure.MODID, "pools/treasure/scarce");
			LootTable lootTable = event.getLootTableManager().getLootTableFromLocation(location);
			LootPool pool = lootTable.getPool("treasure");
			event.getTable().addPool(pool);		
		}
	}
	
	@SubscribeEvent
	public void onServerStopping(FMLServerStoppingEvent event) {
		Treasure.LOGGER.debug("Closing out of world.");
		// clear all resource managers
		TreasureLootTableRegistry.getLootTableMaster().clear();
		TreasureTemplateRegistry.getTemplateManager().clear();
		TreasureMetaRegistry.getMetaManager().clear();
		TreasureDecayRegistry.getDecayManager().clear();
	}
	
	/**
	 * @return the mod
	 */
	public IMod getMod() {
		return mod;
	}

	/**
	 * @param mod the mod to set
	 */
	public void setMod(IMod mod) {
		this.mod = mod;
	}

}
