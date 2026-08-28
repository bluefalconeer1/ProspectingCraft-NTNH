package gcewing.prospecting.client.map;

import static gcewing.prospecting.BaseDataChannel.ClientMessageHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gcewing.prospecting.BaseDataChannel.ChannelInput;
import gcewing.prospecting.BaseDataChannel.ChannelOutput;
import gcewing.prospecting.ProspectingCraft;
import gcewing.prospecting.survey.SurveyChunkSnapshot;
import gcewing.prospecting.survey.SurveyChunkSnapshotCodec;
import gcewing.prospecting.survey.SurveyOreSelection;
import gcewing.prospecting.survey.SurveyOreSelectionCodec;
import gcewing.prospecting.survey.SurveyOreSelectionState;
import gcewing.prospecting.survey.network.SurveyChunkNetworkProtocol;
import gcewing.prospecting.survey.network.SurveyChunkSnapshotCache;

/**
 * Bridges Navigator's chunk demand to the survey network and client cache.
 *
 * <p>
 * Network callbacks enqueue decoded events; {@link #processQueues()} applies them on the client tick and refreshes
 * the full-map layer.
 */
public final class SurveyChunkClientHandler {

    public static final SurveyChunkClientHandler INSTANCE = new SurveyChunkClientHandler();

    private static final Logger LOG = LogManager.getLogger("ProspectingCraft survey network");
    private static final int MAX_REQUESTS_PER_TICK = 16;
    private static final int REQUEST_RETRY_TIMEOUT_TICKS = 30 * 20;

    private final SurveyChunkSnapshotCache cache = new SurveyChunkSnapshotCache();
    private final ConcurrentLinkedQueue<Request> requests = new ConcurrentLinkedQueue<Request>();
    private final ConcurrentLinkedQueue<ReceivedEvent> receivedEvents = new ConcurrentLinkedQueue<ReceivedEvent>();
    private final ConcurrentLinkedQueue<SurveyOreSelectionState> receivedSelectionStates = new ConcurrentLinkedQueue<SurveyOreSelectionState>();
    private SurveyOreSelectionState selectionState;
    private long clientTick;

    private SurveyChunkClientHandler() {}

    public SurveyChunkSnapshot getOrRequest(int dimensionId, int chunkX, int chunkZ) {
        SurveyChunkSnapshot snapshot = cache.get(dimensionId, chunkX, chunkZ);
        if (snapshot != null) return snapshot;
        if (cache.markPending(dimensionId, chunkX, chunkZ, clientTick))
            requests.add(new Request(dimensionId, chunkX, chunkZ));
        return null;
    }

    @ClientMessageHandler(SurveyChunkNetworkProtocol.RESPONSE_MESSAGE)
    public void onSnapshotReceived(ChannelInput input) {
        try {
            receivedEvents.add(ReceivedEvent.forSnapshot(SurveyChunkSnapshotCodec.read(input)));
        } catch (IOException | RuntimeException exception) {
            LOG.error("Could not read survey chunk snapshot", exception);
        }
    }

    @ClientMessageHandler(SurveyChunkNetworkProtocol.INVALIDATION_MESSAGE)
    public void onChunksInvalidated(ChannelInput input) {
        try {
            int dimensionId = input.readInt();
            int minChunkX = input.readInt();
            int minChunkZ = input.readInt();
            int maxChunkX = input.readInt();
            int maxChunkZ = input.readInt();
            if (!SurveyChunkNetworkProtocol.isValidChunkRange(minChunkX, minChunkZ, maxChunkX, maxChunkZ)) return;
            receivedEvents.add(ReceivedEvent.forInvalidation(dimensionId, minChunkX, minChunkZ, maxChunkX, maxChunkZ));
        } catch (RuntimeException exception) {
            LOG.error("Could not read survey chunk invalidation", exception);
        }
    }

    @ClientMessageHandler(SurveyChunkNetworkProtocol.RESET_MESSAGE)
    public void onChunksReset(ChannelInput input) {
        receivedEvents.add(ReceivedEvent.forReset());
    }

    @ClientMessageHandler(SurveyChunkNetworkProtocol.SELECTION_STATE_MESSAGE)
    public void onSelectionStateReceived(ChannelInput input) {
        try {
            receivedSelectionStates.add(SurveyOreSelectionCodec.readState(input));
        } catch (IOException | RuntimeException exception) {
            LOG.error("Could not read survey ore selection state", exception);
        }
    }

