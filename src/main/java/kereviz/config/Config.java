package kereviz.config;

import com.google.gson.*;
import kereviz.Kereviz;
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

public class Config {
    private static final int KEREVIZ_GREEN = 0x14FF00;
    public static Minecraft mc = Minecraft.getMinecraft();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public String name;
    public File file;

    public static String lastConfig;

    public Config(String name, boolean newConfig) {
        this.name = name;
        lastConfig = name;
        if (name.equals("!") || name.equals("default")) {
            this.name = "default";
        }
        this.file = new File("./config/Kereviz/", String.format("%s.json", this.name));
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

            JsonElement parsed = new JsonParser().parse(new BufferedReader(new FileReader(file)));
            if (parsed == null || !parsed.isJsonObject()) {
                ChatUtil.sendFormatted(String.format("%sInvalid config format (&c&o%s&r)&r", Kereviz.clientName, file.getName()));
                return;
            }

            JsonObject jsonObject = parsed.getAsJsonObject();
            for (Module module : Kereviz.moduleManager.modules.values()) {
                JsonElement moduleObj = jsonObject.get(module.getName());
                if (moduleObj != null && moduleObj.isJsonObject()) {
                    JsonObject object = moduleObj.getAsJsonObject();

                    ArrayList<Property<?>> list = Kereviz.propertyManager.properties.get(module.getClass());
                    if (list != null) {
                        for (Property<?> property : list) {
                            if (object.has(property.getName())) {
                                try {
                                    property.read(object);
                                    migrateKerevizAccent(module, property);
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

    public void save() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

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

            PrintWriter printWriter = new PrintWriter(new FileWriter(file));
            printWriter.println(gson.toJson(object));
            printWriter.close();
            ChatUtil.sendFormatted(String.format("%sConfig has been saved (&a&o%s&r)&r", Kereviz.clientName, file.getName()));
        } catch (IOException e) {
            ((IAccessorMinecraft) mc).getLogger().error("Error saving config: " + e.getMessage());
            ChatUtil.sendFormatted(String.format("%sConfig couldn't be saved (&c&o%s&r)&r", Kereviz.clientName, file.getName()));
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
