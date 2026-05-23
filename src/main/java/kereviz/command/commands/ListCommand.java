package kereviz.command.commands;

import kereviz.Kereviz;
import kereviz.command.Command;
import kereviz.module.Module;
import kereviz.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class ListCommand extends Command {
    public ListCommand() {
        super(new ArrayList<>(Arrays.asList("list", "l", "modules", "kereviz")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!Kereviz.moduleManager.modules.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sModules:&r", Kereviz.clientName));
            for (Module module : Kereviz.moduleManager.modules.values()) {
                ChatUtil.sendFormatted(String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.formatModule()));
            }
        }
    }
}
