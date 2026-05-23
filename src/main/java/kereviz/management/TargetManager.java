package kereviz.management;

import kereviz.enums.ChatColors;
import kereviz.config.ClientFiles;

import java.awt.*;

public class TargetManager extends PlayerFileManager {
    public TargetManager() {
        super(ClientFiles.listFile("enemies.txt"), new Color(ChatColors.DARK_RED.toAwtColor()));
    }
}
