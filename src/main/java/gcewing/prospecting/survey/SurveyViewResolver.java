package gcewing.prospecting.survey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds the tier-limited view that is safe to send to a survey owner. */
final class SurveyViewResolver {

    private SurveyViewResolver() {}

    static SurveyColumnSnapshot resolve(PlayerSurveyData data, int dimensionId, int x, int z,
        SurveyOreSelection selection) {
        SurveyArea area = data.getSurveyAreaAt(dimensionId, x, z);
        if (area == null) return SurveyColumnSnapshot.UNSURVEYED;
        SurveyTier tier = area.getTier();
        if (tier == SurveyTier.DESH)
            return SurveyColumnSnapshot.from(area.getColumn(x, z), selection, tier.getTooltipLimit());
        return resolveCoarse(area, x, z, selection);
    }

    private static SurveyColumnSnapshot resolveCoarse(SurveyArea area, int x, int z, SurveyOreSelection selection) {
        SurveyTier tier = area.getTier();
        int cellSize = tier.getCellSize();
        int cellMinX = Math.floorDiv(x, cellSize) * cellSize;
        int cellMinZ = Math.floorDiv(z, cellSize) * cellSize;
        int centerX = cellMinX + cellSize / 2;
        int centerZ = cellMinZ + cellSize / 2;
        int radius = tier.getReadingRadius();

        Map<String, Integer> oreMass = new HashMap<String, Integer>();
        for (int sampleZ = centerZ - radius; sampleZ <= centerZ + radius; sampleZ++)
            for (int sampleX = centerX - radius; sampleX <= centerX + radius; sampleX++) {
                SurveyColumn column = area.getColumn(sampleX, sampleZ);
                if (column == null) continue;
                for (Map.Entry<String, Integer> ore : column.getOreCounts()
                    .entrySet()) if (selection.includes(ore.getKey())) add(oreMass, ore.getKey(), ore.getValue());
            }

        SurveyColumn visibleOres = new SurveyColumn();
        for (Map.Entry<String, Integer> ore : oreMass.entrySet()) visibleOres.addOre(ore.getKey(), ore.getValue());

        List<String> topOreNames = visibleOres.getTopOreNames(tier.getTooltipLimit());
        if (topOreNames.isEmpty()) return SurveyColumnSnapshot.SURVEYED_EMPTY;
        int dominantMass = oreMass.get(topOreNames.get(0));
        int strengthBand = Math.min(tier.getStrengthBands(), 1 + (dominantMass - 1) / tier.getBlocksPerStrengthBand());
        int baseAlpha = SurveyDisplayStrength.bandedAlpha(strengthBand, tier.getStrengthBands());
        int displayAlpha = SurveyCellMask.roundedCornerAlpha(
            baseAlpha,
            cellSize,
            x - cellMinX,
            z - cellMinZ,
            tier.getCornerRadius(),
            tier.getCornerFeatherWidth(),
            tier.getCornerMinimumAlphaPercent());
        return SurveyColumnSnapshot.oreBearing(displayAlpha, topOreNames);
    }

    private static void add(Map<String, Integer> values, String oreName, int amount) {
        Integer previous = values.get(oreName);
        values.put(oreName, previous == null ? amount : previous + amount);
    }
}
