// ------------------------------------------------------
//
// ProspectingCraft - Client Proxy
//
// ------------------------------------------------------

package gcewing.prospecting;

import net.minecraftforge.client.MinecraftForgeClient;

import com.gtnewhorizons.navigator.api.NavigatorApi;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import gcewing.prospecting.client.map.OreSurveyLayerManager;
import gcewing.prospecting.client.map.SurveyChunkClientHandler;

public class ProspectingCraftClient extends BaseModClient<ProspectingCraft> {

    public ProspectingCraftClient(ProspectingCraft mod) {
        super(mod);
        ProspectingCraft.channel.handlers.add(SurveyChunkClientHandler.INSTANCE);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        NavigatorApi.registerLayerManager(OreSurveyLayerManager.INSTANCE);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) SurveyChunkClientHandler.INSTANCE.processQueues();
    }

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        SurveyChunkClientHandler.INSTANCE.reset();
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        SurveyChunkClientHandler.INSTANCE.reset();
    }

    @Override
    protected void registerScreens() {
        addScreen(ProspectingCraft.seismicRecorderGui, SeismicRecorderScreen.class);
        addScreen(ProspectingCraft.gsaKitGui, GSAKitScreen.class);
    }

    @Override
    protected void registerItemRenderers() {
        MinecraftForgeClient.registerItemRenderer(ProspectingCraft.mod.itemSeismicSurvey, new SeismicSurveyRenderer());
    }

    @Override
    protected void registerTileEntityRenderers() {
        addTileEntityRenderer(SeismicRecorderTE.class, new SeismicRecorderTERenderer());
    }

}
