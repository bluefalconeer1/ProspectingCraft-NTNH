package gcewing.prospecting;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/** Moves Surveyor GUI activation from the network callback onto the main server tick. */
public final class SurveyorActivationServerHandler {

    public static final SurveyorActivationServerHandler INSTANCE = new SurveyorActivationServerHandler();

    private static final int MAX_PENDING_REQUESTS = 128;
    private static final int MAX_REQUESTS_PER_TICK = 16;

    private final ConcurrentLinkedQueue<ActivationRequest> requests = new ConcurrentLinkedQueue<ActivationRequest>();
    private final AtomicInteger pendingRequestCount = new AtomicInteger();

    private SurveyorActivationServerHandler() {}

    public void enqueue(EntityPlayer player, SeismicRecorderContainer container) {
        if (!(player instanceof EntityPlayerMP) || container == null) return;
        int pending = pendingRequestCount.incrementAndGet();
        if (pending > MAX_PENDING_REQUESTS) {
            pendingRequestCount.decrementAndGet();
            return;
        }
        requests.add(new ActivationRequest(player, container));
    }

    public void processRequests() {
        for (int i = 0; i < MAX_REQUESTS_PER_TICK; i++) {
            ActivationRequest request = requests.poll();
            if (request == null) return;
            pendingRequestCount.decrementAndGet();
            request.container.tryActivateFor(request.player);
        }
    }

    private static final class ActivationRequest {

        private final EntityPlayer player;
        private final SeismicRecorderContainer container;

        private ActivationRequest(EntityPlayer player, SeismicRecorderContainer container) {
            this.player = player;
            this.container = container;
        }
    }
}
