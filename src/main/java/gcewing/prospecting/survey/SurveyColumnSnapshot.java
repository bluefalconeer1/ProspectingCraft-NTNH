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
    private final int dominantOreCount;
    private final List<String> topOreNames;

    private SurveyColumnSnapshot(State state, String dominantOreName, int dominantOreCount, List<String> topOreNames) {
        this.state = state;
        this.dominantOreName = dominantOreName;
        this.dominantOreCount = dominantOreCount;
        this.topOreNames = topOreNames;
    }

    public static SurveyColumnSnapshot from(SurveyColumn column) {
        return from(column, SurveyOreSelection.showAll());
    }

    public static SurveyColumnSnapshot from(SurveyColumn column, SurveyOreSelection selection) {
        if (column == null) return UNSURVEYED;
        if (selection == null) throw new IllegalArgumentException("Survey ore selection must not be null");

        List<String> topOreNames = Collections
            .unmodifiableList(new ArrayList<String>(column.getTopOreNames(TOP_ORE_LIMIT, selection)));
        if (topOreNames.isEmpty()) return SURVEYED_EMPTY;
        String dominantOreName = topOreNames.get(0);
        return oreBearing(column.getOreCount(dominantOreName), topOreNames);
    }

    static SurveyColumnSnapshot oreBearing(int dominantOreCount, List<String> topOreNames) {
        if (dominantOreCount <= 0) throw new IllegalArgumentException("Dominant ore count must be positive");
        if (topOreNames == null || topOreNames.isEmpty() || topOreNames.size() > TOP_ORE_LIMIT)
            throw new IllegalArgumentException("Ore-bearing columns require between one and five ore names");
        List<String> names = new ArrayList<String>(topOreNames.size());
        for (String oreName : topOreNames) {
            if (oreName == null || oreName.isEmpty()) throw new IllegalArgumentException("Ore name must not be empty");
            if (names.contains(oreName)) throw new IllegalArgumentException("Ore names must be unique");
            names.add(oreName);
        }
        List<String> immutableNames = Collections.unmodifiableList(names);
        return new SurveyColumnSnapshot(State.ORE_BEARING, immutableNames.get(0), dominantOreCount, immutableNames);
    }

    public State getState() {
        return state;
    }

    public String getDominantOreName() {
        return dominantOreName;
    }

    public int getDominantOreCount() {
        return dominantOreCount;
    }

    public List<String> getTopOreNames() {
        return topOreNames;
    }
}
