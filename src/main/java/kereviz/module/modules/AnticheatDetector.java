package kereviz.module.modules;

import kereviz.Kereviz;
import kereviz.event.EventTarget;
import kereviz.event.types.EventType;
import kereviz.events.LoadWorldEvent;
import kereviz.events.PacketEvent;
import kereviz.events.TickEvent;
import kereviz.module.Module;
import kereviz.property.properties.BooleanProperty;
import kereviz.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnticheatDetector extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty chat = new BooleanProperty("chat", true);
    public final BooleanProperty notifications = new BooleanProperty("notifications", true);
    public final BooleanProperty debug = new BooleanProperty("debug", false);

    private final List<Integer> actionNumbers = new ArrayList<>();
    private boolean checking;
    private int ticksPassed;
    private String detected = "Idle";

    public AnticheatDetector() {
        super("AnticheatDetector", false, false, "Detects common server anticheats from transaction packet patterns.");
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.detected};
    }

    @Override
    public synchronized void onEnabled() {
        this.startCheck();
    }

    @Override
    public synchronized void onDisabled() {
        this.actionNumbers.clear();
        this.ticksPassed = 0;
        this.checking = false;
        this.detected = "Idle";
    }

    @EventTarget
    public synchronized void onLoadWorld(LoadWorldEvent event) {
        if (this.isEnabled()) {
            this.startCheck();
        }
    }

    @EventTarget
    public synchronized void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE) {
            return;
        }

        if (event.getPacket() instanceof S01PacketJoinGame) {
            this.startCheck();
            return;
        }

        if (this.checking && event.getPacket() instanceof S32PacketConfirmTransaction) {
            S32PacketConfirmTransaction packet = (S32PacketConfirmTransaction) event.getPacket();
            this.handleTransaction(packet.getActionNumber());
        }
    }

    @EventTarget
    public synchronized void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || !this.checking) {
            return;
        }

        if (++this.ticksPassed > 40) {
            this.finish("None");
        }
    }

    private void startCheck() {
        this.actionNumbers.clear();
        this.ticksPassed = 0;
        this.checking = true;
        this.detected = "Scanning";
    }

    private void handleTransaction(int actionNumber) {
        this.actionNumbers.add(actionNumber);
        this.ticksPassed = 0;

        if (this.debug.getValue()) {
            ChatUtil.sendFormatted(String.format("&7[&a%s&7] &fID: &a%d&r", this.getName(), actionNumber));
        }

        if (this.actionNumbers.size() >= 5) {
            this.analyzeActionNumbers();
        }
    }

    private void analyzeActionNumbers() {
        List<Integer> diffs = this.getDiffs();
        int first = this.actionNumbers.get(0);
        String result = null;

        if (this.isHypixelAddress()) {
            result = "Watchdog";
        } else if (this.allDiffsEqual(diffs)) {
            int diff = diffs.get(0);
            if (diff == 1) {
                if (this.inRange(first, -23772, -23762)) {
                    result = "Vulcan";
                } else if (this.inRange(first, 95, 105) || this.inRange(first, -20005, -19995)) {
                    result = "Matrix";
                } else if (this.inRange(first, -32773, -32762)) {
                    result = "Grizzly";
                } else {
                    result = "Verus";
                }
            } else if (diff == -1) {
                if (this.inRange(first, -8287, -8280)) {
                    result = "Errata";
                } else if (first < -3000) {
                    result = "Intave";
                } else if (this.inRange(first, -5, 0)) {
                    result = "Grim";
                } else if (this.inRange(first, -3000, -2995)) {
                    result = "Karhu";
                } else {
                    result = "Polar";
                }
            }
        } else if (this.actionNumbers.get(0).equals(this.actionNumbers.get(1)) && this.actionDiffsFromIndex(3, 1)) {
            result = "Verus";
        } else if (diffs.get(0) >= 100 && diffs.get(1) == -1 && this.diffsFromIndex(diffs, 2, -1)) {
            result = "Polar";
        } else if (first < -3000 && this.actionNumbers.contains(0)) {
            result = "Intave";
        } else if (this.matchesOldVulcan()) {
            result = "Old Vulcan";
        }

        if (result == null) {
            result = "Unknown";
            if (this.debug.getValue()) {
                this.logNumbers(diffs);
            }
        }

        this.finish(result);
    }

    private void finish(String result) {
        this.detected = result;
        this.checking = false;
        this.ticksPassed = 0;
        this.actionNumbers.clear();

        String message = "Anticheat detected: " + result;
        if (this.notifications.getValue() && Kereviz.notificationManager != null) {
            Kereviz.notificationManager.add(message, 3000L, 0x14FF00);
        }
        if (this.chat.getValue()) {
            ChatUtil.sendFormatted(String.format("%s%s: &a%s&r", Kereviz.clientName, this.getName(), message));
        }
    }

    private List<Integer> getDiffs() {
        List<Integer> diffs = new ArrayList<>();
        for (int i = 1; i < this.actionNumbers.size(); i++) {
            diffs.add(this.actionNumbers.get(i) - this.actionNumbers.get(i - 1));
        }
        return diffs;
    }

    private boolean allDiffsEqual(List<Integer> diffs) {
        if (diffs.isEmpty()) {
            return false;
        }

        int firstDiff = diffs.get(0);
        for (Integer diff : diffs) {
            if (diff != firstDiff) {
                return false;
            }
        }
        return true;
    }

    private boolean actionDiffsFromIndex(int startIndex, int expectedDiff) {
        for (int i = startIndex; i < this.actionNumbers.size(); i++) {
            if (this.actionNumbers.get(i) - this.actionNumbers.get(i - 1) != expectedDiff) {
                return false;
            }
        }
        return true;
    }

    private boolean diffsFromIndex(List<Integer> diffs, int startIndex, int expectedDiff) {
        for (int i = startIndex; i < diffs.size(); i++) {
            if (diffs.get(i) != expectedDiff) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesOldVulcan() {
        return this.actionNumbers.size() >= 3
                && this.actionNumbers.get(0) == -30767
                && this.actionNumbers.get(1) == -30766
                && this.actionNumbers.get(2) == -25767
                && this.actionDiffsFromIndex(4, 1);
    }

    private boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    private boolean isHypixelAddress() {
        ServerData serverData = mc.getCurrentServerData();
        if (serverData == null || serverData.serverIP == null) {
            return false;
        }

        String serverIp = serverData.serverIP.toLowerCase(Locale.ROOT);
        return serverIp.equals("hypixel.net") || serverIp.endsWith(".hypixel.net") || serverIp.contains("hypixel.net:");
    }

    private void logNumbers(List<Integer> diffs) {
        ChatUtil.sendFormatted(String.format("&7[&a%s&7] &fAction Numbers: &7%s&r", this.getName(), this.joinIntegers(this.actionNumbers)));
        ChatUtil.sendFormatted(String.format("&7[&a%s&7] &fDifferences: &7%s&r", this.getName(), this.joinIntegers(diffs)));
    }

    private String joinIntegers(List<Integer> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }
}
