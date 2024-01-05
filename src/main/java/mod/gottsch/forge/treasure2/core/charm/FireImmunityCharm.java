/*
 * This file is part of  Treasure2.
 * Copyright (c) 2021, Mark Gottschling (gottsch)
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
package mod.gottsch.forge.treasure2.core.charm;

import java.util.Random;

import com.someguyssoftware.gottschcore.spatial.ICoords;

import mod.gottsch.forge.treasure2.core.util.ModUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * 
 * @author Mark Gottschling on Dec 27, 2020
 *
 */
public class FireImmunityCharm extends Charm {
	public static final String FIRE_IMMUNITY_TYPE = "fire_immunity";
	private static final Class<?> REGISTERED_EVENT = LivingDamageEvent.class;

	/**
	 * 
	 * @param builder
	 */
	FireImmunityCharm(Builder builder) {
		super(builder);
	}

	@Override
	public Class<?> getRegisteredEvent() {
		return REGISTERED_EVENT;
	}

	/**
	 * NOTE: it is assumed that only the allowable events are calling this action.
	 */
	@Override
	public boolean update(World world, Random random, ICoords coords, PlayerEntity player, Event event, final ICharmEntity entity) {
		boolean result = false;
		// exit if not fire damage
		if (!((LivingDamageEvent)event).getSource().isFire()) {
			return result;
		}
		double newAmount = 0F;
		if (entity.getMana() > 0 && player.isAlive()) {
			// get the fire damage amount
			double amount = ((LivingDamageEvent)event).getAmount();

//			if (entity.getMana() >= amount) {
//				entity.setMana(entity.getMana() - amount);
//				event.setCanceled(true); // don't want to continue to other charms and incur durability cost
//			}
//			else {
//				newAmount = (((LivingDamageEvent)event).getAmount() - (float)entity.getMana());
//				entity.setMana(0);
//			}
			double cost = applyCost(world, random, coords, player, event, entity, amount);
			if (cost < amount) {
				newAmount =+ (amount - cost);
			}
			((LivingDamageEvent)event).setAmount((float) newAmount);
			result = true;
		}    		
		return result;
	}

	@Override
	public ITextComponent getCharmDesc(ICharmEntity entity) {
		return new TranslationTextComponent("tooltip.charm.rate.fire_immunity");
	}
	
	public static class Builder extends Charm.Builder {

		public Builder(Integer level) {
			super(ModUtils.asLocation(makeName(FIRE_IMMUNITY_TYPE, level)), FIRE_IMMUNITY_TYPE, level);
		}

		@Override
		public ICharm build() {
			return  new FireImmunityCharm(this);
		}
	}
}