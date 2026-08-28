// ------------------------------------------------------------------------------------------------
//
// ProspectingCraft - Seismic Recorder Gui Container
//
// ------------------------------------------------------------------------------------------------

package gcewing.prospecting;

import static gcewing.prospecting.BaseDataChannel.*;

import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.tileentity.*;
import net.minecraft.world.*;

public class SeismicRecorderContainer extends BaseContainer {

    public static final int

    guiWidth = 176, guiHeight = 176, leftMargin = 16, topMargin = 24, rightMargin = guiWidth - 16,

        inputSlotsY = topMargin, paperSlotX = leftMargin, inkSlotsX = leftMargin + 22,

        activateButtonX = leftMargin + 96, activateButtonY = topMargin - 2, activateButtonWidth = 48,
        activateButtonHeight = 20,

        gunpowderSlotX = rightMargin - 20, gunpowderSlotY = topMargin + 43,

        statusX = leftMargin + 1, status1Y = topMargin + 24, status2Y = status1Y + 16,

        progressBarX = leftMargin, progressBarY = topMargin + 56, progressBarH = 7;

    SeismicRecorderTE te;

    // public static SeismicRecorderContainer create(EntityPlayer player, World world, BlockPos pos) {
    // TileEntity te = BaseBlockUtils.getWorldTileEntity(world, pos);
    // if (te instanceof SeismicRecorderTE)
    // return new SeismicRecorderContainer(player, (SeismicRecorderTE)te);
    // else
    // return null;
    // }

    public SeismicRecorderContainer(EntityPlayer player, SeismicRecorderTE te) {
        super(guiWidth, guiHeight);
        this.te = te;
        addPlayerSlots(player);
        addContainerSlots();
    }

    protected void addContainerSlots() {
        ProspectingCraft mod = ProspectingCraft.mod;
        IInventory inv = te.getInventory();
        beginContainerSlots();
        addSlotToContainer(new ItemSlot(inv, te.paperSlot, paperSlotX, inputSlotsY, Items.paper, 64));
        for (int i = 0; i < 4; i++) {
            Item item = mod.itemInk[i];
            Slot slot = new ItemSlot(inv, te.blackInkSlot + i, inkSlotsX + 18 * i, inputSlotsY, item, 1);
            addSlotToContainer(slot);
        }
        addSlotToContainer(new ItemSlot(inv, te.gunpowderSlot, gunpowderSlotX, gunpowderSlotY, Items.gunpowder, 64));
        endContainerSlots();
    }

    // @Override
    // void sendStateTo(ICrafting crafter) {
    // crafter.sendProgressBarUpdate(this, 0, te.activated ? 1 : 0);
    // crafter.sendProgressBarUpdate(this, 1, te.phase);
    // }
    //
    // @Override
    // public void updateProgressBar(int i, int value) {
    // switch (i) {
    // case 0:
    // te.activated = value != 0;
    // break;
    // case 1:
    // te.phase = value;
    // break;
    // }
    // }

    @ServerMessageHandler("activateSurveyor")
    public void onActivateSurveyor(EntityPlayer player, ChannelInput data) {
        SurveyorActivationServerHandler.INSTANCE.enqueue(player, this);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return hasUsableSurveyor(player);
    }

    boolean tryActivateFor(EntityPlayer player) {
        if (!isValidActivationContext(
            player != null && !player.isDead,
            player != null && player.openContainer == this,
            te != null && te.getWorldObj() != null && !te.getWorldObj().isRemote,
            player != null && te != null && player.worldObj == te.getWorldObj(),
            hasCurrentTileEntity(),
            hasSurveyorBlock(),
            player != null && te != null && SeismicRecorderBlock.canPlayerAccess(te.getOwnerId(), player.getUniqueID()),
            getDistanceSquared(player))) return false;
        return te.tryBeginScan();
    }

    private boolean hasUsableSurveyor(EntityPlayer player) {
        return isValidActivationContext(
            player != null && !player.isDead,
            true,
            te != null && te.getWorldObj() != null,
            player != null && te != null && player.worldObj == te.getWorldObj(),
            hasCurrentTileEntity(),
            hasSurveyorBlock(),
            player != null && te != null && SeismicRecorderBlock.canPlayerAccess(te.getOwnerId(), player.getUniqueID()),
            getDistanceSquared(player));
    }

    private boolean hasCurrentTileEntity() {
        if (te == null || te.getWorldObj() == null || te.isInvalid()) return false;
        return te.getWorldObj()
            .getTileEntity(te.getX(), te.getY(), te.getZ()) == te;
    }

    private boolean hasSurveyorBlock() {
        return te != null && te.getWorldObj() != null
            && te.getWorldObj()
                .getBlock(te.getX(), te.getY(), te.getZ()) instanceof SeismicRecorderBlock;
    }

    private double getDistanceSquared(EntityPlayer player) {
        if (player == null || te == null) return Double.POSITIVE_INFINITY;
        return player.getDistanceSq(te.getX() + 0.5, te.getY() + 0.5, te.getZ() + 0.5);
    }

    static boolean isValidActivationContext(boolean activePlayer, boolean currentContainer, boolean validWorld,
        boolean sameWorld, boolean currentTileEntity, boolean surveyorBlock, boolean ownerMatches,
        double distanceSquared) {
        return activePlayer && currentContainer
            && validWorld
            && sameWorld
            && currentTileEntity
            && surveyorBlock
            && ownerMatches
            && distanceSquared <= 64.0;
    }
}
