package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class SurveyAreaTest {

    @Test
    public void distinguishesUnsurveyedFromSurveyedEmptyColumns() {
        SurveyArea area = new SurveyArea(0, 100, 200, 128, 128);

        assertFalse(area.isSurveyed(100, 200));
        assertNull(area.getColumn(100, 200));

        area.markSurveyed(100, 200);

        assertTrue(area.isSurveyed(100, 200));
        assertTrue(
            area.getColumn(100, 200)
                .isEmpty());
    }

    @Test
    public void mergesOreCountsAndReturnsDominantAndTopFiveNames() {
        SurveyArea area = new SurveyArea(0, 0, 0, 1, 1);
        area.recordOre(0, 0, "Nickel", 2);
        area.recordOre(0, 0, "Nickel", 3);
        area.recordOre(0, 0, "Iron", 4);
        area.recordOre(0, 0, "Copper", 4);
        area.recordOre(0, 0, "Tin", 3);
        area.recordOre(0, 0, "Gold", 2);
        area.recordOre(0, 0, "Silver", 1);

        SurveyColumn column = area.getColumn(0, 0);
        assertEquals(5, column.getOreCount("Nickel"));
        assertEquals("Nickel", column.getDominantOreName());
        assertEquals(Arrays.asList("Nickel", "Copper", "Iron", "Tin", "Gold"), column.getTopOreNames(5));
    }

    @Test
    public void clearingAColumnLeavesItSurveyedButEmpty() {
        SurveyArea area = new SurveyArea(0, 0, 0, 1, 1);
        area.recordOre(0, 0, "Iron", 1);

        area.markSurveyed(0, 0);

        assertTrue(area.isSurveyed(0, 0));
        assertTrue(
            area.getColumn(0, 0)
                .isEmpty());
    }

    @Test
    public void aggregationLookupDoesNotAllocateSurveyedEmptyColumns() {
        SurveyArea area = new SurveyArea(0, 0, 0, 1, 1);
        area.markSurveyed(0, 0);

        assertNull(area.getOreBearingColumn(0, 0));

        area.recordOre(0, 0, "Iron", 1);

        assertSame(area.getColumn(0, 0), area.getOreBearingColumn(0, 0));
    }
}
