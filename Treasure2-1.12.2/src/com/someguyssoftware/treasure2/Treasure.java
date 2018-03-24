/**
 * 
 */
package com.someguyssoftware.treasure2;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.someguyssoftware.gottschcore.annotation.Credits;
import com.someguyssoftware.gottschcore.command.ShowVersionCommand;
import com.someguyssoftware.gottschcore.config.IConfig;
import com.someguyssoftware.gottschcore.config.ILoggerConfig;
import com.someguyssoftware.gottschcore.mod.AbstractMod;
import com.someguyssoftware.gottschcore.mod.IMod;
import com.someguyssoftware.gottschcore.version.BuildVersion;
import com.someguyssoftware.lootbuilder.db.DbManager;
import com.someguyssoftware.lootbuilder.exception.DatabaseInitializationException;
import com.someguyssoftware.treasure2.client.gui.GuiHandler;
import com.someguyssoftware.treasure2.client.model.BandedChestModel;
import com.someguyssoftware.treasure2.client.model.CrateChestModel;
import com.someguyssoftware.treasure2.client.model.SafeModel;
import com.someguyssoftware.treasure2.client.model.StandardChestModel;
import com.someguyssoftware.treasure2.client.model.StrongboxModel;
import com.someguyssoftware.treasure2.client.render.tileentity.CrateChestTileEntityRenderer;
import com.someguyssoftware.treasure2.client.render.tileentity.SafeTileEntityRenderer;
import com.someguyssoftware.treasure2.client.render.tileentity.StrongboxTileEntityRenderer;
import com.someguyssoftware.treasure2.client.render.tileentity.TreasureChestTileEntityRenderer;
import com.someguyssoftware.treasure2.command.SpawnPitCommand;
import com.someguyssoftware.treasure2.command.SpawnPitOnlyCommand;
import com.someguyssoftware.treasure2.command.SpawnWellCommand;
import com.someguyssoftware.treasure2.command.TreasureChestCommand;
import com.someguyssoftware.treasure2.config.Configs;
import com.someguyssoftware.treasure2.config.TreasureConfig;
import com.someguyssoftware.treasure2.eventhandler.LogoutEventHandler;
import com.someguyssoftware.treasure2.item.TreasureItems;
import com.someguyssoftware.treasure2.tileentity.CrateChestTileEntity;
import com.someguyssoftware.treasure2.tileentity.GoldStrongboxTileEntity;
import com.someguyssoftware.treasure2.tileentity.IronStrongboxTileEntity;
import com.someguyssoftware.treasure2.tileentity.IronboundChestTileEntity;
import com.someguyssoftware.treasure2.tileentity.MoldyCrateChestTileEntity;
import com.someguyssoftware.treasure2.tileentity.PirateChestTileEntity;
import com.someguyssoftware.treasure2.tileentity.SafeTileEntity;
import com.someguyssoftware.treasure2.tileentity.AbstractTreasureChestTileEntity;
import com.someguyssoftware.treasure2.tileentity.WoodChestTileEntity;
import com.someguyssoftware.treasure2.worldgen.ChestWorldGenerator;
import com.someguyssoftware.treasure2.worldgen.WellWorldGenerator;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * IDEA - Wither Chest - 2x high chest with tree texture, stump branches and roots. double door opening, with 90 degree
 * doors, like a cabinet.
 * IDEA - Wither Dirt - around wither tree, tinted black (see dirt, swamp on how color tinting works).
 * IDEA - either have smoke streams rising from wither dirt or have fog blocks.
 * @author Mark Gottschling onDec 22, 2017
 *
 */
@Mod(
		modid=Treasure.MODID,
		name=Treasure.NAME,
		version=Treasure.VERSION,
		dependencies="required-after:gottschcore@[1.3.0,)",
		acceptedMinecraftVersions = "[1.12.2]",
		updateJSON = Treasure.UPDATE_JSON_URL
		)
@Credits(values={"Treasure was first developed by Mark Gottschling on Aug 27, 2014.","Credits to Mason for ideas and debugging."})
public class Treasure extends AbstractMod {

	// constants
	public static final String MODID = "treasure2";
	protected static final String NAME = "Treasure2";
	protected static final String VERSION = "0.5.3";
	public static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/gottsch/gottsch-minecraft-Treasure/master/Treasure2-1.12.2/update.json";

	private static final String VERSION_URL = "";
	private static final BuildVersion MINECRAFT_VERSION = new BuildVersion(1, 12, 2);
	
	// latest version
	private static BuildVersion latestVersion;
	
	// logger
	public static Logger logger = LogManager.getLogger(Treasure.NAME);
	
	@Instance(value=Treasure.MODID)
	public static Treasure instance;
	
