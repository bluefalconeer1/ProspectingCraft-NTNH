package gcewing.prospecting.survey.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class FairSurveyChunkRequestQueueTest {

    private static final UUID FIRST_PLAYER = UUID.fromString("17910713-3ba1-4f00-8000-000000000001");
    private static final UUID SECOND_PLAYER = UUID.fromString("27910713-3ba1-4f00-8000-000000000002");

    @Test
    public void servesPlayersRoundRobinEvenWhenOneQueuedFirst() {
        FairSurveyChunkRequestQueue<String> queue = new FairSurveyChunkRequestQueue<String>(10, 10);
        queue.offer(FIRST_PLAYER, 0, 0, 0, "first-1");
        queue.offer(FIRST_PLAYER, 0, 1, 0, "first-2");
        queue.offer(FIRST_PLAYER, 0, 2, 0, "first-3");
        queue.offer(SECOND_PLAYER, 0, 0, 0, "second-1");
        queue.offer(SECOND_PLAYER, 0, 1, 0, "second-2");

        assertEquals("first-1", queue.poll());
        assertEquals("second-1", queue.poll());
        assertEquals("first-2", queue.poll());
        assertEquals("second-2", queue.poll());
        assertEquals("first-3", queue.poll());
        assertNull(queue.poll());
    }

    @Test
    public void suppressesDuplicatePlayerChunkRequests() {
        FairSurveyChunkRequestQueue<String> queue = new FairSurveyChunkRequestQueue<String>(10, 10);

        assertTrue(queue.offer(FIRST_PLAYER, 0, 3, 4, "original"));
        assertFalse(queue.offer(FIRST_PLAYER, 0, 3, 4, "duplicate"));
        assertTrue(queue.offer(SECOND_PLAYER, 0, 3, 4, "other-player"));
        assertEquals(2, queue.size());
    }

    @Test
    public void enforcesPerPlayerLimitWithoutBlockingAnotherPlayer() {
        FairSurveyChunkRequestQueue<String> queue = new FairSurveyChunkRequestQueue<String>(10, 2);

        assertTrue(queue.offer(FIRST_PLAYER, 0, 0, 0, "first-1"));
        assertTrue(queue.offer(FIRST_PLAYER, 0, 1, 0, "first-2"));
        assertFalse(queue.offer(FIRST_PLAYER, 0, 2, 0, "first-3"));
        assertTrue(queue.offer(SECOND_PLAYER, 0, 0, 0, "second-1"));
    }

    @Test
    public void enforcesGlobalLimit() {
        FairSurveyChunkRequestQueue<String> queue = new FairSurveyChunkRequestQueue<String>(2, 2);

        assertTrue(queue.offer(FIRST_PLAYER, 0, 0, 0, "first"));
        assertTrue(queue.offer(SECOND_PLAYER, 0, 0, 0, "second"));
        assertFalse(queue.offer(SECOND_PLAYER, 0, 1, 0, "overflow"));
        assertEquals(2, queue.size());
    }

    @Test
    public void completedChunkCanBeRequestedAgain() {
        FairSurveyChunkRequestQueue<String> queue = new FairSurveyChunkRequestQueue<String>(2, 2);
        assertTrue(queue.offer(FIRST_PLAYER, 0, 3, 4, "first"));

        assertEquals("first", queue.poll());

        assertTrue(queue.offer(FIRST_PLAYER, 0, 3, 4, "retry"));
        assertEquals("retry", queue.poll());
    }
}
