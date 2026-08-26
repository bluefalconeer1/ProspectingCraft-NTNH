package gcewing.prospecting.survey;

import gcewing.prospecting.BaseConfiguration;

public enum SurveyTier {

    BASE(0, 24, 16, 1, 3, 8, 6, 3, 25),
    STEEL(1, 12, 6, 3, 5, 4, 3, 2, 25),
    DESH(2, 1, 0, 5, 0, 0, 0, 0, 100);

    private final int storageId;
    private final int defaultCellSize;
    private final int defaultReadingRadius;
    private final int defaultTooltipLimit;
    private final int defaultStrengthBands;
    private final int defaultBlocksPerStrengthBand;
    private final int defaultCornerRadius;
    private final int defaultCornerFeatherWidth;
    private final int defaultCornerMinimumAlphaPercent;
    private int cellSize;
    private int readingRadius;
    private int tooltipLimit;
    private int strengthBands;
    private int blocksPerStrengthBand;
    private int cornerRadius;
    private int cornerFeatherWidth;
    private int cornerMinimumAlphaPercent;

    SurveyTier(int storageId, int cellSize, int readingRadius, int tooltipLimit, int strengthBands,
        int blocksPerStrengthBand, int cornerRadius, int cornerFeatherWidth, int cornerMinimumAlphaPercent) {
        this.storageId = storageId;
        defaultCellSize = cellSize;
        defaultReadingRadius = readingRadius;
        defaultTooltipLimit = tooltipLimit;
        defaultStrengthBands = strengthBands;
        defaultBlocksPerStrengthBand = blocksPerStrengthBand;
        defaultCornerRadius = cornerRadius;
        defaultCornerFeatherWidth = cornerFeatherWidth;
        defaultCornerMinimumAlphaPercent = cornerMinimumAlphaPercent;
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

    public int getCornerRadius() {
        return cornerRadius;
    }

    public int getCornerFeatherWidth() {
        return cornerFeatherWidth;
    }

    public int getCornerMinimumAlphaPercent() {
        return cornerMinimumAlphaPercent;
    }

    public static void configure(BaseConfiguration config) {
        if (config == null) throw new IllegalArgumentException("Configuration must not be null");
        BASE.loadConfig(config);
        STEEL.loadConfig(config);
        DESH.resetDefaults();
    }

    public static SurveyTier fromStorageId(int storageId) {
        for (SurveyTier tier : values()) if (tier.storageId == storageId) return tier;
        throw new IllegalArgumentException("Unknown survey tier storage ID: " + storageId);
    }

    private void loadConfig(BaseConfiguration config) {
        String prefix = name().toLowerCase();
        cellSize = bounded(config.getInteger("surveyAccuracy", prefix + "CellSize", defaultCellSize), 1, 64);
        readingRadius = bounded(
            config.getInteger("surveyAccuracy", prefix + "ReadingRadius", defaultReadingRadius),
            0,
            64);
        tooltipLimit = bounded(config.getInteger("surveyAccuracy", prefix + "TooltipLimit", defaultTooltipLimit), 1, 5);
        strengthBands = bounded(
            config.getInteger("surveyAccuracy", prefix + "StrengthBands", defaultStrengthBands),
            1,
            10);
        blocksPerStrengthBand = bounded(
            config.getInteger("surveyAccuracy", prefix + "BlocksPerStrengthBand", defaultBlocksPerStrengthBand),
            1,
            65535);
        cornerRadius = bounded(
            config.getInteger("surveyAccuracy", prefix + "CornerRadius", defaultCornerRadius),
            0,
            cellSize / 2);
        cornerFeatherWidth = cornerRadius == 0 ? 0
            : bounded(
                config.getInteger("surveyAccuracy", prefix + "CornerFeatherWidth", defaultCornerFeatherWidth),
                1,
                cornerRadius);
        cornerMinimumAlphaPercent = bounded(
            config.getInteger("surveyAccuracy", prefix + "CornerMinimumAlphaPercent", defaultCornerMinimumAlphaPercent),
            1,
            100);
    }

    private void resetDefaults() {
        cellSize = defaultCellSize;
        readingRadius = defaultReadingRadius;
        tooltipLimit = defaultTooltipLimit;
        strengthBands = defaultStrengthBands;
        blocksPerStrengthBand = defaultBlocksPerStrengthBand;
        cornerRadius = defaultCornerRadius;
        cornerFeatherWidth = defaultCornerFeatherWidth;
        cornerMinimumAlphaPercent = defaultCornerMinimumAlphaPercent;
    }

    private static int bounded(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
