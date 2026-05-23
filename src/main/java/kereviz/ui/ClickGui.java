package kereviz.ui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kereviz.Kereviz;
import kereviz.config.ClientFiles;
import kereviz.font.FontProcess;
import kereviz.module.Module;
import kereviz.module.modules.*;
import kereviz.module.modules.Timer;
import kereviz.ui.components.CategoryComponent;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import kereviz.font.CFontRenderer;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClickGui extends GuiScreen {
    CFontRenderer fontRenderer = FontProcess.getFont("sans");
    private static ClickGui instance;
    private final File configFile = ClientFiles.uiFile("clickgui.json");
    private final ArrayList<CategoryComponent> categoryList;

    public ClickGui() {
        instance = this;


        List<Module> combatModules = new ArrayList<>();
        combatModules.add(Kereviz.moduleManager.getModule(AimAssist.class));
        combatModules.add(Kereviz.moduleManager.getModule(AutoClicker.class));
        combatModules.add(Kereviz.moduleManager.getModule(KillAura.class));
        combatModules.add(Kereviz.moduleManager.getModule(Wtap.class));
        combatModules.add(Kereviz.moduleManager.getModule(Velocity.class));
        combatModules.add(Kereviz.moduleManager.getModule(ServerLag.class));
        combatModules.add(Kereviz.moduleManager.getModule(Reach.class));
        combatModules.add(Kereviz.moduleManager.getModule(TargetStrafe.class));
        combatModules.add(Kereviz.moduleManager.getModule(NoHitDelay.class));
        combatModules.add(Kereviz.moduleManager.getModule(AntiFireball.class));
        combatModules.add(Kereviz.moduleManager.getModule(LagRange.class));
        combatModules.add(Kereviz.moduleManager.getModule(HitBox.class));
        combatModules.add(Kereviz.moduleManager.getModule(MoreKB.class));
        combatModules.add(Kereviz.moduleManager.getModule(Refill.class));
        combatModules.add(Kereviz.moduleManager.getModule(HitSelect.class));
        combatModules.add(Kereviz.moduleManager.getModule(BackTrack.class));
        combatModules.add(Kereviz.moduleManager.getModule(TimerRangev999.class));
        combatModules.add(Kereviz.moduleManager.getModule(ClickAssits.class));
        combatModules.add(Kereviz.moduleManager.getModule(Criticals.class));
        combatModules.add(Kereviz.moduleManager.getModule(BlockHit.class));
        combatModules.add(Kereviz.moduleManager.getModule(SprintReset.class));

        List<Module> movementModules = new ArrayList<>();
        movementModules.add(Kereviz.moduleManager.getModule(AntiAFK.class));
        movementModules.add(Kereviz.moduleManager.getModule(Fly.class));
        movementModules.add(Kereviz.moduleManager.getModule(FastBow.class));
        movementModules.add(Kereviz.moduleManager.getModule(Timer.class));
        movementModules.add(Kereviz.moduleManager.getModule(Speed.class));
        movementModules.add(Kereviz.moduleManager.getModule(LongJump.class));
        movementModules.add(Kereviz.moduleManager.getModule(Sprint.class));
        movementModules.add(Kereviz.moduleManager.getModule(SafeWalk.class));
        movementModules.add(Kereviz.moduleManager.getModule(Jesus.class));
        movementModules.add(Kereviz.moduleManager.getModule(Blink.class));
        movementModules.add(Kereviz.moduleManager.getModule(NoFall.class));
        movementModules.add(Kereviz.moduleManager.getModule(NoSlow.class));
        movementModules.add(Kereviz.moduleManager.getModule(KeepSprint.class));
        movementModules.add(Kereviz.moduleManager.getModule(Eagle.class));
        movementModules.add(Kereviz.moduleManager.getModule(NoJumpDelay.class));
        movementModules.add(Kereviz.moduleManager.getModule(AntiVoid.class));

        List<Module> renderModules = new ArrayList<>();
        renderModules.add(Kereviz.moduleManager.getModule(ESP.class));
        renderModules.add(Kereviz.moduleManager.getModule(Chams.class));
        renderModules.add(Kereviz.moduleManager.getModule(FreeLook.class));
        renderModules.add(Kereviz.moduleManager.getModule(FullBright.class));
        renderModules.add(Kereviz.moduleManager.getModule(Tracers.class));
        renderModules.add(Kereviz.moduleManager.getModule(NameTags.class));
        renderModules.add(Kereviz.moduleManager.getModule(Xray.class));
        renderModules.add(Kereviz.moduleManager.getModule(TargetHUD.class));
        renderModules.add(Kereviz.moduleManager.getModule(Indicators.class));
        renderModules.add(Kereviz.moduleManager.getModule(BedESP.class));
        renderModules.add(Kereviz.moduleManager.getModule(ItemESP.class));
        renderModules.add(Kereviz.moduleManager.getModule(ViewClip.class));
        renderModules.add(Kereviz.moduleManager.getModule(NoHurtCam.class));
        renderModules.add(Kereviz.moduleManager.getModule(HUD.class));
        renderModules.add(Kereviz.moduleManager.getModule(GuiModule.class));
        renderModules.add(Kereviz.moduleManager.getModule(ChestESP.class));
        renderModules.add(Kereviz.moduleManager.getModule(Trajectories.class));
        renderModules.add(Kereviz.moduleManager.getModule(Radar.class));
        renderModules.add(Kereviz.moduleManager.getModule(RenderFixes.class));
        renderModules.add(Kereviz.moduleManager.getModule(FPScounter.class));
        renderModules.add(Kereviz.moduleManager.getModule(WaterMark.class));
        renderModules.add(Kereviz.moduleManager.getModule(WaterMark2.class));
        renderModules.add(Kereviz.moduleManager.getModule(HitParticleEffects.class));
        renderModules.add(Kereviz.moduleManager.getModule(DynamicIsland.class));
        renderModules.add(Kereviz.moduleManager.getModule(ESP2D.class));
        renderModules.add(Kereviz.moduleManager.getModule(TeamHealthDisplay.class));
        renderModules.add(Kereviz.moduleManager.getModule(SeasonDisplay.class));
        renderModules.add(Kereviz.moduleManager.getModule(Animations.class));

        List<Module> playerModules = new ArrayList<>();
        playerModules.add(Kereviz.moduleManager.getModule(AutoHeal.class));
        playerModules.add(Kereviz.moduleManager.getModule(FakeLag.class));
        playerModules.add(Kereviz.moduleManager.getModule(AutoTool.class));
        playerModules.add(Kereviz.moduleManager.getModule(ChestStealer.class));
        playerModules.add(Kereviz.moduleManager.getModule(InvManager.class));
        playerModules.add(Kereviz.moduleManager.getModule(InvWalk.class));
        playerModules.add(Kereviz.moduleManager.getModule(Scaffold.class));
        playerModules.add(Kereviz.moduleManager.getModule(AutoBlockIn.class));
        playerModules.add(Kereviz.moduleManager.getModule(AutoSwap.class));
        playerModules.add(Kereviz.moduleManager.getModule(SpeedMine.class));
        playerModules.add(Kereviz.moduleManager.getModule(FastPlace.class));
        playerModules.add(Kereviz.moduleManager.getModule(GhostHand.class));
        playerModules.add(Kereviz.moduleManager.getModule(MCF.class));
        playerModules.add(Kereviz.moduleManager.getModule(AntiDebuff.class));
        playerModules.add(Kereviz.moduleManager.getModule(FlagDetector.class));  // i mean this use S08PacketPlayerPosLook so it suck
        playerModules.add(Kereviz.moduleManager.getModule(AutoGapple.class));
        playerModules.add(Kereviz.moduleManager.getModule(ThrowAura.class));

        List<Module> miscModules = new ArrayList<>();
        miscModules.add(Kereviz.moduleManager.getModule(Spammer.class));
        miscModules.add(Kereviz.moduleManager.getModule(BedNuker.class));
        miscModules.add(Kereviz.moduleManager.getModule(BedTracker.class));
        miscModules.add(Kereviz.moduleManager.getModule(LightningTracker.class));
        miscModules.add(Kereviz.moduleManager.getModule(NoRotate.class));
        miscModules.add(Kereviz.moduleManager.getModule(NickHider.class));
        miscModules.add(Kereviz.moduleManager.getModule(AntiObbyTrap.class));
        miscModules.add(Kereviz.moduleManager.getModule(AntiObfuscate.class));
        miscModules.add(Kereviz.moduleManager.getModule(AutoAnduril.class));
        miscModules.add(Kereviz.moduleManager.getModule(InventoryClicker.class));
        miscModules.add(Kereviz.moduleManager.getModule(Disabler.class));
        miscModules.add(Kereviz.moduleManager.getModule(ClientSpoofer.class));
        miscModules.add(Kereviz.moduleManager.getModule(AutoHypixel.class));

        List<Module> discordModules = new ArrayList<>();
        discordModules.add(Kereviz.moduleManager.getModule(RichPresence.class));

        Comparator<Module> comparator = Comparator.comparing(m -> m.getName().toLowerCase());
        combatModules.sort(comparator);
        movementModules.sort(comparator);
        renderModules.sort(comparator);
        playerModules.sort(comparator);
        miscModules.sort(comparator);
        discordModules.sort(comparator);

        Set<Module> registered = new HashSet<>();
        registered.addAll(combatModules);
        registered.addAll(movementModules);
        registered.addAll(renderModules);
        registered.addAll(playerModules);
        registered.addAll(miscModules);
        registered.addAll(discordModules);

        for (Module module : Kereviz.moduleManager.modules.values()) {
            if (!registered.contains(module)) {
                throw new RuntimeException(module.getClass().getName() + " is unregistered to click gui.");
            }
        }

        this.categoryList = new ArrayList<>();
        int topOffset = 5;

        CategoryComponent combat = new CategoryComponent("Combat", combatModules);
        combat.setY(topOffset);
        categoryList.add(combat);
        topOffset += 20;

        CategoryComponent movement = new CategoryComponent("Movement", movementModules);
        movement.setY(topOffset);
        categoryList.add(movement);
        topOffset += 20;

        CategoryComponent render = new CategoryComponent("Render", renderModules);
        render.setY(topOffset);
        categoryList.add(render);
        topOffset += 20;

        CategoryComponent player = new CategoryComponent("Player", playerModules);
        player.setY(topOffset);
        categoryList.add(player);
        topOffset += 20;

        CategoryComponent misc = new CategoryComponent("Misc", miscModules);
        misc.setY(topOffset);
        categoryList.add(misc);
        topOffset += 20;

        CategoryComponent discord = new CategoryComponent("Discord", discordModules);
        discord.setY(topOffset);
        categoryList.add(discord);

        loadPositions();
    }

    public static ClickGui getInstance() {
        return instance;
    }

    public void initGui() {
        super.initGui();
    }

    public void drawScreen(int x, int y, float p) {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 100).getRGB());

        fontRenderer.drawStringWithShadow("Kereviz Client " + Kereviz.version, 4, this.height - 3 - fontRenderer.FONT_HEIGHT * 2, new Color(60, 162, 253).getRGB());
        fontRenderer.drawStringWithShadow("Kereviz", 4, this.height - 3 - fontRenderer.FONT_HEIGHT, new Color(60, 162, 253).getRGB());

        for (CategoryComponent category : categoryList) {
            category.render(this.mc.fontRendererObj);
            category.handleDrag(x, y);

            for (Component module : category.getModules()) {
                module.update(x, y);
            }
        }

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int scrollDir = wheel > 0 ? 1 : -1;
            for (CategoryComponent category : categoryList) {
                category.onScroll(x, y, scrollDir);
            }
        }
    }

    public void mouseClicked(int x, int y, int mouseButton) {
        Iterator<CategoryComponent> btnCat = categoryList.iterator();
        while (true) {
            CategoryComponent category;
            do {
                do {
                    if (!btnCat.hasNext()) {
                        return;
                    }

                    category = btnCat.next();
                    if (category.insideArea(x, y) && !category.isHovered(x, y) && !category.mousePressed(x, y) && mouseButton == 0) {
                        category.mousePressed(true);
                        category.xx = x - category.getX();
                        category.yy = y - category.getY();
                    }

                    if (category.mousePressed(x, y) && mouseButton == 0) {
                        category.setOpened(!category.isOpened());
                    }

                    if (category.isHovered(x, y) && mouseButton == 0) {
                        category.setPin(!category.isPin());
                    }
                } while (!category.isOpened());
            } while (category.getModules().isEmpty());

            for (Component c : category.getModules()) {
                c.mouseDown(x, y, mouseButton);
            }
        }

    }

    public void mouseReleased(int x, int y, int mouseButton) {
        Iterator<CategoryComponent> iterator = categoryList.iterator();

        CategoryComponent categoryComponent;
        while (iterator.hasNext()) {
            categoryComponent = iterator.next();
            if (mouseButton == 0) {
                categoryComponent.mousePressed(false);
            }
        }

        iterator = categoryList.iterator();

        while (true) {
            do {
                do {
                    if (!iterator.hasNext()) {
                        return;
                    }

                    categoryComponent = iterator.next();
                } while (!categoryComponent.isOpened());
            } while (categoryComponent.getModules().isEmpty());

            for (Component component : categoryComponent.getModules()) {
                component.mouseReleased(x, y, mouseButton);
            }
        }
    }

    public void keyTyped(char typedChar, int key) {
        if (key == 1) {
            this.mc.displayGuiScreen(null);
        } else {
            Iterator<CategoryComponent> btnCat = categoryList.iterator();

            while (true) {
                CategoryComponent cat;
                do {
                    do {
                        if (!btnCat.hasNext()) {
                            return;
                        }

                        cat = btnCat.next();
                    } while (!cat.isOpened());
                } while (cat.getModules().isEmpty());

                for (Component component : cat.getModules()) {
                    component.keyTyped(typedChar, key);
                }
            }
        }
    }

    public void onGuiClosed() {
        savePositions();
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private void savePositions() {
        if (configFile.getParentFile() != null && !configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }
        JsonObject json = new JsonObject();
        for (CategoryComponent cat : categoryList) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", cat.getX());
            pos.addProperty("y", cat.getY());
            pos.addProperty("open", cat.isOpened());
            json.add(cat.getName(), pos);
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPositions() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            for (CategoryComponent cat : categoryList) {
                if (json.has(cat.getName())) {
                    JsonObject pos = json.getAsJsonObject(cat.getName());
                    cat.setX(pos.get("x").getAsInt());
                    cat.setY(pos.get("y").getAsInt());
                    cat.setOpened(pos.get("open").getAsBoolean());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
