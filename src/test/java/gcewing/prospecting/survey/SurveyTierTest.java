package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

public class SurveyTierTest {

    @After
    public void resetTierDefaults() {
        SurveyTier.resetAllDefaults();
    }

    @Test
    public void developmentDefaultsDescribeIncreasingPrecision() {
        assertEquals(24, SurveyTier.BASE.getCellSize());
        assertEquals(16, SurveyTier.BASE.getReadingRadius());
        assertEquals(1, SurveyTier.BASE.getTooltipLimit());
        assertEquals(3, SurveyTier.BASE.getStrengthBands());
        assertEquals(8, SurveyTier.BASE.getBlocksPerStrengthBand());

        assertEquals(12, SurveyTier.STEEL.getCellSize());
        assertEquals(6, SurveyTier.STEEL.getReadingRadius());
        assertEquals(3, SurveyTier.STEEL.getTooltipLimit());
        assertEquals(5, SurveyTier.STEEL.getStrengthBands());
        assertEquals(4, SurveyTier.STEEL.getBlocksPerStrengthBand());

        assertEquals(1, SurveyTier.DESH.getCellSize());
        assertEquals(0, SurveyTier.DESH.getReadingRadius());
        assertEquals(5, SurveyTier.DESH.getTooltipLimit());
        assertEquals(0, SurveyTier.DESH.getStrengthBands());
        assertEquals(0, SurveyTier.DESH.getBlocksPerStrengthBand());
    }

    @Test
    public void storageIdsDoNotDependOnEnumNames() {
        for (SurveyTier tier : SurveyTier.values()) assertEquals(tier, SurveyTier.fromStorageId(tier.getStorageId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownStorageIds() {
        SurveyTier.fromStorageId(99);
    }

    @Test
    public void deshCellSizeAndRadiusAreConfigurable() {
        SurveyTier.configure(deshSettings(4, 3));

        assertEquals(4, SurveyTier.DESH.getCellSize());
        assertEquals(3, SurveyTier.DESH.getReadingRadius());
        assertEquals(0, SurveyTier.DESH.getStrengthBands());
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
}
