package gcewing.prospecting.survey.network;

import static gcewing.prospecting.BaseDataChannel.ServerMessageHandler;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import gcewing.prospecting.BaseDataChannel.ChannelInput;
import gcewing.prospecting.BaseDataChannel.ChannelOutput;
import gcewing.prospecting.ProspectingCraft;
import gcewing.prospecting.survey.PlayerSurveyData;
import gcewing.prospecting.survey.SurveyArea;
import gcewing.prospecting.survey.SurveyChunkSnapshot;
import gcewing.prospecting.survey.SurveyChunkSnapshotCodec;
import gcewing.prospecting.survey.SurveyOreCatalog;
import gcewing.prospecting.survey.SurveyOreSelection;
import gcewing.prospecting.survey.SurveyOreSelectionCodec;
import gcewing.prospecting.survey.SurveyOreSelectionState;

public final class SurveyChunkServerHandler {

    public static final SurveyChunkServerHandler INSTANCE = new SurveyChunkServerHandler();

    private static final Logger LOG = LogManager.getLogger("ProspectingCraft survey network");
    private static final int MAX_PENDING_REQUESTS = 4096;
    private static final int MAX_REQUESTS_PER_TICK = 16;
    private static final int MAX_PENDING_SELECTION_REQUESTS = 128;
    private static final int MAX_SELECTION_REQUESTS_PER_TICK = 16;

