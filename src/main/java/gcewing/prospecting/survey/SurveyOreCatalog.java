package gcewing.prospecting.survey;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** Builds the server-approved picker catalog from registered block ores plus names retained in saved surveys. */
public final class SurveyOreCatalog {

    private SurveyOreCatalog() {}

    public static Set<String> getSelectableOreNames(PlayerSurveyData playerData) {
        if (playerData == null) throw new IllegalArgumentException("Player survey data must not be null");
        Set<String> oreNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        oreNames.addAll(OreDictionaryResolver.getRegisteredCanonicalBlockOreNames());
        oreNames.addAll(playerData.getDiscoveredOreNames());
        return Collections.unmodifiableSet(oreNames);
    }
}
