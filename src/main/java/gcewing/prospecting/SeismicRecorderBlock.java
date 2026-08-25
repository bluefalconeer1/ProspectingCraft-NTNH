//------------------------------------------------------------------------------------------------
//
//   ProspectingCraft - Seismic Recorder block
//
//------------------------------------------------------------------------------------------------

package gcewing.prospecting;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.world.*;

// import gcewing.prospecting.BaseMod.ModelSpec;

public class SeismicRecorderBlock extends BaseBlock<SeismicRecorderTE> {

    public SeismicRecorderBlock() {
        super(Material.wood, BaseOrientation.orient4WaysByState, SeismicRecorderTE.class);
        String tb = "seismic_recorder_bottom";
        String tt = "seismic_recorder_top";
        String tf = "seismic_recorder_front";
        String ts = "seismic_recorder_side";
        setModelAndTextures("cube.smeg", tb, tt, tf, ts);
    }
    
    @Override
    public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block block) {
        //System.out.printf("SeismicRecorderBlock.onNeighborBlockChange\n");
        if (!world.isRemote) {
            boolean signal = world.isBlockIndirectlyGettingPowered(pos.x, pos.y, pos.z);
            SeismicRecorderTE te = getTileEntity(world, pos);
            if (te != null)
                te.inputSignal(signal);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
        EnumFacing side, float cx, float cy, float cz)
    {
        ProspectingCraft.mod.openGui(player, ProspectingCraft.seismicRecorderGui, world, pos);
        return true;
    }

}
