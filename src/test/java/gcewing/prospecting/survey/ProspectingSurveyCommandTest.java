package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

public class ProspectingSurveyCommandTest {

    @Test
    public void exposesShortCommandAlias() {
        assertEquals(Collections.singletonList("ps"), new ProspectingSurveyCommand().getCommandAliases());
    }

    @Test
    public void acceptsLongAndShortDiagnosticSubcommands() {
        assertTrue(ProspectingSurveyCommand.isColumnDiagnostic("column"));
        assertTrue(ProspectingSurveyCommand.isColumnDiagnostic("CHECK"));
        assertFalse(ProspectingSurveyCommand.isColumnDiagnostic("ores"));
    }
}
