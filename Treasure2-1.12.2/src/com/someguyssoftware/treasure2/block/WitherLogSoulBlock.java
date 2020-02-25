/**
 * 
 */
package com.someguyssoftware.treasure2.block;

import java.util.Random;

import com.someguyssoftware.gottschcore.block.CardinalDirectionFacadeBlock;
import com.someguyssoftware.gottschcore.positional.Coords;
import com.someguyssoftware.gottschcore.random.RandomHelper;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.config.TreasureConfig;
import com.someguyssoftware.treasure2.item.TreasureItems;
import com.someguyssoftware.treasure2.particle.AbstractMistParticle;
import com.someguyssoftware.treasure2.particle.PoisonMistParticle;
import com.someguyssoftware.treasure2.particle.WitherMistParticle;
import com.someguyssoftware.treasure2.tileentity.MistEmitterTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author Mark Gottschling on Jun 6, 2018
 *
 */
public class WitherLogSoulBlock extends CardinalDirectionFacadeBlock
		implements IFogSupport, ITreasureBlock, ITileEntityProvider {

	public static final PropertyEnum<Appearance> APPEARANCE = PropertyEnum.create("appearance", Appearance.class);

	/*
	 * An array of AxisAlignedBB bounds for the bounding box
	 */
	AxisAlignedBB[] bounds = new AxisAlignedBB[4];

	/**
	 * 
	 * @param modID
	 * @param name
	 */
	public WitherLogSoulBlock(String modID, String name) {
		super(modID, name, Material.WOOD);
		setSoundType(SoundType.WOOD);
		setCreativeTab(Treasure.TREASURE_TAB);
		setHardness(3.0F);
		this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
				.withProperty(APPEARANCE, Appearance.NONE));
	}

	/**
	 * 
	 */
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] { FACING, APPEARANCE });
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new MistEmitterTileEntity();
	}

	/**
	 * 
	 */
	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		// check the 4x4x4 area and set all fog blocks to CHECK_DECAY = true
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		// if all the blocks in the immediate area are loaded
		if (worldIn.isAreaLoaded(new BlockPos(x - 5, y - 5, z - 5), new BlockPos(x + 5, y + 5, z + 5))) {
			// use a MutatableBlockPos instead of Cube\Coords or BlockPos to say the recreation of many objects
			BlockPos.MutableBlockPos mbp = new BlockPos.MutableBlockPos();

			for (int x1 = -5; x1 <= 5; ++x1) {
				for (int y1 = -5; y1 <= 5; ++y1) {
					for (int z1 = -5; z1 <= 5; ++z1) {
						// that just checks a value.
						IBlockState inspectBlockState = worldIn.getBlockState(mbp.setPos(x + x1, y + y1, z + z1));
						if (inspectBlockState.getMaterial() == TreasureItems.FOG) {
							worldIn.setBlockState(mbp, inspectBlockState.withProperty(FogBlock.CHECK_DECAY, true));
						}
					}
				}
			}
		}
		super.breakBlock(worldIn, pos, state);
	}

	/**
	 * NOTE randomDisplayTick is on the client side only. The server is not keeping track of any
	 * particles NOTE cannot control the number of ticks per randomDisplayTick() call - it is not
	 * controlled by tickRate()
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
		if (WorldInfo.isServerSide(world)) {
			return;
		}

		if (!TreasureConfig.WORLD_GEN.getGeneralProperties().enablePoisonFog) {
			return;
		}
		// get the appearance property
		Appearance appearance = state.getValue(APPEARANCE);

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		int numberOfTorches = 0;
		// if all the blocks in the immediate area are loaded
		if (world.isAreaLoaded(new BlockPos(x - 3, y - 3, z - 3), new BlockPos(x + 3, y + 3, z + 3))) {
			// use a MutatableBlockPos instead of Cube\Coords or BlockPos to say the recreation of many objects
			BlockPos.MutableBlockPos mbp = new BlockPos.MutableBlockPos();

			// change the randomness of particle creation
			// o torches = 100%
			// 1 torch = 50%
			// 2 torches = 25%
			// 3 torches = 0%
			for (int x1 = -3; x1 <= 3; ++x1) {
				for (int y1 = -3; y1 <= 3; ++y1) {
					for (int z1 = -3; z1 <= 3; ++z1) {
						// that just checks a value.
						IBlockState inspectBlockState = world.getBlockState(mbp.setPos(x + x1, y + y1, z + z1));
						Block inspectBlock = inspectBlockState.getBlock();

						// if the block is a torch, then destroy the fog and return
						if (inspectBlock instanceof BlockTorch) {
							numberOfTorches++;
						}
						if (numberOfTorches >= 3) {
							x1 = 99;
							y1 = 99;
							z1 = 99;
							break;
						}
					}
				}
			}
		}

		boolean isCreateParticle = true;
		if (numberOfTorches == 1) {
			isCreateParticle = RandomHelper.checkProbability(random, 50);
		} else if (numberOfTorches == 2) {
			isCreateParticle = RandomHelper.checkProbability(random, 25);
		} else if (numberOfTorches > 2) {
			isCreateParticle = false;
		}

		if (!isCreateParticle) {
			return;
		}

		// TODO check appearance - if face block, then particles need to be generated on ground and further
		// away (since main tree is 2x2
		// appearance affects how wide to spawn the mist (because the main tree is 2x2, and the soul log is
		// higher)
		// (this is hard-coded knowledge - bad)
		double xPos = 0;
		double yPos = y;
		double zPos = 0;

		if (appearance == Appearance.FACE) {
			// initial positions - has a spread area of up to 2.5 blocks radius
			float xOffset = (random.nextFloat() * 5.0F) - 2.5F;
			if (xOffset >= 0.0F) {
				xOffset = Math.max(1.5F, xOffset);
			} else {
				xOffset = Math.min(-0.5F, xOffset);
			}
			xPos = (x + 0.5F) + xOffset;

			// + state.getBoundingBox(world, pos).maxY; // + 1.0;
			float zOffset = (random.nextFloat() * 5.0F) - 2.5F;
			if (zOffset > 0.0F) {
				zOffset = Math.max(0.5F, zOffset);
			} else {
				zOffset = Math.min(-1.5F, zOffset);
			}
			zPos = (z + 0.5F) + zOffset;

			// y is 2 blocks down as the face it up the trunk
			yPos = y - 2.0F;
		} else {
			// initial positions - has a spread area of up to 1.5 blocks
			xPos = (x + 0.5F) + (random.nextFloat() * 3.0F) - 1.5F;
			// + state.getBoundingBox(world, pos).maxY; // + 1.0;
			zPos = (z + 0.5F) + (random.nextFloat() * 3.0F) - 1.5F;
		}

		// initial velocities
		double velocityX = 0;
		double velocityY = 0;
		double velocityZ = 0;

		AbstractMistParticle mistParticle = null;
		if (appearance == Appearance.FACE) {
			mistParticle = new WitherMistParticle(world, xPos, yPos, zPos, velocityX, velocityY, velocityZ,
					new Coords(pos));
		} else {
			mistParticle = new PoisonMistParticle(world, xPos, yPos, zPos, velocityX, velocityY, velocityZ,
					new Coords(pos));
		}
		// remember to init!
		mistParticle.init();
		Minecraft.getMinecraft().effectRenderer.addEffect(mistParticle);

		// TODO add wil-o-the-wisp particles
	}

	/**
	 * 
	 */
	public boolean canSustainFog(IBlockState state, IBlockAccess world, BlockPos pos) {
		return true;
	}

	/**
	 * Convert the given metadata into a BlockState for this Block
	 */
	@Override
	public IBlockState getStateFromMeta(int meta) {
		IBlockState state = super.getStateFromMeta(meta & 0x07);
		state = state.withProperty(APPEARANCE, (meta & 0x08) == 0 ? Appearance.NONE : Appearance.FACE);
		return state;
	}

	/**
	 * Convert the BlockState into the correct metadata value
	 */
	@Override
	public int getMetaFromState(IBlockState state) {
		int facingBits = 0;
		facingBits = state.getValue(FACING).getIndex();
		facingBits = (facingBits == -1) ? 2 : facingBits;

		int appearanceBits = state.getValue(APPEARANCE).getValue() == 0 ? 0 : 8;
		return facingBits + appearanceBits;
	}

	/**
	 * Drops WitherRootItem or a stick.
	 */
	@Override
	public Item getItemDropped(IBlockState state, Random random, int fortune) {
		if (RandomHelper.checkProbability(random, TreasureConfig.WITHER_TREE.witherRootItemGenProbability)) {
			return TreasureItems.WITHER_ROOT_ITEM;
		}
		return Items.STICK;
	}

	/**
	 * @return the bounds
	 */
	public AxisAlignedBB[] getBounds() {
		return bounds;
	}

	/**
	 * @param bounds the bounds to set
	 */
	public void setBounds(AxisAlignedBB[] bounds) {
		this.bounds = bounds;
	}

	/**
	 * 
	 * @author Mark Gottschling on Jun 7, 2018
	 *
	 */
	public static enum Appearance implements IStringSerializable {
		NONE("none", 0), FACE("face", 1);

		private final String name;
		private final int value;

		private Appearance(String name, int value) {
			this.name = name;
			this.value = value;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the value
		 */
		public int getValue() {
			return value;
		}
	}
}
