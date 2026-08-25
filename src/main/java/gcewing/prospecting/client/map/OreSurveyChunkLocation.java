package gcewing.prospecting.client.map;

import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;

public class OreSurveyChunkLocation implements ILocationProvider {

    private final int dimensionId;
    private final int minBlockX;
    private final int minBlockZ;

    public OreSurveyChunkLocation(int chunkX, int chunkZ, int dimensionId) {
        this.dimensionId = dimensionId;
        minBlockX = chunkX << 4;
        minBlockZ = chunkZ << 4;
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
