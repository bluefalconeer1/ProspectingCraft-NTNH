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
import net.minecraft.world.*;

public class SeismicRecorderScreen extends BaseGui.Screen {

    static String screenTitle = "Geological Surveyor";
    static String[] statusMessages = { "Ready", "Analyzing...", "Finished", "Out of Paper", "Out of Gunpowder",
        "Out of Black Ink", "Out of Magenta Ink", "Out of Yellow Ink", "Out of Cyan Ink", };

    protected SeismicRecorderTE te;

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
    protected void drawBackgroundLayer() {
        bindTexture(ProspectingCraft.mod.resourceLocation("textures/gui/seismic_recorder.png"), 256, 256);
        drawTexturedRect(0, 0, guiWidth, guiHeight, 0, 0);
        drawColorMode();
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

    protected void drawColorMode() {
        int x = colorModeX + 2;
        int y = colorModeY + 2;
        int u = te.colorMode ? 16 : 0;
        drawTexturedRect(x, y, 12, 12, u, 256 - 32);
    }

    @Override
    public void mousePressed(int x, int y, int button) {
        if (x >= colorModeX && x < colorModeX + 16 && y >= colorModeY && y < colorModeY + 16) {
            ChannelOutput data = ProspectingCraft.channel.openServerContainer("toggleColorMode");
            data.close();
        } else super.mousePressed(x, y, button);
    }

}
