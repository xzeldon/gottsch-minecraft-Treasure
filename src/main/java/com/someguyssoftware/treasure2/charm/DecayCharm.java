/**
 * 
 */
package com.someguyssoftware.treasure2.charm;

import java.util.List;
import java.util.Random;

import com.someguyssoftware.gottschcore.positional.ICoords;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 
 * @author Mark Gottschling on May 4, 2020
 *
 */
public class DecayCharm extends Charm {
	public static String DECAY_TYPE = "decay";
	private static float DECAY_RATE = 2F;
	private static final Class<?> REGISTERED_EVENT = LivingUpdateEvent.class;

	/**
	 * 
	 * @param builder
	 */
	DecayCharm(Builder builder) {
		super(builder);
	}

	@Override
	public boolean isCurse() {
		return true;
	}
	
	@Override
	public Class<?> getRegisteredEvent() {
		return REGISTERED_EVENT;
	}

	/**
	 * NOTE: it is assumed that only the allowable events are calling this action.
	 */
	@Override
	public boolean update(World world, Random random, ICoords coords, EntityPlayer player, Event event, final ICharmEntity entity) {
		boolean result = false;
		//		Treasure.logger.debug("in decay");
		if (world.getTotalWorldTime() % 100 == 0) {
			if (!player.isDead && entity.getValue() > 0 && player.getHealth() > 0.0) {
				//			Treasure.logger.debug("player is alive and charm is good still...");
				player.setHealth(MathHelper.clamp(player.getHealth() - 2.0F, 0.0F, player.getMaxHealth()));				
				entity.setValue(MathHelper.clamp(entity.getValue() - 1.0,  0D, entity.getValue()));
				//				Treasure.logger.debug("new data -> {}", data);
				result = true;
			}

		}
		return result;
	}

	/**
	 * 
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag, ICharmEntity entity) {
		TextFormatting color = TextFormatting.DARK_RED;
		tooltip.add("  " + color + getLabel(entity));
		tooltip.add(" " + TextFormatting.GRAY +  "" + TextFormatting.ITALIC + I18n.translateToLocalFormatted("tooltip.charm.decay_rate"));
	}
}
