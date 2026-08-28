package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class SurveyAreaIndexTest {

    @Test
    public void narrowsWidelySeparatedSurveysToTheLocalRegion() {
        SurveyAreaIndex index = new SurveyAreaIndex();
        SurveyArea target = null;
        for (int i = 0; i < 1000; i++) {
            SurveyArea area = surveyedArea(SurveyTier.BASE, 0, i * 1024, 0, 1, 1);
            index.add(area);
            if (i == 500) target = area;
        }

        assertEquals(1, index.getCandidateCount(0, 500 * 1024, 0));
        assertSame(target, index.getAt(0, 500 * 1024, 0));
    }

    @Test
    public void preservesHigherTierThenNewestSurveyPrecedence() {
        SurveyAreaIndex index = new SurveyAreaIndex();
        SurveyArea olderDesh = surveyedArea(SurveyTier.DESH, 0, 0, 0, 1, 1);
        SurveyArea newerBase = surveyedArea(SurveyTier.BASE, 0, 0, 0, 1, 1);
        SurveyArea newestDesh = surveyedArea(SurveyTier.DESH, 0, 0, 0, 1, 1);
        index.add(olderDesh);
        index.add(newerBase);

        assertSame(olderDesh, index.getAt(0, 0, 0));

        index.add(newestDesh);

        assertSame(newestDesh, index.getAt(0, 0, 0));
    }

    @Test
    public void indexesSurveyAcrossNegativeRegionBoundaries() {
        SurveyAreaIndex index = new SurveyAreaIndex();
        SurveyArea area = new SurveyArea(SurveyTier.BASE, 0, -129, -129, 128, 128);
        area.markSurveyed(-129, -129);
        area.markSurveyed(-2, -2);
        index.add(area);

        assertSame(area, index.getAt(0, -129, -129));
        assertSame(area, index.getAt(0, -2, -2));
        assertNull(index.getAt(0, 0, 0));
    }

    @Test
    public void removalClearsUuidAndSpatialLookups() {
        SurveyAreaIndex index = new SurveyAreaIndex();
        SurveyArea area = surveyedArea(SurveyTier.BASE, 0, 10, 20, 1, 1);
        index.add(area);

        assertSame(area, index.remove(area.getSurveyId()));
        assertNull(index.get(area.getSurveyId()));
        assertNull(index.getAt(0, 10, 20));
        assertNull(index.remove(area.getSurveyId()));
    }

    @Test
    public void oversizedAreaUsesBoundedFallbackWithoutChangingResults() {
        SurveyAreaIndex index = new SurveyAreaIndex();
        SurveyArea area = new SurveyArea(SurveyTier.BASE, 0, 0, 0, 2049, 128);
        area.markSurveyed(2048, 0);
        index.add(area);

        assertEquals(1, index.getCandidateCount(0, 2048, 0));
        assertSame(area, index.getAt(0, 2048, 0));
    }

    private static SurveyArea surveyedArea(SurveyTier tier, int dimensionId, int minX, int minZ, int width,
        int height) {
        SurveyArea area = new SurveyArea(tier, dimensionId, minX, minZ, width, height);
        area.markSurveyed(minX, minZ);
        return area;
    }
}
