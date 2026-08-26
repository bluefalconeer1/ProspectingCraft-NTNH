package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

public class SurveyMapStyleConfigTest {

    @After
    public void resetStyleDefaults() {
        SurveyMapStyleConfig.resetDefaults();
    }

    @Test
    public void parsesReadableRgbColorFormats() {
        assertEquals(Integer.valueOf(0x12ABEF), SurveyMapStyleConfig.parseColor("#12ABEF"));
        assertEquals(Integer.valueOf(0x12ABEF), SurveyMapStyleConfig.parseColor("0x12abef"));
        assertEquals(Integer.valueOf(0x12ABEF), SurveyMapStyleConfig.parseColor("12ABEF"));
        assertNull(SurveyMapStyleConfig.parseColor("#12345"));
        assertNull(SurveyMapStyleConfig.parseColor("not-a-color"));
    }

    @Test
    public void loadsCustomAndAdditionalOreColorsIgnoringNameCase() {
        Map<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("Iron", "#123456");
        colors.put("Coltan", "#ABCDEF");
        SurveyMapStyleConfig.configure(settings(100, 110, "#777777", "#B06DD6", colors));

        assertEquals(0x123456, SurveyMapStyleConfig.getOreColor("iron"));
        assertEquals(0xABCDEF, SurveyMapStyleConfig.getOreColor("COLTAN"));
    }

    @Test
    public void scalesOreOpacityWithoutChangingServerStrengthData() {
        SurveyMapStyleConfig.configure(settings(50, 90, "#102030", "#405060", Collections.<String, String>emptyMap()));

        assertEquals(36, SurveyMapStyleConfig.applyOreOpacity(72));
        assertEquals(108, SurveyMapStyleConfig.applyOreOpacity(216));
        assertEquals(90, SurveyMapStyleConfig.getUnsurveyedAlpha());
        assertEquals(0x102030, SurveyMapStyleConfig.getUnsurveyedColor());
        assertEquals(0x405060, SurveyMapStyleConfig.getOreColor("UnknownOre"));
    }

    @Test
    public void invalidColorsRetainSafeDefaults() {
        Map<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("Iron", "broken");
        SurveyMapStyleConfig.configure(settings(100, 110, "also-broken", "invalid", colors));

        assertEquals(0xD8C7B8, SurveyMapStyleConfig.getOreColor("Iron"));
        assertEquals(0x777777, SurveyMapStyleConfig.getUnsurveyedColor());
        assertEquals(0xB06DD6, SurveyMapStyleConfig.getOreColor("UnknownOre"));
    }

    private static SurveyMapStyleConfig.Settings settings(final int oreOpacityPercent, final int unsurveyedOpacity,
        final String unsurveyedColor, final String unknownOreColor, final Map<String, String> colors) {
        return new SurveyMapStyleConfig.Settings() {

            @Override
            public int getInteger(String category, String key, int defaultValue) {
                if (key.equals("oreOpacityPercent")) return oreOpacityPercent;
                if (key.equals("unsurveyedOpacity")) return unsurveyedOpacity;
                return defaultValue;
            }

            @Override
            public String getString(String category, String key, String defaultValue) {
                if (key.equals("unsurveyedColor")) return unsurveyedColor;
                if (key.equals("unknownOreColor")) return unknownOreColor;
                return defaultValue;
            }

            @Override
            public Map<String, String> getColorValues() {
                return colors;
            }
        };
    }
}
