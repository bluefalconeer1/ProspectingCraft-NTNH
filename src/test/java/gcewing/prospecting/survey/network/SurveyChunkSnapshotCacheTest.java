package gcewing.prospecting.survey.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

import gcewing.prospecting.survey.PlayerSurveyData;
import gcewing.prospecting.survey.SurveyChunkSnapshot;

public class SurveyChunkSnapshotCacheTest {

    private static final UUID OWNER_ID = UUID.fromString("67910713-3ba1-4f00-8000-000000000006");

    @Test
    public void deduplicatesPendingRequestsAndStoresResponses() {
        SurveyChunkSnapshotCache cache = new SurveyChunkSnapshotCache();
        SurveyChunkSnapshot snapshot = newSnapshot(2, -3, 4);

        assertNull(cache.get(2, -3, 4));
        assertTrue(cache.markPending(2, -3, 4));
        assertFalse(cache.markPending(2, -3, 4));

        cache.accept(snapshot);

        assertSame(snapshot, cache.get(2, -3, 4));
        assertFalse(cache.markPending(2, -3, 4));
    }

    @Test
    public void clearRemovesSnapshotsAndPendingRequests() {
        SurveyChunkSnapshotCache cache = new SurveyChunkSnapshotCache();
        cache.accept(newSnapshot(0, 1, 2));
        cache.markPending(0, 3, 4);

        cache.clear();

        assertNull(cache.get(0, 1, 2));
        assertTrue(cache.markPending(0, 3, 4));
    }

    @Test
    public void invalidatesOnlySnapshotsInsideTheCompletedSurvey() {
        SurveyChunkSnapshotCache cache = new SurveyChunkSnapshotCache();
        SurveyChunkSnapshot inside = newSnapshot(0, -2, 3);
        SurveyChunkSnapshot outside = newSnapshot(0, 7, 3);
        SurveyChunkSnapshot otherDimension = newSnapshot(1, -2, 3);
        cache.accept(inside);
        cache.accept(outside);
        cache.accept(otherDimension);

        cache.invalidate(0, -4, 1, 4, 5);

        assertNull(cache.get(0, -2, 3));
        assertSame(outside, cache.get(0, 7, 3));
        assertSame(otherDimension, cache.get(1, -2, 3));
    }

    @Test
    public void invalidationKeepsAnInFlightRequestPending() {
        SurveyChunkSnapshotCache cache = new SurveyChunkSnapshotCache();
        assertTrue(cache.markPending(0, 2, 3));

        cache.invalidate(0, 2, 3, 2, 3);

        assertFalse(cache.markPending(0, 2, 3));
        SurveyChunkSnapshot response = newSnapshot(0, 2, 3);
        cache.accept(response);
        assertSame(response, cache.get(0, 2, 3));
    }

    private static SurveyChunkSnapshot newSnapshot(int dimensionId, int chunkX, int chunkZ) {
        PlayerSurveyData data = new PlayerSurveyData(PlayerSurveyData.getDataName(OWNER_ID));
        return SurveyChunkSnapshot.from(data, dimensionId, chunkX, chunkZ);
    }
}
