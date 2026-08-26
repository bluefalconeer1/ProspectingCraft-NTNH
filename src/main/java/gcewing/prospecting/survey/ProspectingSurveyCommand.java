package gcewing.prospecting.survey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import gcewing.prospecting.survey.network.SurveyChunkServerHandler;

/** Player ore-display controls and temporary operator survey diagnostics. */
public class ProspectingSurveyCommand extends CommandBase {

    private static final List<String> COMMAND_ALIASES = Collections.singletonList("ps");

    @Override
    public String getCommandName() {
        return "pcsurvey";
    }

    @Override
    public List<String> getCommandAliases() {
        return COMMAND_ALIASES;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/pcsurvey ores <all|none|only <ore>...> | /pcsurvey column <x> <z> | /ps check <x> <z>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) {
        if (arguments.length > 0 && "ores".equalsIgnoreCase(arguments[0])) {
            processOreSelection(sender, arguments);
            return;
        }
        if (arguments.length == 3 && isColumnDiagnostic(arguments[0])) {
            processColumn(sender, arguments);
            return;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }

    static boolean isColumnDiagnostic(String subcommand) {
        return "column".equalsIgnoreCase(subcommand) || "check".equalsIgnoreCase(subcommand);
    }

    private void processOreSelection(ICommandSender sender, String[] arguments) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        PlayerSurveyData data = PlayerSurveyData.get(player.worldObj, player.getUniqueID());
        if (arguments.length == 1) {
            showOreSelection(sender, data.getOreSelection());
            return;
        }

        SurveyOreSelection selection;
        if (arguments.length == 2 && "all".equalsIgnoreCase(arguments[1])) {
            selection = SurveyOreSelection.showAll();
        } else if (arguments.length == 2 && "none".equalsIgnoreCase(arguments[1])) {
            selection = SurveyOreSelection.custom(Collections.<String>emptyList());
        } else if (arguments.length >= 3 && "only".equalsIgnoreCase(arguments[1])) {
            selection = SurveyOreSelection.custom(resolveDiscoveredOreNames(data, arguments));
        } else {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        data.setOreSelection(selection);
        SurveyChunkServerHandler.INSTANCE.notifySelectionChanged(player);
        showOreSelection(sender, selection);
    }

    private static List<String> resolveDiscoveredOreNames(PlayerSurveyData data, String[] arguments) {
        Set<String> discoveredOreNames = SurveyOreCatalog.getSelectableOreNames(data);
        List<String> resolvedNames = new ArrayList<String>();
        for (int i = 2; i < arguments.length; i++) {
            String resolvedName = findIgnoringCase(discoveredOreNames, arguments[i]);
            if (resolvedName == null)
                throw new CommandException("Ore is not a selectable registered block ore: " + arguments[i]);
            resolvedNames.add(resolvedName);
        }
        return resolvedNames;
    }

    private static String findIgnoringCase(Set<String> names, String requestedName) {
        for (String name : names) if (name.equalsIgnoreCase(requestedName)) return name;
        return null;
    }

    private static void showOreSelection(ICommandSender sender, SurveyOreSelection selection) {
        if (selection.getMode() == SurveyOreSelection.Mode.SHOW_ALL) {
            sender.addChatMessage(new ChatComponentText("Ore Survey display: all discovered ores"));
            return;
        }
        if (selection.getSelectedOreNames()
            .isEmpty()) {
            sender.addChatMessage(new ChatComponentText("Ore Survey display: no ores"));
            return;
        }
        sender
            .addChatMessage(new ChatComponentText("Ore Survey display only: " + join(selection.getSelectedOreNames())));
    }

    private static String join(Iterable<String> names) {
        StringBuilder result = new StringBuilder();
        for (String name : names) {
            if (result.length() > 0) result.append(", ");
            result.append(name);
        }
        return result.toString();
    }

    private void processColumn(ICommandSender sender, String[] arguments) {
        if (!sender.canCommandSenderUseCommand(2, getCommandName()))
            throw new CommandException("commands.generic.permission");

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        int x = parseInt(sender, arguments[1]);
        int z = parseInt(sender, arguments[2]);
        int dimensionId = player.worldObj.provider.dimensionId;
        PlayerSurveyData data = PlayerSurveyData.get(player.worldObj, player.getUniqueID());
        SurveyColumn column = data.getColumn(dimensionId, x, z);

        sender
            .addChatMessage(new ChatComponentText("Survey column: dimension " + dimensionId + ", X=" + x + ", Z=" + z));
        if (column == null) {
            sender.addChatMessage(new ChatComponentText("Surveyed: no"));
            return;
        }

        sender.addChatMessage(new ChatComponentText("Surveyed: yes"));
        List<String> oreNames = column.getTopOreNames(Integer.MAX_VALUE);
        if (oreNames.isEmpty()) {
            sender.addChatMessage(new ChatComponentText("Ores: none"));
            return;
        }
        for (String oreName : oreNames)
            sender.addChatMessage(new ChatComponentText(oreName + ": " + column.getOreCount(oreName)));
    }
}
