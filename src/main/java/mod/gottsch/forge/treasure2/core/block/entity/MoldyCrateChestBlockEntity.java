/*
 * This file is part of  Treasure2.
 * Copyright (c) 2018 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.treasure2.core.block.entity;

import mod.gottsch.forge.treasure2.core.util.LangUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 
 * @author Mark Gottschling on Jan 19, 2018
 *
 */
public class MoldyCrateChestBlockEntity extends CrateChestBlockEntity {

	/**
	 * 
	 * @param texture
	 */
	public MoldyCrateChestBlockEntity(BlockPos pos, BlockState state) {
		super(TreasureBlockEntities.MOLDY_CRATE_CHEST_BLOCK_ENTITY_TYPE.get(), pos, state);
	}

	@Override
	public Component getDefaultName() {
		return Component.translatable(LangUtil.screen("moldy_crate_chest.name"));
	}

	@Override
	public void doChestOpenEffects(Level level, Player player, BlockPos pos) {
		super.doChestOpenEffects(level, player, pos);
		// spawn particles

		if (level.isClientSide()) {
			RandomSource random = getLevel().getRandom();
			for(int k = 0; k < 20; ++k) {
				level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, 
						(double)getBlockPos().getX() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), 
						(double)getBlockPos().getY() + random.nextDouble() + random.nextDouble(),
						(double)getBlockPos().getZ() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1),
						0D, 0D, 0D);
			}
		}
	}
}
