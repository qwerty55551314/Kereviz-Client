package kereviz.management;

import kereviz.enums.ChatColors;
import kereviz.config.ClientFiles;

import java.awt.*;

public class FriendManager extends PlayerFileManager {
    public FriendManager() {
        super(ClientFiles.listFile("friends.txt"), new Color(ChatColors.DARK_GREEN.toAwtColor()));
    }
}
