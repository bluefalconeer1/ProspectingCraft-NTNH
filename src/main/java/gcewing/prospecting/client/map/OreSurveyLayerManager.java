package gcewing.prospecting.client.map;

import javax.annotation.Nullable;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.LayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;

public class OreSurveyLayerManager extends LayerManager {

    public static final OreSurveyLayerManager INSTANCE = new OreSurveyLayerManager();

    private OreSurveyLayerManager() {
        super(OreSurveyButtonManager.INSTANCE);
    }

    @Override
    protected @Nullable LayerRenderer addLayerRenderer(LayerManager manager, SupportedMods mod) {
        if (mod != SupportedMods.XaeroWorldMap) return null;
        return new OreSurveyLayerRenderer(manager);
    }

    @Override
    protected ILocationProvider generateLocation(int chunkX, int chunkZ, int dimensionId) {
        return new OreSurveyChunkLocation(chunkX, chunkZ, dimensionId);
    }

    @Override
    public void updateElement(ILocationProvider location) {
        OreSurveyChunkLocation chunk = (OreSurveyChunkLocation) location;
        chunk.setSnapshot(
            SurveyChunkClientHandler.INSTANCE
                .getOrRequest(chunk.getDimensionId(), chunk.getChunkX(), chunk.getChunkZ()));
    }
}
