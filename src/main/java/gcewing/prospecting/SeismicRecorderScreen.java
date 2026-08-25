// ------------------------------------------------------------------------------------------------
//
// ProspectingCraft - Seismic Recorder Gui Screen
//
// ------------------------------------------------------------------------------------------------

package gcewing.prospecting;

import static gcewing.prospecting.BaseDataChannel.*;
import static gcewing.prospecting.SeismicRecorderContainer.*;

import net.minecraft.client.gui.*;
import net.minecraft.entity.player.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.StatCollector;
import net.minecraft.world.*;

public class SeismicRecorderScreen extends BaseGui.Screen {

    private static final int ACTIVATE_BUTTON_ID = 0;
    static String screenTitle = "Geological Surveyor";

    protected SeismicRecorderTE te;
    protected GuiButton activateButton;

    // public static SeismicRecorderScreen create(EntityPlayer player, World world, BlockPos pos) {
    // TileEntity te = BaseBlockUtils.getWorldTileEntity(world, pos);
    // if (te instanceof SeismicRecorderTE)
    // return new SeismicRecorderScreen(player, (SeismicRecorderTE)te);
    // else
    // return null;
    // }

    // public SeismicRecorderScreen(EntityPlayer player, SeismicRecorderTE te) {
    // super(new SeismicRecorderContainer(player, te), guiWidth, guiHeight);
    // this.te = te;
    // }

    public SeismicRecorderScreen(SeismicRecorderContainer cont) {
        super(cont, guiWidth, guiHeight);
        this.te = cont.te;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        activateButton = new GuiButton(
            ACTIVATE_BUTTON_ID,
            guiLeft + activateButtonX,
            guiTop + activateButtonY,
            activateButtonWidth,
            activateButtonHeight,
            StatCollector.translateToLocal("prospectingcraft.surveyor.activate"));
        buttonList.add(activateButton);
        updateActivateButton();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateActivateButton();
    }

    @Override
    protected void drawBackgroundLayer() {
        bindTexture(ProspectingCraft.mod.resourceLocation("textures/gui/seismic_recorder.png"), 256, 256);
        drawTexturedRect(0, 0, guiWidth, guiHeight, 0, 0);
        drawProgress();
        int cx = xSize / 2;
        setTextColor(0x004c66);
        drawCenteredString(screenTitle, cx, 8);
        drawStatus();
    }

    protected void drawStatus() {
        SeismicRecorderTE.ReadyStatus status = te.getReadyStatus();
        if (status.error) setTextColor(0xffff00);
        else setTextColor(0x00ff00);
        drawString(status.message, statusX, status2Y);
    }

    protected void drawProgress() {
        if (te.activated) {
            int i = te.phase / 8;
            int w = 7 * i;
            drawTexturedRect(progressBarX, progressBarY, w, progressBarH, 0, 256 - 7);
        }
    }

    protected void updateActivateButton() {
        if (activateButton != null) activateButton.enabled = te.getReadyStatus() == SeismicRecorderTE.ReadyStatus.RDY;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ACTIVATE_BUTTON_ID && button.enabled) {
            ChannelOutput data = ProspectingCraft.channel.openServerContainer("activateSurveyor");
            data.close();
        }
    }

}
