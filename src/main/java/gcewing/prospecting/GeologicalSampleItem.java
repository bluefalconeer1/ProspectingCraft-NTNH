// ------------------------------------------------------
//
// ProspectingCraft Geological Sample Item
//
// ------------------------------------------------------

package gcewing.prospecting;

import java.util.*;

import net.minecraft.block.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;

import cpw.mods.fml.relauncher.*;

public class GeologicalSampleItem extends Item {

    public static BlockPos getSamplePosition(ItemStack stack) {
        if (stack.getItem() instanceof GeologicalSampleItem) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt != null) {
                int x = nbt.getInteger("xCoord");
                int y = nbt.getInteger("yCoord");
                int z = nbt.getInteger("zCoord");
                return new BlockPos(x, y, z);
            }
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List info, boolean advanced) {
        BlockPos pos = getSamplePosition(stack);
        if (pos != null) info.add(String.format("From X=%s Y=%s Z=%s", pos.x, pos.y, pos.z));
    }

}
