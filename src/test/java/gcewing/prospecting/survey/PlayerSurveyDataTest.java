package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

public class PlayerSurveyDataTest {

    private static final UUID OWNER_ID = UUID.fromString("17910713-3ba1-4f00-8000-000000000001");

    @Test
    public void playerSurveyDataSurvivesNbtRoundTrip() {
        PlayerSurveyData original = newData(OWNER_ID);
        SurveyArea area = new SurveyArea(7, 10, 20, 2, 1);
        area.recordOre(10, 20, "Nickel", 4);
        area.markSurveyed(11, 20);
        original.addSurveyArea(area);
        original.setOreSelection(SurveyOreSelection.custom(Arrays.asList("Iron", "Gold")));

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);
        PlayerSurveyData restored = newData(OWNER_ID);
        restored.readFromNBT(nbt);

        assertEquals(OWNER_ID, restored.getOwnerId());
        assertEquals(
            SurveyOreSelection.Mode.CUSTOM,
            restored.getOreSelection()
                .getMode());
        assertTrue(
            restored.getOreSelection()
                .includes("Iron"));
        assertTrue(
            restored.getOreSelection()
                .includes("Gold"));
        assertFalse(
            restored.getOreSelection()
                .includes("Lignite"));
        assertEquals(1, restored.getSurveyAreaCount());
        assertEquals(
            area.getSurveyId(),
            restored.getSurveyArea(area.getSurveyId())
                .getSurveyId());
        assertEquals(
            4,
            restored.getColumn(7, 10, 20)
                .getOreCount("Nickel"));
        assertTrue(
            restored.getColumn(7, 11, 20)
                .isEmpty());
        assertNull(restored.getColumn(0, 10, 20));
    }

    @Test
    public void onePlayerDatabaseRetainsSurveysFromMultipleDimensions() {
        PlayerSurveyData original = newData(OWNER_ID);
        SurveyArea overworld = new SurveyArea(0, 10, 20, 1, 1);
        overworld.recordOre(10, 20, "Iron", 3);
        original.addSurveyArea(overworld);
        SurveyArea nether = new SurveyArea(-1, 10, 20, 1, 1);
        nether.recordOre(10, 20, "Gold", 2);
        original.addSurveyArea(nether);

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);
        PlayerSurveyData restored = newData(OWNER_ID);
        restored.readFromNBT(nbt);

        assertEquals(
            3,
            restored.getColumn(0, 10, 20)
                .getOreCount("Iron"));
        assertEquals(
            2,
            restored.getColumn(-1, 10, 20)
                .getOreCount("Gold"));
        assertEquals(2, restored.getSurveyAreaCount());
    }

    @Test
    public void oldSurveyDataWithoutSelectionDefaultsToShowAll() {
        PlayerSurveyData original = newData(OWNER_ID);
        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);
        nbt.removeTag("customOreSelection");
        nbt.removeTag("selectedOres");

        PlayerSurveyData restored = newData(OWNER_ID);
        restored.readFromNBT(nbt);

        assertEquals(
            SurveyOreSelection.Mode.SHOW_ALL,
            restored.getOreSelection()
                .getMode());
        assertTrue(
            restored.getOreSelection()
                .includes("Lignite"));
    }

    @Test
    public void newestSurveyedColumnReplacesOlderData() {
        PlayerSurveyData data = newData(OWNER_ID);
        SurveyArea older = new SurveyArea(0, 0, 0, 2, 1);
        older.recordOre(0, 0, "Iron", 3);
        older.recordOre(1, 0, "Copper", 2);
        data.addSurveyArea(older);

        SurveyArea newer = new SurveyArea(0, 0, 0, 1, 1);
        newer.markSurveyed(0, 0);
        data.addSurveyArea(newer);

        assertTrue(data.isSurveyed(0, 0, 0));
        assertTrue(
            data.getColumn(0, 0, 0)
                .isEmpty());
        assertEquals(
            2,
            data.getColumn(0, 1, 0)
                .getOreCount("Copper"));
        assertFalse(data.isSurveyed(0, 2, 0));
    }

    @Test
    public void higherTierSurveyCannotBeDowngradedByANewerLowTierScan() {
        PlayerSurveyData data = newData(OWNER_ID);
        SurveyArea desh = new SurveyArea(SurveyTier.DESH, 0, 0, 0, 1, 1);
        desh.recordOre(0, 0, "Iron", 3);
        data.addSurveyArea(desh);

        SurveyArea newerBase = new SurveyArea(SurveyTier.BASE, 0, 0, 0, 1, 1);
        newerBase.recordOre(0, 0, "Copper", 2);
        data.addSurveyArea(newerBase);

        assertEquals(
            SurveyTier.DESH,
            data.getSurveyAreaAt(0, 0, 0)
                .getTier());
        assertEquals(
            3,
            data.getColumn(0, 0, 0)
                .getOreCount("Iron"));
    }

    @Test
    public void reportsUniqueDiscoveredOreNamesIgnoringCase() {
        PlayerSurveyData data = newData(OWNER_ID);
        SurveyArea first = new SurveyArea(0, 0, 0, 2, 1);
        first.recordOre(0, 0, "Iron", 3);
        first.recordOre(1, 0, "Lignite", 2);
        data.addSurveyArea(first);

        SurveyArea second = new SurveyArea(0, 10, 10, 2, 1);
        second.recordOre(10, 10, "iron", 1);
        second.recordOre(11, 10, "Gold", 4);
        data.addSurveyArea(second);

        assertEquals(
            Arrays.asList("Gold", "Iron", "Lignite"),
            new java.util.ArrayList<String>(data.getDiscoveredOreNames()));
    }

    @Test
    public void canonicalizesSelectionAgainstDiscoveredOres() {
        PlayerSurveyData data = newData(OWNER_ID);
        SurveyArea area = new SurveyArea(0, 0, 0, 1, 1);
        area.recordOre(0, 0, "Lignite", 3);
        data.addSurveyArea(area);

        SurveyOreSelection selection = data
            .canonicalizeOreSelection(SurveyOreSelection.custom(Arrays.asList("lIGNite")));

        assertEquals(SurveyOreSelection.Mode.CUSTOM, selection.getMode());
        assertTrue(selection.includes("Lignite"));
    }

    @Test
    public void canonicalizesSelectionAgainstProvidedRuntimeCatalog() {
        PlayerSurveyData data = newData(OWNER_ID);

        SurveyOreSelection selection = data.canonicalizeOreSelection(
            SurveyOreSelection.custom(Arrays.asList("coltan")),
            Arrays.asList("Coltan", "Lignite"));

        assertTrue(selection.includes("Coltan"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSelectionOfUndiscoveredOre() {
        PlayerSurveyData data = newData(OWNER_ID);

        data.canonicalizeOreSelection(SurveyOreSelection.custom(Arrays.asList("TypoOre")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDataSavedForAnotherOwner() {
        PlayerSurveyData firstOwner = newData(OWNER_ID);
        NBTTagCompound nbt = new NBTTagCompound();
        firstOwner.writeToNBT(nbt);

        UUID otherOwner = UUID.fromString("27910713-3ba1-4f00-8000-000000000002");
        newData(otherOwner).readFromNBT(nbt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDuplicateSurveyIds() {
        PlayerSurveyData data = newData(OWNER_ID);
        UUID surveyId = UUID.fromString("37910713-3ba1-4f00-8000-000000000003");
        data.addSurveyArea(new SurveyArea(surveyId, 0, 0, 0, 1, 1));

        data.addSurveyArea(new SurveyArea(surveyId, 0, 10, 10, 1, 1));
    }

    @Test
    public void removesOnlyTheRequestedSurveyArea() {
        PlayerSurveyData data = newData(OWNER_ID);
        SurveyArea retained = new SurveyArea(0, 0, 0, 1, 1);
        SurveyArea removed = new SurveyArea(0, 10, 10, 1, 1);
        data.addSurveyArea(retained);
        data.addSurveyArea(removed);

        assertEquals(removed, data.removeSurveyArea(removed.getSurveyId()));
        assertEquals(1, data.getSurveyAreaCount());
        assertEquals(retained, data.getSurveyArea(retained.getSurveyId()));
        assertNull(data.getSurveyArea(removed.getSurveyId()));
        assertNull(data.removeSurveyArea(removed.getSurveyId()));
    }

    @Test
    public void skipsMalformedAreaAndContinuesLoadingLaterAreas() {
        SurveyArea first = new SurveyArea(0, 0, 0, 1, 1);
        first.recordOre(0, 0, "Iron", 2);
        SurveyArea last = new SurveyArea(0, 20, 20, 1, 1);
        last.recordOre(20, 20, "Gold", 3);

        PlayerSurveyData encoded = newData(OWNER_ID);
        encoded.setOreSelection(SurveyOreSelection.custom(Arrays.asList("Iron")));
        NBTTagCompound nbt = new NBTTagCompound();
        encoded.writeToNBT(nbt);
        NBTTagList areas = new NBTTagList();
        areas.appendTag(SurveyAreaNbt.write(first));
        NBTTagCompound malformed = SurveyAreaNbt.write(new SurveyArea(0, 10, 10, 1, 1));
        malformed.setInteger("formatVersion", 99);
        areas.appendTag(malformed);
        areas.appendTag(SurveyAreaNbt.write(last));
        nbt.setTag("areas", areas);

        PlayerSurveyData restored = newData(OWNER_ID);
        restored.readFromNBT(nbt);

        assertEquals(2, restored.getSurveyAreaCount());
        assertEquals(
            first.getSurveyId(),
            restored.getSurveyArea(first.getSurveyId())
                .getSurveyId());
        assertEquals(
            last.getSurveyId(),
            restored.getSurveyArea(last.getSurveyId())
                .getSurveyId());
        assertTrue(
            restored.getOreSelection()
                .includes("Iron"));
    }

    @Test
    public void invalidTopLevelDataLeavesExistingStateUntouched() {
        PlayerSurveyData data = newData(OWNER_ID);
        SurveyArea existing = new SurveyArea(0, 0, 0, 1, 1);
        existing.recordOre(0, 0, "Lignite", 4);
        data.addSurveyArea(existing);
        data.setOreSelection(SurveyOreSelection.custom(Arrays.asList("Lignite")));

        NBTTagCompound invalid = new NBTTagCompound();
        data.writeToNBT(invalid);
        invalid.setInteger("formatVersion", 99);
        try {
            data.readFromNBT(invalid);
            fail("Expected invalid player survey format to be rejected");
        } catch (IllegalArgumentException expected) {}

        assertEquals(1, data.getSurveyAreaCount());
        assertEquals(
            existing.getSurveyId(),
            data.getSurveyArea(existing.getSurveyId())
                .getSurveyId());
        assertTrue(
            data.getOreSelection()
                .includes("Lignite"));
    }

    private static PlayerSurveyData newData(UUID ownerId) {
        return new PlayerSurveyData(PlayerSurveyData.getDataName(ownerId));
    }
}
