package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SurveyCellMaskTest {

    @Test
    public void keepsTheCenterAndEdgeMidpointsAtFullStrength() {
        assertEquals(144, alphaAt(12, 12));
        assertEquals(144, alphaAt(12, 0));
        assertEquals(144, alphaAt(0, 12));
    }

    @Test
    public void fadesCornersSymmetricallyWithoutCreatingTransparentGaps() {
        int cornerAlpha = alphaAt(0, 0);

        assertEquals(36, cornerAlpha);
        assertEquals(cornerAlpha, alphaAt(23, 0));
        assertEquals(cornerAlpha, alphaAt(0, 23));
        assertEquals(cornerAlpha, alphaAt(23, 23));
        assertTrue(alphaAt(2, 2) > cornerAlpha);
        assertTrue(alphaAt(2, 2) < 144);
    }

    @Test
    public void aZeroRadiusPreservesTheSquareCell() {
        assertEquals(144, SurveyCellMask.roundedCornerAlpha(144, 24, 0, 0, 0, 0, 100));
    }

    private static int alphaAt(int localX, int localZ) {
        return SurveyCellMask.roundedCornerAlpha(144, 24, localX, localZ, 6, 3, 25);
    }
}
