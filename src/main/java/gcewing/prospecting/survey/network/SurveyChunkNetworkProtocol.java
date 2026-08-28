package gcewing.prospecting.survey.network;

/** Message identifiers and coordinate validation shared by the private survey client/server protocol. */
public final class SurveyChunkNetworkProtocol {

    public static final String REQUEST_MESSAGE = "requestSurveyChunk";
    public static final String RESPONSE_MESSAGE = "surveyChunk";
    public static final String INVALIDATION_MESSAGE = "invalidateSurveyChunks";
    public static final String RESET_MESSAGE = "resetSurveyChunks";
    public static final String SELECTION_REQUEST_MESSAGE = "requestSurveyOreSelection";
    public static final String SELECTION_STATE_MESSAGE = "surveyOreSelection";
    public static final String SELECTION_UPDATE_MESSAGE = "updateSurveyOreSelection";

    private static final int MAX_ABSOLUTE_CHUNK_COORDINATE = 1_875_000;
    private static final int MAX_INVALIDATION_CHUNK_SPAN = 16;

    private SurveyChunkNetworkProtocol() {}

    public static boolean isValidChunkCoordinate(int coordinate) {
        return coordinate >= -MAX_ABSOLUTE_CHUNK_COORDINATE && coordinate <= MAX_ABSOLUTE_CHUNK_COORDINATE;
    }

    public static boolean isValidChunkRange(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        return isValidChunkCoordinate(minChunkX) && isValidChunkCoordinate(minChunkZ)
            && isValidChunkCoordinate(maxChunkX)
            && isValidChunkCoordinate(maxChunkZ)
            && minChunkX <= maxChunkX
            && minChunkZ <= maxChunkZ
            && maxChunkX - minChunkX < MAX_INVALIDATION_CHUNK_SPAN
            && maxChunkZ - minChunkZ < MAX_INVALIDATION_CHUNK_SPAN;
    }
}
