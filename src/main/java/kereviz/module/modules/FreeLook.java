package kereviz.module.modules;

import kereviz.event.EventTarget;
import kereviz.event.types.EventType;
import kereviz.events.TickEvent;
import kereviz.module.Module;
import kereviz.property.properties.BooleanProperty;
import kereviz.property.properties.ModeProperty;
import kereviz.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public class FreeLook extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean active;
    private static float cameraYaw;
    private static float cameraPitch;
    private static float prevCameraYaw;
    private static float prevCameraPitch;

    public final ModeProperty perspective = new ModeProperty("perspective", 0, new String[]{"BACK", "FRONT"});
    public final BooleanProperty restorePerspective = new BooleanProperty("restore-perspective", true);
    public final BooleanProperty hold = new BooleanProperty("hold", false);

    private int previousPerspective;
    private boolean changedPerspective;

    public FreeLook() {
        super("FreeLook", false, false, "Look around in third person without changing your player view.");
        this.setKey(Keyboard.KEY_LMENU);
    }

    public static boolean isActive() {
        return active && mc.thePlayer != null && mc.theWorld != null;
    }

    public static void applyMouseDelta(float yawDelta, float pitchDelta) {
        if (!isActive()) {
            return;
        }

        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;
        cameraYaw += yawDelta * 0.15F;
        cameraPitch = MathHelper.clamp_float(cameraPitch - pitchDelta * 0.15F, -90.0F, 90.0F);
    }

    public static float getCameraYaw() {
        return cameraYaw;
    }

    public static float getCameraPitch() {
        return cameraPitch;
    }

    public static float getPrevCameraYaw() {
        return prevCameraYaw;
    }

    public static float getPrevCameraPitch() {
        return prevCameraPitch;
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            active = false;
            return;
        }

        cameraYaw = prevCameraYaw = mc.thePlayer.rotationYaw;
        cameraPitch = prevCameraPitch = mc.thePlayer.rotationPitch;
        previousPerspective = mc.gameSettings.thirdPersonView;
        changedPerspective = false;
        applyPerspective();
        active = true;
    }

    @Override
    public void onDisabled() {
        active = false;
        if (restorePerspective.getValue() && changedPerspective && mc.gameSettings != null) {
            mc.gameSettings.thirdPersonView = previousPerspective;
        }
        changedPerspective = false;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            this.setEnabled(false);
            return;
        }

        applyPerspective();
        if (hold.getValue() && this.getKey() != 0 && !KeyBindUtil.isKeyDown(this.getKey())) {
            this.setEnabled(false);
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{perspective.getModeString()};
    }

    private void applyPerspective() {
        int targetPerspective = perspective.getValue() == 1 ? 2 : 1;
        if (mc.gameSettings.thirdPersonView != targetPerspective) {
            if (!changedPerspective) {
                previousPerspective = mc.gameSettings.thirdPersonView;
            }
            mc.gameSettings.thirdPersonView = targetPerspective;
            changedPerspective = true;
        }
    }
}
