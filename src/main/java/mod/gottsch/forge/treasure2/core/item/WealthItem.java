/*
 * This file is part of  Treasure2.
 * Copyright (c) 2022 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.treasure2.core.item;


import java.util.List;

import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.treasure2.core.config.Config;
import mod.gottsch.forge.treasure2.core.tags.TreasureTags;
import mod.gottsch.forge.treasure2.core.util.LangUtil;
import mod.gottsch.forge.treasure2.core.wishable.TreasureWishableHandlers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;


/**
 * @author Mark Gottschling on Aug 17, 2021
 *
 */
public class WealthItem extends Item {

	/**
	 * 
	 * @param properties
	 */
	public WealthItem(Properties properties) {
		super(properties.stacksTo(Config.SERVER.wealth.wealthMaxStackSize.get()));
	}

	/**
	 * 
	 */
	@Override
	public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
		// standard coin info
		tooltip.add(new TranslatableComponent(LangUtil.tooltip("wishable")).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
	}

	/**
	 * 
	 */
	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {
		Level level = entityItem.level;
		if (WorldInfo.isClientSide(level)) {
			return super.onEntityItemUpdate(stack, entityItem);
		}

		// check if registered
		if (!stack.is(TreasureTags.Items.WISHABLES)) {
			return super.onEntityItemUpdate(stack, entityItem);
		}

		if (TreasureWishableHandlers.DEFAULT_WISHABLE_HANDLER.isValidLocation(entityItem)) {
			TreasureWishableHandlers.DEFAULT_WISHABLE_HANDLER.doWishable(entityItem);
			return true;
		}
		return super.onEntityItemUpdate(stack, entityItem);
	}
}
