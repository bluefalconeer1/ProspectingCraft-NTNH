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
        if (column == null) return UNSURVEYED;
        if (column.isEmpty()) return SURVEYED_EMPTY;

        String dominantOreName = column.getDominantOreName();
        List<String> topOreNames = Collections
            .unmodifiableList(new ArrayList<String>(column.getTopOreNames(TOP_ORE_LIMIT)));
        return new SurveyColumnSnapshot(
            State.ORE_BEARING,
            dominantOreName,
            column.getOreCount(dominantOreName),
            topOreNames);
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
