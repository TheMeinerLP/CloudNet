package eu.cloudnetservice.cloudnet.v2.master.command;

import eu.cloudnetservice.cloudnet.v2.command.Command;
import eu.cloudnetservice.cloudnet.v2.command.CommandSender;
import eu.cloudnetservice.cloudnet.v2.master.CloudNet;
import org.jline.reader.ParsedLine;

import java.util.ArrayList;
import java.util.List;

public class CommandHelp extends Command {

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    public CommandHelp() {
        super("help", "cloudnet.command.help");
    }

    @Override
    public void onExecuteCommand(CommandSender sender, ParsedLine parsedLine) {
        List<String> messages = new ArrayList<>(CloudNet.getInstance().getCommandManager().getCommands().size());
        int maxLength = 0;
        for (String command : CloudNet.getInstance().getCommandManager().getCommands()) {
            if (command.length() > maxLength) {
                maxLength = command.length();
            }
        }

        for (String command : CloudNet.getInstance().getCommandManager().getCommands()) {
            StringBuilder stringBuilder = new StringBuilder();
            int spaces = maxLength - command.length();
            stringBuilder.append(command);
            for (int i = 0; i < spaces; i++) {
                stringBuilder.append(" ");
            }
            stringBuilder.append(" | §8");
            stringBuilder.append(CloudNet.getInstance().getCommandManager().getCommand(command).getDescription());
            messages.add(stringBuilder.toString());

        }
        sender.sendMessage(messages.toArray(EMPTY_STRING_ARRAY));
    }
}
