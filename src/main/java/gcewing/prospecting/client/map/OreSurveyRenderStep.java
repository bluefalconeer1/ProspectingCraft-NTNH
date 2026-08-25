package gcewing.prospecting.client.map;

import com.gtnewhorizons.navigator.api.model.steps.UniversalRenderStep;
import com.gtnewhorizons.navigator.api.util.DrawUtils;

public class OreSurveyRenderStep extends UniversalRenderStep<OreSurveyChunkLocation> {

    private static final int CHUNK_WIDTH = 16;
    private static final int UNSURVEYED_COLOR = 0x777777;
    private static final int UNSURVEYED_ALPHA = 110;

    public OreSurveyRenderStep(OreSurveyChunkLocation location) {
        super(location);
        setSize(CHUNK_WIDTH);
    }

    @Override
    public void draw(double x, double y, float drawScale, double zoom) {
        if (isMinimap()) return;
        DrawUtils.drawRect(x, y, getAdjustedWidth(), getAdjustedHeight(), UNSURVEYED_COLOR, UNSURVEYED_ALPHA);
    }
}
