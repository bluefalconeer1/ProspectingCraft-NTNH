package gcewing.prospecting;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SeismicRecorderContainerTest {

    @Test
    public void permitsCurrentOwnedNearbySurveyorOnServer() {
        assertTrue(SeismicRecorderContainer.isValidActivationContext(true, true, true, true, true, true, true, 64.0));
    }

    @Test
    public void rejectsStaleOrUntrustedActivationContext() {
        assertFalse(SeismicRecorderContainer.isValidActivationContext(false, true, true, true, true, true, true, 0));
        assertFalse(SeismicRecorderContainer.isValidActivationContext(true, false, true, true, true, true, true, 0));
        assertFalse(SeismicRecorderContainer.isValidActivationContext(true, true, false, true, true, true, true, 0));
        assertFalse(SeismicRecorderContainer.isValidActivationContext(true, true, true, false, true, true, true, 0));
        assertFalse(SeismicRecorderContainer.isValidActivationContext(true, true, true, true, false, true, true, 0));
        assertFalse(SeismicRecorderContainer.isValidActivationContext(true, true, true, true, true, false, true, 0));
        assertFalse(SeismicRecorderContainer.isValidActivationContext(true, true, true, true, true, true, false, 0));
        assertFalse(SeismicRecorderContainer.isValidActivationContext(true, true, true, true, true, true, true, 64.01));
    }
}