    private final ConcurrentLinkedQueue<Request> requests = new ConcurrentLinkedQueue<Request>();
    private final AtomicInteger pendingRequestCount = new AtomicInteger();
    private final ConcurrentLinkedQueue<SelectionRequest> selectionRequests = new ConcurrentLinkedQueue<SelectionRequest>();
    private final AtomicInteger pendingSelectionRequestCount = new AtomicInteger();

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
        processSelectionRequests();
        for (int i = 0; i < MAX_REQUESTS_PER_TICK; i++) {
            Request request = requests.poll();
            if (request == null) return;
            pendingRequestCount.decrementAndGet();
            sendSnapshot(request);
        }
    }

    @ServerMessageHandler(SurveyChunkNetworkProtocol.SELECTION_REQUEST_MESSAGE)
    public void onSelectionRequested(EntityPlayer requestingPlayer, ChannelInput input) {
        if (requestingPlayer instanceof EntityPlayerMP)
            queueSelectionRequest(new SelectionRequest((EntityPlayerMP) requestingPlayer, null));
    }

    @ServerMessageHandler(SurveyChunkNetworkProtocol.SELECTION_UPDATE_MESSAGE)
    public void onSelectionUpdated(EntityPlayer requestingPlayer, ChannelInput input) {
        if (!(requestingPlayer instanceof EntityPlayerMP)) return;
        try {
            SurveyOreSelection requestedSelection = SurveyOreSelectionCodec.readSelection(input);
            queueSelectionRequest(new SelectionRequest((EntityPlayerMP) requestingPlayer, requestedSelection));
        } catch (IOException | RuntimeException exception) {
            LOG.warn(
                "Rejected malformed survey ore selection from " + requestingPlayer.getCommandSenderName(),
                exception);
        }
    }

    private void queueSelectionRequest(SelectionRequest request) {
        int pending = pendingSelectionRequestCount.incrementAndGet();
        if (pending > MAX_PENDING_SELECTION_REQUESTS) {
            pendingSelectionRequestCount.decrementAndGet();
            return;
        }
        selectionRequests.add(request);
    }

    private void processSelectionRequests() {
        for (int i = 0; i < MAX_SELECTION_REQUESTS_PER_TICK; i++) {
            SelectionRequest request = selectionRequests.poll();
            if (request == null) return;
            pendingSelectionRequestCount.decrementAndGet();
            processSelectionRequest(request);
        }
    }

    private void processSelectionRequest(SelectionRequest request) {
        EntityPlayerMP player = request.player;
        if (player.isDead) return;
        PlayerSurveyData playerData = PlayerSurveyData.get(player.worldObj, player.getUniqueID());
        if (request.selection != null) try {
            SurveyOreSelection validatedSelection = playerData
                .canonicalizeOreSelection(request.selection, SurveyOreCatalog.getSelectableOreNames(playerData));
            playerData.setOreSelection(validatedSelection);
            notifySelectionChanged(player);
        } catch (IllegalArgumentException exception) {
            LOG.warn("Rejected invalid survey ore selection from " + player.getCommandSenderName(), exception);
        }
        sendSelectionState(player);
    }

    public void notifySurveyCompleted(UUID ownerId, SurveyArea surveyArea) {
        if (ownerId == null || surveyArea == null) return;
        EntityPlayerMP owner = findConnectedPlayer(ownerId);
        if (owner == null) return;

        int minChunkX = surveyArea.getMinX() >> 4;
        int minChunkZ = surveyArea.getMinZ() >> 4;
        int maxChunkX = (surveyArea.getMinX() + surveyArea.getWidth() - 1) >> 4;
        int maxChunkZ = (surveyArea.getMinZ() + surveyArea.getHeight() - 1) >> 4;
        try {
            ChannelOutput output = ProspectingCraft.channel
                .openPlayer(owner, SurveyChunkNetworkProtocol.INVALIDATION_MESSAGE);
            output.writeInt(surveyArea.getDimensionId());
            output.writeInt(minChunkX);
            output.writeInt(minChunkZ);
            output.writeInt(maxChunkX);
            output.writeInt(maxChunkZ);
            output.close();
        } catch (RuntimeException exception) {
            LOG.error("Could not invalidate completed survey chunks", exception);
        }
    }

    public void notifySelectionChanged(EntityPlayerMP player) {
        if (player == null) return;
        try {
            ProspectingCraft.channel.openPlayer(player, SurveyChunkNetworkProtocol.RESET_MESSAGE)
                .close();
        } catch (RuntimeException exception) {
            LOG.error("Could not reset survey chunks after ore selection changed", exception);
        }
    }

    private void sendSelectionState(EntityPlayerMP player) {
        try {
            PlayerSurveyData playerData = PlayerSurveyData.get(player.worldObj, player.getUniqueID());
            SurveyOreSelectionState state = new SurveyOreSelectionState(
                playerData.getOreSelection(),
                SurveyOreCatalog.getSelectableOreNames(playerData));
            ChannelOutput output = ProspectingCraft.channel
                .openPlayer(player, SurveyChunkNetworkProtocol.SELECTION_STATE_MESSAGE);
            SurveyOreSelectionCodec.writeState(output, state);
            output.close();
        } catch (IOException | RuntimeException exception) {
            LOG.error("Could not send survey ore selection state", exception);
        }
    }

    private void sendSnapshot(Request request) {
        EntityPlayerMP player = request.player;
        if (player.isDead || player.dimension != request.dimensionId) return;
        try {
            PlayerSurveyData playerData = PlayerSurveyData.get(player.worldObj, player.getUniqueID());
            SurveyChunkSnapshot snapshot = SurveyChunkSnapshot
                .from(playerData, request.dimensionId, request.chunkX, request.chunkZ, playerData.getOreSelection());
            ChannelOutput output = ProspectingCraft.channel
                .openPlayer(player, SurveyChunkNetworkProtocol.RESPONSE_MESSAGE);
            SurveyChunkSnapshotCodec.write(output, snapshot);
            output.close();
        } catch (IOException | RuntimeException exception) {
            LOG.error("Could not send survey chunk snapshot", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityPlayerMP findConnectedPlayer(UUID playerId) {
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null) return null;
        ServerConfigurationManager configurationManager = server.getConfigurationManager();
        for (EntityPlayerMP player : (List<EntityPlayerMP>) configurationManager.playerEntityList)
            if (playerId.equals(player.getUniqueID())) return player;
        return null;
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

    private static final class SelectionRequest {

        private final EntityPlayerMP player;
        private final SurveyOreSelection selection;

        private SelectionRequest(EntityPlayerMP player, SurveyOreSelection selection) {
            this.player = player;
            this.selection = selection;
        }
    }
}
