//------------------------------------------------------
//
//   ProspectingCraft - Prospecting Pick Item
//
//------------------------------------------------------

package gcewing.prospecting;

import net.minecraft.block.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;

public class ProspectingPickItem extends BaseItem {

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
        int x, int y, int z, int side, float hitX, float hitY, float hitZ)
    {
        Block block = world.getBlock(x, y, z);
        if (block == Blocks.stone || block == Blocks.gravel) {
            if (!world.isRemote) {
                world.setBlock(x, y, z, Blocks.air);
                ItemStack sampleStack = new ItemStack(ProspectingCraft.itemGeologicalSample, 1);
                NBTTagCompound nbt = new NBTTagCompound();
                nbt.setInteger("xCoord", x);
                nbt.setInteger("yCoord", y);
                nbt.setInteger("zCoord", z);
                sampleStack.setTagCompound(nbt);
                BaseBlockUtils.spawnItemStackAsEntity(world, new BlockPos(x, y, z), sampleStack);
            }
            return true;
        }
        else
            return false;
    }
    
}
