package gcewing.prospecting.survey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SurveyColumnSnapshot {

    public enum State {
        UNSURVEYED,
        SURVEYED_EMPTY,
        ORE_BEARING
    }

    public static final SurveyColumnSnapshot UNSURVEYED = new SurveyColumnSnapshot(
        State.UNSURVEYED,
        null,
        0,
        Collections.<String>emptyList());
    public static final SurveyColumnSnapshot SURVEYED_EMPTY = new SurveyColumnSnapshot(
        State.SURVEYED_EMPTY,
        null,
        0,
        Collections.<String>emptyList());

    private static final int TOP_ORE_LIMIT = 5;

    private final State state;
    private final String dominantOreName;
    private final int oreAlpha;
    private final List<String> topOreNames;

    private SurveyColumnSnapshot(State state, String dominantOreName, int oreAlpha, List<String> topOreNames) {
        this.state = state;
        this.dominantOreName = dominantOreName;
        this.oreAlpha = oreAlpha;
        this.topOreNames = topOreNames;
    }

    public static SurveyColumnSnapshot from(SurveyColumn column) {
        return from(column, SurveyOreSelection.showAll());
    }

    public static SurveyColumnSnapshot from(SurveyColumn column, SurveyOreSelection selection) {
        return from(column, selection, TOP_ORE_LIMIT);
    }

    public static SurveyColumnSnapshot from(SurveyColumn column, SurveyOreSelection selection, int topOreLimit) {
        if (column == null) return UNSURVEYED;
        if (selection == null) throw new IllegalArgumentException("Survey ore selection must not be null");
        if (topOreLimit <= 0 || topOreLimit > TOP_ORE_LIMIT)
            throw new IllegalArgumentException("Top ore limit must be between one and five");

        List<String> topOreNames = Collections
            .unmodifiableList(new ArrayList<String>(column.getTopOreNames(topOreLimit, selection)));
        if (topOreNames.isEmpty()) return SURVEYED_EMPTY;
        String dominantOreName = topOreNames.get(0);
        return oreBearing(SurveyDisplayStrength.exactAlpha(column.getOreCount(dominantOreName)), topOreNames);
    }

    static SurveyColumnSnapshot oreBearing(int oreAlpha, List<String> topOreNames) {
        if (oreAlpha <= 0 || oreAlpha > 255) throw new IllegalArgumentException("Ore alpha must be between 1 and 255");
        if (topOreNames == null || topOreNames.isEmpty() || topOreNames.size() > TOP_ORE_LIMIT)
            throw new IllegalArgumentException("Ore-bearing columns require between one and five ore names");
        List<String> names = new ArrayList<String>(topOreNames.size());
        for (String oreName : topOreNames) {
            if (oreName == null || oreName.isEmpty()) throw new IllegalArgumentException("Ore name must not be empty");
            if (names.contains(oreName)) throw new IllegalArgumentException("Ore names must be unique");
            names.add(oreName);
        }
        List<String> immutableNames = Collections.unmodifiableList(names);
        return new SurveyColumnSnapshot(State.ORE_BEARING, immutableNames.get(0), oreAlpha, immutableNames);
    }

    public State getState() {
        return state;
    }

    public String getDominantOreName() {
        return dominantOreName;
    }

    public int getOreAlpha() {
        return oreAlpha;
    }

    public List<String> getTopOreNames() {
        return topOreNames;
    }
}
