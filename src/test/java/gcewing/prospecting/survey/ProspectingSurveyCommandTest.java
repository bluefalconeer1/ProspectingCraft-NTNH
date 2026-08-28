package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.minecraft.command.WrongUsageException;

import org.junit.Test;

public class ProspectingSurveyCommandTest {

    @Test
    public void usageOnlyAdvertisesPlayerOreSelection() {
        assertEquals("/pcsurvey ores <all|none|only <ore>...>", new ProspectingSurveyCommand().getCommandUsage(null));
        assertNull(new ProspectingSurveyCommand().getCommandAliases());
    }

    @Test(expected = WrongUsageException.class)
    public void rejectsRemovedColumnDiagnostic() {
        new ProspectingSurveyCommand().processCommand(null, new String[] { "check", "0", "0" });
    }
}
