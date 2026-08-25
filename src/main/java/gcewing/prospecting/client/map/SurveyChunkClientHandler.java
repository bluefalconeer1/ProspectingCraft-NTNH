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
import gcewing.prospecting.survey.network.SurveyChunkNetworkProtocol;
import gcewing.prospecting.survey.network.SurveyChunkSnapshotCache;

public final class SurveyChunkClientHandler {

    public static final SurveyChunkClientHandler INSTANCE = new SurveyChunkClientHandler();

    private static final Logger LOG = LogManager.getLogger("ProspectingCraft survey network");
    private static final int MAX_REQUESTS_PER_TICK = 16;

    private final SurveyChunkSnapshotCache cache = new SurveyChunkSnapshotCache();
    private final ConcurrentLinkedQueue<Request> requests = new ConcurrentLinkedQueue<Request>();
    private final ConcurrentLinkedQueue<SurveyChunkSnapshot> responses = new ConcurrentLinkedQueue<SurveyChunkSnapshot>();

    private SurveyChunkClientHandler() {}

    public SurveyChunkSnapshot getOrRequest(int dimensionId, int chunkX, int chunkZ) {
        SurveyChunkSnapshot snapshot = cache.get(dimensionId, chunkX, chunkZ);
        if (snapshot != null) return snapshot;
        if (cache.markPending(dimensionId, chunkX, chunkZ)) requests.add(new Request(dimensionId, chunkX, chunkZ));
        return null;
    }

    @ClientMessageHandler(SurveyChunkNetworkProtocol.RESPONSE_MESSAGE)
    public void onSnapshotReceived(ChannelInput input) {
        try {
            responses.add(SurveyChunkSnapshotCodec.read(input));
        } catch (IOException | RuntimeException exception) {
            LOG.error("Could not read survey chunk snapshot", exception);
        }
    }

    public void processQueues() {
        boolean receivedSnapshot = false;
        SurveyChunkSnapshot snapshot;
        while ((snapshot = responses.poll()) != null) {
            cache.accept(snapshot);
            receivedSnapshot = true;
        }
        if (receivedSnapshot) OreSurveyLayerManager.INSTANCE.forceRefresh();

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
        responses.clear();
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
}
