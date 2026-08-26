package gcewing.prospecting.survey;

public final class SurveyDisplayStrength {

    public static final int MINIMUM_ALPHA = 72;
    public static final int MAXIMUM_ALPHA = 216;

    private static final int ALPHA_PER_ADDITIONAL_BLOCK = 12;

    private SurveyDisplayStrength() {}

    public static int exactAlpha(int oreCount) {
        if (oreCount <= 0) throw new IllegalArgumentException("Ore count must be positive");
        long alpha = MINIMUM_ALPHA + (long) (oreCount - 1) * ALPHA_PER_ADDITIONAL_BLOCK;
        return (int) Math.min(alpha, MAXIMUM_ALPHA);
    }

    public static int bandedAlpha(int band, int bandCount) {
        if (bandCount <= 0) throw new IllegalArgumentException("Band count must be positive");
        if (band <= 0 || band > bandCount) throw new IllegalArgumentException("Band is outside the configured range");
        if (bandCount == 1) return MAXIMUM_ALPHA;
        int alphaRange = MAXIMUM_ALPHA - MINIMUM_ALPHA;
        return MINIMUM_ALPHA + (int) Math.round((double) (band - 1) * alphaRange / (bandCount - 1));
    }
}
