package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

public class SurveyChunkSnapshotTest {

    private static final UUID OWNER_ID = UUID.fromString("47910713-3ba1-4f00-8000-000000000004");

    @Test
    public void capturesUnsurveyedEmptyAndOreBearingColumns() {
        PlayerSurveyData data = newData();
        SurveyArea area = new SurveyArea(3, 32, 48, 2, 1);
        area.markSurveyed(32, 48);
        area.recordOre(33, 48, "Nickel", 5);
        area.recordOre(33, 48, "Copper", 4);
        area.recordOre(33, 48, "Iron", 3);
        area.recordOre(33, 48, "Tin", 2);
        area.recordOre(33, 48, "Gold", 1);
        area.recordOre(33, 48, "Silver", 1);
        data.addSurveyArea(area);

        SurveyChunkSnapshot snapshot = SurveyChunkSnapshot.from(data, 3, 2, 3);

        assertEquals(false, snapshot.isFullyUnsurveyed());
        assertEquals(3, snapshot.getDimensionId());
        assertEquals(2, snapshot.getChunkX());
        assertEquals(3, snapshot.getChunkZ());
        assertSame(SurveyColumnSnapshot.SURVEYED_EMPTY, snapshot.getColumn(0, 0));

        SurveyColumnSnapshot oreColumn = snapshot.getColumn(1, 0);
        assertEquals(SurveyColumnSnapshot.State.ORE_BEARING, oreColumn.getState());
        assertEquals("Nickel", oreColumn.getDominantOreName());
        assertEquals(120, oreColumn.getOreAlpha());
        assertEquals(Arrays.asList("Nickel", "Copper", "Iron", "Tin", "Gold"), oreColumn.getTopOreNames());

        SurveyColumnSnapshot unsurveyed = snapshot.getColumn(2, 0);
        assertSame(SurveyColumnSnapshot.UNSURVEYED, unsurveyed);
        assertNull(unsurveyed.getDominantOreName());
        assertEquals(0, unsurveyed.getOreAlpha());
    }

    @Test
    public void identifiesAChunkWithNoSurveyedColumns() {
        SurveyChunkSnapshot snapshot = SurveyChunkSnapshot.from(newData(), 0, 4, -7);

        assertEquals(true, snapshot.isFullyUnsurveyed());
    }

    @Test
    public void mapsNegativeChunkCoordinatesToTheCorrectColumns() {
        PlayerSurveyData data = newData();
        SurveyArea area = new SurveyArea(0, -16, -16, 16, 16);
        area.recordOre(-16, -16, "Iron", 2);
        area.recordOre(-1, -1, "Gold", 3);
        data.addSurveyArea(area);

        SurveyChunkSnapshot snapshot = SurveyChunkSnapshot.from(data, 0, -1, -1);

        assertEquals(
            "Iron",
            snapshot.getColumn(0, 0)
                .getDominantOreName());
        assertEquals(
            "Gold",
            snapshot.getColumn(15, 15)
                .getDominantOreName());
    }

    @Test
    public void recalculatesVisibleRankingFromThePlayersSelectionWithoutChangingRawData() {
        PlayerSurveyData data = newData();
        SurveyArea area = new SurveyArea(0, 0, 0, 1, 1);
        area.recordOre(0, 0, "Coal", 10);
        area.recordOre(0, 0, "Iron", 4);
        area.recordOre(0, 0, "Gold", 3);
        data.addSurveyArea(area);

        SurveyChunkSnapshot snapshot = SurveyChunkSnapshot
            .from(data, 0, 0, 0, SurveyOreSelection.custom(Arrays.asList("iron")));
        SurveyColumnSnapshot column = snapshot.getColumn(0, 0);

        assertEquals(SurveyColumnSnapshot.State.ORE_BEARING, column.getState());
        assertEquals("Iron", column.getDominantOreName());
        assertEquals(108, column.getOreAlpha());
        assertEquals(Arrays.asList("Iron"), column.getTopOreNames());
        assertEquals(
            10,
            data.getColumn(0, 0, 0)
                .getOreCount("Coal"));
        assertEquals(
            3,
            data.getColumn(0, 0, 0)
                .getOreCount("Gold"));
    }

    @Test
    public void treatsAColumnWithoutAnySelectedOresAsSurveyedEmpty() {
        PlayerSurveyData data = newData();
        SurveyArea area = new SurveyArea(0, 0, 0, 1, 1);
        area.recordOre(0, 0, "Coal", 10);
        data.addSurveyArea(area);

        SurveyChunkSnapshot snapshot = SurveyChunkSnapshot
            .from(data, 0, 0, 0, SurveyOreSelection.custom(Arrays.asList("Iron")));

        assertSame(SurveyColumnSnapshot.SURVEYED_EMPTY, snapshot.getColumn(0, 0));
        assertEquals(
            10,
            data.getColumn(0, 0, 0)
                .getOreCount("Coal"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsColumnsOutsideTheChunk() {
        SurveyChunkSnapshot.from(newData(), 0, 0, 0)
            .getColumn(16, 0);
    }

    private static PlayerSurveyData newData() {
        return new PlayerSurveyData(PlayerSurveyData.getDataName(OWNER_ID));
    }
}
