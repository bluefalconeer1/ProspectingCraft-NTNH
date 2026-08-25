//------------------------------------------------------
//
//   ProspectingCraft - Seismic Survey Item
//
//------------------------------------------------------

package gcewing.prospecting;

import java.util.*;

import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.storage.MapData;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S34PacketMaps;

public class SeismicSurveyItem extends ItemMap {
    
    @Override
    public void updateMapData(World world, Entity entity, MapData data) {
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List info, boolean advanced) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null) {
            int cx = nbt.getInteger("xCenter");
            int cz = nbt.getInteger("zCenter");
            info.add(String.format("X: %s to %s", cx - 64, cx + 63));
            info.add(String.format("Z: %s to %s", cz - 64, cz + 63));
        }
    }

}
