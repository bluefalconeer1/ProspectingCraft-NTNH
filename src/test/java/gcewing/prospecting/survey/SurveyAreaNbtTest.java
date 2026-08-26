package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class SurveyAreaNbtTest {

    @Test
    public void surveyAreaSurvivesNbtRoundTrip() {
        SurveyArea original = new SurveyArea(SurveyTier.STEEL, 7, -64, 32, 3, 2);
        original.markSurveyed(-64, 32);
        original.recordOre(-63, 32, "Nickel", 3);
        original.recordOre(-63, 32, "Iron", 1);
        original.recordOre(-62, 33, "Copper", 2);

        NBTTagCompound nbt = SurveyAreaNbt.write(original);
        SurveyArea restored = SurveyAreaNbt.read(nbt);

        assertEquals(original.getSurveyId(), restored.getSurveyId());
        assertEquals(SurveyTier.STEEL, restored.getTier());
        assertEquals(7, restored.getDimensionId());
        assertEquals(-64, restored.getMinX());
        assertEquals(32, restored.getMinZ());
        assertEquals(3, restored.getWidth());
        assertEquals(2, restored.getHeight());
        assertTrue(restored.isSurveyed(-64, 32));
        assertTrue(
            restored.getColumn(-64, 32)
                .isEmpty());
        assertEquals(
            3,
            restored.getColumn(-63, 32)
                .getOreCount("Nickel"));
        assertEquals(
            1,
            restored.getColumn(-63, 32)
                .getOreCount("Iron"));
        assertEquals(
            2,
            restored.getColumn(-62, 33)
                .getOreCount("Copper"));
        assertFalse(restored.isSurveyed(-64, 33));
    }

    @Test
    public void oldExactSurveyDataDefaultsToDeshTier() {
        SurveyArea original = new SurveyArea(SurveyTier.BASE, 0, 0, 0, 1, 1);
        original.recordOre(0, 0, "Iron", 3);
        NBTTagCompound nbt = SurveyAreaNbt.write(original);
        nbt.setInteger("formatVersion", 2);
        nbt.removeTag("tier");

        SurveyArea restored = SurveyAreaNbt.read(nbt);

        assertEquals(SurveyTier.DESH, restored.getTier());
        assertEquals(
            3,
            restored.getColumn(0, 0)
                .getOreCount("Iron"));
    }

    @Test
    public void oldNetherOreNamesAreGroupedWhenLoaded() {
        SurveyArea original = new SurveyArea(0, 0, 0, 1, 1);
        original.recordOre(0, 0, "Uranium", 3);
        NBTTagCompound nbt = SurveyAreaNbt.write(original);
        nbt.getTagList("columns", 10)
            .getCompoundTagAt(0)
            .getTagList("ores", 10)
            .getCompoundTagAt(0)
            .setString("name", "NetherUranium");

        SurveyArea restored = SurveyAreaNbt.read(nbt);

        assertEquals(
            3,
            restored.getColumn(0, 0)
                .getOreCount("Uranium"));
        assertEquals(
            "Uranium",
            restored.getColumn(0, 0)
                .getDominantOreName());
    }

    @Test
    public void oldVolcanicOreEntriesAreIgnoredWhenLoaded() {
        SurveyArea original = new SurveyArea(0, 0, 0, 1, 1);
        original.recordOre(0, 0, "Iron", 3);
        NBTTagCompound nbt = SurveyAreaNbt.write(original);
        nbt.getTagList("columns", 10)
            .getCompoundTagAt(0)
            .getTagList("ores", 10)
            .getCompoundTagAt(0)
            .setString("name", "Volcanic");

        SurveyArea restored = SurveyAreaNbt.read(nbt);

        assertTrue(restored.isSurveyed(0, 0));
        assertTrue(
            restored.getColumn(0, 0)
                .isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownFormatVersions() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("formatVersion", 99);

        SurveyAreaNbt.read(nbt);
    }
}
