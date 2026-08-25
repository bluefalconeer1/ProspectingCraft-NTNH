package gcewing.prospecting.client.map;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SurveyMapStyleTest {

    @Test
    public void usesFixedColorsForKnownOres() {
        assertEquals(0xD8C7B8, SurveyMapStyle.getOreColor("Iron"));
        assertEquals(0xFFD54F, SurveyMapStyle.getOreColor("Gold"));
        assertEquals(0xBFDFAF, SurveyMapStyle.getOreColor("Nickel"));
    }

    @Test
    public void usesOneFixedFallbackForUnknownOres() {
        assertEquals(SurveyMapStyle.getOreColor("Unobtainium"), SurveyMapStyle.getOreColor("AnotherUnknownOre"));
    }

    @Test
    public void makesDenserColumnsMoreOpaqueAndCapsTheResult() {
        assertEquals(72, SurveyMapStyle.getOreAlpha(1));
        assertEquals(120, SurveyMapStyle.getOreAlpha(5));
        assertEquals(216, SurveyMapStyle.getOreAlpha(100));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAnEmptyOreColumn() {
        SurveyMapStyle.getOreAlpha(0);
    }
}
