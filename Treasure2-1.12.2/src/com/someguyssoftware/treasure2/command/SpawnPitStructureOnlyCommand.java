/**
 * 
 */
package com.someguyssoftware.treasure2.command;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import com.someguyssoftware.gottschcore.positional.Coords;
import com.someguyssoftware.gottschcore.positional.ICoords;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.enums.PitTypes;
import com.someguyssoftware.treasure2.enums.Pits;
import com.someguyssoftware.treasure2.generator.ChestGeneratorData;
import com.someguyssoftware.treasure2.generator.GeneratorResult;
import com.someguyssoftware.treasure2.generator.pit.IPitGenerator;
import com.someguyssoftware.treasure2.worldgen.SurfaceChestWorldGenerator;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 
 * @author Mark Gottschling on Jan 25, 2018
 *
 */
public class SpawnPitStructureOnlyCommand extends CommandBase {
	private static final String PIT_TYPE_ARG = "pit";
	
	@Override
	public String getName() {
		return "t2-pitstructureonly";
	}

	@Override
	public String getUsage(ICommandSender var1) {
		return "/t2-pitstructureonly <x> <y> <z> [-pit <type>]: spawns a Treasure! pit structure at location (x,y,z)";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) {
		Treasure.logger.debug("Starting to build Treasure! pit structure only...");
		try {
			int x, y, z = 0;
			x = Integer.parseInt(args[0]);
			y = Integer.parseInt(args[1]);
			z = Integer.parseInt(args[2]);
			
			// set the coords args to blank (so the cli parser doesn't puke on any negative
			// values - thinks they are arguments
			args[0] = args[1] = args[2] = "";

			// create the parser
			CommandLineParser parser = new DefaultParser();

			// create Options object
			Options options = new Options();
			options.addOption(PIT_TYPE_ARG, true, "");

			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			String pitType = line.getOptionValue(PIT_TYPE_ARG);
			
			Pits pit = Pits.SIMPLE_PIT;
			if (line.hasOption(PIT_TYPE_ARG)) {
				String pitName = line.getOptionValue(PIT_TYPE_ARG);
				pit = Pits.valueOf(pitName.toUpperCase());
			}

			World world = commandSender.getEntityWorld();
			Random random = new Random();
			ICoords spawnCoords = new Coords(x, y, z);
			ICoords surfaceCoords = WorldInfo.getDryLandSurfaceCoords(world, new Coords(x, WorldInfo.getHeightValue(world, spawnCoords), z));

			// select a pit generator
			Map<Pits, IPitGenerator<GeneratorResult<ChestGeneratorData>>> pitGenMap = SurfaceChestWorldGenerator.pitGens.row(PitTypes.STRUCTURE);
			IPitGenerator<GeneratorResult<ChestGeneratorData>> pitGenerator = pitGenMap.get(pit);
			pitGenerator.generate(world, random, surfaceCoords , spawnCoords);
		}		
		catch(Exception e) {
			Treasure.logger.error("Error generating Treasure! pit structure:", e);
		}
	}
	
    /**
     * Get a list of options for when the user presses the TAB key
     */
	@Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length > 3) {
        	if (args[args.length - 2].equals("-" + PIT_TYPE_ARG)) {
        		return getListOfStringsMatchingLastWord(args, Pits.getNames());
        	}
        }		
		return Collections.emptyList();
    }
}
