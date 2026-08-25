//------------------------------------------------------
//
//   ProspectingCraft - Prospector's Eyeglass Item
//
//------------------------------------------------------

package gcewing.prospecting;

import java.util.*;

import net.minecraft.block.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;

import static gcewing.prospecting.GSAKitTE.*;

public class EyeglassItem extends BaseItem {

    public static boolean debug = false;

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
        int x, int y, int z, int side, float hitX, float hitY, float hitZ)
    {
        Block block = world.getBlock(x, y, z);
        if (block == Blocks.stone || block == Blocks.gravel) {
            if (!world.isRemote) {
                BlockPos pos = new BlockPos(x, y, z);
                List<Bucket> results = GSAKitTE.scanRegion(world, pos);
                if (debug) {
                    System.out.printf("%s:\n", pos);
                    for (Bucket b : results)
                        System.out.printf("%.5f %s\n", 100 * b.weight, b.name);
                }
                reportResult(player, pos, results);
            }
            return true;
        }
        else
            return false;
    }
    
    protected void reportResult(EntityPlayer player, BlockPos pos, List<Bucket> results) {
        String report = "Nothing obvious";
        if (results.size() > 0) {
            Bucket b = results.get(0);
            double pc = 100 * b.weight;
            if (pc >= 0.1) {
                if (pc >= 3.0)
                    report = "Strong signs";
                else if (pc >= 0.5)
                    report = "Signs";
                else
                    report = "Faint signs";
                report = report + " of " + BaseUtils.translateToLocal(b.name);
            }
        }
        report = String.format("%s at X=%s, Y=%s, Z=%s", report, pos.getX(), pos.getY(), pos.getZ());
        if (debug)
            System.out.printf("%s\n", report);
        BaseUtils.addChatMessage(player, report);
    }
    
}
