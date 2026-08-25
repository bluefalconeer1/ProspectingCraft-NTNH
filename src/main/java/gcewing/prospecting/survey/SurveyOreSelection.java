package gcewing.prospecting.survey;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class SurveyOreSelection {

    public enum Mode {
        SHOW_ALL,
        CUSTOM
    }

    private static final SurveyOreSelection SHOW_ALL = new SurveyOreSelection(
        Mode.SHOW_ALL,
        Collections.<String>emptySet());

    private final Mode mode;
    private final Set<String> selectedOreNames;

    private SurveyOreSelection(Mode mode, Set<String> selectedOreNames) {
        this.mode = mode;
        this.selectedOreNames = selectedOreNames;
    }

    public static SurveyOreSelection showAll() {
        return SHOW_ALL;
    }

    public static SurveyOreSelection custom(Iterable<String> selectedOreNames) {
        if (selectedOreNames == null) throw new IllegalArgumentException("Selected ore names must not be null");
        Set<String> normalizedNames = new TreeSet<String>();
        for (String oreName : selectedOreNames) {
            if (oreName == null) continue;
            String normalizedName = normalize(oreName);
            if (!normalizedName.isEmpty()) normalizedNames.add(normalizedName);
        }
        return new SurveyOreSelection(Mode.CUSTOM, Collections.unmodifiableSet(normalizedNames));
    }

    public Mode getMode() {
        return mode;
    }

    public boolean includes(String oreName) {
        return mode == Mode.SHOW_ALL || oreName != null && selectedOreNames.contains(normalize(oreName));
    }

    public Set<String> getSelectedOreNames() {
        return selectedOreNames;
    }

    private static String normalize(String oreName) {
        return oreName.trim()
            .toLowerCase(Locale.ROOT);
    }
}
