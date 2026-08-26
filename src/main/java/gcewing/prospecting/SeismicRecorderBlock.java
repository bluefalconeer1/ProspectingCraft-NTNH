// ------------------------------------------------------------------------------------------------
//
// ProspectingCraft - Seismic Recorder block
//
// ------------------------------------------------------------------------------------------------

package gcewing.prospecting;

import java.util.UUID;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.world.*;

import gcewing.prospecting.survey.SurveyTier;

// import gcewing.prospecting.BaseMod.ModelSpec;

public class SeismicRecorderBlock extends BaseBlock<SeismicRecorderTE> {

    private final SurveyTier surveyTier;

    public SeismicRecorderBlock() {
        this(SurveyTier.BASE);
    }

    public SeismicRecorderBlock(SurveyTier surveyTier) {
        super(Material.wood, BaseOrientation.orient4WaysByState, SeismicRecorderTE.class);
        if (surveyTier == null) throw new IllegalArgumentException("surveyTier must not be null");
        this.surveyTier = surveyTier;
        String tb = "seismic_recorder_bottom";
        String suffix = getTextureSuffix(surveyTier);
        String tt = "seismic_recorder_top" + suffix;
        String tf = "seismic_recorder_front" + suffix;
        String ts = "seismic_recorder_side" + suffix;
        setModelAndTextures("cube.smeg", tb, tt, tf, ts);
    }

    static String getTextureSuffix(SurveyTier surveyTier) {
        if (surveyTier == SurveyTier.STEEL) return "_steel";
        if (surveyTier == SurveyTier.DESH) return "_desh";
        return "";
    }

    public SurveyTier getSurveyTier() {
        return surveyTier;
    }

    @Override
    public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block block) {
        // System.out.printf("SeismicRecorderBlock.onNeighborBlockChange\n");
        if (!world.isRemote) {
            boolean signal = world.isBlockIndirectlyGettingPowered(pos.x, pos.y, pos.z);
            SeismicRecorderTE te = getTileEntity(world, pos);
            if (te != null) te.inputSignal(signal);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side,
        float cx, float cy, float cz) {
        if (world.isRemote) return true;
        SeismicRecorderTE te = getTileEntity(world, pos);
        if (te == null) return true;
        UUID playerId = player.getUniqueID();
        if (te.getOwnerId() == null) te.setOwnerId(playerId);
        if (!canPlayerAccess(te.getOwnerId(), playerId)) {
            player.addChatComponentMessage(new ChatComponentTranslation("prospectingcraft.surveyor.not_owner"));
            return true;
        }
        ProspectingCraft.mod.openGui(player, ProspectingCraft.seismicRecorderGui, world, pos);
        return true;
    }

    static boolean canPlayerAccess(UUID ownerId, UUID playerId) {
        return ownerId == null || ownerId.equals(playerId);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity,
        ItemStack stack) {
        if (!world.isRemote && entity instanceof EntityPlayer) {
            SeismicRecorderTE te = getTileEntity(world, pos);
            if (te != null) te.setOwnerId(entity.getUniqueID());
        }
    }

}
