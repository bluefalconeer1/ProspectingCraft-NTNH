package gcewing.prospecting.survey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the tier-limited, ore-selection-filtered view that is safe to send to a survey owner.
 *
 * <p>
 * Cells are aligned to global coordinates. Each cell samples an inclusive square around its center, sums canonical
 * ore mass, then applies the selected area's tooltip and opacity limits.
 */
final class SurveyViewResolver {

    private SurveyViewResolver() {}

    static SurveyColumnSnapshot resolve(PlayerSurveyData data, int dimensionId, int x, int z,
        SurveyOreSelection selection) {
        return new ResolutionSession(data, dimensionId, selection).resolve(x, z);
    }

    static ResolutionSession begin(PlayerSurveyData data, int dimensionId, SurveyOreSelection selection) {
        return new ResolutionSession(data, dimensionId, selection);
    }

    private static SurveyColumnSnapshot resolveCoarse(SurveyArea area, int cellMinX, int cellMinZ,
        SurveyOreSelection selection) {
        SurveyTier tier = area.getTier();
        int cellSize = tier.getCellSize();
        int centerX = cellMinX + cellSize / 2;
        int centerZ = cellMinZ + cellSize / 2;
        int radius = tier.getReadingRadius();

        Map<String, Integer> oreMass = new HashMap<String, Integer>();
        for (int sampleZ = centerZ - radius; sampleZ <= centerZ + radius; sampleZ++)
            for (int sampleX = centerX - radius; sampleX <= centerX + radius; sampleX++) {
                SurveyColumn column = area.getOreBearingColumn(sampleX, sampleZ);
                if (column == null) continue;
                for (Map.Entry<String, Integer> ore : column.getOreCounts()
                    .entrySet()) if (selection.includes(ore.getKey())) add(oreMass, ore.getKey(), ore.getValue());
            }

        SurveyColumn visibleOres = new SurveyColumn();
        for (Map.Entry<String, Integer> ore : oreMass.entrySet()) visibleOres.addOre(ore.getKey(), ore.getValue());

        List<String> topOreNames = visibleOres.getTopOreNames(tier.getTooltipLimit());
        if (topOreNames.isEmpty()) return SurveyColumnSnapshot.SURVEYED_EMPTY;
        int dominantMass = oreMass.get(topOreNames.get(0));
        return SurveyColumnSnapshot.oreBearing(resolveBaseAlpha(tier, dominantMass), topOreNames);
    }

    private static int resolveBaseAlpha(SurveyTier tier, int dominantMass) {
        if (tier.getStrengthBands() == 0) return SurveyDisplayStrength.exactAlpha(dominantMass);
        int strengthBand = Math.min(tier.getStrengthBands(), 1 + (dominantMass - 1) / tier.getBlocksPerStrengthBand());
        return SurveyDisplayStrength.bandedAlpha(strengthBand, tier.getStrengthBands());
    }

    private static void add(Map<String, Integer> values, String oreName, int amount) {
        Integer previous = values.get(oreName);
        values.put(oreName, previous == null ? amount : previous + amount);
    }

    static final class ResolutionSession {

        private final PlayerSurveyData data;
        private final int dimensionId;
        private final SurveyOreSelection selection;
        private final Map<CellKey, SurveyColumnSnapshot> resolvedCells = new HashMap<CellKey, SurveyColumnSnapshot>();

        private ResolutionSession(PlayerSurveyData data, int dimensionId, SurveyOreSelection selection) {
            this.data = data;
            this.dimensionId = dimensionId;
            this.selection = selection;
        }

        SurveyColumnSnapshot resolve(int x, int z) {
            SurveyArea area = data.getSurveyAreaAt(dimensionId, x, z);
            if (area == null) return SurveyColumnSnapshot.UNSURVEYED;
            int cellSize = area.getTier()
                .getCellSize();
            int cellMinX = Math.floorDiv(x, cellSize) * cellSize;
            int cellMinZ = Math.floorDiv(z, cellSize) * cellSize;
            CellKey key = new CellKey(area.getSurveyId(), cellMinX, cellMinZ);
            SurveyColumnSnapshot resolved = resolvedCells.get(key);
            if (resolved == null) {
                resolved = resolveCoarse(area, cellMinX, cellMinZ, selection);
                resolvedCells.put(key, resolved);
            }
            return resolved;
        }

        int getComputedCellCount() {
            return resolvedCells.size();
        }
    }

    private static final class CellKey {

        private final UUID surveyId;
        private final int minX;
        private final int minZ;

        private CellKey(UUID surveyId, int minX, int minZ) {
            this.surveyId = surveyId;
            this.minX = minX;
            this.minZ = minZ;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof CellKey)) return false;
            CellKey other = (CellKey) object;
            return minX == other.minX && minZ == other.minZ && surveyId.equals(other.surveyId);
        }

        @Override
        public int hashCode() {
            int result = surveyId.hashCode();
            result = 31 * result + minX;
            result = 31 * result + minZ;
            return result;
        }
    }
}
