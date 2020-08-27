package eu.cloudnetservice.cloudnet.v2.wrapper.command;

import eu.cloudnetservice.cloudnet.v2.command.Command;
import eu.cloudnetservice.cloudnet.v2.command.CommandSender;
import eu.cloudnetservice.cloudnet.v2.wrapper.util.FileUtility;
import org.jline.reader.ParsedLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CommandClearCache extends Command {

    public CommandClearCache() {
        super("clearcache", "cloudnet.command.clearcache");
    }

    @Override
    public void onExecuteCommand(CommandSender sender, ParsedLine parsedLine, String[] args) {
        try {
            FileUtility.deleteDirectory(new File("local/cache"));
            Files.createDirectories(Paths.get("local/cache/web_templates"));
            Files.createDirectories(Paths.get("local/cache/web_plugins"));

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        sender.sendMessage("The Cache was cleared!");
    }
}
