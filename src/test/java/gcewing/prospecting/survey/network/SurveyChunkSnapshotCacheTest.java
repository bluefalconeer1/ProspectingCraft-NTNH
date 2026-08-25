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

    private static SurveyChunkSnapshot newSnapshot(int dimensionId, int chunkX, int chunkZ) {
        PlayerSurveyData data = new PlayerSurveyData(PlayerSurveyData.getDataName(OWNER_ID));
        return SurveyChunkSnapshot.from(data, dimensionId, chunkX, chunkZ);
    }
}
