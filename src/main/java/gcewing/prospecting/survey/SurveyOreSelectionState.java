package gcewing.prospecting.survey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class SurveyOreSelectionState {

    private final SurveyOreSelection selection;
    private final List<String> selectableOreNames;

    public SurveyOreSelectionState(SurveyOreSelection selection, Iterable<String> selectableOreNames) {
        if (selection == null) throw new IllegalArgumentException("Survey ore selection must not be null");
        if (selectableOreNames == null) throw new IllegalArgumentException("Selectable ore names must not be null");
        this.selection = selection;
        Set<String> sortedNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (String oreName : selectableOreNames) {
            if (oreName == null || oreName.trim()
                .isEmpty()) throw new IllegalArgumentException("Selectable ore names must not be empty");
            sortedNames.add(oreName);
        }
        this.selectableOreNames = Collections.unmodifiableList(new ArrayList<String>(sortedNames));
    }

    public SurveyOreSelection getSelection() {
        return selection;
    }

    public List<String> getSelectableOreNames() {
        return selectableOreNames;
    }
}
