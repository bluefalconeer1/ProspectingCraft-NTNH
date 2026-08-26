package gcewing.prospecting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import gcewing.prospecting.survey.SurveyTier;

public class SeismicRecorderTETest {

    private static final UUID OWNER_ID = UUID.fromString("17910713-3ba1-4f00-8000-000000000001");

    @Test
    public void ownerIdSurvivesNbtRoundTrip() {
        UUID expectedSurveyId = UUID.fromString("27910713-3ba1-4f00-8000-000000000002");
        SeismicRecorderTE original = new SeismicRecorderTE() {

            @Override
            public void markChanged() {}
        };
        original.setOwnerId(OWNER_ID);
        original.activeSurveyId = expectedSurveyId;

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeContentsToNBT(nbt);

        SeismicRecorderTE restored = new SeismicRecorderTE();
        restored.readContentsFromNBT(nbt);

        assertEquals(OWNER_ID, restored.getOwnerId());
        assertEquals(expectedSurveyId, restored.activeSurveyId);
        assertEquals(7, restored.getSizeInventory());
    }

    @Test
    public void existingSurveyorCreatesBaseTierScans() {
        assertEquals(SurveyTier.BASE, new SeismicRecorderTE().getSurveyTier());
    }

    @Test
    public void surveyTierComesFromThePlacedSurveyorBlock() {
        SeismicRecorderBlock steelSurveyor = new SeismicRecorderBlock(SurveyTier.STEEL);
        SeismicRecorderBlock deshSurveyor = new SeismicRecorderBlock(SurveyTier.DESH);

        assertEquals(SurveyTier.STEEL, SeismicRecorderTE.surveyTierForBlock(steelSurveyor));
        assertEquals(SurveyTier.DESH, SeismicRecorderTE.surveyTierForBlock(deshSurveyor));
        assertEquals(SurveyTier.BASE, SeismicRecorderTE.surveyTierForBlock(Blocks.stone));
    }

    @Test
    public void upgradedSurveyorsSelectTheirTierTextureSuffix() {
        assertEquals("", SeismicRecorderBlock.getTextureSuffix(SurveyTier.BASE));
        assertEquals("_steel", SeismicRecorderBlock.getTextureSuffix(SurveyTier.STEEL));
        assertEquals("_desh", SeismicRecorderBlock.getTextureSuffix(SurveyTier.DESH));
    }

    @Test
    public void requiresAllFourInkColorsInTheirMatchingSlots() {
        TestSeismicRecorderTE recorder = readyRecorder();

        assertEquals(SeismicRecorderTE.ReadyStatus.RDY, recorder.getReadyStatus());
        recorder.setInventorySlotContents(SeismicRecorderTE.cyanInkSlot, new ItemStack(recorder.inkItems[1]));

        assertEquals(SeismicRecorderTE.ReadyStatus.OOI3, recorder.getReadyStatus());
        assertFalse(recorder.isItemValidForSlot(SeismicRecorderTE.cyanInkSlot, new ItemStack(recorder.inkItems[1])));
    }

    @Test
    public void consumesOnePaperAndGunpowderAndDamagesEveryInk() {
        TestSeismicRecorderTE recorder = readyRecorder();

        recorder.consumeSurveySupplies();

        assertEquals(1, recorder.getStackInSlot(SeismicRecorderTE.paperSlot).stackSize);
        assertEquals(1, recorder.getStackInSlot(SeismicRecorderTE.gunpowderSlot).stackSize);
        for (int slot = SeismicRecorderTE.blackInkSlot; slot <= SeismicRecorderTE.cyanInkSlot; slot++) {
            assertSame(
                recorder.inkItems[slot - SeismicRecorderTE.blackInkSlot],
                recorder.getStackInSlot(slot)
                    .getItem());
            assertEquals(
                1,
                recorder.getStackInSlot(slot)
                    .getItemDamage());
        }
    }

    @Test
    public void guiActivationUsesTheSameServerReadinessCheck() {
        int[] starts = { 0 };
        TestSeismicRecorderTE recorder = readyRecorder();
        recorder.startCounter = starts;

        assertTrue(recorder.tryBeginScan());
        assertEquals(1, starts[0]);
        recorder.setInventorySlotContents(SeismicRecorderTE.paperSlot, null);
        assertFalse(recorder.tryBeginScan());
        assertEquals(1, starts[0]);
    }

    @Test
    public void redstoneActivationOnlyRunsOnARisingEdge() {
        final int[] starts = { 0 };
        SeismicRecorderTE recorder = new SeismicRecorderTE() {

            @Override
            public boolean tryBeginScan() {
                starts[0]++;
                return true;
            }
        };

        recorder.inputSignal(true);
        recorder.inputSignal(true);
        recorder.inputSignal(false);
        recorder.inputSignal(true);

        assertEquals(2, starts[0]);
    }

    private static TestSeismicRecorderTE readyRecorder() {
        TestSeismicRecorderTE recorder = new TestSeismicRecorderTE();
        recorder.ownerId = OWNER_ID;
        recorder.numGeophones = 3;
        recorder.geophonesOkay = true;
        recorder.setInventorySlotContents(SeismicRecorderTE.paperSlot, new ItemStack(recorder.paperItem, 2));
        recorder.setInventorySlotContents(SeismicRecorderTE.gunpowderSlot, new ItemStack(recorder.gunpowderItem, 2));
        for (int i = 0; i < recorder.inkItems.length; i++)
            recorder.setInventorySlotContents(SeismicRecorderTE.blackInkSlot + i, new ItemStack(recorder.inkItems[i]));
        return recorder;
    }

    private static class TestSeismicRecorderTE extends SeismicRecorderTE {

        private final Item paperItem = new Item();
        private final Item gunpowderItem = new Item();
        private final Item[] inkItems = { new InkBottleItem(), new InkBottleItem(), new InkBottleItem(),
            new InkBottleItem() };
        private int[] startCounter;

        @Override
        protected Item getPaperItem() {
            return paperItem;
        }

        @Override
        protected Item getGunpowderItem() {
            return gunpowderItem;
        }

        @Override
        protected Item getExpectedInkItem(int inkIndex) {
            return inkItems[inkIndex];
        }

        @Override
        protected void beginScan() {
            if (startCounter != null) startCounter[0]++;
        }
    }
}
