package gcewing.prospecting.survey;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import net.minecraftforge.common.config.Property;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gcewing.prospecting.BaseConfiguration;

public final class SurveyMapStyleConfig {

    private static final Logger LOG = LogManager.getLogger("ProspectingCraft survey map style");
    private static final String STYLE_CATEGORY = "surveymapstyle";
    private static final String COLOR_CATEGORY = "surveymapcolors";
    private static final int DEFAULT_ORE_OPACITY_PERCENT = 100;
    private static final int DEFAULT_UNSURVEYED_COLOR = 0x777777;
    private static final int DEFAULT_UNSURVEYED_ALPHA = 110;
    private static final int DEFAULT_UNKNOWN_ORE_COLOR = 0xB06DD6;
    private static final Map<String, Integer> DEFAULT_ORE_COLORS = createDefaultOreColors();

    private static int oreOpacityPercent;
    private static int unsurveyedColor;
    private static int unsurveyedAlpha;
    private static int unknownOreColor;
    private static Map<String, Integer> oreColors;

    static {
        resetDefaults();
    }

    private SurveyMapStyleConfig() {}

    interface Settings {

        int getInteger(String category, String key, int defaultValue);

        String getString(String category, String key, String defaultValue);

        Map<String, String> getColorValues();
    }

    public static void configure(final BaseConfiguration config) {
        if (config == null) throw new IllegalArgumentException("Configuration must not be null");
        config.addCustomCategoryComment(
            STYLE_CATEGORY,
            "Client-side Ore Survey colors and opacity. Changes require a game restart.");
        config.addCustomCategoryComment(
            COLOR_CATEGORY,
            "Client-side ore colors in #RRGGBB format. Additional canonical ore names may be added.");
        for (Map.Entry<String, Integer> entry : DEFAULT_ORE_COLORS.entrySet())
            config.getString(COLOR_CATEGORY, entry.getKey(), formatColor(entry.getValue()));

        final Map<String, String> configuredColors = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Property> entry : config.getCategory(COLOR_CATEGORY)
            .entrySet())
            configuredColors.put(
                entry.getKey(),
                entry.getValue()
                    .getString());
        configure(new Settings() {

            @Override
            public int getInteger(String category, String key, int defaultValue) {
                return config.getInteger(category, key, defaultValue);
            }

            @Override
            public String getString(String category, String key, String defaultValue) {
                return config.getString(category, key, defaultValue);
            }

            @Override
            public Map<String, String> getColorValues() {
                return configuredColors;
            }
        });
    }

    static void configure(Settings settings) {
        if (settings == null) throw new IllegalArgumentException("Configuration settings must not be null");
        resetDefaults();
        oreOpacityPercent = bounded(
            settings.getInteger(STYLE_CATEGORY, "oreOpacityPercent", DEFAULT_ORE_OPACITY_PERCENT),
            10,
            200);
        unsurveyedAlpha = bounded(
            settings.getInteger(STYLE_CATEGORY, "unsurveyedOpacity", DEFAULT_UNSURVEYED_ALPHA),
            0,
            255);
        unsurveyedColor = configuredColor(
            "unsurveyedColor",
            settings.getString(STYLE_CATEGORY, "unsurveyedColor", formatColor(DEFAULT_UNSURVEYED_COLOR)),
            DEFAULT_UNSURVEYED_COLOR);
        unknownOreColor = configuredColor(
            "unknownOreColor",
            settings.getString(STYLE_CATEGORY, "unknownOreColor", formatColor(DEFAULT_UNKNOWN_ORE_COLOR)),
            DEFAULT_UNKNOWN_ORE_COLOR);

        Map<String, Integer> configuredOreColors = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
        configuredOreColors.putAll(DEFAULT_ORE_COLORS);
        for (Map.Entry<String, String> entry : settings.getColorValues()
            .entrySet()) {
            Integer parsed = parseColor(entry.getValue());
            if (parsed != null) configuredOreColors.put(entry.getKey(), parsed);
            else LOG.warn("Ignoring invalid Ore Survey color for " + entry.getKey() + ": " + entry.getValue());
        }
        oreColors = Collections.unmodifiableMap(configuredOreColors);
    }

    public static int getOreColor(String oreName) {
        Integer color = oreName == null ? null : oreColors.get(oreName);
        return color == null ? unknownOreColor : color;
    }

    public static int getUnsurveyedColor() {
        return unsurveyedColor;
    }

    public static int getUnsurveyedAlpha() {
        return unsurveyedAlpha;
    }

    public static int applyOreOpacity(int serverAlpha) {
        if (serverAlpha <= 0 || serverAlpha > 255)
            throw new IllegalArgumentException("Server ore alpha must be between 1 and 255");
        return bounded((int) Math.round(serverAlpha * oreOpacityPercent / 100.0), 1, 255);
    }

    static Integer parseColor(String value) {
        if (value == null) return null;
        String hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        else if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);
        if (hex.length() != 6) return null;
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static void resetDefaults() {
        oreOpacityPercent = DEFAULT_ORE_OPACITY_PERCENT;
        unsurveyedColor = DEFAULT_UNSURVEYED_COLOR;
        unsurveyedAlpha = DEFAULT_UNSURVEYED_ALPHA;
        unknownOreColor = DEFAULT_UNKNOWN_ORE_COLOR;
        Map<String, Integer> defaults = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
        defaults.putAll(DEFAULT_ORE_COLORS);
        oreColors = Collections.unmodifiableMap(defaults);
    }

    private static int configuredColor(String key, String value, int defaultColor) {
        Integer parsed = parseColor(value);
        if (parsed != null) return parsed;
        LOG.warn("Ignoring invalid Ore Survey " + key + ": " + value);
        return defaultColor;
    }

    private static String formatColor(int color) {
        return String.format(Locale.ROOT, "#%06X", color);
    }

    private static int bounded(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    private static Map<String, Integer> createDefaultOreColors() {
        Map<String, Integer> colors = new LinkedHashMap<String, Integer>();
        colors.put("Coal", 0x343A40);
        colors.put("Iron", 0xD8C7B8);
        colors.put("Gold", 0xFFD54F);
        colors.put("Copper", 0xD9824B);
        colors.put("Tin", 0xB0C4DE);
        colors.put("Nickel", 0xBFDFAF);
        colors.put("Lead", 0x586B8A);
        colors.put("Silver", 0xD9E7F2);
        colors.put("Redstone", 0xD32F2F);
        colors.put("Lapis", 0x3454D1);
        colors.put("Diamond", 0x45D9D0);
        colors.put("Emerald", 0x35B66A);
        colors.put("Uranium", 0x7CB342);
        colors.put("Thorium", 0x4F7A48);
        colors.put("Platinum", 0xE3E2F5);
        colors.put("Tungsten", 0x455A64);
        colors.put("Cobalt", 0x3568C0);
        colors.put("Titanium", 0x8D90A8);
        return Collections.unmodifiableMap(colors);
    }
}
