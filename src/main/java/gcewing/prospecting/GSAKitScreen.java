// ------------------------------------------------------------------------------------------------
//
// ProspectingCraft - Sample Analyser Gui Screen
//
// ------------------------------------------------------------------------------------------------

package gcewing.prospecting;

import static gcewing.prospecting.BaseDataChannel.*;
import static gcewing.prospecting.BaseGui.*;
import static gcewing.prospecting.BaseGuiButtons.*;
import static gcewing.prospecting.GSAKitContainer.*;
import static gcewing.prospecting.GSAKitTE.*;
import static org.lwjgl.opengl.GL11.*;

import java.util.*;

import net.minecraft.client.gui.*;
import net.minecraft.entity.player.*;
import net.minecraft.tileentity.*;
import net.minecraft.world.*;

public class GSAKitScreen extends BaseGui.Screen {

    static String screenTitle = "Sample Analysis Kit";

    protected GSAKitContainer container;
    protected GSAKitTE te;
    protected Button addToBookButton;

    public GSAKitScreen(GSAKitContainer cont) {
        super(cont, guiWidth, guiHeight);
        this.container = cont;
        this.te = cont.te;
        addToBookButton = new Button(12, 12, "+", action(this, "addToBook"));
        root.add(194, 25, addToBookButton);
    }

    @Override
    public void updateScreen() {
        // System.out.printf("GSAKitScreen.updateScreen\n");
        super.updateScreen();
        addToBookButton.setEnabled(te.hasStackInSlot(bookSlot));
        if (te.results == null) container.message = null;
    }

    @Override
    protected void drawBackgroundLayer() {
        bindTexture(ProspectingCraft.mod.resourceLocation("textures/gui/gsa_kit.png"), 256, 256);
        drawTexturedRect(0, 0, guiWidth, guiHeight, 0, 0);
        drawProgress();
        drawResults();
        drawMessage();
        setTextColor(0x004c66);
        drawString(screenTitle, 8, 8);
    }

    protected void drawProgress() {
        if (te.state == State.ANALYSING) {
            if (te.yMax > te.yMin) {
                double progress = (double) (te.yCurrent - te.yMin) / (double) (te.yMax - te.yMin);
                double h = progress * 32;
                drawTexturedRect(144, 11 + 32 - h, 16, h, 240, 32 - h);
            }
        }
    }

    protected void drawResults() {
        int x = 105;
        int y = 47;
        setTextColor(0x0000ff);
        List<String> lines = formatResults();
        for (String line : lines) {
            drawString(line, x, y);
            y += 10;
        }
    }

    protected String getResultHeading() {
        BlockPos pos = te.samplePos;
        if (pos != null) return String.format("X:%s Y:%s Z:%s", pos.getX(), pos.getY(), pos.getZ());
        else return "";
    }

    protected List<String> formatResults() {
        List<Bucket> results = te.results;
        List<String> lines = new ArrayList<>();
        lines.add(getResultHeading());
        if (results != null) {
            int n = 0;
            for (Bucket b : results) {
                if (n++ >= 12) break;
                double percent = 100.0 * b.weight; // / te.totalWeight;
                String spc;
                if (percent >= 0.01) spc = String.format("%.2g%%", percent);
                else if (percent >= 0.0001)
                    // spc = "<0.01%";
                    spc = String.format("%.2gppm", percent * 10000);
                else spc = "<1ppm";
                String line = String.format("%s %s", spc, BaseUtils.translateToLocal(b.name));
                lines.add(line);
            }
        }
        return lines;
    }

    protected void drawMessage() {
        String text = container.message;
        if (text != null) {
            glPushMatrix();
            glTranslatef(105, 177, 0);
            glScalef(0.5f, 0.5f, 1);
            setTextColor(0x0000ff);
            drawString(text, 0, 0);
            glPopMatrix();
        }
    }

    public void addToBook() {
        List<String> lines = formatResults();
        String text = BaseStringUtils.join("\n", lines);
        // System.out.printf("GSAKitScreen.addToBook: text = %s\n", text);
        ChannelOutput data = ProspectingCraft.channel.openServerContainer("addToBook");
        data.writeUTF(text);
        data.close();
    }

}
