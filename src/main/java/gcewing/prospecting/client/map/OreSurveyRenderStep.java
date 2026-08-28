package gcewing.prospecting.client.map;

import com.gtnewhorizons.navigator.api.model.steps.UniversalRenderStep;
import com.gtnewhorizons.navigator.api.util.DrawUtils;

import gcewing.prospecting.survey.SurveyChunkSnapshot;
import gcewing.prospecting.survey.SurveyColumnSnapshot;

/** Renders one 16-by-16 survey snapshot on the full map and deliberately skips minimap rendering. */
public class OreSurveyRenderStep extends UniversalRenderStep<OreSurveyChunkLocation> {

    private static final int CHUNK_WIDTH = 16;
    private static final double COLUMN_WIDTH = 1.0;

    public OreSurveyRenderStep(OreSurveyChunkLocation location) {
        super(location);
        setSize(CHUNK_WIDTH);
    }

    @Override
    public void draw(double x, double y, float drawScale, double zoom) {
        if (isMinimap()) return;

        SurveyChunkSnapshot snapshot = location.getSnapshot();
        if (snapshot == null || snapshot.isFullyUnsurveyed()) {
            drawUnsurveyed(x, y, getAdjustedWidth(), getAdjustedHeight());
            return;
        }

        for (int localZ = 0; localZ < CHUNK_WIDTH; localZ++) for (int localX = 0; localX < CHUNK_WIDTH; localX++) {
            drawColumn(x + localX, y + localZ, snapshot.getColumn(localX, localZ));
        }
    }

    private static void drawColumn(double x, double y, SurveyColumnSnapshot column) {
        switch (column.getState()) {
            case UNSURVEYED:
                drawUnsurveyed(x, y, COLUMN_WIDTH, COLUMN_WIDTH);
                break;
            case SURVEYED_EMPTY:
                break;
            case ORE_BEARING:
                DrawUtils.drawRect(
                    x,
                    y,
                    COLUMN_WIDTH,
                    COLUMN_WIDTH,
                    SurveyMapStyle.getOreColor(column.getDominantOreName()),
                    SurveyMapStyle.getRenderedOreAlpha(column.getOreAlpha()));
                break;
            default:
                throw new IllegalStateException("Unknown survey column state: " + column.getState());
        }
    }

    private static void drawUnsurveyed(double x, double y, double width, double height) {
        DrawUtils
            .drawRect(x, y, width, height, SurveyMapStyle.getUnsurveyedColor(), SurveyMapStyle.getUnsurveyedAlpha());
    }

    SurveyColumnSnapshot getHoveredOreColumn(double mouseX, double mouseZ) {
        return OreSurveyHoverResolver.findOreColumn(location.getSnapshot(), getX(), getY(), mouseX, mouseZ);
    }
}
