package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

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

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);
        PlayerSurveyData restored = newData(OWNER_ID);
        restored.readFromNBT(nbt);

        assertEquals(OWNER_ID, restored.getOwnerId());
        assertEquals(1, restored.getSurveyAreaCount());
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

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDataSavedForAnotherOwner() {
        PlayerSurveyData firstOwner = newData(OWNER_ID);
        NBTTagCompound nbt = new NBTTagCompound();
        firstOwner.writeToNBT(nbt);

        UUID otherOwner = UUID.fromString("27910713-3ba1-4f00-8000-000000000002");
        newData(otherOwner).readFromNBT(nbt);
    }

    private static PlayerSurveyData newData(UUID ownerId) {
        return new PlayerSurveyData(PlayerSurveyData.getDataName(ownerId));
    }
}
