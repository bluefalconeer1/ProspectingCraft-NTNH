package gcewing.prospecting;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class SeismicRecorderBlockTest {

    private static final UUID OWNER_ID = UUID.fromString("17910713-3ba1-4f00-8000-000000000001");
    private static final UUID OTHER_PLAYER_ID = UUID.fromString("27910713-3ba1-4f00-8000-000000000002");

    @Test
    public void unownedSurveyorCanBeClaimed() {
        assertTrue(SeismicRecorderBlock.canPlayerAccess(null, OTHER_PLAYER_ID));
    }

    @Test
    public void ownerCanAccessSurveyor() {
        assertTrue(SeismicRecorderBlock.canPlayerAccess(OWNER_ID, OWNER_ID));
    }

    @Test
    public void otherPlayerCannotAccessSurveyor() {
        assertFalse(SeismicRecorderBlock.canPlayerAccess(OWNER_ID, OTHER_PLAYER_ID));
    }
}
