package gcewing.prospecting.survey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class SurveyOreSelectionTest {

    @Test
    public void showAllIncludesEveryCanonicalOreName() {
        assertTrue(
            SurveyOreSelection.showAll()
                .includes("Lignite"));
        assertTrue(
            SurveyOreSelection.showAll()
                .includes("Iron"));
    }

    @Test
    public void customSelectionMatchesWithoutCaseOrWhitespaceSensitivity() {
        SurveyOreSelection selection = SurveyOreSelection.custom(Arrays.asList(" iron ", "GOLD"));

        assertTrue(selection.includes("Iron"));
        assertTrue(selection.includes("gold"));
        assertFalse(selection.includes("Lignite"));
    }

    @Test
    public void emptyCustomSelectionDisplaysNoOres() {
        SurveyOreSelection selection = SurveyOreSelection.custom(Arrays.asList("", "   ", null));

        assertFalse(selection.includes("Iron"));
    }

    @Test
    public void oldNetherSelectionMatchesGroupedBaseOre() {
        SurveyOreSelection selection = SurveyOreSelection.custom(Arrays.asList("NetherUranium"));

        assertTrue(selection.includes("Uranium"));
        assertTrue(selection.includes("NetherUranium"));
    }

    @Test
    public void volcanicIsExcludedFromAllAndCustomSelections() {
        assertFalse(
            SurveyOreSelection.showAll()
                .includes("Volcanic"));
        assertFalse(
            SurveyOreSelection.custom(Arrays.asList("Volcanic"))
                .includes("Volcanic"));
    }
}
