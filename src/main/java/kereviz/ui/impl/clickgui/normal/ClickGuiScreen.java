package kereviz.ui.impl.clickgui.normal;

import kereviz.Kereviz;
import kereviz.module.Module;
import kereviz.module.modules.*;
import kereviz.module.modules.Timer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ClickGuiScreen extends GuiScreen {
    private static final double FRICTION = 0.85;
    private static final double SNAP_STRENGTH = 0.15;
    private static final long ANIMATION_DURATION = 250L;
    private static ClickGuiScreen instance;
    private final ArrayList<Frame> frames;
    private Frame draggingComponent = null;
    private int scrollY = 0;
    private int targetScrollY = 0;
    private double velocity = 0;
    private boolean isClosing = false;
    private long openTime = 0L;
    private long lastFrameTime;

    public ClickGuiScreen() {
        this.frames = new ArrayList<>();

        List<Module> combatModules = Arrays.asList(
                Kereviz.moduleManager.getModule(AimAssist.class),
                Kereviz.moduleManager.getModule(AutoClicker.class),
                Kereviz.moduleManager.getModule(KillAura.class),
                Kereviz.moduleManager.getModule(Wtap.class),
                Kereviz.moduleManager.getModule(Velocity.class),
                Kereviz.moduleManager.getModule(ServerLag.class),
                Kereviz.moduleManager.getModule(Reach.class),
                Kereviz.moduleManager.getModule(TargetStrafe.class),
                Kereviz.moduleManager.getModule(NoHitDelay.class),
                Kereviz.moduleManager.getModule(AntiFireball.class),
                Kereviz.moduleManager.getModule(KnockbackDelay.class),
                Kereviz.moduleManager.getModule(LagRange.class),
                Kereviz.moduleManager.getModule(HitBox.class),
                Kereviz.moduleManager.getModule(MoreKB.class),
                Kereviz.moduleManager.getModule(Refill.class),
                Kereviz.moduleManager.getModule(HitSelect.class),
                Kereviz.moduleManager.getModule(BackTrack.class),
                Kereviz.moduleManager.getModule(TimerRangev999.class),
                Kereviz.moduleManager.getModule(ClickAssits.class),
                Kereviz.moduleManager.getModule(Criticals.class),
                Kereviz.moduleManager.getModule(BlockHit.class),
                Kereviz.moduleManager.getModule(SprintReset.class),
                Kereviz.moduleManager.getModule(Displace.class)
        );

        List<Module> movementModules = Arrays.asList(
                Kereviz.moduleManager.getModule(AntiAFK.class),
                Kereviz.moduleManager.getModule(Fly.class),
                Kereviz.moduleManager.getModule(FastBow.class),
                Kereviz.moduleManager.getModule(Timer.class),
                Kereviz.moduleManager.getModule(Speed.class),
                Kereviz.moduleManager.getModule(LongJump.class),
                Kereviz.moduleManager.getModule(Sprint.class),
                Kereviz.moduleManager.getModule(SafeWalk.class),
                Kereviz.moduleManager.getModule(Jesus.class),
                Kereviz.moduleManager.getModule(Blink.class),
                Kereviz.moduleManager.getModule(NoFall.class),
                Kereviz.moduleManager.getModule(NoSlow.class),
                Kereviz.moduleManager.getModule(KeepSprint.class),
                Kereviz.moduleManager.getModule(Eagle.class),
                Kereviz.moduleManager.getModule(NoJumpDelay.class),
                Kereviz.moduleManager.getModule(AntiVoid.class)
        );

        List<Module> renderModules = Arrays.asList(
                Kereviz.moduleManager.getModule(ESP.class),
                Kereviz.moduleManager.getModule(Chams.class),
                Kereviz.moduleManager.getModule(FreeLook.class),
                Kereviz.moduleManager.getModule(FullBright.class),
                Kereviz.moduleManager.getModule(Tracers.class),
                Kereviz.moduleManager.getModule(NameTags.class),
                Kereviz.moduleManager.getModule(Xray.class),
                Kereviz.moduleManager.getModule(TargetESP.class),
                Kereviz.moduleManager.getModule(TargetHUD.class),
                Kereviz.moduleManager.getModule(Indicators.class),
                Kereviz.moduleManager.getModule(BedESP.class),
                Kereviz.moduleManager.getModule(ItemESP.class),
                Kereviz.moduleManager.getModule(ViewClip.class),
                Kereviz.moduleManager.getModule(NoHurtCam.class),
                Kereviz.moduleManager.getModule(HUD.class),
                Kereviz.moduleManager.getModule(ChestESP.class),
                Kereviz.moduleManager.getModule(Trajectories.class),
                Kereviz.moduleManager.getModule(Radar.class),
                Kereviz.moduleManager.getModule(FPScounter.class),
                Kereviz.moduleManager.getModule(WaterMark.class),
                Kereviz.moduleManager.getModule(WaterMark2.class),
                Kereviz.moduleManager.getModule(HitParticleEffects.class),
                Kereviz.moduleManager.getModule(DynamicIsland.class),
                Kereviz.moduleManager.getModule(ESP2D.class),
                Kereviz.moduleManager.getModule(RiseClickGUIModule.class),
                Kereviz.moduleManager.getModule(TeamHealthDisplay.class),
                Kereviz.moduleManager.getModule(SeasonDisplay.class),
                Kereviz.moduleManager.getModule(Animations.class),
                Kereviz.moduleManager.getModule(ClickGUIModule.class)
        );

        List<Module> playerModules = Arrays.asList(
                Kereviz.moduleManager.getModule(AutoHeal.class),
                Kereviz.moduleManager.getModule(FakeLag.class),
                Kereviz.moduleManager.getModule(AutoTool.class),
                Kereviz.moduleManager.getModule(ChestStealer.class),
                Kereviz.moduleManager.getModule(InvManager.class),
                Kereviz.moduleManager.getModule(InvWalk.class),
                Kereviz.moduleManager.getModule(Scaffold.class),
                Kereviz.moduleManager.getModule(AutoBlockIn.class),
                Kereviz.moduleManager.getModule(AutoSwap.class),
                Kereviz.moduleManager.getModule(SpeedMine.class),
                Kereviz.moduleManager.getModule(FastPlace.class),
                Kereviz.moduleManager.getModule(GhostHand.class),
                Kereviz.moduleManager.getModule(MCF.class),
                Kereviz.moduleManager.getModule(AntiDebuff.class),
                Kereviz.moduleManager.getModule(FlagDetector.class),
                Kereviz.moduleManager.getModule(AutoGapple.class),
                Kereviz.moduleManager.getModule(ThrowAura.class)
        );

        List<Module> miscModules = Arrays.asList(
                Kereviz.moduleManager.getModule(Spammer.class),
                Kereviz.moduleManager.getModule(BedNuker.class),
                Kereviz.moduleManager.getModule(BedTracker.class),
                Kereviz.moduleManager.getModule(LightningTracker.class),
                Kereviz.moduleManager.getModule(NoRotate.class),
                Kereviz.moduleManager.getModule(NickHider.class),
                Kereviz.moduleManager.getModule(AntiObbyTrap.class),
                Kereviz.moduleManager.getModule(AntiObfuscate.class),
                Kereviz.moduleManager.getModule(AutoAnduril.class),
                Kereviz.moduleManager.getModule(InventoryClicker.class),
                Kereviz.moduleManager.getModule(Disabler.class),
                Kereviz.moduleManager.getModule(ClientSpoofer.class),
                Kereviz.moduleManager.getModule(AutoHypixel.class)
        );

        List<Module> discordModules = Arrays.asList(
                Kereviz.moduleManager.getModule(RichPresence.class)
        );

        Comparator<Module> comparator = Comparator.comparing(m -> m.getName().toLowerCase());
        combatModules.sort(comparator);
        movementModules.sort(comparator);
        renderModules.sort(comparator);
        playerModules.sort(comparator);
        miscModules.sort(comparator);
        discordModules.sort(comparator);

        int currentX = 20;
        int currentY = 20;
        int frameWidth = 110;
        int frameHeight = 24;

        List<Module> combat = new ArrayList<>(combatModules);
        combat.removeIf(m -> m == null);
        if (!combat.isEmpty()) {
            frames.add(new Frame("Combat", combat, currentX, currentY, frameWidth, frameHeight));
            currentX += (frameWidth + 15);
        }

        List<Module> movement = new ArrayList<>(movementModules);
        movement.removeIf(m -> m == null);
        if (!movement.isEmpty()) {
            frames.add(new Frame("Movement", movement, currentX, currentY, frameWidth, frameHeight));
            currentX += (frameWidth + 15);
        }

        List<Module> render = new ArrayList<>(renderModules);
        render.removeIf(m -> m == null);
        if (!render.isEmpty()) {
            frames.add(new Frame("Render", render, currentX, currentY, frameWidth, frameHeight));
            currentX += (frameWidth + 15);
        }

        List<Module> player = new ArrayList<>(playerModules);
        player.removeIf(m -> m == null);
        if (!player.isEmpty()) {
            frames.add(new Frame("Player", player, currentX, currentY, frameWidth, frameHeight));
            currentX += (frameWidth + 15);
        }

        List<Module> misc = new ArrayList<>(miscModules);
        misc.removeIf(m -> m == null);
        if (!misc.isEmpty()) {
            frames.add(new Frame("Misc", misc, currentX, currentY, frameWidth, frameHeight));
            currentX += (frameWidth + 15);
        }

        List<Module> discord = new ArrayList<>(discordModules);
        discord.removeIf(m -> m == null);
        if (!discord.isEmpty()) {
            frames.add(new Frame("Discord", discord, currentX, currentY, frameWidth, frameHeight));
        }
    }

    public static ClickGuiScreen getInstance() {
        if (instance == null) {
            instance = new ClickGuiScreen();
        }
        return instance;
    }

    public static void resetInstance() {
        instance = null;
    }

    @Override
    public void initGui() {
        super.initGui();
        kereviz.util.font.FontManager.initializeFonts();
        this.isClosing = false;
        this.openTime = System.currentTimeMillis();
        this.lastFrameTime = System.nanoTime();
        this.scrollY = 0;
        this.targetScrollY = 0;
        this.velocity = 0;
    }

    public void close() {
        if (isClosing) return;
        this.isClosing = true;
        this.openTime = System.currentTimeMillis();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long currentFrameTime = System.nanoTime();
        float deltaTime = (currentFrameTime - lastFrameTime) / 1_000_000_000.0f;
        lastFrameTime = currentFrameTime;
        updateScroll();
        long elapsedTime = System.currentTimeMillis() - openTime;
        if (isClosing && elapsedTime > ANIMATION_DURATION) {
            mc.displayGuiScreen(null);
            return;
        }
        float screenAlpha = isClosing ? (1.0f - Math.min(1.0f, (float) elapsedTime / ANIMATION_DURATION)) : Math.min(1.0f, (float) elapsedTime / ANIMATION_DURATION);
        screenAlpha = (float) (1.0 - Math.pow(1.0 - screenAlpha, 3));
        if (screenAlpha > 0.01f) {
            for (Frame frame : frames) {
                frame.render(mouseX, mouseY, partialTicks, screenAlpha, false, scrollY, deltaTime);
            }
            HUD hud = (HUD) Kereviz.moduleManager.getModule(HUD.class);
            if (hud != null) {
                hud.renderEditorOverlay(mouseX, mouseY);
            }
        }
        try {
            Module invWalkModule = Kereviz.moduleManager.getModule("InvWalk");
            if (invWalkModule != null && invWalkModule.isEnabled()) {
                handleInvWalk();
            }
        } catch (Exception ignored) {
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void handleInvWalk() {
        KeyBinding[] keys = {
                mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSprint,
                mc.gameSettings.keyBindSneak
        };
        for (KeyBinding key : keys) {
            KeyBinding.setKeyBindState(key.getKeyCode(), Keyboard.isKeyDown(key.getKeyCode()));
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        if (isClosing) return;
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            velocity += wheel > 0 ? -30 : 30;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isClosing) return;
        super.mouseClicked(mouseX, mouseY, mouseButton);
        HUD hud = (HUD) Kereviz.moduleManager.getModule(HUD.class);
        if (hud != null && hud.mouseClickedEditor(mouseX, mouseY, mouseButton)) {
            return;
        }
        for (int i = frames.size() - 1; i >= 0; i--) {
            Frame frame = frames.get(i);
            if (frame.mouseClicked(mouseX, mouseY, mouseButton, scrollY)) {
                draggingComponent = frame;
                frames.remove(i);
                frames.add(frame);
                return;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (isClosing) return;
        super.mouseReleased(mouseX, mouseY, state);
        if (draggingComponent != null) {
            draggingComponent.mouseReleased(mouseX, mouseY, state, scrollY);
            draggingComponent = null;
        }
        HUD hud = (HUD) Kereviz.moduleManager.getModule(HUD.class);
        if (hud != null) {
            hud.mouseReleasedEditor();
        }
        for (Frame frame : frames) {
            frame.mouseReleased(mouseX, mouseY, state, scrollY);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isClosing) return;
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        HUD hud = (HUD) Kereviz.moduleManager.getModule(HUD.class);
        if (hud != null && hud.isEditorDragging()) {
            hud.mouseDraggedEditor(mouseX, mouseY);
        } else if (draggingComponent != null) {
            draggingComponent.updatePosition(mouseX, mouseY);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (isClosing) return;
        if (System.currentTimeMillis() - this.openTime < 100) return;
        boolean isBindingKey = false;
        for (Frame frame : frames) {
            if (frame.isAnyComponentBinding()) {
                isBindingKey = true;
                break;
            }
        }
        if (isBindingKey) {
            for (Frame frame : frames) {
                frame.keyTyped(typedChar, keyCode);
            }
            return;
        }
        Module clickGUIModule = Kereviz.moduleManager.getModule("ClickGUI");
        if (keyCode == Keyboard.KEY_ESCAPE || (clickGUIModule != null && keyCode == clickGUIModule.getKey())) {
            close();
            return;
        }
        for (Frame frame : frames) {
            frame.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void updateScroll() {
        targetScrollY += (int) velocity;
        velocity *= FRICTION;
        int maxScroll = getMaxScroll();
        targetScrollY = Math.max(0, Math.min(targetScrollY, maxScroll));
        int delta = targetScrollY - scrollY;
        scrollY += (int) (delta * SNAP_STRENGTH);
        if (Math.abs(velocity) < 0.5) velocity = 0;
        if (Math.abs(delta) < 1 && Math.abs(velocity) < 0.5) scrollY = targetScrollY;
    }

    private int getMaxScroll() {
        int max = 0;
        for (Frame frame : frames) {
            int bottom = frame.getY() + (int) frame.getCurrentHeight();
            if (bottom > max) max = bottom;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        return Math.max(0, max - sr.getScaledHeight() + 20);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Module guiModule = Kereviz.moduleManager.getModule("ClickGUI");
        if (guiModule != null) {
            guiModule.setEnabled(false);
        }
    }
}
