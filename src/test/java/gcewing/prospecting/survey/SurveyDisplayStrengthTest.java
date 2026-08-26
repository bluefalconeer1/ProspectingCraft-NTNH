package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SurveyDisplayStrengthTest {

    @Test
    public void exactStrengthPreservesExistingOpacityCurve() {
        assertEquals(72, SurveyDisplayStrength.exactAlpha(1));
        assertEquals(120, SurveyDisplayStrength.exactAlpha(5));
        assertEquals(216, SurveyDisplayStrength.exactAlpha(100));
    }

    @Test
    public void bandsSpanTheExistingOpacityRange() {
        assertEquals(72, SurveyDisplayStrength.bandedAlpha(1, 3));
        assertEquals(144, SurveyDisplayStrength.bandedAlpha(2, 3));
        assertEquals(216, SurveyDisplayStrength.bandedAlpha(3, 3));
    }
}
