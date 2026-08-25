package gcewing.prospecting.survey;

public class SurveyChunkSnapshot {

    public static final int CHUNK_WIDTH = 16;
    public static final int COLUMN_COUNT = CHUNK_WIDTH * CHUNK_WIDTH;

    private final int dimensionId;
    private final int chunkX;
    private final int chunkZ;
    private final SurveyColumnSnapshot[] columns = new SurveyColumnSnapshot[COLUMN_COUNT];

    private SurveyChunkSnapshot(int dimensionId, int chunkX, int chunkZ) {
        this.dimensionId = dimensionId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public static SurveyChunkSnapshot from(PlayerSurveyData data, int dimensionId, int chunkX, int chunkZ) {
        if (data == null) throw new IllegalArgumentException("Player survey data must not be null");
        SurveyChunkSnapshot snapshot = new SurveyChunkSnapshot(dimensionId, chunkX, chunkZ);
        int minBlockX = chunkX << 4;
        int minBlockZ = chunkZ << 4;
        for (int localZ = 0; localZ < CHUNK_WIDTH; localZ++) for (int localX = 0; localX < CHUNK_WIDTH; localX++) {
            SurveyColumn column = data.getColumn(dimensionId, minBlockX + localX, minBlockZ + localZ);
            snapshot.columns[getIndex(localX, localZ)] = SurveyColumnSnapshot.from(column);
        }
        return snapshot;
    }

    public int getDimensionId() {
        return dimensionId;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public SurveyColumnSnapshot getColumn(int localX, int localZ) {
        return columns[getRequiredIndex(localX, localZ)];
    }

    private static int getRequiredIndex(int localX, int localZ) {
        if (localX < 0 || localX >= CHUNK_WIDTH || localZ < 0 || localZ >= CHUNK_WIDTH)
            throw new IllegalArgumentException("Local column coordinates are outside this chunk");
        return getIndex(localX, localZ);
    }

    private static int getIndex(int localX, int localZ) {
        return localZ * CHUNK_WIDTH + localX;
    }
}
