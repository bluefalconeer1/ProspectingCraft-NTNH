package gcewing.prospecting;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class SeismicRecorderTETest {

    @Test
    public void ownerIdSurvivesNbtRoundTrip() {
        UUID expectedOwnerId = UUID.fromString("17910713-3ba1-4f00-8000-000000000001");
        UUID expectedSurveyId = UUID.fromString("27910713-3ba1-4f00-8000-000000000002");
        SeismicRecorderTE original = new SeismicRecorderTE() {

            @Override
            public void markChanged() {}
        };
        original.setOwnerId(expectedOwnerId);
        original.activeSurveyId = expectedSurveyId;

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeContentsToNBT(nbt);

        SeismicRecorderTE restored = new SeismicRecorderTE();
        restored.readContentsFromNBT(nbt);

        assertEquals(expectedOwnerId, restored.getOwnerId());
        assertEquals(expectedSurveyId, restored.activeSurveyId);
    }
}
