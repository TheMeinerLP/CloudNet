package eu.cloudnetservice.cloudnet.v2.master.command;

import eu.cloudnetservice.cloudnet.v2.command.Command;
import eu.cloudnetservice.cloudnet.v2.command.CommandSender;
import eu.cloudnetservice.cloudnet.v2.command.TabCompletable;
import eu.cloudnetservice.cloudnet.v2.console.completer.CloudNetCompleter;
import eu.cloudnetservice.cloudnet.v2.lib.utility.document.Document;
import eu.cloudnetservice.cloudnet.v2.logging.color.ChatColor;
import eu.cloudnetservice.cloudnet.v2.master.CloudNet;
import eu.cloudnetservice.cloudnet.v2.master.network.packet.out.PacketOutConsoleSettings;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.completer.ArgumentCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandConsole extends Command implements TabCompletable {

    public CommandConsole() {
        super("console", "cloudnet.command.master.console", "c");
        description = "Allows to customize some console options";
    }

    @Override
    public void onExecuteCommand(final CommandSender sender, final ParsedLine parsedLine, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§8console §a<option> §9<value> §8| Manipulate a option for the console");
            sender.sendMessage("Options: ");
            sender.sendMessage("- showmenu | Allows to disable or enable a selection menu for tab completion");
            sender.sendMessage("- showdescription | Allows to disable or enable description for tab completion");
            sender.sendMessage("- showgroup | Allows to disable or enable grouping for tab completion");
            sender.sendMessage("- autolist | Allows to disable or enable auto list for tab completion");
            sender.sendMessage("- aliases | Allows to disable or enable aliases tab completion");
            sender.sendMessage("- elof | Allows to disable or enable erasing line on finish");
            sender.sendMessage("- color | Allows to change the color of the default tab completion");
            sender.sendMessage("- groupcolor | Allows to change the grouping color of the default tab completion");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "showdescription":
                    boolean parseBoolean = Boolean.parseBoolean(args[1]);
                    CloudNet.getInstance().getConfig().setShowDescription(parseBoolean);
                    LineReader lineReader = CloudNet.getInstance().getConsoleManager().getLineReader();
                    if (lineReader instanceof LineReaderImpl) {
                        Completer completer = ((LineReaderImpl) lineReader).getCompleter();
                        if (completer instanceof CloudNetCompleter) {
                            ((CloudNetCompleter) completer).setShowDescription(parseBoolean);
                        }
                    }
                    CloudNet.getInstance().getCommandManager().setShowDescription(parseBoolean);
                    sender.sendMessage("§aUpdate visibility of description on tab completion to: " + parseBoolean);
                    updateWrappers();
                    break;
                case "showmenu":
                    parseBoolean = Boolean.parseBoolean(args[1]);
                    lineReader = CloudNet.getInstance().getConsoleManager().getLineReader();
                    lineReader.option(LineReader.Option.MENU_COMPLETE, parseBoolean);
                    lineReader.option(LineReader.Option.AUTO_MENU, parseBoolean);
                    CloudNet.getInstance().getConfig().setShowMenu(parseBoolean);
                    sender.sendMessage("§aUpdate visibility of menu on tab completion to: " + parseBoolean);
                    updateWrappers();
                    break;
                case "showgroup":
                    parseBoolean = Boolean.parseBoolean(args[1]);
                    lineReader = CloudNet.getInstance().getConsoleManager().getLineReader();
                    lineReader.option(LineReader.Option.GROUP, parseBoolean);
                    lineReader.option(LineReader.Option.AUTO_GROUP, parseBoolean);
                    CloudNet.getInstance().getConfig().setShowGroup(parseBoolean);
                    sender.sendMessage("§aUpdate visibility of grouping on tab completion to: " + parseBoolean);
                    updateWrappers();
                    break;
                case "autolist":
                    parseBoolean = Boolean.parseBoolean(args[1]);
                    lineReader = CloudNet.getInstance().getConsoleManager().getLineReader();
                    lineReader.option(LineReader.Option.AUTO_LIST, parseBoolean);
                    CloudNet.getInstance().getConfig().setAutoList(parseBoolean);
                    sender.sendMessage("§aUpdate visibility of auto listining on tab completion to: " + parseBoolean);
                    updateWrappers();
                    break;
                case "elof":
                    parseBoolean = Boolean.parseBoolean(args[1]);
                    lineReader = CloudNet.getInstance().getConsoleManager().getLineReader();
                    lineReader.option(LineReader.Option.ERASE_LINE_ON_FINISH, parseBoolean);
                    CloudNet.getInstance().getConfig().setElof(parseBoolean);
                    sender.sendMessage("§aUpdate option of erase on line finish to: " + parseBoolean);
                    updateWrappers();
                    break;
                case "aliases":
                    parseBoolean = Boolean.parseBoolean(args[1]);
                    CloudNet.getInstance().getConfig().setAliases(parseBoolean);
                    CloudNet.getInstance().getCommandManager().setAliases(parseBoolean);
                    sender.sendMessage("§aUpdate option of aliases to: " + parseBoolean);
                    updateWrappers();
                    break;
                case "color":
                    ChatColor color = ChatColor.getByChar(args[1].charAt(1));
                    lineReader = CloudNet.getInstance().getConsoleManager().getLineReader();
                    if (lineReader instanceof LineReaderImpl) {
                        Completer completer = ((LineReaderImpl) lineReader).getCompleter();
                        if (completer instanceof ArgumentCompleter) {
                            ((CloudNetCompleter) ((ArgumentCompleter) completer).getCompleters().get(0)).setColor(color.toString());
                        }
                    }
                    CloudNet.getInstance().getConfig().setColor(color.toString());
                    sender.sendMessage("§aUpdate color to: " + color.toString() + color.getName());
                    updateWrappers();
                    break;
                case "groupcolor":
                    color = ChatColor.getByChar(args[1].charAt(1));
                    lineReader = CloudNet.getInstance().getConsoleManager().getLineReader();
                    if (lineReader instanceof LineReaderImpl) {
                        Completer completer = ((LineReaderImpl) lineReader).getCompleter();
                        if (completer instanceof ArgumentCompleter) {
                            ((CloudNetCompleter) ((ArgumentCompleter) completer).getCompleters().get(0)).setGroupColor(color.toString());
                        }
                    }
                    CloudNet.getInstance().getConfig().setGroupColor(color.toString());
                    sender.sendMessage("§aUpdate group color to: " + color.toString() + color.getName());
                    updateWrappers();
                    break;
                default:
                    sender.sendMessage("§cThis option is not available!");
                    break;
            }
        }

    }

    @Override
    public List<Candidate> onTab(final long argsLength, final String lastWord, final ParsedLine parsedLine, final String[] args) {
        List<Candidate> candidates = new ArrayList<>();

        if (parsedLine.words().size() >= 1 && (parsedLine.words().get(0).equalsIgnoreCase("c") || parsedLine.words()
                                                                                                            .get(0)
                                                                                                            .equalsIgnoreCase("console"))) {
            if (parsedLine.words().size() >= 2 && (parsedLine.words().get(1).equalsIgnoreCase("showdescription") || parsedLine.words()
                                                                                                                              .get(1)
                                                                                                                              .equalsIgnoreCase(
                                                                                                                                  "showmenu") || parsedLine
                .words()
                .get(1)
                .equalsIgnoreCase("showgroup") || parsedLine.words().get(1).equalsIgnoreCase("autolist") || parsedLine.words()
                                                                                                                      .get(1)
                                                                                                                      .equalsIgnoreCase(
                                                                                                                          "elof") || parsedLine
                .words()
                .get(1)
                .equalsIgnoreCase(
                    "aliases"))) {
                candidates.add(new Candidate("true", "§9True", "Value", "Enable option", null, null, true));
                candidates.add(new Candidate("false", "§9False", "Value", "Disable option", null, null, true));
                return candidates;
            }
            if (parsedLine.words().size() >= 2 && (parsedLine.words().get(1).equalsIgnoreCase("groupcolor") || parsedLine.words()
                                                                                                                         .get(1)
                                                                                                                         .equalsIgnoreCase(
                                                                                                                             "color"))) {
                candidates.addAll(Arrays.stream(ChatColor.values())
                                        .map(chatColor -> new Candidate(chatColor.toString(),
                                                                        chatColor.toString() + chatColor.getName(),
                                                                        "Color",
                                                                        "Color to choose",
                                                                        null,
                                                                        null,
                                                                        true))
                                        .collect(
                                            Collectors.toList()));
                return candidates;
            }
            candidates.add(new Candidate("showdescription",
                                         "§aShowDescription",
                                         "Option",
                                         "Allows to disable or enable description for tab completion",
                                         null,
                                         null,
                                         true));
            candidates.add(new Candidate("showmenu",
                                         "§aShowMenu",
                                         "Option",
                                         "Allows to disable or enable a selection menu for tab completion",
                                         null,
                                         null,
                                         true));
            candidates.add(new Candidate("showgroup",
                                         "§aShowGroup",
                                         "Option",
                                         "Allows to disable or enable grouping for tab completion",
                                         null,
                                         null,
                                         true));
            candidates.add(new Candidate("autolist",
                                         "§aautolist",
                                         "Option",
                                         "Allows to disable or enable auto list for tab completion",
                                         null,
                                         null,
                                         true));
            candidates.add(new Candidate("elof",
                                         "§aelof",
                                         "Option",
                                         "Allows to disable or enable erasing line on finish",
                                         null,
                                         null,
                                         true));
            candidates.add(new Candidate("color",
                                         "§acolor",
                                         "Option",
                                         "Allows to change the color of the default tab completion",
                                         null,
                                         null,
                                         true));
            candidates.add(new Candidate("groupcolor",
                                         "§agroupcolor",
                                         "Option",
                                         "Allows to change the grouping color of the default tab completion",
                                         null,
                                         null,
                                         true));
            candidates.add(new Candidate("aliases",
                                         "§aaliases",
                                         "Option",
                                         "Allows to disable or enable aliases tab completion",
                                         null,
                                         null,
                                         true));
            return candidates;
        }

        return candidates;
    }

    private void updateWrappers() {
        CloudNet.getInstance().getWrappers().values().forEach(
            wrapper -> wrapper.sendPacket(new PacketOutConsoleSettings(new Document()
                                                                           .append("console",
                                                                                   new Document()
                                                                                       .append("aliases", CloudNet.getInstance().getConfig().isAliases())
                                                                                       .append("showdescription", CloudNet.getInstance().getConfig().isShowDescription())
                                                                                       .append("showgroup", CloudNet.getInstance().getConfig().isShowGroup())
                                                                                       .append("elof", CloudNet.getInstance().getConfig().isElof())
                                                                                       .append("showmenu", CloudNet.getInstance().getConfig().isShowMenu())
                                                                                       .append("autolist", CloudNet.getInstance().getConfig().isAutoList())
                                                                                       .append("groupcolor", CloudNet.getInstance().getConfig().getGroupColor())
                                                                                       .append("color", CloudNet.getInstance().getConfig().getColor())
                                                                           )))
        );
    }
}
