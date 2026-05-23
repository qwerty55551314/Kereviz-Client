package kereviz.module.modules;

import kereviz.config.AnimationConfig;
import kereviz.config.AnimationMode;
import kereviz.module.Module;
import kereviz.property.properties.BooleanProperty;
import kereviz.property.properties.IntProperty;
import kereviz.property.properties.ModeProperty;

/**
 * Animations Module
 * Original logic by syuto/animations-1.6, integrated into Kereviz Client.
 */
public class Animations extends Module {

    public final ModeProperty mode = new ModeProperty("Mode", 0,
            new String[]{"VANILLA", "EXHIBITION", "ETB", "SIGMA", "DORTWARE", "PLAIN",
                    "SPIN", "AVATAR", "SWONG", "SWANG", "SWANK", "STYLES",
                    "NUDGE", "PUNCH", "JIGSAW", "SLIDE"});

    public final IntProperty scale = new IntProperty("Scale", 100, 50, 150);
    public final IntProperty swingSpeed = new IntProperty("SwingSpeed", 0, 0, 100);

    public Animations() {
        super("Animations", true, false, "Customizes player animations like swinging and blocking.");
    }

    @Override
    public void onEnabled() {
        syncConfig();
    }

    @Override
    public void onDisabled() {
        AnimationConfig.setEnabled(false);
    }

    private void syncConfig() {
        AnimationConfig.setEnabled(true);
        AnimationMode[] modes = AnimationMode.values();
        if (mode.getValue() < modes.length) {
            AnimationConfig.setMode(modes[mode.getValue()]);
        }
        AnimationConfig.setScale(scale.getValue());
        AnimationConfig.setSwingSpeed(swingSpeed.getValue());
    }

    public void onUpdate() {
        if (this.isEnabled()) {
            syncConfig();
        }
    }

    @Override
    public String[] getSuffix() {
        String[] modes = {"Vanilla", "Exhibition", "ETB", "Sigma", "Dortware", "Plain",
                "Spin", "Avatar", "Swong", "Swang", "Swank", "Styles",
                "Nudge", "Punch", "Jigsaw", "Slide"};
        return new String[]{modes[mode.getValue()]};
    }
}
