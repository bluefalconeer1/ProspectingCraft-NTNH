package gcewing.prospecting.survey.network;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Bounded, duplicate-free per-player queues served in round-robin order. */
final class FairSurveyChunkRequestQueue<T> {

    private final int maxPendingRequests;
    private final int maxPendingRequestsPerPlayer;
    private final Map<UUID, ArrayDeque<Entry<T>>> requestsByPlayer = new HashMap<UUID, ArrayDeque<Entry<T>>>();
    private final ArrayDeque<UUID> activePlayers = new ArrayDeque<UUID>();
    private final Set<Key> pendingKeys = new HashSet<Key>();
    private int pendingRequestCount;

    FairSurveyChunkRequestQueue(int maxPendingRequests, int maxPendingRequestsPerPlayer) {
        if (maxPendingRequests <= 0 || maxPendingRequestsPerPlayer <= 0)
            throw new IllegalArgumentException("Survey request queue limits must be positive");
        this.maxPendingRequests = maxPendingRequests;
        this.maxPendingRequestsPerPlayer = maxPendingRequestsPerPlayer;
    }

    public synchronized boolean offer(UUID playerId, int dimensionId, int chunkX, int chunkZ, T request) {
        if (playerId == null || request == null) throw new IllegalArgumentException("Survey request must not be null");
        Key key = new Key(playerId, dimensionId, chunkX, chunkZ);
        if (pendingKeys.contains(key) || pendingRequestCount >= maxPendingRequests) return false;

        ArrayDeque<Entry<T>> playerRequests = requestsByPlayer.get(playerId);
        if (playerRequests != null && playerRequests.size() >= maxPendingRequestsPerPlayer) return false;
        if (playerRequests == null) {
            playerRequests = new ArrayDeque<Entry<T>>();
            requestsByPlayer.put(playerId, playerRequests);
            activePlayers.addLast(playerId);
        }
        playerRequests.addLast(new Entry<T>(key, request));
        pendingKeys.add(key);
        pendingRequestCount++;
        return true;
    }

    public synchronized T poll() {
        UUID playerId = activePlayers.pollFirst();
        if (playerId == null) return null;
        ArrayDeque<Entry<T>> playerRequests = requestsByPlayer.get(playerId);
        Entry<T> entry = playerRequests.removeFirst();
        if (playerRequests.isEmpty()) requestsByPlayer.remove(playerId);
        else activePlayers.addLast(playerId);
        pendingKeys.remove(entry.key);
        pendingRequestCount--;
        return entry.request;
    }

    synchronized int size() {
        return pendingRequestCount;
    }

    private static final class Entry<T> {

        private final Key key;
        private final T request;

        private Entry(Key key, T request) {
            this.key = key;
            this.request = request;
        }
    }

    private static final class Key {

        private final UUID playerId;
        private final int dimensionId;
        private final int chunkX;
        private final int chunkZ;

        private Key(UUID playerId, int dimensionId, int chunkX, int chunkZ) {
            this.playerId = playerId;
            this.dimensionId = dimensionId;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof Key)) return false;
            Key other = (Key) object;
            return dimensionId == other.dimensionId && chunkX == other.chunkX
                && chunkZ == other.chunkZ
                && playerId.equals(other.playerId);
        }

        @Override
        public int hashCode() {
            int result = playerId.hashCode();
            result = 31 * result + dimensionId;
            result = 31 * result + chunkX;
            result = 31 * result + chunkZ;
            return result;
        }
    }
}
