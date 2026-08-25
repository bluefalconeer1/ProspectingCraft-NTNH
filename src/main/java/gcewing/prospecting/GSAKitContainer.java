// ------------------------------------------------------------------------------------------------
//
// ProspectingCraft - Sample Analyser Gui Container
//
// ------------------------------------------------------------------------------------------------

package gcewing.prospecting;

import static gcewing.prospecting.BaseDataChannel.*;
import static gcewing.prospecting.GSAKitTE.*;

import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.world.*;
import net.minecraftforge.common.util.*;

public class GSAKitContainer extends BaseContainer {

    protected static final int guiWidth = 224, guiHeight = 192,

        playerSlotsX = 9, playerSlotsY = 23,

        inputSlotsX = 98, inputSlotsY = 23, bookSlotX = 171;

    public GSAKitTE te;
    public String message;

    public GSAKitContainer(EntityPlayer player, GSAKitTE te) {
        super(guiWidth, guiHeight);
        this.te = te;
        addPlayerSlotsRotated(player, playerSlotsX, playerSlotsY);
        addContainerSlots();
    }

    protected void addContainerSlots() {
        ProspectingCraft mod = ProspectingCraft.mod;
        IInventory inv = te.getInventory();
        beginContainerSlots();
        addSlotToContainer(
            new ItemSlot(inv, sampleSlot, inputSlotsX, inputSlotsY, ProspectingCraft.itemGeologicalSample, 1));
        addSlotToContainer(
            new ItemSlot(inv, litmusPaperSlot, inputSlotsX + 18, inputSlotsY, ProspectingCraft.itemLitmusPaper, 64));
        addSlotToContainer(new ItemSlot(inv, bookSlot, bookSlotX, inputSlotsY, Items.writable_book, 1));
        endContainerSlots();
    }

    @ServerMessageHandler("addToBook")
    public void onAddToBook(EntityPlayer player, ChannelInput data) {
        String text = data.readUTF();
        if (text.length() > 256) text = text.substring(0, 256);
        // System.out.printf("GSAKitContainer.onAddToBook: text = %s\n", text);
        ItemStack stack = te.getStackInSlot(bookSlot);
        if (stack != null) {
            // System.out.printf("GSAKitContainer.onAddToBook: found book\n");
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt == null) {
                // System.out.printf("GSAKitContainer.onAddToBook: creating NBTTagCompound\n");
                nbt = new NBTTagCompound();
                stack.setTagCompound(nbt);
            }
            // System.out.printf("GSAKitContainer.onAddToBook: NBTTagCompound = %s\n", nbt);
            NBTTagList pages = nbt.getTagList("pages", Constants.NBT.TAG_STRING);
            int n = pages.tagCount();
            // System.out.printf("GSAKitContainer.onAddToBook: book has %s pages\n", n);
            for (int i = 0; i < n; i++) if (text.equals(pages.getStringTagAt(i))) {
                sendAddedToBook(player, i, false);
                return;
            }
            pages.appendTag(new NBTTagString(text));
            nbt.setTag("pages", pages);
            te.markChanged();
            sendAddedToBook(player, n, true);
        }
    }

    protected void sendAddedToBook(EntityPlayer player, int pageIndex, boolean added) {
        // System.out.printf("GSAKitContainer.sendAddedToBook: pageIndex = %s, added = %s\n",
        // pageIndex, added);
        ChannelOutput data = ProspectingCraft.channel.openClientContainer(player, "addedToBook");
        data.writeInt(pageIndex);
        data.writeBoolean(added);
        data.close();
    }

    @ClientMessageHandler("addedToBook")
    public void onAddedToBook(ChannelInput data) {
        int pageIndex = data.readInt();
        boolean added = data.readBoolean();
        int pageNo = pageIndex + 1;
        String text;
        if (added) text = "Added to book on page " + pageNo;
        else text = "Already in book on page " + pageNo;
        showMessage(text);
    }

    protected void showMessage(String text) {
        message = text;
        BaseUtils.addClientChatMessage(text);
    }

}
