package gcewing.prospecting.survey.network;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import gcewing.prospecting.survey.SurveyChunkSnapshot;

public class SurveyChunkSnapshotCache {

    private static final int MAX_CACHED_SNAPSHOTS = 8192;
    private static final int MAX_PENDING_REQUESTS = 4096;

    private final Map<Key, SurveyChunkSnapshot> snapshots = new LinkedHashMap<Key, SurveyChunkSnapshot>(
        16,
        0.75f,
        true);
    private final Set<Key> pending = new HashSet<Key>();

    public synchronized SurveyChunkSnapshot get(int dimensionId, int chunkX, int chunkZ) {
        return snapshots.get(new Key(dimensionId, chunkX, chunkZ));
    }

    public synchronized boolean markPending(int dimensionId, int chunkX, int chunkZ) {
        Key key = new Key(dimensionId, chunkX, chunkZ);
        if (snapshots.containsKey(key)) return false;
        if (pending.size() >= MAX_PENDING_REQUESTS) return false;
        return pending.add(key);
    }

    public synchronized void accept(SurveyChunkSnapshot snapshot) {
        Key key = new Key(snapshot.getDimensionId(), snapshot.getChunkX(), snapshot.getChunkZ());
        if (!snapshots.containsKey(key) && snapshots.size() >= MAX_CACHED_SNAPSHOTS) {
            Iterator<Key> eldest = snapshots.keySet()
                .iterator();
            eldest.next();
            eldest.remove();
        }
        snapshots.put(key, snapshot);
        pending.remove(key);
    }

    public synchronized void invalidate(int dimensionId, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        Iterator<Key> iterator = snapshots.keySet()
            .iterator();
        while (iterator.hasNext()) {
            Key key = iterator.next();
            if (key.dimensionId == dimensionId && key.chunkX >= minChunkX
                && key.chunkX <= maxChunkX
                && key.chunkZ >= minChunkZ
                && key.chunkZ <= maxChunkZ) iterator.remove();
        }
    }

    public synchronized void clear() {
        snapshots.clear();
        pending.clear();
    }

    private static final class Key {

        private final int dimensionId;
        private final int chunkX;
        private final int chunkZ;

        private Key(int dimensionId, int chunkX, int chunkZ) {
            this.dimensionId = dimensionId;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof Key)) return false;
            Key other = (Key) object;
            return dimensionId == other.dimensionId && chunkX == other.chunkX && chunkZ == other.chunkZ;
        }

        @Override
        public int hashCode() {
            int result = dimensionId;
            result = 31 * result + chunkX;
            result = 31 * result + chunkZ;
            return result;
        }
    }
}