    public void requestSelectionState() {
        selectionState = null;
        receivedSelectionStates.clear();
        ProspectingCraft.channel.openServer(SurveyChunkNetworkProtocol.SELECTION_REQUEST_MESSAGE)
            .close();
    }

    public SurveyOreSelectionState getSelectionState() {
        return selectionState;
    }

    public void updateSelection(SurveyOreSelection selection) {
        try {
            ChannelOutput output = ProspectingCraft.channel
                .openServer(SurveyChunkNetworkProtocol.SELECTION_UPDATE_MESSAGE);
            SurveyOreSelectionCodec.writeSelection(output, selection);
            output.close();
        } catch (IOException | RuntimeException exception) {
            LOG.error("Could not update survey ore selection", exception);
        }
    }

    public void processQueues() {
        clientTick++;
        SurveyOreSelectionState receivedState;
        while ((receivedState = receivedSelectionStates.poll()) != null) selectionState = receivedState;

        boolean surveyDataChanged = false;
        boolean fullCacheReset = false;
        ReceivedEvent event;
        while ((event = receivedEvents.poll()) != null) {
            if (event.reset) {
                cache.clear();
                requests.clear();
                fullCacheReset = true;
            } else if (event.snapshot != null) cache.accept(event.snapshot);
            else cache
                .invalidate(event.dimensionId, event.minChunkX, event.minChunkZ, event.maxChunkX, event.maxChunkZ);
            surveyDataChanged = true;
        }
        boolean requestExpired = cache.expirePending(clientTick, REQUEST_RETRY_TIMEOUT_TICKS) > 0;
        if (fullCacheReset) OreSurveyLayerManager.INSTANCE.clearFullCache();
        else if (surveyDataChanged || requestExpired) OreSurveyLayerManager.INSTANCE.forceRefresh();

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || minecraft.theWorld == null) return;
        for (int i = 0; i < MAX_REQUESTS_PER_TICK; i++) {
            Request request = requests.poll();
            if (request == null) return;
            sendRequest(request);
        }
    }

    public void reset() {
        cache.clear();
        requests.clear();
        receivedEvents.clear();
        receivedSelectionStates.clear();
        selectionState = null;
        clientTick = 0;
        OreSurveyLayerManager.INSTANCE.clearFullCache();
    }

    private static void sendRequest(Request request) {
        ChannelOutput output = ProspectingCraft.channel.openServer(SurveyChunkNetworkProtocol.REQUEST_MESSAGE);
        output.writeInt(request.dimensionId);
        output.writeInt(request.chunkX);
        output.writeInt(request.chunkZ);
        output.close();
    }

    private static final class Request {

        private final int dimensionId;
        private final int chunkX;
        private final int chunkZ;

        private Request(int dimensionId, int chunkX, int chunkZ) {
            this.dimensionId = dimensionId;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }

    private static final class ReceivedEvent {

        private final SurveyChunkSnapshot snapshot;
        private final int dimensionId;
        private final int minChunkX;
        private final int minChunkZ;
        private final int maxChunkX;
        private final int maxChunkZ;
        private final boolean reset;

        private ReceivedEvent(SurveyChunkSnapshot snapshot, int dimensionId, int minChunkX, int minChunkZ,
            int maxChunkX, int maxChunkZ, boolean reset) {
            this.snapshot = snapshot;
            this.dimensionId = dimensionId;
            this.minChunkX = minChunkX;
            this.minChunkZ = minChunkZ;
            this.maxChunkX = maxChunkX;
            this.maxChunkZ = maxChunkZ;
            this.reset = reset;
        }

        private static ReceivedEvent forSnapshot(SurveyChunkSnapshot snapshot) {
            return new ReceivedEvent(snapshot, 0, 0, 0, 0, 0, false);
        }

        private static ReceivedEvent forInvalidation(int dimensionId, int minChunkX, int minChunkZ, int maxChunkX,
            int maxChunkZ) {
            return new ReceivedEvent(null, dimensionId, minChunkX, minChunkZ, maxChunkX, maxChunkZ, false);
        }

        private static ReceivedEvent forReset() {
            return new ReceivedEvent(null, 0, 0, 0, 0, 0, true);
        }
    }
}
