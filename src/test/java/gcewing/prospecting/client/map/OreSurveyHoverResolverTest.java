package gcewing.prospecting.client.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

import gcewing.prospecting.survey.PlayerSurveyData;
import gcewing.prospecting.survey.SurveyArea;
import gcewing.prospecting.survey.SurveyChunkSnapshot;
import gcewing.prospecting.survey.SurveyColumnSnapshot;

public class OreSurveyHoverResolverTest {

    private static final UUID OWNER_ID = UUID.fromString("77910713-3ba1-4f00-8000-000000000007");

    @Test
    public void resolvesTheExactOreBearingColumn() {
        SurveyChunkSnapshot snapshot = newSnapshot();

        SurveyColumnSnapshot column = OreSurveyHoverResolver.findOreColumn(snapshot, 100, -50, 101, -50);

        assertEquals(Arrays.asList("Iron", "Gold"), column.getTopOreNames());
    }

    @Test
    public void ignoresSurveyedEmptyAndUnsurveyedColumns() {
        SurveyChunkSnapshot snapshot = newSnapshot();

        assertNull(OreSurveyHoverResolver.findOreColumn(snapshot, 100, -50, 100, -50));
        assertNull(OreSurveyHoverResolver.findOreColumn(snapshot, 100, -50, 102, -50));
    }

    @Test
    public void ignoresColumnsOutsideTheRenderedChunk() {
        SurveyChunkSnapshot snapshot = newSnapshot();

        assertNull(OreSurveyHoverResolver.findOreColumn(snapshot, 100, -50, 99, -50));
        assertNull(OreSurveyHoverResolver.findOreColumn(snapshot, 100, -50, 116, -50));
        assertNull(OreSurveyHoverResolver.findOreColumn(snapshot, 100, -50, 101, -51));
        assertNull(OreSurveyHoverResolver.findOreColumn(snapshot, 100, -50, 101, -34));
    }

    private static SurveyChunkSnapshot newSnapshot() {
        PlayerSurveyData data = new PlayerSurveyData(PlayerSurveyData.getDataName(OWNER_ID));
        SurveyArea area = new SurveyArea(0, 32, 48, 2, 1);
        area.markSurveyed(32, 48);
        area.recordOre(33, 48, "Iron", 3);
        area.recordOre(33, 48, "Gold", 2);
        data.addSurveyArea(area);
        return SurveyChunkSnapshot.from(data, 0, 2, 3);
    }
}
