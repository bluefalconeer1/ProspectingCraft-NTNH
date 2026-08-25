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
        te.tryBeginScan();
    }
}
