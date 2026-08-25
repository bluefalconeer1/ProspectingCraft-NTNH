package gcewing.prospecting.survey;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

/** Temporary operator command for validating survey data during development. */
public class ProspectingSurveyCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "pcsurvey";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/pcsurvey column <x> <z>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) {
        if (arguments.length != 3 || !"column".equals(arguments[0]))
            throw new WrongUsageException(getCommandUsage(sender));

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
