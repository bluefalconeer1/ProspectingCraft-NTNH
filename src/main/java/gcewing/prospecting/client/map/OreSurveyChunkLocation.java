package gcewing.prospecting.client.map;

import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;

import gcewing.prospecting.survey.SurveyChunkSnapshot;

public class OreSurveyChunkLocation implements ILocationProvider {

    private final int dimensionId;
    private final int chunkX;
    private final int chunkZ;
    private final int minBlockX;
    private final int minBlockZ;
    private SurveyChunkSnapshot snapshot;

    public OreSurveyChunkLocation(int chunkX, int chunkZ, int dimensionId) {
        this.dimensionId = dimensionId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        minBlockX = chunkX << 4;
        minBlockZ = chunkZ << 4;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public SurveyChunkSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(SurveyChunkSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public int getDimensionId() {
        return dimensionId;
    }

    @Override
    public double getBlockX() {
        return minBlockX + 0.5;
    }

    @Override
    public double getBlockZ() {
        return minBlockZ + 0.5;
    }
}
