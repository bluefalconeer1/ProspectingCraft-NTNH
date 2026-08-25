package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

public class SurveyOreSelectionCodecTest {

    @Test
    public void customSelectionStateSurvivesRoundTrip() throws IOException {
        SurveyOreSelection selection = SurveyOreSelection.custom(Arrays.asList("Lignite", "Gold"));
        SurveyOreSelectionState original = new SurveyOreSelectionState(
            selection,
            Arrays.asList("Lignite", "Iron", "Gold"));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        SurveyOreSelectionCodec.writeState(new DataOutputStream(bytes), original);
        SurveyOreSelectionState restored = SurveyOreSelectionCodec
            .readState(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertEquals(
            SurveyOreSelection.Mode.CUSTOM,
            restored.getSelection()
                .getMode());
        assertTrue(
            restored.getSelection()
                .includes("Lignite"));
        assertTrue(
            restored.getSelection()
                .includes("Gold"));
        assertFalse(
            restored.getSelection()
                .includes("Iron"));
        assertEquals(Arrays.asList("Gold", "Iron", "Lignite"), restored.getSelectableOreNames());
    }

    @Test
    public void showAllSelectionSurvivesUpdateRoundTrip() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        SurveyOreSelectionCodec.writeSelection(new DataOutputStream(bytes), SurveyOreSelection.showAll());

        SurveyOreSelection restored = SurveyOreSelectionCodec
            .readSelection(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertEquals(SurveyOreSelection.Mode.SHOW_ALL, restored.getMode());
        assertTrue(restored.includes("AnyFutureOre"));
    }

    @Test(expected = IOException.class)
    public void rejectsUnsupportedVersion() throws IOException {
        SurveyOreSelectionCodec.readSelection(new DataInputStream(new ByteArrayInputStream(new byte[] { 2 })));
    }
}
