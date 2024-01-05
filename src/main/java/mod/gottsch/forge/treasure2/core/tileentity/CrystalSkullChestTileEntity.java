package mod.gottsch.forge.treasure2.core.tileentity;

import mod.gottsch.forge.treasure2.core.chest.ChestSlotCount;
import mod.gottsch.forge.treasure2.core.inventory.SkullChestContainer;
import mod.gottsch.forge.treasure2.core.inventory.TreasureContainers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * 
 * @author Mark Gottschling on Dec 9, 2020
 *
 */
public class CrystalSkullChestTileEntity extends AbstractTreasureChestTileEntity {
	
	/**
	 * 
	 * @param texture
	 */
	public CrystalSkullChestTileEntity() {
		super(TreasureTileEntities.CRYSTAL_SKULL_CHEST_TILE_ENTITY_TYPE);
		setCustomName(new TranslationTextComponent("display.crystal_skull_chest.name"));
	}
	
	/**
	 * 
	 * @param windowID
	 * @param inventory
	 * @param player
	 * @return
	 */
	public Container createServerContainer(int windowID, PlayerInventory inventory, PlayerEntity player) {
		return new SkullChestContainer(windowID, TreasureContainers.SKULL_CHEST_CONTAINER_TYPE, inventory, this);
	}
	
	/**
	 * @return the numberOfSlots
	 */
	@Override
	public int getNumberOfSlots() {
		return ChestSlotCount.SKULL.getSize();
	}
}