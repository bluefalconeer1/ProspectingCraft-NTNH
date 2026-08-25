//------------------------------------------------------
//
//   ProspectingCraft - Client Proxy
//
//------------------------------------------------------

package gcewing.prospecting;

import net.minecraftforge.client.MinecraftForgeClient;

public class ProspectingCraftClient extends BaseModClient<ProspectingCraft> {

    public ProspectingCraftClient(ProspectingCraft mod) {
        super(mod);
        debugModelRegistration = true;

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
