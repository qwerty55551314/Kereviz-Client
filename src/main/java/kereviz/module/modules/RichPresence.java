package kereviz.module.modules;

import kereviz.event.EventTarget;
import kereviz.event.types.EventType;
import kereviz.events.TickEvent;
import kereviz.module.Module;
import kereviz.property.properties.BooleanProperty;
import kereviz.property.properties.TextProperty;
import kereviz.util.discord.DiscordIpcClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public class RichPresence extends Module {
    private static final String APPLICATION_ID = "1507758197656522863";
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty showServer = new BooleanProperty("Show Server", true);
    public final BooleanProperty showUsername = new BooleanProperty("Show Username", true);
    public final BooleanProperty showElapsed = new BooleanProperty("Show Elapsed", true);
    public final TextProperty detailsText = new TextProperty("Details", "Kereviz Client", () -> !showServer.getValue());
    public final TextProperty serverPrefix = new TextProperty("Server Prefix", "Server", () -> showServer.getValue());
    public final TextProperty usernamePrefix = new TextProperty("Username Prefix", "Account", () -> showUsername.getValue());
    public final TextProperty idleText = new TextProperty("Idle Text", "Main Menu", () -> showServer.getValue());

    private final DiscordIpcClient ipcClient = new DiscordIpcClient(APPLICATION_ID);
    private long startedAt;
    private long lastUpdate;
    private String lastSignature = "";

    public RichPresence() {
        super("RichPresence", true, false, "Shows Kereviz Client status on Discord.");
        this.startedAt = System.currentTimeMillis() / 1000L;
    }

    @Override
    public void onEnabled() {
        this.startedAt = System.currentTimeMillis() / 1000L;
        this.lastUpdate = 0L;
        this.lastSignature = "";
        updatePresence(true);
    }

    @Override
    public void onDisabled() {
        this.ipcClient.clearActivity();
        this.ipcClient.close();
        this.lastSignature = "";
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.POST || !this.isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastUpdate >= 5000L) {
            updatePresence(false);
        }
    }

    private void updatePresence(boolean force) {
        String details = this.showServer.getValue()
                ? this.serverPrefix.getValue() + ": " + getServerName()
                : this.detailsText.getValue();
        String state = this.showUsername.getValue()
                ? this.usernamePrefix.getValue() + ": " + getUsername()
                : "";
        long timestamp = this.showElapsed.getValue() ? this.startedAt : 0L;
        String signature = details + "\n" + state + "\n" + timestamp;

        if (!force && signature.equals(this.lastSignature)) {
            this.lastUpdate = System.currentTimeMillis();
            return;
        }

        if (this.ipcClient.setActivity(details, state, timestamp, "Kereviz Client")) {
            this.lastSignature = signature;
        }
        this.lastUpdate = System.currentTimeMillis();
    }

    private String getUsername() {
        try {
            return mc.getSession() == null ? "Unknown" : mc.getSession().getUsername();
        } catch (Exception ignored) {
            return "Unknown";
        }
    }

    private String getServerName() {
        try {
            if (mc.theWorld == null) {
                return this.idleText.getValue();
            }
            if (mc.isIntegratedServerRunning()) {
                return "Singleplayer";
            }
            ServerData data = mc.getCurrentServerData();
            if (data != null && data.serverIP != null && !data.serverIP.trim().isEmpty()) {
                return data.serverIP;
            }
        } catch (Exception ignored) {
        }
        return this.idleText.getValue();
    }
}
