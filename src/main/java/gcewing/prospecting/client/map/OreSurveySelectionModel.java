package gcewing.prospecting.client.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import gcewing.prospecting.survey.SurveyOreSelection;
import gcewing.prospecting.survey.SurveyOreSelectionState;

/** Mutable screen model that converts all/none/individual UI choices into an immutable selection request. */
final class OreSurveySelectionModel {

    private final List<String> oreNames;
    private final Set<String> selectedOreNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private boolean showAll;

    OreSurveySelectionModel(SurveyOreSelectionState state) {
        if (state == null) throw new IllegalArgumentException("Survey ore selection state must not be null");
        oreNames = new ArrayList<String>(state.getSelectableOreNames());
        SurveyOreSelection selection = state.getSelection();
        showAll = selection.getMode() == SurveyOreSelection.Mode.SHOW_ALL;
        if (!showAll) for (String oreName : oreNames) if (selection.includes(oreName)) selectedOreNames.add(oreName);
    }

    List<String> getOreNames() {
        return oreNames;
    }

    boolean isShowAll() {
        return showAll;
    }

    boolean isSelected(String oreName) {
        return showAll || selectedOreNames.contains(oreName);
    }

    int getSelectedCount() {
        return showAll ? oreNames.size() : selectedOreNames.size();
    }

    void selectAll() {
        showAll = true;
        selectedOreNames.clear();
    }

    void selectNone() {
        showAll = false;
        selectedOreNames.clear();
    }

    void toggle(String oreName) {
        if (showAll) {
            selectedOreNames.addAll(oreNames);
            showAll = false;
        }
        if (!selectedOreNames.remove(oreName)) selectedOreNames.add(oreName);
    }

    SurveyOreSelection toSelection() {
        return showAll ? SurveyOreSelection.showAll() : SurveyOreSelection.custom(selectedOreNames);
    }
}
