package gcewing.prospecting.survey.network;

public final class SurveyChunkNetworkProtocol {

    public static final String REQUEST_MESSAGE = "requestSurveyChunk";
    public static final String RESPONSE_MESSAGE = "surveyChunk";

    private static final int MAX_ABSOLUTE_CHUNK_COORDINATE = 1_875_000;

    private SurveyChunkNetworkProtocol() {}

    public static boolean isValidChunkCoordinate(int coordinate) {
        return coordinate >= -MAX_ABSOLUTE_CHUNK_COORDINATE && coordinate <= MAX_ABSOLUTE_CHUNK_COORDINATE;
    }
}
