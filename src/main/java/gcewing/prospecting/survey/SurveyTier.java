package gcewing.prospecting.survey;

import gcewing.prospecting.BaseConfiguration;

public enum SurveyTier {

    BASE(0, 24, 16, 1, 3, 8),
    STEEL(1, 12, 6, 3, 5, 4),
    DESH(2, 1, 0, 5, 0, 0);

    private final int storageId;
    private final int defaultCellSize;
    private final int defaultReadingRadius;
    private final int defaultTooltipLimit;
    private final int defaultStrengthBands;
    private final int defaultBlocksPerStrengthBand;
    private int cellSize;
    private int readingRadius;
    private int tooltipLimit;
    private int strengthBands;
    private int blocksPerStrengthBand;

    interface IntegerSettings {

        int getInteger(String category, String key, int defaultValue);
    }

    SurveyTier(int storageId, int cellSize, int readingRadius, int tooltipLimit, int strengthBands,
        int blocksPerStrengthBand) {
        this.storageId = storageId;
        defaultCellSize = cellSize;
        defaultReadingRadius = readingRadius;
        defaultTooltipLimit = tooltipLimit;
        defaultStrengthBands = strengthBands;
        defaultBlocksPerStrengthBand = blocksPerStrengthBand;
        resetDefaults();
    }

    public int getStorageId() {
        return storageId;
    }

    public int getCellSize() {
        return cellSize;
    }

    public int getReadingRadius() {
        return readingRadius;
    }

    public int getTooltipLimit() {
        return tooltipLimit;
    }

    /** Zero means the current continuous opacity calculation. */
    public int getStrengthBands() {
        return strengthBands;
    }

    public int getBlocksPerStrengthBand() {
        return blocksPerStrengthBand;
    }

    public static void configure(BaseConfiguration config) {
        if (config == null) throw new IllegalArgumentException("Configuration must not be null");
        configure(new IntegerSettings() {

            @Override
            public int getInteger(String category, String key, int defaultValue) {
                return config.getInteger(category, key, defaultValue);
            }
        });
    }

    static void configure(IntegerSettings settings) {
        if (settings == null) throw new IllegalArgumentException("Configuration settings must not be null");
        BASE.loadConfig(settings);
        STEEL.loadConfig(settings);
        DESH.loadConfig(settings);
    }

    static void resetAllDefaults() {
        for (SurveyTier tier : values()) tier.resetDefaults();
    }

    public static SurveyTier fromStorageId(int storageId) {
        for (SurveyTier tier : values()) if (tier.storageId == storageId) return tier;
        throw new IllegalArgumentException("Unknown survey tier storage ID: " + storageId);
    }

    private void loadConfig(IntegerSettings config) {
        String prefix = name().toLowerCase();
        cellSize = bounded(config.getInteger("surveyAccuracy", prefix + "CellSize", defaultCellSize), 1, 64);
        readingRadius = bounded(
            config.getInteger("surveyAccuracy", prefix + "ReadingRadius", defaultReadingRadius),
            0,
            64);
        tooltipLimit = bounded(config.getInteger("surveyAccuracy", prefix + "TooltipLimit", defaultTooltipLimit), 1, 5);
        if (defaultStrengthBands == 0) {
            strengthBands = 0;
            blocksPerStrengthBand = 0;
            return;
        }
        strengthBands = bounded(
            config.getInteger("surveyAccuracy", prefix + "StrengthBands", defaultStrengthBands),
            1,
            10);
        blocksPerStrengthBand = bounded(
            config.getInteger("surveyAccuracy", prefix + "BlocksPerStrengthBand", defaultBlocksPerStrengthBand),
            1,
            65535);
    }

    private void resetDefaults() {
        cellSize = defaultCellSize;
        readingRadius = defaultReadingRadius;
        tooltipLimit = defaultTooltipLimit;
        strengthBands = defaultStrengthBands;
        blocksPerStrengthBand = defaultBlocksPerStrengthBand;
    }

    private static int bounded(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
