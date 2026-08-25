//------------------------------------------------------------------------------------------------
//
//   ProspectingCraft - Container slot holding a specific kind of item
//
//------------------------------------------------------------------------------------------------

package gcewing.prospecting;

import net.minecraft.inventory.*;
import net.minecraft.item.*;

public class ItemSlot extends Slot {

    protected Item item;
    protected int stackLimit;

    public ItemSlot(IInventory inv, int i, int x, int y, Item item, int stackLimit) {
        super(inv, i, x, y);
        this.item = item;
        this.stackLimit = stackLimit;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return stack.getItem() == item;
    }

    @Override
    public int getSlotStackLimit() {
        return stackLimit;
    }

}

