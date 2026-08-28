package gcewing.prospecting.client.map;

import gcewing.prospecting.survey.SurveyDisplayStrength;
import gcewing.prospecting.survey.SurveyMapStyleConfig;

/** Client rendering facade for configured colors and opacity scaling. */
public final class SurveyMapStyle {

    private SurveyMapStyle() {}

    public static int getOreColor(String oreName) {
        return SurveyMapStyleConfig.getOreColor(oreName);
    }

    public static int getOreAlpha(int dominantOreCount) {
        return getRenderedOreAlpha(SurveyDisplayStrength.exactAlpha(dominantOreCount));
    }

    public static int getRenderedOreAlpha(int serverAlpha) {
        return SurveyMapStyleConfig.applyOreOpacity(serverAlpha);
    }

    public static int getUnsurveyedColor() {
        return SurveyMapStyleConfig.getUnsurveyedColor();
    }

    public static int getUnsurveyedAlpha() {
        return SurveyMapStyleConfig.getUnsurveyedAlpha();
    }
}
