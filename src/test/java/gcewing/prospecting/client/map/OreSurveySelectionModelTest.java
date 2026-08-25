package gcewing.prospecting.client.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import gcewing.prospecting.survey.SurveyOreSelection;
import gcewing.prospecting.survey.SurveyOreSelectionState;

public class OreSurveySelectionModelTest {

    @Test
    public void showAllIncludesFutureOresUntilABoxIsCleared() {
        OreSurveySelectionModel model = model(SurveyOreSelection.showAll());

        assertTrue(model.isShowAll());
        assertTrue(model.isSelected("Iron"));
        model.toggle("Iron");

        assertFalse(model.isShowAll());
        assertFalse(model.isSelected("Iron"));
        assertTrue(model.isSelected("Gold"));
    }

    @Test
    public void allAndNonePreserveTheirDistinctMeanings() {
        OreSurveySelectionModel model = model(SurveyOreSelection.custom(Arrays.asList("Iron")));

        model.selectNone();
        assertEquals(0, model.getSelectedCount());
        assertEquals(
            SurveyOreSelection.Mode.CUSTOM,
            model.toSelection()
                .getMode());

        model.selectAll();
        assertEquals(2, model.getSelectedCount());
        assertEquals(
            SurveyOreSelection.Mode.SHOW_ALL,
            model.toSelection()
                .getMode());
    }

    private static OreSurveySelectionModel model(SurveyOreSelection selection) {
        return new OreSurveySelectionModel(new SurveyOreSelectionState(selection, Arrays.asList("Iron", "Gold")));
    }
}
