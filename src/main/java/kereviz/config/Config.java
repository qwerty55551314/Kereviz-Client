package kereviz.config;

import com.google.gson.*;
import kereviz.Kereviz;
import kereviz.management.PlayerFileManager;
import kereviz.mixin.IAccessorMinecraft;
import kereviz.module.Module;
import kereviz.property.properties.ColorProperty;
import kereviz.property.properties.ModeProperty;
import kereviz.property.properties.PercentProperty;
import kereviz.util.ChatUtil;
import kereviz.property.Property;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class Config {
    private static final int KEREVIZ_GREEN = 0x14FF00;
    private static final int FORMAT_VERSION = 2;
    public static Minecraft mc = Minecraft.getMinecraft();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public String name;
    public File file;

    public static String lastConfig;

    public Config(String name, boolean newConfig) {
        this.name = ClientFiles.normalizeConfigName(name);
        lastConfig = this.name;
        this.file = ClientFiles.configFile(this.name);
        try {
            file.getParentFile().mkdirs();
            if (newConfig) {
                ((IAccessorMinecraft) mc).getLogger().info(String.format("Created: %s", this.file.getName()));
            }
        } catch (Exception e) {
            ((IAccessorMinecraft) mc).getLogger().error(e.getMessage());
        }
    }

    public void load() {
        try {
            if (!file.exists()) {
                ChatUtil.sendFormatted(String.format("%sConfig file not found (&c&o%s&r). Creating default config...&r", Kereviz.clientName, file.getName()));
                save();
                return;
            }

            JsonElement parsed;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                parsed = new JsonParser().parse(reader);
            }
            if (parsed == null || !parsed.isJsonObject()) {
                ChatUtil.sendFormatted(String.format("%sInvalid config format (&c&o%s&r)&r", Kereviz.clientName, file.getName()));
                return;
            }

            JsonObject jsonObject = parsed.getAsJsonObject();
            boolean legacyMigration = this.shouldRunLegacyMigration(jsonObject);
            JsonObject modulesObject = getModulesObject(jsonObject);
            loadModules(modulesObject, legacyMigration);
            loadLists(jsonObject);
            loadUi(jsonObject);
            lastConfig = this.name;
            ChatUtil.sendFormatted(String.format("%sConfig has been loaded (&a&o%s&r)&r", Kereviz.clientName, file.getName()));
        } catch (FileNotFoundException e) {
            ChatUtil.sendFormatted(String.format("%sConfig file not found (&c&o%s&r)&r", Kereviz.clientName, file.getName()));
        } catch (JsonSyntaxException e) {
            ChatUtil.sendFormatted(String.format("%sConfig has invalid JSON syntax (&c&o%s&r)&r", Kereviz.clientName, file.getName()));
            ((IAccessorMinecraft) mc).getLogger().error("JSON Syntax Error: " + e.getMessage());
        } catch (Exception e) {
            ((IAccessorMinecraft) mc).getLogger().error("Error loading config: " + e.getMessage());
            ChatUtil.sendFormatted(String.format("%sConfig couldn't be loaded (&c&o%s&r)&r", Kereviz.clientName, file.getName()));
        }
    }

    private JsonObject getModulesObject(JsonObject jsonObject) {
        JsonElement modules = jsonObject.get("modules");
        return modules != null && modules.isJsonObject() ? modules.getAsJsonObject() : jsonObject;
    }

    private boolean shouldRunLegacyMigration(JsonObject jsonObject) {
        JsonElement format = jsonObject.get("format");
        if (format == null || !format.isJsonPrimitive()) {
            return true;
        }
        try {
            return format.getAsInt() < FORMAT_VERSION;
        } catch (Exception ignored) {
            return true;
        }
    }

    private void loadModules(JsonObject jsonObject, boolean legacyMigration) {
        for (Module module : Kereviz.moduleManager.modules.values()) {
            JsonElement moduleObj = jsonObject.get(module.getName());
            if (moduleObj == null && "MLG".equals(module.getName())) {
                moduleObj = jsonObject.get("WaterMLG");
            }
            if (moduleObj != null && moduleObj.isJsonObject()) {
                JsonObject object = moduleObj.getAsJsonObject();

                ArrayList<Property<?>> list = Kereviz.propertyManager.properties.get(module.getClass());
                if (list != null) {
                    for (Property<?> property : list) {
                        if (object.has(property.getName())) {
                            try {
                                property.read(object);
                                if (legacyMigration) {
                                    migrateKerevizAccent(module, property);
                                }
                            } catch (Exception e) {
                                ((IAccessorMinecraft) mc).getLogger().warn(String.format("Failed to load property %s for module %s", property.getName(), module.getName()));
                            }
                        }
                    }
                }

                if (object.has("toggled")) {
                    JsonElement toggled = object.get("toggled");
                    if (toggled != null && toggled.isJsonPrimitive()) {
                        module.setEnabled(toggled.getAsBoolean());
                    }
                }

                if (object.has("key")) {
                    JsonElement key = object.get("key");
                    if (key != null && key.isJsonPrimitive()) {
                        module.setKey(key.getAsInt());
                    }
                }

                if (object.has("hidden")) {
                    JsonElement hidden = object.get("hidden");
                    if (hidden != null && hidden.isJsonPrimitive()) {
                        module.setHidden(hidden.getAsBoolean());
                    }
                }
            }
        }
    }

    public void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            JsonObject object = new JsonObject();
            object.addProperty("format", FORMAT_VERSION);
            object.addProperty("name", this.name);
            object.addProperty("savedAt", System.currentTimeMillis());
            object.addProperty("clientVersion", Kereviz.version == null ? "dev" : Kereviz.version);
            object.add("modules", writeModules());
            object.add("lists", writeLists());
            object.add("ui", writeUi());

            try (PrintWriter printWriter = new PrintWriter(new FileWriter(file))) {
                printWriter.println(gson.toJson(object));
            }
            lastConfig = this.name;
            ChatUtil.sendFormatted(String.format("%sConfig has been saved (&a&o%s&r)&r", Kereviz.clientName, file.getName()));
        } catch (IOException e) {
            ((IAccessorMinecraft) mc).getLogger().error("Error saving config: " + e.getMessage());
            ChatUtil.sendFormatted(String.format("%sConfig couldn't be saved (&c&o%s&r)&r", Kereviz.clientName, file.getName()));
        }
    }

    private JsonObject writeModules() {
        JsonObject object = new JsonObject();
        for (Module module : Kereviz.moduleManager.modules.values()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.addProperty("toggled", module.isEnabled());
            moduleObject.addProperty("key", module.getKey());
            moduleObject.addProperty("hidden", module.isHidden());

            ArrayList<Property<?>> list = Kereviz.propertyManager.properties.get(module.getClass());
            if (list != null) {
                for (Property<?> property : list) {
                    try {
                        property.write(moduleObject);
                    } catch (Exception e) {
                        ((IAccessorMinecraft) mc).getLogger().warn(String.format("Failed to save property %s for module %s", property.getName(), module.getName()));
                    }
                }
            }
            object.add(module.getName(), moduleObject);
        }
        return object;
    }

    private JsonObject writeLists() {
        JsonObject object = new JsonObject();
        if (Kereviz.friendManager != null) {
            object.add("friends", writePlayerList(Kereviz.friendManager));
        }
        if (Kereviz.targetManager != null) {
            object.add("enemies", writePlayerList(Kereviz.targetManager));
        }
        return object;
    }

    private JsonArray writePlayerList(PlayerFileManager manager) {
        JsonArray array = new JsonArray();
        for (String player : manager.getPlayers()) {
            if (player != null && !player.trim().isEmpty()) {
                array.add(new JsonPrimitive(player.trim()));
            }
        }
        return array;
    }

    private void loadLists(JsonObject jsonObject) {
        JsonElement lists = jsonObject.get("lists");
        if (lists == null || !lists.isJsonObject()) {
            return;
        }

        JsonObject object = lists.getAsJsonObject();
        loadPlayerList(object, "friends", Kereviz.friendManager);
        loadPlayerList(object, "enemies", Kereviz.targetManager);
    }

    private void loadPlayerList(JsonObject object, String key, PlayerFileManager manager) {
        if (manager == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return;
        }

        Set<String> uniquePlayers = new LinkedHashSet<>();
        for (JsonElement element : object.getAsJsonArray(key)) {
            if (element != null && element.isJsonPrimitive()) {
                String player = element.getAsString().trim();
                if (!player.isEmpty()) {
                    uniquePlayers.add(player);
                }
            }
        }

        manager.players.clear();
        manager.players.addAll(uniquePlayers);
        manager.save();
    }

    private JsonObject writeUi() {
        JsonObject object = new JsonObject();
        object.add("menu", MenuConfig.toJson());

        JsonObject clickGui = readJsonFile(ClientFiles.uiFile("clickgui.json"));
        if (clickGui != null) {
            object.add("clickGui", clickGui);
        }
        return object;
    }

    private void loadUi(JsonObject jsonObject) {
        JsonElement ui = jsonObject.get("ui");
        if (ui == null || !ui.isJsonObject()) {
            return;
        }

        JsonObject object = ui.getAsJsonObject();
        JsonElement menu = object.get("menu");
        if (menu != null && menu.isJsonObject()) {
            MenuConfig.readFrom(menu.getAsJsonObject());
        }

        JsonElement clickGui = object.get("clickGui");
        if (clickGui != null && clickGui.isJsonObject()) {
            writeJsonFile(ClientFiles.uiFile("clickgui.json"), clickGui.getAsJsonObject());
        }
    }

    private JsonObject readJsonFile(File file) {
        if (!file.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            JsonElement parsed = new JsonParser().parse(reader);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeJsonFile(File file, JsonObject object) {
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println(gson.toJson(object));
            }
        } catch (IOException ignored) {
        }
    }

    private void migrateKerevizAccent(Module module, Property<?> property) {
        String moduleName = module.getName();
        String propertyName = property.getName();

        if ("HUD".equals(moduleName)) {
            migrateHudAccent(property, propertyName);
        }

        if (!(property instanceof ColorProperty)) {
            return;
        }

        boolean kerevizAccent =
                ("HUD".equals(moduleName) && "custom-color-1".equals(propertyName)) ||
                ("DynamicIsland".equals(moduleName) && "AccentColor".equals(propertyName));
        if (!kerevizAccent) {
            return;
        }

        Object value = property.getValue();
        if (!(value instanceof Integer)) {
            return;
        }

        int rgb = (Integer) value & 0xFFFFFF;
        if (isOldKerevizAccent(rgb)) {
            property.setValue(KEREVIZ_GREEN);
        }
    }

    private void migrateHudAccent(Property<?> property, String propertyName) {
        if ("color".equals(propertyName) && property instanceof ModeProperty) {
            Object value = property.getValue();
            if (value instanceof Integer && (Integer) value < 3) {
                property.setValue(3);
            }
            return;
        }

        if ("color-saturation".equals(propertyName) && property instanceof PercentProperty) {
            Object value = property.getValue();
            if (value instanceof Integer && (Integer) value < 100) {
                property.setValue(100);
            }
        }
    }

    private boolean isOldKerevizAccent(int rgb) {
        switch (rgb & 0xFFFFFF) {
            case 0x81C784:
            case 0xFF81C7:
            case 0x00FF00:
            case 0xFFFF00:
            case 0xFFFF55:
            case 0xFFD54F:
                return true;
            default:
                return false;
        }
    }
}
