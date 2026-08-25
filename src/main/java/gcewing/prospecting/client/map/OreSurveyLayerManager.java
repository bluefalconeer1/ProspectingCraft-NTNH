package gcewing.prospecting.client.map;

import javax.annotation.Nullable;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.LayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.layers.UniversalLayerRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;

public class OreSurveyLayerManager extends LayerManager {

    public static final OreSurveyLayerManager INSTANCE = new OreSurveyLayerManager();

    private OreSurveyLayerManager() {
        super(OreSurveyButtonManager.INSTANCE);
    }

    @Override
    protected @Nullable LayerRenderer addLayerRenderer(LayerManager manager, SupportedMods mod) {
        if (mod != SupportedMods.XaeroWorldMap) return null;
        return new UniversalLayerRenderer(manager)
            .withRenderStep(location -> new OreSurveyRenderStep((OreSurveyChunkLocation) location));
    }

    @Override
    protected ILocationProvider generateLocation(int chunkX, int chunkZ, int dimensionId) {
        return new OreSurveyChunkLocation(chunkX, chunkZ, dimensionId);
    }
}
