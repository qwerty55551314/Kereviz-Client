package kereviz.config;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

public final class ClientFiles {
    public static final String ROOT_FOLDER_NAME = "Kereviz Client";

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final File BASE_DIR = new File(mc.mcDataDir == null ? new File(".") : mc.mcDataDir, ROOT_FOLDER_NAME);
    private static final File CONFIGS_DIR = new File(BASE_DIR, "configs");
    private static final File ACCOUNTS_DIR = new File(BASE_DIR, "accounts");
    private static final File LISTS_DIR = new File(BASE_DIR, "lists");
    private static final File UI_DIR = new File(BASE_DIR, "ui");
    private static boolean initialized;

    private ClientFiles() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        ensure(BASE_DIR);
        ensure(CONFIGS_DIR);
        ensure(ACCOUNTS_DIR);
        ensure(LISTS_DIR);
        ensure(UI_DIR);
        migrateLegacyFiles();
        initialized = true;
    }

    public static File baseDir() {
        init();
        return BASE_DIR;
    }

    public static File configsDir() {
        init();
        return CONFIGS_DIR;
    }

    public static File accountFile(String name) {
        init();
        return new File(ACCOUNTS_DIR, name);
    }

    public static File listFile(String name) {
        init();
        return new File(LISTS_DIR, name);
    }

    public static File uiFile(String name) {
        init();
        return new File(UI_DIR, name);
    }

    public static File configFile(String name) {
        init();
        return new File(CONFIGS_DIR, normalizeConfigName(name) + ".json");
    }

    public static File[] listConfigFiles() {
        init();
        File[] files = CONFIGS_DIR.listFiles((dir, fileName) -> fileName.toLowerCase(Locale.ROOT).endsWith(".json"));
        if (files == null) {
            return new File[0];
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        return files;
    }

    public static boolean deleteConfig(String name) {
        File file = configFile(name);
        return file.exists() && file.delete();
    }

    public static boolean renameConfig(String oldName, String newName) {
        File oldFile = configFile(oldName);
        File newFile = configFile(newName);
        if (!oldFile.exists() || newFile.exists()) {
            return false;
        }

        File parent = newFile.getParentFile();
        if (parent != null) {
            ensure(parent);
        }
        return oldFile.renameTo(newFile);
    }

    public static String normalizeConfigName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.equals("!") || name.equalsIgnoreCase("default")) {
            return "default";
        }
        if (name.toLowerCase(Locale.ROOT).endsWith(".json")) {
            name = name.substring(0, name.length() - 5);
        }

        name = name.replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]", "_").trim();
        while (name.endsWith(".") || name.endsWith(" ")) {
            name = name.substring(0, name.length() - 1);
        }
        if (name.isEmpty()) {
            name = "default";
        }
        return name.length() > 64 ? name.substring(0, 64).trim() : name;
    }

    public static String displayName(File file) {
        String name = file.getName();
        return name.toLowerCase(Locale.ROOT).endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    public static String path(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException ignored) {
            return file.getAbsolutePath();
        }
    }

    private static void migrateLegacyFiles() {
        File legacyDir = new File("./config/Kereviz/");
        if (legacyDir.isDirectory()) {
            File[] configs = legacyDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json") && !name.equalsIgnoreCase("menu.json"));
            if (configs != null) {
                for (File config : configs) {
                    copyIfMissing(config, new File(CONFIGS_DIR, config.getName()));
                }
            }

            copyIfMissing(new File(legacyDir, "menu.json"), new File(UI_DIR, "menu.json"));
            copyIfMissing(new File(legacyDir, "clickgui.txt"), new File(UI_DIR, "clickgui.json"));
            copyIfMissing(new File(legacyDir, "friends.txt"), new File(LISTS_DIR, "friends.txt"));
            copyIfMissing(new File(legacyDir, "enemies.txt"), new File(LISTS_DIR, "enemies.txt"));
        }

        copyIfMissing(new File(mc.mcDataDir, "kereviz.accounts.json"), new File(ACCOUNTS_DIR, "kereviz.accounts.json"));
    }

    private static void copyIfMissing(File source, File target) {
        if (!source.exists() || target.exists()) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null) {
            ensure(parent);
        }
        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException ignored) {
        }
    }

    private static void ensure(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
}
