package gcewing.prospecting.survey.network;

import static gcewing.prospecting.BaseDataChannel.ServerMessageHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gcewing.prospecting.BaseDataChannel.ChannelInput;
import gcewing.prospecting.BaseDataChannel.ChannelOutput;
import gcewing.prospecting.ProspectingCraft;
import gcewing.prospecting.survey.PlayerSurveyData;
import gcewing.prospecting.survey.SurveyChunkSnapshot;
import gcewing.prospecting.survey.SurveyChunkSnapshotCodec;

public final class SurveyChunkServerHandler {

    public static final SurveyChunkServerHandler INSTANCE = new SurveyChunkServerHandler();

    private static final Logger LOG = LogManager.getLogger("ProspectingCraft survey network");
    private static final int MAX_PENDING_REQUESTS = 4096;
    private static final int MAX_REQUESTS_PER_TICK = 16;

    private final ConcurrentLinkedQueue<Request> requests = new ConcurrentLinkedQueue<Request>();
    private final AtomicInteger pendingRequestCount = new AtomicInteger();

    private SurveyChunkServerHandler() {}

    @ServerMessageHandler(SurveyChunkNetworkProtocol.REQUEST_MESSAGE)
    public void onChunkRequested(EntityPlayer requestingPlayer, ChannelInput input) {
        int dimensionId = input.readInt();
        int chunkX = input.readInt();
        int chunkZ = input.readInt();
        if (!(requestingPlayer instanceof EntityPlayerMP)) return;
        if (requestingPlayer.dimension != dimensionId) return;
        if (!SurveyChunkNetworkProtocol.isValidChunkCoordinate(chunkX)
            || !SurveyChunkNetworkProtocol.isValidChunkCoordinate(chunkZ)) return;

        int pending = pendingRequestCount.incrementAndGet();
        if (pending > MAX_PENDING_REQUESTS) {
            pendingRequestCount.decrementAndGet();
            return;
        }
        requests.add(new Request((EntityPlayerMP) requestingPlayer, dimensionId, chunkX, chunkZ));
    }

    public void processRequests() {
        for (int i = 0; i < MAX_REQUESTS_PER_TICK; i++) {
            Request request = requests.poll();
            if (request == null) return;
            pendingRequestCount.decrementAndGet();
            sendSnapshot(request);
        }
    }

    private void sendSnapshot(Request request) {
        EntityPlayerMP player = request.player;
        if (player.isDead || player.dimension != request.dimensionId) return;
        try {
            PlayerSurveyData playerData = PlayerSurveyData.get(player.worldObj, player.getUniqueID());
            SurveyChunkSnapshot snapshot = SurveyChunkSnapshot
                .from(playerData, request.dimensionId, request.chunkX, request.chunkZ);
            ChannelOutput output = ProspectingCraft.channel
                .openPlayer(player, SurveyChunkNetworkProtocol.RESPONSE_MESSAGE);
            SurveyChunkSnapshotCodec.write(output, snapshot);
            output.close();
        } catch (IOException | RuntimeException exception) {
            LOG.error("Could not send survey chunk snapshot", exception);
        }
    }

    private static final class Request {

        private final EntityPlayerMP player;
        private final int dimensionId;
        private final int chunkX;
        private final int chunkZ;

        private Request(EntityPlayerMP player, int dimensionId, int chunkX, int chunkZ) {
            this.player = player;
            this.dimensionId = dimensionId;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }
}
