package gcewing.prospecting.client.map;

import gcewing.prospecting.survey.SurveyChunkSnapshot;
import gcewing.prospecting.survey.SurveyColumnSnapshot;

final class OreSurveyHoverResolver {

    private OreSurveyHoverResolver() {}

    static SurveyColumnSnapshot findOreColumn(SurveyChunkSnapshot snapshot, double chunkMapX, double chunkMapZ,
        double mouseMapX, double mouseMapZ) {
        if (snapshot == null) return null;

        int localX = (int) Math.floor(mouseMapX - chunkMapX);
        int localZ = (int) Math.floor(mouseMapZ - chunkMapZ);
        if (localX < 0 || localX >= SurveyChunkSnapshot.CHUNK_WIDTH
            || localZ < 0
            || localZ >= SurveyChunkSnapshot.CHUNK_WIDTH) return null;

        SurveyColumnSnapshot column = snapshot.getColumn(localX, localZ);
        return column.getState() == SurveyColumnSnapshot.State.ORE_BEARING ? column : null;
    }
}
