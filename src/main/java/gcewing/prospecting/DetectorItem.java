// ------------------------------------------------------------------
//
// ProspectingCraft - Base class for handhend detector items
//
// ------------------------------------------------------------------

package gcewing.prospecting;

import static gcewing.prospecting.BaseBlockUtils.*;
import static gcewing.prospecting.BaseUtils.*;
import static java.lang.Math.*;

import java.util.*;

import net.minecraft.block.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraftforge.oredict.OreDictionary;

public class DetectorItem extends BaseItem {

    protected static int scanRangeH = 16;
    protected static int scanRangeV = 64;

    public static boolean debug = false;

    protected String[] targetOreNames;
    protected int[] targetOreIDs;

    public DetectorItem(String... targetOreNames) {
        targetOreIDs = new int[targetOreNames.length];
        for (int i = 0; i < targetOreIDs.length; i++) targetOreIDs[i] = OreDictionary.getOreID(targetOreNames[i]);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            BlockPos pos = new BlockPos(ifloor(player.posX), ifloor(player.posY), ifloor(player.posZ));
            String report = scanRegion(world, pos);
            addChatMessage(player, report);
        }
        return stack;
    }

    protected String scanRegion(World world, BlockPos center) {
        int rh = scanRangeH;
        int rv = scanRangeV;
        int xc = center.getX();
        int yc = center.getY();
        int zc = center.getZ();
        int y0 = max(0, yc - rv);
        int y1 = min(255, yc + rv);
        double weight = 0;
        double totalWeight = 0;
        for (int i = -rh; i <= rh; i++) for (int j = -rh; j <= rh; j++) {
            int x = xc + i;
            int z = zc + j;
            for (int y = y0; y <= y1; y++) {
                int k = y - yc;
                if ((i | j | k) != 0) {
                    double w = 1.0 / (i * i + j * j + k * k);
                    totalWeight += w;
                    BlockPos pos = new BlockPos(x, y, z);
                    IBlockState state = getWorldBlockState(world, pos);
                    ItemStack stack = newBlockStack(state);
                    if (isTarget(stack)) weight += w;
                }
            }
        }
        String name = translateToLocal(getUnlocalizedName());
        String report = String.format("%s reads %.2f", name, 1000 * weight / totalWeight);
        return report;
    }

    protected boolean isTarget(ItemStack stack) {
        int[] ids = OreDictionary.getOreIDs(stack);
        for (int id : ids) for (int i = 0; i < targetOreIDs.length; i++) if (id == targetOreIDs[i]) return true;
        return false;
    }

}
