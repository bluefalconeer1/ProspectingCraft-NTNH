package gcewing.prospecting.client.map;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SurveyMapStyleTest {

    @Test
    public void usesFixedColorsForKnownOres() {
        assertEquals(0x999999, SurveyMapStyle.getOreColor("Iron"));
        assertEquals(0xC59334, SurveyMapStyle.getOreColor("Gold"));
        assertEquals(0xA28F87, SurveyMapStyle.getOreColor("Nickel"));
        assertEquals(0x5E98C5, SurveyMapStyle.getOreColor("HeeEndium"));
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
