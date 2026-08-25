package gcewing.prospecting.survey;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SurveyArea {

    private final int dimensionId;
    private final int minX;
    private final int minZ;
    private final int width;
    private final int height;
    private final BitSet surveyedColumns;
    private final Map<Integer, SurveyColumn> oreColumns = new HashMap<Integer, SurveyColumn>();

    public SurveyArea(int dimensionId, int minX, int minZ, int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Survey dimensions must be positive");
        this.dimensionId = dimensionId;
        this.minX = minX;
        this.minZ = minZ;
        this.width = width;
        this.height = height;
        surveyedColumns = new BitSet(width * height);
    }

    public int getDimensionId() {
        return dimensionId;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void markSurveyed(int x, int z) {
        int index = getRequiredIndex(x, z);
        surveyedColumns.set(index);
        oreColumns.remove(index);
    }

    public void recordOre(int x, int z, String oreName, int amount) {
        int index = getRequiredIndex(x, z);
        surveyedColumns.set(index);
        SurveyColumn column = oreColumns.get(index);
        if (column == null) {
            column = new SurveyColumn();
            oreColumns.put(index, column);
        }
        column.addOre(oreName, amount);
    }

    public boolean isSurveyed(int x, int z) {
        int index = getIndex(x, z);
        return index >= 0 && surveyedColumns.get(index);
    }

    public SurveyColumn getColumn(int x, int z) {
        int index = getIndex(x, z);
        if (index < 0 || !surveyedColumns.get(index)) return null;
        SurveyColumn column = oreColumns.get(index);
        return column == null ? new SurveyColumn() : column;
    }

    BitSet copySurveyedColumns() {
        return (BitSet) surveyedColumns.clone();
    }

    Map<Integer, SurveyColumn> getOreColumns() {
        return Collections.unmodifiableMap(oreColumns);
    }

    private int getRequiredIndex(int x, int z) {
        int index = getIndex(x, z);
        if (index < 0) throw new IllegalArgumentException("Coordinates are outside this survey area");
        return index;
    }

    private int getIndex(int x, int z) {
        int localX = x - minX;
        int localZ = z - minZ;
        if (localX < 0 || localX >= width || localZ < 0 || localZ >= height) return -1;
        return localZ * width + localX;
    }
}
