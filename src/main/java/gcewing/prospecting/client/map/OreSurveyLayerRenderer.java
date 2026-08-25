package gcewing.prospecting.client.map;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;

import com.gtnewhorizons.navigator.api.model.layers.InteractableLayer;
import com.gtnewhorizons.navigator.api.model.layers.LayerManager;
import com.gtnewhorizons.navigator.api.model.layers.UniversalLayerRenderer;
import com.gtnewhorizons.navigator.api.model.steps.UniversalRenderStep;

import gcewing.prospecting.survey.SurveyColumnSnapshot;

/** Supplies passive full-map hover text without waypoint or click behavior. */
public class OreSurveyLayerRenderer extends UniversalLayerRenderer implements InteractableLayer {

    private List<String> hoveredOreNames = Collections.emptyList();

    public OreSurveyLayerRenderer(LayerManager manager) {
        super(manager);
        withRenderStep(location -> new OreSurveyRenderStep((OreSurveyChunkLocation) location));
    }

    @Override
    public void onMouseMove(int mouseX, int mouseY) {
        hoveredOreNames = Collections.emptyList();
        List<UniversalRenderStep<?>> steps = getRenderSteps();
        for (int i = steps.size() - 1; i >= 0; i--) {
            UniversalRenderStep<?> step = steps.get(i);
            if (!(step instanceof OreSurveyRenderStep)) continue;
            SurveyColumnSnapshot column = ((OreSurveyRenderStep) step).getHoveredOreColumn(mouseX, mouseY);
            if (column == null) continue;
            hoveredOreNames = column.getTopOreNames();
            return;
        }
    }

    @Override
    public boolean onMapClick(boolean rightClick, int mouseX, int mouseY, int mapX, int mapZ) {
        return false;
    }

    @Override
    public List<String> getTooltip() {
        return hoveredOreNames;
    }

    @Override
    public void drawCustomTooltip(FontRenderer fontRenderer, int mouseX, int mouseY, int displayWidth,
        int displayHeight) {}

    @Override
    public boolean onKeyPressed(int keyCode) {
        return false;
    }
}
