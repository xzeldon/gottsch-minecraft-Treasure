/*
 * This file is part of  Treasure2.
 * Copyright (c) 2020 Mark Gottschling (gottsch)
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

import mod.gottsch.forge.treasure2.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 
 * @author Mark Gottschling on Jul 5, 2020
 *
 */
public class EmberLock extends LockItem {

	/**
	 * 
	 * @param properties
	 * @param keys
	 */
	public EmberLock(Item.Properties properties, KeyItem[] keys) {
		super(properties, keys);
	}
	
	/**
	 * 
	 * @param modID
	 * @param name
	 * @param keys
	 */
//	@Deprecated
//	public EmberLock(String modID, String name, Item.Properties properties, KeyItem[] keys) {
//		super(modID, name, properties, keys);
//	}

	/**
	 * Format: (Additions)
	 * 
	 * Specials: [text] [color=gold]
	 */
	@Override
	public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
	}

	@Override
	public  void appendHoverSpecials(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flag) {
		TranslatableComponent specials = new TranslatableComponent(LangUtil.tooltip("key_lock.ember_lock.specials"));
		for (String s : specials.getString().split("~")) {	
			tooltip.add(new TranslatableComponent(LangUtil.INDENT2)
					.append(new TextComponent(s).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)));
		}
	}
}