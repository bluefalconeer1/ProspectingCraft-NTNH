package gcewing.prospecting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import gcewing.prospecting.survey.PlayerSurveyData;
import gcewing.prospecting.survey.SurveyArea;
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
    public void scanConvertsWorldMetadataToDroppedItemMetadata() {
        Block leavesWithWorldStateFlags = new Block(Material.leaves) {

            @Override
            public int damageDropped(int metadata) {
                return metadata & 3;
            }
        };

        assertEquals(0, SeismicRecorderTE.itemMetadataForScannedBlock(leavesWithWorldStateFlags, 8));
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

    @Test
    public void missingActiveSurveyRecordAbortsWithoutScanning() {
        TestSeismicRecorderTE recorder = new TestSeismicRecorderTE();
        recorder.ownerId = OWNER_ID;
        recorder.playerSurveyData = new PlayerSurveyData(PlayerSurveyData.getDataName(OWNER_ID));
        recorder.activeSurveyId = UUID.fromString("27910713-3ba1-4f00-8000-000000000002");
        recorder.activated = true;
        recorder.phase = 42;

        recorder.updateActiveScan();

        assertFalse(recorder.activated);
        assertEquals(0, recorder.phase);
        assertEquals(0, recorder.scannedRows);
        assertNull(recorder.activeSurveyId);
    }

    @Test
    public void restoredActiveSurveyContinuesAtItsSavedRow() {
        TestSeismicRecorderTE recorder = new TestSeismicRecorderTE();
        recorder.ownerId = OWNER_ID;
        recorder.playerSurveyData = new PlayerSurveyData(PlayerSurveyData.getDataName(OWNER_ID));
        SurveyArea area = new SurveyArea(SurveyTier.BASE, 0, 0, 0, 128, 128);
        recorder.playerSurveyData.addSurveyArea(area);
        recorder.activeSurveyId = area.getSurveyId();
        recorder.activated = true;
        recorder.phase = 42;

        recorder.updateActiveScan();

        assertTrue(recorder.activated);
        assertEquals(43, recorder.phase);
        assertEquals(1, recorder.scannedRows);
        assertEquals(area.getSurveyId(), recorder.activeSurveyId);
    }

    @Test
    public void cancellingActiveScanDiscardsOnlyItsPartialSurveyAndInvalidatesMap() {
        TestSeismicRecorderTE recorder = new TestSeismicRecorderTE();
        recorder.ownerId = OWNER_ID;
        recorder.playerSurveyData = new PlayerSurveyData(PlayerSurveyData.getDataName(OWNER_ID));
        SurveyArea completed = new SurveyArea(SurveyTier.BASE, 0, 0, 0, 128, 128);
        completed.markSurveyed(0, 0);
        recorder.playerSurveyData.addSurveyArea(completed);
        SurveyArea partial = new SurveyArea(SurveyTier.BASE, 0, 0, 0, 128, 128);
        partial.markSurveyed(0, 0);
        recorder.playerSurveyData.addSurveyArea(partial);
        recorder.activeSurveyId = partial.getSurveyId();
        recorder.activated = true;
        recorder.phase = 42;

        recorder.cancelActiveScan();

        assertFalse(recorder.activated);
        assertEquals(0, recorder.phase);
        assertNull(recorder.activeSurveyId);
        assertEquals(1, recorder.playerSurveyData.getSurveyAreaCount());
        assertSame(completed, recorder.playerSurveyData.getSurveyArea(completed.getSurveyId()));
        assertNull(recorder.playerSurveyData.getSurveyArea(partial.getSurveyId()));
        assertSame(partial, recorder.invalidatedSurvey);
    }

    @Test
    public void findsValidGeophoneTrioAfterAnInvalidEarlyTrio() {
        BlockPos recorder = new BlockPos(0, 0, 0);
        BlockPos east = new BlockPos(8, 0, 0);
        BlockPos tooCloseToEast = new BlockPos(9, 0, 0);
        BlockPos south = new BlockPos(0, 0, 8);
        BlockPos west = new BlockPos(-8, 0, 0);

        assertFalse(SeismicRecorderTE.hasWellSeparatedTrio(recorder, Arrays.asList(east, tooCloseToEast, south)));
        assertTrue(SeismicRecorderTE.hasWellSeparatedTrio(recorder, Arrays.asList(east, tooCloseToEast, south, west)));
    }

    @Test
    public void excludesGeophonesOutsideExactHorizontalSearchArea() {
        BlockPos recorder = new BlockPos(100, 64, -100);

        assertTrue(SeismicRecorderTE.isInsideGeophoneSearchArea(recorder, new BlockPos(115, 200, -115)));
        assertFalse(SeismicRecorderTE.isInsideGeophoneSearchArea(recorder, new BlockPos(116, 64, -100)));
        assertFalse(SeismicRecorderTE.isInsideGeophoneSearchArea(recorder, new BlockPos(100, 64, -116)));
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
        private PlayerSurveyData playerSurveyData;
        private int scannedRows;
        private SurveyArea invalidatedSurvey;

        @Override
        public void markChanged() {}

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

        @Override
        protected PlayerSurveyData getPlayerSurveyData() {
            return playerSurveyData;
        }

        @Override
        protected void scanRow(int row, PlayerSurveyData data, SurveyArea area) {
            scannedRows++;
        }

        @Override
        protected void notifySurveyChanged(SurveyArea surveyArea) {
            invalidatedSurvey = surveyArea;
        }
    }
}
