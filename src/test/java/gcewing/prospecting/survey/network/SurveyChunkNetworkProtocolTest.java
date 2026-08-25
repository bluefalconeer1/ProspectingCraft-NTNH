package gcewing.prospecting.survey.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SurveyChunkNetworkProtocolTest {

    @Test
    public void acceptsACompletedSurveyChunkRange() {
        assertTrue(SurveyChunkNetworkProtocol.isValidChunkRange(-5, 2, 3, 10));
    }

    @Test
    public void rejectsReversedOrOversizedChunkRanges() {
        assertFalse(SurveyChunkNetworkProtocol.isValidChunkRange(4, 2, 3, 10));
        assertFalse(SurveyChunkNetworkProtocol.isValidChunkRange(0, 0, 16, 0));
    }

    @Test
    public void rejectsChunkRangesOutsideMinecraftBounds() {
        assertFalse(SurveyChunkNetworkProtocol.isValidChunkRange(-1_875_001, 0, 0, 0));
        assertFalse(SurveyChunkNetworkProtocol.isValidChunkRange(0, 0, 1_875_001, 0));
    }
}
