package kereviz.command.commands;

import kereviz.Kereviz;
import kereviz.command.Command;
import kereviz.config.ClientFiles;
import kereviz.config.Config;
import kereviz.enums.ChatColors;
import kereviz.util.ChatUtil;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super(new ArrayList<>(Arrays.asList("config", "cfg", "c")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            sendUsage(args.get(0));
            return;
        }

        String subCommand = args.get(1);
        if (subCommand.equalsIgnoreCase("l")) {
            subCommand = args.size() < 3 ? "list" : "load";
        }

        String sub = subCommand.toLowerCase(Locale.ROOT);
        switch (sub) {
            case "load":
            case "reload":
                load(args);
                return;
            case "s":
            case "save":
                save(args);
                return;
            case "rename":
            case "ren":
                rename(args);
                return;
            case "delete":
            case "del":
            case "remove":
            case "rm":
                delete(args);
                return;
            case "list":
                list();
                return;
            case "current":
            case "active":
                ChatUtil.sendFormatted(String.format("%sCurrent config: &a&o%s&r", Kereviz.clientName, Config.lastConfig));
                return;
            case "f":
            case "folder":
            case "dir":
            case "directory":
                folder();
                return;
            default:
                ChatUtil.sendFormatted(String.format("%sInvalid argument (&o%s&r)&r", Kereviz.clientName, args.get(1)));
                sendUsage(args.get(0));
        }
    }

    private void load(ArrayList<String> args) {
        if (args.size() < 3) {
            ChatUtil.sendFormatted(String.format("%sMissing config name&r", Kereviz.clientName));
            return;
        }
        new Config(join(args, 2, args.size()), false).load();
    }

    private void save(ArrayList<String> args) {
        if (args.size() < 3) {
            new Config(Config.lastConfig, true).save();
            return;
        }
        new Config(join(args, 2, args.size()), true).save();
    }

    private void rename(ArrayList<String> args) {
        if (args.size() < 4) {
            ChatUtil.sendFormatted(String.format("%sUsage: .config rename <old> to <new>&r", Kereviz.clientName));
            return;
        }

        int split = -1;
        for (int i = 2; i < args.size(); i++) {
            if (args.get(i).equalsIgnoreCase("to")) {
                split = i;
                break;
            }
        }

        String oldName;
        String newName;
        if (split > 2 && split < args.size() - 1) {
            oldName = join(args, 2, split);
            newName = join(args, split + 1, args.size());
        } else {
            oldName = args.get(2);
            newName = join(args, 3, args.size());
        }

        String normalizedNewName = ClientFiles.normalizeConfigName(newName);
        if (ClientFiles.renameConfig(oldName, normalizedNewName)) {
            if (Config.lastConfig != null && Config.lastConfig.equalsIgnoreCase(ClientFiles.normalizeConfigName(oldName))) {
                Config.lastConfig = normalizedNewName;
            }
            ChatUtil.sendFormatted(String.format("%sRenamed config to &a&o%s&r", Kereviz.clientName, normalizedNewName));
        } else {
            ChatUtil.sendFormatted(String.format("%sCouldn't rename config. Check the name or duplicate target.&r", Kereviz.clientName));
        }
    }

    private void delete(ArrayList<String> args) {
        if (args.size() < 3) {
            ChatUtil.sendFormatted(String.format("%sMissing config name&r", Kereviz.clientName));
            return;
        }

        String name = join(args, 2, args.size());
        if (ClientFiles.deleteConfig(name)) {
            ChatUtil.sendFormatted(String.format("%sDeleted config (&a&o%s&r)&r", Kereviz.clientName, ClientFiles.normalizeConfigName(name)));
        } else {
            ChatUtil.sendFormatted(String.format("%sConfig not found (&c&o%s&r)&r", Kereviz.clientName, ClientFiles.normalizeConfigName(name)));
        }
    }

    private void list() {
        File[] configs = ClientFiles.listConfigFiles();
        if (configs.length == 0) {
            ChatUtil.sendFormatted(String.format("%sNo configs found (&o%s&r)&r", Kereviz.clientName, ClientFiles.path(ClientFiles.configsDir())));
            return;
        }

        ChatUtil.sendFormatted(String.format("%sConfigs:&r", Kereviz.clientName));
        for (File file : configs) {
            String name = ClientFiles.displayName(file);
            String formatted = ChatColors.formatColor(String.format("&7>>&r &o%s&r &7(%s)&r", name, formatAge(file)));
            String command = String.format(".config load %s", name);
            ChatUtil.send(
                    new ChatComponentText(formatted)
                            .setChatStyle(
                                    new ChatStyle()
                                            .setChatClickEvent(new ClickEvent(Action.RUN_COMMAND, command))
                                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(command)))
                            )
            );
        }
    }

    private void folder() {
        try {
            Desktop.getDesktop().open(ClientFiles.baseDir());
        } catch (Exception e) {
            ChatUtil.sendFormatted(String.format("%sFailed to open (&o%s&r)&r", Kereviz.clientName, ClientFiles.path(ClientFiles.baseDir())));
        }
    }

    private void sendUsage(String command) {
        ChatUtil.sendFormatted(
                String.format(
                        "%sUsage: .%s load/save/delete <name> | .%s rename <old> to <new> | .%s list/folder/current&r",
                        Kereviz.clientName,
                        command,
                        command,
                        command
                )
        );
    }

    private String join(ArrayList<String> args, int start, int end) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args.get(i));
        }
        return builder.toString();
    }

    private String formatAge(File file) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - file.lastModified()) / 1000L);
        if (seconds < 60L) {
            return "just now";
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes + "m ago";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + "h ago";
        }
        return (hours / 24L) + "d ago";
    }
}
