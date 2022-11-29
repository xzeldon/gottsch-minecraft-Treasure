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
package mod.gottsch.forge.treasure2.core.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;

import mod.gottsch.forge.gottschcore.config.AbstractConfig;
import mod.gottsch.forge.treasure2.Treasure;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;

/**
 * 
 * @author Mark Gottschling on Nov 7, 2022
 *
 */
@EventBusSubscriber(modid = Treasure.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config extends AbstractConfig {
	public static final String CATEGORY_DIV = "##############################";
	public static final String UNDERLINE_DIV = "------------------------------";

	public static final ForgeConfigSpec SERVER_SPEC;
	public static final ServerConfig SERVER;

	public static final ForgeConfigSpec CLIENT_SPEC;
	public static final ClientConfig CLIENT;
	
	public static final ForgeConfigSpec COMMON_SPEC;
	public static final CommonConfig COMMON;
	
	// setup as a singleton
	public static Config instance = new Config();
		
	static {
		final Pair<CommonConfig, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder()
				.configure(CommonConfig::new);
		COMMON_SPEC = commonSpecPair.getRight();
		COMMON = commonSpecPair.getLeft();
		
		final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
				.configure(ServerConfig::new);
		SERVER_SPEC = specPair.getRight();
		SERVER = specPair.getLeft();

		final Pair<ClientConfig, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder()
				.configure(ClientConfig::new);
		CLIENT_SPEC = clientSpecPair.getRight();
		CLIENT = clientSpecPair.getLeft();
	}
	
	private Config() {}
	
	/**
	 * 
	 */
	public static void register() {
		registerCommonConfig();
		registerClientConfig();
		registerServerConfig();
	}
	
	private static void registerCommonConfig() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
	}
	
	private static void registerClientConfig() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
	}
	
	private static void registerServerConfig() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
	}
	
	/*
	 * 
	 */
	public static class CommonConfig {
		public static Logging logging;
		public CommonConfig(ForgeConfigSpec.Builder builder) {
			logging = new Logging(builder);
		}
	}
	
	/*
	 * 
	 */
	public static class ClientConfig {
		public ClientGui gui;

		public ClientConfig(ForgeConfigSpec.Builder builder) {
			gui = new ClientGui(builder);
		}
	}
	
	public static class ClientGui {
		public BooleanValue enableCustomChestInventoryGui;
		public ForgeConfigSpec.BooleanValue enableFog;
		
		ClientGui(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV, " GUI properties", CATEGORY_DIV)
			.push("gui");

			enableCustomChestInventoryGui = builder
					.comment(" Enable/Disable whether to use Treasure2's custom guis for chest inventory screens.")
					.define("enableCustomChestInventoryGui", true);
						
			enableFog = builder
					.comment(" Enable/disable white fog.")
					.define("Enable fog:", true);
			
			builder.pop();
		}
	}
	/*
	 * 
	 */
	public static class ServerConfig {
		public KeysAndLocks keysAndLocks;
		public Wealth wealth;
		public Effects effects;
		public Integration integration;
		public Markers markers;

		public ServerConfig(ForgeConfigSpec.Builder builder) {
			keysAndLocks = new KeysAndLocks(builder);	
			wealth = new Wealth(builder);
			effects = new Effects(builder);
			integration = new Integration(builder);
			markers = new Markers(builder);
		}
		
		/*
		 * 
		 */
		public static class Integration {
			public ConfigValue<List<? extends String>> dimensionsWhiteList;
			
			public Integration(final ForgeConfigSpec.Builder builder)	 {
				builder.comment(CATEGORY_DIV, " Integration properties", CATEGORY_DIV)
				.push("integration");
				
				dimensionsWhiteList = builder
						.comment(" Permitted Dimensions for Treasure2 execution.", 
								" Treasure2 was designed for 'normal' overworld-type dimensions.", 
								" This setting does not use any wildcards (*). You must explicitly set the dimensions that are allowed.", 
								" ex. minecraft:overworld")
						.defineList("Dimension White List:", Arrays.asList(new String []{"minecraft:overworld"}), s -> s instanceof String);
				builder.pop();
			}
		}
		
		/*
		 * 
		 */
		public static class KeysAndLocks {
			public BooleanValue enableKeyBreaks;
			public BooleanValue enableLockDrops;
			public ConfigValue<Integer> pilferersLockPickMaxUses;
			public ConfigValue<Integer> thiefsLockPickMaxUses;
			public ConfigValue<Integer> woodKeyMaxUses;
			public ConfigValue<Integer> stoneKeyMaxUses;
			public ConfigValue<Integer> emberKeyMaxUses;
			public ConfigValue<Integer> leafKeyMaxUses;
			public ConfigValue<Integer> lightningKeyMaxUses;
			public ConfigValue<Integer> ironKeyMaxUses;
			public ConfigValue<Integer> goldKeyMaxUses;
			public ConfigValue<Integer> diamondKeyMaxUses;
			public ConfigValue<Integer> emeraldKeyMaxUses;
			public ConfigValue<Integer> rubyKeyMaxUses;
			public ConfigValue<Integer> sapphireKeyMaxUses;
			public ConfigValue<Integer> metallurgistsKeyMaxUses;
			public ConfigValue<Integer> skeletonKeyMaxUses;
			public ConfigValue<Integer> jewelledKeyMaxUses;
			public ConfigValue<Integer> spiderKeyMaxUses;
			public ConfigValue<Integer> witherKeyMaxUses;
			
			KeysAndLocks(final ForgeConfigSpec.Builder builder) {
				builder.comment(CATEGORY_DIV, " Keys and Locks properties", CATEGORY_DIV)
				.push("keysAndLocks");

				enableKeyBreaks = builder
						.comment(" Enable/Disable whether a Key can break when attempting to unlock a Lock.")
						.define("enableKeyBreaks", true);

				enableLockDrops = builder
						.comment(" Enable/Disable whether a Lock item is dropped when unlocked by Key item.")
						.define("enableLockDrops", true);
				
				pilferersLockPickMaxUses = builder
						.comment(" The maximum uses for a given pilferers lock pick.")
						.defineInRange("pilferersLockPickMaxUses", 10, 1, 32000);

				thiefsLockPickMaxUses = builder
						.comment(" The maximum uses for a given thiefs lock pick.")
						.defineInRange("thiefsLockPickMaxUses", 10, 1, 32000);

				woodKeyMaxUses = builder
						.comment(" The maximum uses for a given wooden key.")
						.defineInRange("woodKeyMaxUses", 20, 1, 32000);

				stoneKeyMaxUses = builder
						.comment(" The maximum uses for a given stone key.")
						.defineInRange("stoneKeyMaxUses", 10, 1, 32000);

				emberKeyMaxUses = builder
						.comment(" The maximum uses for a given ember key.")
						.defineInRange("emberKeyMaxUses", 15, 1, 32000);

				leafKeyMaxUses = builder
						.comment(" The maximum uses for a given leaf key.")
						.defineInRange("leafKeyMaxUses", 15, 1, 32000); 

				lightningKeyMaxUses = builder
						.comment(" The maximum uses for a given lightning key.")
						.defineInRange("lightningKeyMaxUses", 10, 1, 32000); 

				ironKeyMaxUses = builder
						.comment(" The maximum uses for a given iron key.")
						.defineInRange("ironKeyMaxUses", 10, 1, 32000);

				goldKeyMaxUses = builder
						.comment(" The maximum uses for a given gold key.")
						.defineInRange("goldKeyMaxUses", 15, 1, 32000);

				diamondKeyMaxUses = builder
						.comment(" The maximum uses for a given diamond key.")
						.defineInRange("diamondKeyMaxUses", 20, 1, 32000);

				emeraldKeyMaxUses = builder
						.comment(" The maximum uses for a given emerald key.")
						.defineInRange("emeraldKeyMaxUses", 10, 1, 32000);

				rubyKeyMaxUses = builder
						.comment(" The maximum uses for a given ruby key.")
						.defineInRange("rubyKeyMaxUses", 5, 1, 32000);

				sapphireKeyMaxUses = builder
						.comment(" The maximum uses for a given sapphire key.")
						.defineInRange("sapphireKeyMaxUses", 5, 1, 32000);

				metallurgistsKeyMaxUses = builder
						.comment(" The maximum uses for a given metallurgists key.")
						.defineInRange("metallurgistsKeyMaxUses", 25, 1, 32000);

				skeletonKeyMaxUses = builder
						.comment(" The maximum uses for a given skeleton key.")
						.defineInRange("skeletonKeyMaxUses", 5, 1, 32000);

				jewelledKeyMaxUses = builder
						.comment(" The maximum uses for a given jewelled key.")
						.defineInRange("jewelledKeyMaxUses", 5, 1, 32000);

				spiderKeyMaxUses = builder
						.comment(" The maximum uses for a given spider key.")
						.defineInRange("spiderKeyMaxUses", 5, 1, 32000);

				witherKeyMaxUses = builder
						.comment(" The maximum uses for a given wither key.")
						.defineInRange("witherKeyMaxUses", 5, 1, 32000);
				
				builder.pop();
			}
		}
		
		/*
		 * 
		 */
		public static class Wealth {
			public ForgeConfigSpec.ConfigValue<Integer> wealthMaxStackSize;
			
			public Wealth(final ForgeConfigSpec.Builder builder)	 {
				builder.comment(CATEGORY_DIV, " Treasure Loot and Valuables properties", CATEGORY_DIV)
				.push("wealth");
				
				wealthMaxStackSize = builder
						.comment(" The maximum size of a wealth item stacks. ex. Coins, Gems, Pearls")
						.defineInRange("wealthMaxStackSize", 8, 1, 64);
				builder.pop();
			}
		}
		
		/*
		 * 
		 */
		public static class Markers {
			public ForgeConfigSpec.BooleanValue enableMarkers;
			public ForgeConfigSpec.BooleanValue enableMarkerStructures;
			public ForgeConfigSpec.ConfigValue<Integer> minGravestonesPerChest;
			public ForgeConfigSpec.ConfigValue<Integer> maxGravestonesPerChest;
			public ForgeConfigSpec.ConfigValue<Integer> markerStructureProbability;
			public ForgeConfigSpec.BooleanValue enableGravestoneSpawner;
			public ForgeConfigSpec.ConfigValue<Integer> gravestoneSpawnerProbability;
			
			public Markers(final ForgeConfigSpec.Builder builder)	 {
				builder.comment(CATEGORY_DIV, " Gravestones and Markers properties", CATEGORY_DIV)
				.push("treasure markers");
				
				enableMarkers = builder
						.comment(" Enable/disable whether chest markers (gravestones, bones)  are generated when generating treasure chests.")
						.define("Enable markers:", true);
				
				enableMarkerStructures = builder
						.comment(" Enable/disable whether structures (buildings) are generated when generating  treasure chests.")
						.define("Enable structure markers:", true);
				
				minGravestonesPerChest = builder
						.comment(" The minimum number of markers (gravestones, bones) per chest.")
						.defineInRange("Minimum markers per chest:", 2, 1, 5);
				
				maxGravestonesPerChest = builder
						.comment(" The maximum number of markers (gravestones, bones) per chest.")
						.defineInRange("Maximum markers per chest:", 5, 1, 10);
				
				markerStructureProbability = builder
						.comment(" The probability that a marker will be a structure.")
						.defineInRange("Probability that marker will be a structure:", 15, 1, 100);
				
				enableGravestoneSpawner = builder
						.comment(" Enable/disable whether gravestone markers can spawn mobs (ex. Bound Soul).")
						.define("Enable gravestone markers to spawn mobs:", true);
				
				gravestoneSpawnerProbability = builder
						.comment(" The probability that a gravestone will spawn a mob.", " Currently gravestones can spawn Bound Souls.")
						.defineInRange("Probability that grave marker will spawn a mob:", 25, 1, 100);
								
				builder.pop();
			}
		}
		
		/*
		 * 
		 */
		public static class Effects {
			public BooleanValue enableUndiscoveredEffects;

			
			public Effects(final ForgeConfigSpec.Builder builder)	 {
				builder.comment(CATEGORY_DIV, " Effects and GUI Elements", CATEGORY_DIV)
				.push("effects");
				
				enableUndiscoveredEffects = builder
						.comment(" Enable/disable whether 'undiscovered' chests (ie spawned and not found) will display effects such as light source, particles, or glow.")
						.define("enableUndiscoveredEffects", true);
		
				builder.pop();
			}
		}
	}
	
	/**
	 * Chest Config
	 */
	public static final ForgeConfigSpec CHESTS_CONFIG_SPEC;
		
	// TODO make this a map and part of the transform() method loads into a map
	/*
	 * exposed chest configurations
	 */
	public static List<ChestConfiguration> chestConfigs;
	
	static {
		final Pair<ChestConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
				.configure(ChestConfig::new);
		CHESTS_CONFIG_SPEC = specPair.getRight();
	}
	
	/*
	 * internal config class only. used for loading the defaultconfig file
	 * and transformed into the exposed chestConfigs property
	 */
	private static class ChestConfig {
		public ChestConfig(ForgeConfigSpec.Builder builder) {
			builder.comment("####", " rarities = common, uncommon, scarce, rare, epic, legendary, mythical", "####").define("chestConfigs", new ArrayList<>());
			builder.build();
		}
	}

	/**
	 * 
	 * @param configData
	 */
	public static void transform(CommentedConfig configData) {
		// convert the data to an object
		ChestConfigsHolder holder = new ObjectConverter().toObject(configData, ChestConfigsHolder::new);
		// get the list from the holder and set the config property
		chestConfigs = holder.chestConfigs;
	}
	
	/**
	 * A temporary holder class.
	 *
	 */
	private static class ChestConfigsHolder {
		public List<ChestConfiguration> chestConfigs;
	}
}


