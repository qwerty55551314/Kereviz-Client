package kereviz.util.discord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DiscordIpcClient {
    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;

    private final String applicationId;
    private RandomAccessFile pipe;

    public DiscordIpcClient(String applicationId) {
        this.applicationId = applicationId;
    }

    public boolean isConnected() {
        return this.pipe != null;
    }

    public synchronized boolean setActivity(String details, String state, long startTimestamp, String largeText) {
        if (!connect()) {
            return false;
        }

        StringBuilder activity = new StringBuilder();
        activity.append("{");
        activity.append("\"details\":").append(json(details));
        if (state != null && !state.trim().isEmpty()) {
            activity.append(",\"state\":").append(json(state));
        }
        if (startTimestamp > 0L) {
            activity.append(",\"timestamps\":{\"start\":").append(startTimestamp).append("}");
        }
        if (largeText != null && !largeText.trim().isEmpty()) {
            activity.append(",\"assets\":{\"large_text\":").append(json(largeText)).append("}");
        }
        activity.append("}");

        return sendActivity(activity.toString());
    }

    public synchronized void clearActivity() {
        if (connect()) {
            sendActivity("null");
        }
    }

    public synchronized void close() {
        if (this.pipe != null) {
            try {
                writePacket(OP_CLOSE, "");
            } catch (Exception ignored) {
            }
            try {
                this.pipe.close();
            } catch (IOException ignored) {
            }
            this.pipe = null;
        }
    }

    private boolean connect() {
        if (this.pipe != null) {
            return true;
        }

        for (String path : getPipePaths()) {
            try {
                this.pipe = new RandomAccessFile(path, "rw");
                writePacket(OP_HANDSHAKE, "{\"v\":1,\"client_id\":" + json(this.applicationId) + "}");
                return true;
            } catch (Exception ignored) {
                closeQuietly();
            }
        }
        return false;
    }

    private boolean sendActivity(String activityJson) {
        String payload = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + getPid()
                + ",\"activity\":" + activityJson + "},\"nonce\":"
                + json(UUID.randomUUID().toString()) + "}";
        try {
            writePacket(OP_FRAME, payload);
            return true;
        } catch (Exception ignored) {
            closeQuietly();
            return false;
        }
    }

    private void writePacket(int op, String payload) throws IOException {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(8 + body.length);
        writeIntLE(out, op);
        writeIntLE(out, body.length);
        out.write(body);
        this.pipe.write(out.toByteArray());
    }

    private static void writeIntLE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private static int getPid() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int index = name.indexOf('@');
            return Integer.parseInt(index >= 0 ? name.substring(0, index) : name);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static List<String> getPipePaths() {
        List<String> paths = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        for (int i = 0; i < 10; i++) {
            if (os.contains("win")) {
                paths.add("\\\\.\\pipe\\discord-ipc-" + i);
            } else {
                addUnixPipe(paths, System.getenv("XDG_RUNTIME_DIR"), i);
                addUnixPipe(paths, System.getenv("TMPDIR"), i);
                addUnixPipe(paths, System.getenv("TMP"), i);
                addUnixPipe(paths, System.getenv("TEMP"), i);
                addUnixPipe(paths, "/tmp", i);
            }
        }
        return paths;
    }

    private static void addUnixPipe(List<String> paths, String base, int index) {
        if (base != null && !base.trim().isEmpty()) {
            paths.add(base + "/discord-ipc-" + index);
        }
    }

    private static String json(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
        return out.toString();
    }

    private void closeQuietly() {
        if (this.pipe != null) {
            try {
                this.pipe.close();
            } catch (IOException ignored) {
            }
            this.pipe = null;
        }
    }
}
