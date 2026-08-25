// ------------------------------------------------------------------------------------------------
//
// Greg's Mod Base - GUI Button Widget
//
// ------------------------------------------------------------------------------------------------

package gcewing.prospecting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import gcewing.prospecting.BaseGui.*;

public class BaseGuiButtons {

    protected static int nextId = 1;

    public static class Button extends Widget {

        protected GuiButton base;
        protected IWidgetContainer parent;
        public int left, top;
        public Action action;

        protected static final ResourceLocation buttonTextures = new ResourceLocation("textures/gui/widgets.png");

        public Button(int width, int height, String title, Action action) {
            super(width, height);
            this.base = new GuiButton(nextId++, 0, 0, width, height, title);
            this.action = action;
        }

        public boolean getEnabled() {
            return base.enabled;
        }

        public void setEnabled(boolean state) {
            base.enabled = state;
        }

        public boolean getVissble() {
            return base.visible;
        }

        public void setVisible(boolean state) {
            base.visible = state;
        }

        @Override
        public void draw(Screen scr, int mouseX, int mouseY) {
            // base.drawButton(Minecraft.getMinecraft(), mouseX, mouseY);
            scr.gSave();
            scr.bindTexture(buttonTextures, 256, 256);
            boolean hovering = mouseX >= 0 && mouseY >= 0 && mouseX < width && mouseY < height;
            int k = base.getHoverState(hovering);
            scr.drawBorderedRect(0, 0, width, height, 1, 47 + k * 20, 198, 18, 2, 2);
            if (!base.enabled) scr.setTextColor(10526880);
            else if (hovering) scr.setTextColor(16777120);
            else scr.setTextColor(14737632);
            scr.drawCenteredString(base.displayString, width / 2, (height - 8) / 2);
            scr.gRestore();
        }

        @Override
        public void mousePressed(MouseCoords m, int button) {
            if (base.mousePressed(Minecraft.getMinecraft(), m.x, m.y)) {
                if (action != null) action.perform();
            }
        }

        // @Override
        // public void mouseReleased(MouseCoords m, int button) {
        // if (action != null)
        // action.perform();
        // }

    }

}
