package gcewing.prospecting.survey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Transient spatial and UUID index over a player's persistent survey areas. */
final class SurveyAreaIndex {

    private static final int CELL_SIZE = 128;
    private static final int MAX_INDEX_CELLS_PER_AREA = 16;

    private final Map<UUID, IndexedArea> areasById = new HashMap<UUID, IndexedArea>();
    private final Map<CellKey, List<IndexedArea>> areasByCell = new HashMap<CellKey, List<IndexedArea>>();
    private final List<IndexedArea> fallbackAreas = new ArrayList<IndexedArea>();
    private long nextInsertionOrder;

    public void add(SurveyArea area) {
        if (area == null) throw new IllegalArgumentException("Survey area must not be null");
        if (areasById.containsKey(area.getSurveyId()))
            throw new IllegalArgumentException("Survey ID is already present: " + area.getSurveyId());

        IndexedArea indexedArea = new IndexedArea(area, nextInsertionOrder++);
        areasById.put(area.getSurveyId(), indexedArea);
        CellRange range = CellRange.forArea(area);
        if (range.getCellCount() > MAX_INDEX_CELLS_PER_AREA) {
            indexedArea.fallback = true;
            fallbackAreas.add(indexedArea);
            return;
        }
        for (long cellZ = range.minCellZ; cellZ <= range.maxCellZ; cellZ++)
            for (long cellX = range.minCellX; cellX <= range.maxCellX; cellX++) {
                CellKey key = new CellKey(area.getDimensionId(), cellX, cellZ);
                List<IndexedArea> cellAreas = areasByCell.get(key);
                if (cellAreas == null) {
                    cellAreas = new ArrayList<IndexedArea>();
                    areasByCell.put(key, cellAreas);
                }
                cellAreas.add(indexedArea);
                indexedArea.cells.add(key);
            }
    }

    public SurveyArea get(UUID surveyId) {
        IndexedArea indexedArea = surveyId == null ? null : areasById.get(surveyId);
        return indexedArea == null ? null : indexedArea.area;
    }

    public SurveyArea remove(UUID surveyId) {
        IndexedArea indexedArea = surveyId == null ? null : areasById.remove(surveyId);
        if (indexedArea == null) return null;
        if (indexedArea.fallback) fallbackAreas.remove(indexedArea);
        else for (CellKey key : indexedArea.cells) {
            List<IndexedArea> cellAreas = areasByCell.get(key);
            cellAreas.remove(indexedArea);
            if (cellAreas.isEmpty()) areasByCell.remove(key);
        }
        return indexedArea.area;
    }

    public SurveyArea getAt(int dimensionId, int x, int z) {
        CellKey key = new CellKey(dimensionId, Math.floorDiv(x, CELL_SIZE), Math.floorDiv(z, CELL_SIZE));
        SurveyArea bestArea = null;
        long bestInsertionOrder = Long.MIN_VALUE;
        List<IndexedArea> candidates = areasByCell.get(key);
        if (candidates != null) for (IndexedArea candidate : candidates) {
            if (isBetterCandidate(candidate, x, z, bestArea, bestInsertionOrder)) {
                bestArea = candidate.area;
                bestInsertionOrder = candidate.insertionOrder;
            }
        }
        for (IndexedArea candidate : fallbackAreas) {
            if (candidate.area.getDimensionId() == dimensionId
                && isBetterCandidate(candidate, x, z, bestArea, bestInsertionOrder)) {
                bestArea = candidate.area;
                bestInsertionOrder = candidate.insertionOrder;
            }
        }
        return bestArea;
    }

    int getCandidateCount(int dimensionId, int x, int z) {
        List<IndexedArea> candidates = areasByCell
            .get(new CellKey(dimensionId, Math.floorDiv(x, CELL_SIZE), Math.floorDiv(z, CELL_SIZE)));
        return (candidates == null ? 0 : candidates.size()) + fallbackAreas.size();
    }

    private static boolean isBetterCandidate(IndexedArea candidate, int x, int z, SurveyArea bestArea,
        long bestInsertionOrder) {
        SurveyArea area = candidate.area;
        if (!area.isSurveyed(x, z)) return false;
        if (bestArea == null) return true;
        int tierComparison = Integer.compare(
            area.getTier()
                .getStorageId(),
            bestArea.getTier()
                .getStorageId());
        return tierComparison > 0 || tierComparison == 0 && candidate.insertionOrder > bestInsertionOrder;
    }

    private static final class IndexedArea {

        private final SurveyArea area;
        private final long insertionOrder;
        private final List<CellKey> cells = new ArrayList<CellKey>();
        private boolean fallback;

        private IndexedArea(SurveyArea area, long insertionOrder) {
            this.area = area;
            this.insertionOrder = insertionOrder;
        }
    }

    private static final class CellRange {

        private final long minCellX;
        private final long minCellZ;
        private final long maxCellX;
        private final long maxCellZ;

        private CellRange(long minCellX, long minCellZ, long maxCellX, long maxCellZ) {
            this.minCellX = minCellX;
            this.minCellZ = minCellZ;
            this.maxCellX = maxCellX;
            this.maxCellZ = maxCellZ;
        }

        private static CellRange forArea(SurveyArea area) {
            long maxX = (long) area.getMinX() + area.getWidth() - 1;
            long maxZ = (long) area.getMinZ() + area.getHeight() - 1;
            return new CellRange(
                Math.floorDiv((long) area.getMinX(), CELL_SIZE),
                Math.floorDiv((long) area.getMinZ(), CELL_SIZE),
                Math.floorDiv(maxX, CELL_SIZE),
                Math.floorDiv(maxZ, CELL_SIZE));
        }

        private long getCellCount() {
            return (maxCellX - minCellX + 1) * (maxCellZ - minCellZ + 1);
        }
    }

    private static final class CellKey {

        private final int dimensionId;
        private final long cellX;
        private final long cellZ;

        private CellKey(int dimensionId, long cellX, long cellZ) {
            this.dimensionId = dimensionId;
            this.cellX = cellX;
            this.cellZ = cellZ;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof CellKey)) return false;
            CellKey other = (CellKey) object;
            return dimensionId == other.dimensionId && cellX == other.cellX && cellZ == other.cellZ;
        }

        @Override
        public int hashCode() {
            int result = dimensionId;
            result = 31 * result + (int) (cellX ^ cellX >>> 32);
            result = 31 * result + (int) (cellZ ^ cellZ >>> 32);
            return result;
        }
    }
}
