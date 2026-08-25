// ----------------------------------------------------------------
//
// ProspectingCraft - Seismic Recorder Tile Entity Renderer
//
// ----------------------------------------------------------------

package gcewing.prospecting;

import static gcewing.prospecting.BaseModClient.*;

public class SeismicRecorderTERenderer extends BaseTileEntityRenderer {

    protected static ITexture red = new BaseTexture.Solid(1, 0, 0).emissive(),
        green = new BaseTexture.Solid(0, 1, 0).emissive();

    public void render(BaseTileEntity te, float dt, int destroyStage, Trans3 t, IRenderTarget target) {
        // System.out.printf("SeismicRecorderTERenderer.render\n");
        if (te instanceof SeismicRecorderTE) renderSeismicRecorder((SeismicRecorderTE) te, t, target);
    }

    protected void renderSeismicRecorder(SeismicRecorderTE te, Trans3 t, IRenderTarget target) {
        switch (te.getReadyStatus()) {
            case FIN:
                renderReadyLight(t, target);
                renderFinishedLight(t, target);
                break;
            case RUN:
                renderReadyLight(t, target);
                if (te.blink()) renderFinishedLight(t, target);
                break;
            case RDY:
                renderReadyLight(t, target);
                break;
            default:
                renderErrorLight(t, target);
        }
    }

    protected void renderReadyLight(Trans3 t, IRenderTarget target) {
        renderLight(0, green, t, target);
    }

    protected void renderFinishedLight(Trans3 t, IRenderTarget target) {
        renderLight(1, green, t, target);
    }

    protected void renderErrorLight(Trans3 t, IRenderTarget target) {
        renderLight(2, red, t, target);
    }

    protected void renderLight(int n, ITexture tex, Trans3 t, IRenderTarget target) {
        // System.out.printf("SeismicRecorderTERenderer.renderLight: %s\n", n);
        double x = 4 / 32f + n * 3 / 32f, y = 7 / 32f, w = 2 / 32f, h = 5 / 32f, z = 0.51;
        target.setTexture(tex);
        target.setNormal(new Vector3(0, 0, 1));
        target.beginQuad();
        addVertex(x, y, z, t, target);
        addVertex(x + w, y, z, t, target);
        addVertex(x + w, y + h, z, t, target);
        addVertex(x, y + h, z, t, target);
        target.endFace();
    }

    protected void addVertex(double x, double y, double z, Trans3 t, IRenderTarget target) {
        // System.out.printf("SeismicRecorderTERenderer.addVertex: (%,3f, %,3f, %,3f)\n", x, y, z);
        target.addVertex(t.p(x, y, z), 0, 0);
    }

}
