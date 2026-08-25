//------------------------------------------------------
//
//   ProspectingCraft - Seismic Survey Renderer
//
//------------------------------------------------------

package gcewing.prospecting;

import static org.lwjgl.opengl.GL11.*;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.IItemRenderer;
import net.minecraft.client.Minecraft;

public class SeismicSurveyRenderer implements IItemRenderer {

    protected static int textColor = 0xffffff;

    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.FIRST_PERSON_MAP;
    }
    
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }
    
    /*
     * Data Parameters:
     * EntityPlayer player - The player holding the map
     * RenderEngine engine - The RenderEngine instance
     * MapData mapData - The map data
     */

    public void renderItem(ItemRenderType type, ItemStack stack, Object... args) {
//         System.out.printf("SeismicSurveyRenderer.renderItem\n");
        MapData data = (MapData)args[2];
        Minecraft mc = Minecraft.getMinecraft();
        MapItemRenderer stdMapRenderer = mc.entityRenderer.getMapItemRenderer();
        if (data != null)
            stdMapRenderer.func_148250_a(data, false);
        else
            System.out.printf("ProspectingCraft: SeismicSurveyRenderer.renderItem: map data is null\n");
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null) {
            int x0 = nbt.getInteger("xCenter");
            int z0 = nbt.getInteger("zCenter");
            String text1 = "Geological Survey - Seismic";
            String text2 = String.format("X: %s to %s   Z: %s to %s", x0 - 64, x0 + 63, z0 - 64, z0 + 63);
            FontRenderer fr = mc.fontRenderer;
            glPushMatrix();
            glTranslatef(64f, 0.5f, -0.04f);
            glScalef(0.75f, 0.75f, 1.0f);
            fr.drawString(text1, -fr.getStringWidth(text1) / 2, 2, textColor);
            fr.drawString(text2, -fr.getStringWidth(text2) / 2, 12, textColor);
            glPopMatrix();
        }
    }

}
