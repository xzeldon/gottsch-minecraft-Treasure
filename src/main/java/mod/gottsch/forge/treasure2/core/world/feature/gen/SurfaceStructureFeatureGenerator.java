/*
 * This file is part of  Treasure2.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.treasure2.core.world.feature.gen;

import java.util.List;
import java.util.Optional;

import mod.gottsch.forge.gottschcore.enums.IRarity;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.IWorldGenContext;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.treasure2.Treasure;
import mod.gottsch.forge.treasure2.core.config.ChestConfiguration.ChestRarity;
import mod.gottsch.forge.treasure2.core.generator.ChestGeneratorData;
import mod.gottsch.forge.treasure2.core.generator.GeneratorResult;
import mod.gottsch.forge.treasure2.core.generator.pit.IPitGenerator;
import mod.gottsch.forge.treasure2.core.generator.ruin.IRuinGenerator;
import mod.gottsch.forge.treasure2.core.generator.ruin.SurfaceRuinGenerator;
import mod.gottsch.forge.treasure2.core.registry.RuinGeneratorRegistry;
import mod.gottsch.forge.treasure2.core.structure.StructureCategory;

/**
 * 
 * @author Mark Gottschling May 12, 2023
 *
 */
public class SurfaceStructureFeatureGenerator implements IFeatureGenerator {

	@Override
	public Optional<GeneratorResult<ChestGeneratorData>> generate(IWorldGenContext context, ICoords spawnCoords,
			IRarity rarity, ChestRarity rarityConfig) {
		
		Treasure.LOGGER.debug("surface coords -> {}", spawnCoords.toShortString());
		if (!WorldInfo.isHeightValid(spawnCoords)) {
			Treasure.LOGGER.debug("surface coords are invalid -> {}", spawnCoords.toShortString());
			return Optional.empty();
		}

		// check if it has 50% land
		if (!WorldInfo.isSolidBase(context.level(), spawnCoords, 2, 2, 50)) {
			Treasure.LOGGER.debug("coords -> {} does not meet solid base requires for {} x {}", spawnCoords.toShortString(), 2, 2);
			return Optional.empty();
		}
		
		// select a ruins generator
		IRuinGenerator<GeneratorResult<ChestGeneratorData>> ruinGenerator = selectGenerator(context, spawnCoords, rarity);
		
		// select a template
		// generate structure
		Optional<GeneratorResult<ChestGeneratorData>> ruinResult = ruinGenerator.generate(context, spawnCoords, null);
		if (!ruinResult.isPresent()) {
			return Optional.empty();
		}
		Treasure.LOGGER.debug("surface result -> {}", ruinResult.toString());
//		
//		ICoords chestCoords = new Coords(spawnCoords);
//		Treasure.LOGGER.debug("surface chest coords -> {}", chestCoords);
				
		ruinResult.get().getData().setSpawnCoords(spawnCoords);
		
		return ruinResult;
	}

	/**
	 * 
	 * @param context
	 * @param coords
	 * @param rarity
	 * @return
	 */
	public IRuinGenerator<GeneratorResult<ChestGeneratorData>> selectGenerator(IWorldGenContext context, ICoords coords, IRarity rarity) {
		List<IRuinGenerator<GeneratorResult<ChestGeneratorData>>> generators = RuinGeneratorRegistry.get(StructureCategory.TERRANEAN);
		IRuinGenerator<GeneratorResult<ChestGeneratorData>> generator = generators.get(context.random().nextInt(generators.size()));
		return generator;
	}

}
