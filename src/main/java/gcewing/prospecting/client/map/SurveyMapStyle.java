package gcewing.prospecting.client.map;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class SurveyMapStyle {

    public static final int UNSURVEYED_COLOR = 0x777777;
    public static final int UNSURVEYED_ALPHA = 110;

    private static final int UNKNOWN_ORE_COLOR = 0xB06DD6;
    private static final int MIN_ORE_ALPHA = 72;
    private static final int ORE_ALPHA_PER_ADDITIONAL_BLOCK = 12;
    private static final int MAX_ORE_ALPHA = 216;
    private static final Map<String, Integer> ORE_COLORS = createOreColors();

    private SurveyMapStyle() {}

    public static int getOreColor(String oreName) {
        Integer color = ORE_COLORS.get(oreName);
        return color == null ? UNKNOWN_ORE_COLOR : color;
    }

    public static int getOreAlpha(int dominantOreCount) {
        if (dominantOreCount <= 0) throw new IllegalArgumentException("Dominant ore count must be positive");
        long alpha = MIN_ORE_ALPHA + (long) (dominantOreCount - 1) * ORE_ALPHA_PER_ADDITIONAL_BLOCK;
        return (int) Math.min(alpha, MAX_ORE_ALPHA);
    }

    private static Map<String, Integer> createOreColors() {
        Map<String, Integer> colors = new HashMap<String, Integer>();
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
