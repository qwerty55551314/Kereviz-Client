package kereviz.property.properties;

import com.google.gson.JsonObject;
import kereviz.property.Property;

import java.util.function.BooleanSupplier;

public class ColorProperty extends Property<Integer> {
    public ColorProperty(String name, Integer color) {
        this(name, color, null);
    }

    public ColorProperty(String string, Integer color, BooleanSupplier check) {
        super(string, color, rgb -> rgb != null, check);
    }

    @Override
    public String getValuePrompt() {
        return "RGB";
    }

    @Override
    public String formatValue() {
        String hex = String.format("%06X", normalizeRgb(this.getValue()));
        return String.format("&c%s&a%s&9%s", hex.substring(0, 2), hex.substring(2, 4), hex.substring(4, 6));
    }

    @Override
    public boolean parseString(String string) {
        return this.setValue(parseRgb(string));
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return this.parseString(jsonObject.get(this.getName()).getAsString());
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), String.format("%06X", normalizeRgb(this.getValue())));
    }

    private static int parseRgb(String raw) {
        String hex = raw == null ? "" : raw.trim().replace("#", "");
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if (hex.length() > 6) {
            hex = hex.substring(hex.length() - 6);
        }
        if (hex.length() < 6) {
            hex = String.format("%6s", hex).replace(' ', '0');
        }
        return (int) (Long.parseLong(hex, 16) & 0xFFFFFF);
    }

    private static int normalizeRgb(Integer rgb) {
        return rgb == null ? 0xFFFFFF : rgb & 0xFFFFFF;
    }
}
