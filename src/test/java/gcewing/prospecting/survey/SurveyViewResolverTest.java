package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.UUID;

import org.junit.After;
import org.junit.Test;

public class SurveyViewResolverTest {

    private static final UUID OWNER_ID = UUID.fromString("87910713-3ba1-4f00-8000-000000000008");

    @After
    public void resetTierDefaults() {
        SurveyTier.resetAllDefaults();
    }

    @Test
    public void baseTierDisplaysEvenASingleOreAcrossItsTwentyFourBlockCell() {
        PlayerSurveyData data = dataWithArea(SurveyTier.BASE);
        data.getSurveyAreaAt(0, 0, 0)
            .recordOre(0, 0, "Iron", 1);

        SurveyChunkSnapshot snapshot = SurveyChunkSnapshot.from(data, 0, 0, 0);

        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            SurveyColumnSnapshot column = snapshot.getColumn(x, z);
            assertEquals(SurveyColumnSnapshot.State.ORE_BEARING, column.getState());
            assertEquals("Iron", column.getDominantOreName());
            assertEquals(
                1,
                column.getTopOreNames()
                    .size());
        }
        assertEquals(
            72,
            snapshot.getColumn(12, 12)
                .getOreAlpha());
        assertEquals(
            72,
            snapshot.getColumn(0, 0)
                .getOreAlpha());
    }

    @Test
    public void baseTierStrengthUsesTotalOreMassRatherThanDepositShape() {
        assertEquals(72, resolveBaseMass(1).getOreAlpha());
        assertEquals(72, resolveBaseMass(8).getOreAlpha());
        assertEquals(144, resolveBaseMass(9).getOreAlpha());
        assertEquals(144, resolveBaseMass(16).getOreAlpha());
        assertEquals(216, resolveBaseMass(17).getOreAlpha());
    }

    @Test
    public void baseTierCombinesDispersedOreWithoutAssumingARectangularDeposit() {
        PlayerSurveyData data = dataWithArea(SurveyTier.BASE);
        SurveyArea area = data.getSurveyAreaAt(0, 0, 0);
        area.recordOre(0, 0, "Copper", 1);
        area.recordOre(3, 5, "Copper", 1);
        area.recordOre(8, 1, "Copper", 1);
        area.recordOre(14, 14, "Copper", 1);
        area.recordOre(22, 6, "Copper", 1);

        SurveyColumnSnapshot column = SurveyChunkSnapshot.from(data, 0, 0, 0)
            .getColumn(15, 15);

        assertEquals("Copper", column.getDominantOreName());
        assertEquals(72, column.getOreAlpha());
    }

    @Test
    public void baseTierRadiusReportsANearbyDepositWithoutRevealingItsColumn() {
        PlayerSurveyData data = dataWithArea(SurveyTier.BASE);
        data.getSurveyAreaAt(0, 0, 0)
            .recordOre(27, 0, "Gold", 1);

        SurveyChunkSnapshot snapshot = SurveyChunkSnapshot.from(data, 0, 0, 0);

        assertEquals(
            "Gold",
            snapshot.getColumn(0, 0)
                .getDominantOreName());
        assertSame(
            SurveyColumnSnapshot.SURVEYED_EMPTY,
            SurveyViewResolver.resolve(data, 0, 48, 0, SurveyOreSelection.showAll()));
    }

    @Test
    public void baseTierRanksByMassButSendsOnlyOneOreName() {
        PlayerSurveyData data = dataWithArea(SurveyTier.BASE);
        SurveyArea area = data.getSurveyAreaAt(0, 0, 0);
        placeRectangle(area, 0, 0, 4, 4, "Iron", 1);
        placeRectangle(area, 4, 0, 4, 4, "Gold", 2);

        SurveyColumnSnapshot column = SurveyChunkSnapshot.from(data, 0, 0, 0)
            .getColumn(0, 0);

        assertEquals("Gold", column.getDominantOreName());
        assertEquals(
            1,
            column.getTopOreNames()
                .size());
    }

    @Test
    public void deshTierRetainsExactColumns() {
        PlayerSurveyData data = dataWithArea(SurveyTier.DESH);
        SurveyArea area = data.getSurveyAreaAt(0, 0, 0);
        area.recordOre(2, 3, "Iron", 4);

        SurveyChunkSnapshot snapshot = SurveyChunkSnapshot.from(data, 0, 0, 0);

        assertEquals(
            "Iron",
            snapshot.getColumn(2, 3)
                .getDominantOreName());
        assertEquals(
            108,
            snapshot.getColumn(2, 3)
                .getOreAlpha());
        assertSame(SurveyColumnSnapshot.SURVEYED_EMPTY, snapshot.getColumn(3, 3));
    }

    @Test
    public void configuredDeshCellAndRadiusProduceAContinuousRegionalView() {
        SurveyTier.configure(deshSettings(4, 3));
        PlayerSurveyData data = dataWithArea(SurveyTier.DESH);
        data.getSurveyAreaAt(0, 0, 0)
            .recordOre(5, 2, "Iron", 1);

        SurveyColumnSnapshot column = SurveyChunkSnapshot.from(data, 0, 0, 0)
            .getColumn(0, 0);

        assertEquals("Iron", column.getDominantOreName());
        assertEquals(72, column.getOreAlpha());
    }

    @Test
    public void snapshotSessionComputesEachAlignedTierCellOnlyOnce() {
        assertEquals(1, resolveChunkAndCountCells(SurveyTier.BASE));
        assertEquals(4, resolveChunkAndCountCells(SurveyTier.STEEL));
        assertEquals(256, resolveChunkAndCountCells(SurveyTier.DESH));
    }

    @Test
    public void overlappingSurveyAreasDoNotShareCachedCellResults() {
        PlayerSurveyData data = new PlayerSurveyData(PlayerSurveyData.getDataName(OWNER_ID));
        SurveyArea older = new SurveyArea(SurveyTier.BASE, 0, 0, 0, 16, 16);
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) older.markSurveyed(x, z);
        older.recordOre(0, 0, "Iron", 1);
        data.addSurveyArea(older);
        SurveyArea newer = new SurveyArea(SurveyTier.BASE, 0, 8, 0, 8, 16);
        for (int z = 0; z < 16; z++) for (int x = 8; x < 16; x++) newer.markSurveyed(x, z);
        newer.recordOre(8, 0, "Gold", 1);
        data.addSurveyArea(newer);

        SurveyViewResolver.ResolutionSession resolver = SurveyViewResolver.begin(data, 0, SurveyOreSelection.showAll());

        assertEquals(
            "Iron",
            resolver.resolve(0, 0)
                .getDominantOreName());
        assertEquals(
            "Gold",
            resolver.resolve(8, 0)
                .getDominantOreName());
        assertEquals(2, resolver.getComputedCellCount());
    }

    private static SurveyColumnSnapshot resolveBaseMass(int amount) {
        PlayerSurveyData data = dataWithArea(SurveyTier.BASE);
        data.getSurveyAreaAt(0, 0, 0)
            .recordOre(0, 0, "Iron", amount);
        return SurveyChunkSnapshot.from(data, 0, 0, 0)
            .getColumn(12, 12);
    }

    private static int resolveChunkAndCountCells(SurveyTier tier) {
        PlayerSurveyData data = dataWithArea(tier);
        SurveyViewResolver.ResolutionSession resolver = SurveyViewResolver.begin(data, 0, SurveyOreSelection.showAll());
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) resolver.resolve(x, z);
        return resolver.getComputedCellCount();
    }

    private static PlayerSurveyData dataWithArea(SurveyTier tier) {
        PlayerSurveyData data = new PlayerSurveyData(PlayerSurveyData.getDataName(OWNER_ID));
        SurveyArea area = new SurveyArea(tier, 0, 0, 0, 64, 64);
        for (int z = 0; z < 64; z++) for (int x = 0; x < 64; x++) area.markSurveyed(x, z);
        data.addSurveyArea(area);
        return data;
    }

    private static SurveyTier.IntegerSettings deshSettings(final int cellSize, final int readingRadius) {
        return new SurveyTier.IntegerSettings() {

            @Override
            public int getInteger(String category, String key, int defaultValue) {
                if (key.equals("deshCellSize")) return cellSize;
                if (key.equals("deshReadingRadius")) return readingRadius;
                return defaultValue;
            }
        };
    }

    private static void placeRectangle(SurveyArea area, int minX, int minZ, int width, int height, String oreName,
        int amount) {
        for (int z = minZ; z < minZ + height; z++)
            for (int x = minX; x < minX + width; x++) area.recordOre(x, z, oreName, amount);
    }
}
