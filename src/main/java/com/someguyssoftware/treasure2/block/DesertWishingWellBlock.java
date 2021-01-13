/**
 * 
 */
package com.someguyssoftware.treasure2.block;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.someguyssoftware.gottschcore.block.ModBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;

/**
 * @author Mark Gottschling
 *
 */
public class DesertWishingWellBlock extends ModBlock implements IWishingWellBlock {
	// logger
	public static Logger logger = LogManager.getLogger(DesertWishingWellBlock.class);
    
    VoxelShape shape = Block.makeCuboidShape(0, 0, 0, 16, 16, 16);

	/**
	 * 
	 * @param material
	 */
	public DesertWishingWellBlock(String modID, String name, Block.Properties properties) {
		super(modID, name, properties);
	}

    /**
     * 
     * @param state
     * @param worldIn
     * @param pos
     * @param context
     * @return
     */
    @Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return shape;
    }
}