	/*
	 *  Treasure Creative Tab
	 *  Must be instantized <b>before</b> any registry events so that it is available to assign to blocks and items.
	 */
	public static CreativeTabs TREASURE_TAB = new CreativeTabs(CreativeTabs.getNextID(), Treasure.MODID + ":" + TreasureConfig.TREASURE_TAB_ID) {
		@SideOnly(Side.CLIENT)
		public ItemStack getTabIconItem() {
			return new ItemStack(TreasureItems.TREASURE_TAB, 1);
		}
	};
    
	// forge world generators
    public static Map<String, IWorldGenerator> worldGenerators = new HashMap<>();
    
	/**
	 * 
	 */
	public Treasure() {}
	
	/**
	 * 
	 * @param event
	 */
	@Override
	@EventHandler
	public void preInt(FMLPreInitializationEvent event) {
		super.preInt(event);
		
		// register additional events
		MinecraftForge.EVENT_BUS.register(new LogoutEventHandler(getInstance()));
		
		// create and load the config files
		Configs.init(this, event.getModConfigurationDirectory());
		
		// configure logging
		addRollingFileAppenderToLogger(Treasure.NAME, Treasure.NAME + "Appender", (ILoggerConfig) getConfig());
		
		// register the GUI handler
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
		
		// start the database
		try {
			DbManager.start((TreasureConfig)getConfig());
		} catch (DatabaseInitializationException e) {
			logger.error("Unable to start database manager:", e);
			getConfig().setModEnabled(false);
			// TODO create another PlayerLoggedIn Event that checks if the database failed initialization and inform player.
		}
	}
	
	/**
	 * 
	 * @param event
	 */
	@EventHandler
    public void serverStarted(FMLServerStartingEvent event) {
		if (!getConfig().isModEnabled()) return;
		
    	// add a show version command
    	event.registerServerCommand(new ShowVersionCommand(this));
    	
		// register additional commands
    	event.registerServerCommand(new TreasureChestCommand());
    	event.registerServerCommand(new SpawnPitCommand());
    	event.registerServerCommand(new SpawnPitOnlyCommand());
    	event.registerServerCommand(new SpawnWellCommand());
    }
	
	/**
	 * 
	 */
	@Override
	@EventHandler
	public void init(FMLInitializationEvent event) {
		// don't process is mod is disabled
		if (!getConfig().isModEnabled()) return;
		
		super.init(event);
		
		// register world generators
		worldGenerators.put("chest", new ChestWorldGenerator());
		worldGenerators.put("well", new WellWorldGenerator());
		int genWeight = 0;
		for (Entry<String, IWorldGenerator> gen : worldGenerators.entrySet()) {
			GameRegistry.registerWorldGenerator(gen.getValue(), genWeight++);
		}
		
	}
	
	/**
	 * 
	 */
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		if (!getConfig().isModEnabled()) return;		

		// perform any post init
		super.postInit(event);
	}
	

	
	/* (non-Javadoc)
	 * @see com.someguyssoftware.gottschcore.mod.IMod#getConfig()
	 */
	@Override
	public IConfig getConfig() {
		return Configs.modConfig;
	}

	/* (non-Javadoc)
	 * @see com.someguyssoftware.gottschcore.mod.IMod#getMinecraftVersion()
	 */
	@Override
	public BuildVersion getMinecraftVersion() {
		return Treasure.MINECRAFT_VERSION;
	}

	/* (non-Javadoc)
	 * @see com.someguyssoftware.gottschcore.mod.IMod#getVerisionURL()
	 */
	@Override
	public String getVerisionURL() {
		return Treasure.VERSION_URL;
	}

	/* (non-Javadoc)
	 * @see com.someguyssoftware.gottschcore.mod.IMod#getName()
	 */
	@Override
	public String getName() {
		return Treasure.NAME;
	}

	/* (non-Javadoc)
	 * @see com.someguyssoftware.gottschcore.mod.IMod#getId()
	 */
	@Override
	public String getId() {
		return Treasure.MODID;
	}

	/* (non-Javadoc)
	 * @see com.someguyssoftware.gottschcore.mod.AbstractMod#getInstance()
	 */
	@Override
	public IMod getInstance() {
		return Treasure.instance;
	}

	/* (non-Javadoc)
	 * @see com.someguyssoftware.gottschcore.mod.AbstractMod#getVersion()
	 */
	@Override
	public String getVersion() {
		return Treasure.VERSION;
	}

	@Override
	public BuildVersion getModLatestVersion() {
		return latestVersion;
	}

	@Override
	public void setModLatestVersion(BuildVersion version) {
		Treasure.latestVersion = version;		
	}
	
	@Override
	public String getUpdateURL() {
		return Treasure.UPDATE_JSON_URL;
	}

}
