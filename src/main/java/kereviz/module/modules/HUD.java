package kereviz.module.modules;

import kereviz.Kereviz;
import kereviz.enums.BlinkModules;
import kereviz.enums.ChatColors;
import kereviz.event.EventTarget;
import kereviz.event.types.EventType;
import kereviz.events.Render2DEvent;
import kereviz.events.TickEvent;
import kereviz.font.CFontRenderer;
import kereviz.font.FontProcess;
import kereviz.mixin.IAccessorGuiChat;
import kereviz.module.Module;
import kereviz.util.ColorUtil;
import kereviz.util.RenderUtil;
import kereviz.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private List<Module> activeModules = new ArrayList<>();
    public final ModeProperty colorMode = new ModeProperty(
            "color", 3, new String[]{"RAINBOW", "CHROMA", "ASTOLFO", "CUSTOM1", "CUSTOM12", "CUSTOM123"}
    );
    public final FloatProperty colorSpeed = new FloatProperty("color-speed", 1.0F, 0.5F, 1.5F);
    public final PercentProperty colorSaturation = new PercentProperty("color-saturation", 100);
    public final PercentProperty colorBrightness = new PercentProperty("color-brightness", 100);
    public final ColorProperty custom1 = new ColorProperty("custom-color-1", new Color(0x14FF00).getRGB(), () -> this.colorMode.getValue() == 3 || this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom2 = new ColorProperty("custom-color-2", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom3 = new ColorProperty("custom-color-3", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 5);
    public final ModeProperty posX = new ModeProperty("position-x", 0, new String[]{"LEFT", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 0, new String[]{"TOP", "BOTTOM"});
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 2000);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 2000);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final ModeProperty interfaceMode = new ModeProperty("interface", 0, new String[]{"KEREVIZ", "CREIDA"});
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty showBar = new BooleanProperty("bar", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final FloatProperty colorDistance = new FloatProperty("color-dist", 50F, 10F, 100F);
    public final BooleanProperty suffixes = new BooleanProperty("suffixes", true);
    public final BooleanProperty lowerCase = new BooleanProperty("lower-case", false);
    public final BooleanProperty chatOutline = new BooleanProperty("chat-outline", true);
    public final BooleanProperty blinkTimer = new BooleanProperty("blink-timer", true);
    public final BooleanProperty toggleSound = new BooleanProperty("toggle-sounds", true);
    public final BooleanProperty toggleAlerts = new BooleanProperty("toggle-alerts", false);
    public final ModeProperty fontMode = new ModeProperty("font-mode", 0, new String[]{"SANS", "MINECRAFT", "NUNITO"});
    public final BooleanProperty creidaFont = new BooleanProperty("creida-font", true, () -> this.interfaceMode.getValue() == 1);
    public final BooleanProperty creidaWatermark = new BooleanProperty("creida-watermark", true, () -> this.interfaceMode.getValue() == 1);
    public final BooleanProperty rounded = new BooleanProperty("rounded", true);
    public final FloatProperty cornerRadius = new FloatProperty("corner-radius", 4.0F, 1.0F, 8.0F, () -> rounded.getValue());
    public final FloatProperty padding = new FloatProperty("padding", 2.0F, 0.0F, 6.0F);
    private boolean editorDragging;
    private float editorDragX;
    private float editorDragY;

    private String getModuleName(Module module) {
        String moduleName = module.getName();
        if (this.lowerCase.getValue()) {
            moduleName = moduleName.toLowerCase(Locale.ROOT);
        }
        return moduleName;
    }

    CFontRenderer fontRenderer;
    private CFontRenderer nunitoFontRenderer;
    private final net.minecraft.client.gui.FontRenderer mcFont = mc.fontRendererObj;
    private int lastFontMode = -1; // Cache to prevent unnecessary updates

    private String[] getModuleSuffix(Module module) {
        String[] moduleSuffix = module.getSuffix();
        if (this.lowerCase.getValue()) {
            for (int i = 0; i < moduleSuffix.length; i++) {
                moduleSuffix[i] = moduleSuffix[i].toLowerCase();
            }
        }
        return moduleSuffix;
    }


    private int getModuleWidth(Module module) {
        if (this.interfaceMode.getValue() == 1) {
            return Math.round(this.getCreidaModuleWidth(module));
        }
        return this.calculateStringWidth(
                this.getModuleName(module), this.getModuleSuffix(module)
        );
    }

    private int calculateStringWidth(String string, String[] arr) {
        int width;
        switch (fontMode.getValue()) {
            case 0: // SANS
                width = fontRenderer.getStringWidth(string);
                break;
            case 1: // MINECRAFT
                width = mcFont.getStringWidth(string);
                break;
            default:
                width = fontRenderer.getStringWidth(string);
                break;
        }
        if (this.suffixes.getValue()) {
            for (String str : arr) {
                switch (fontMode.getValue()) {
                    case 1: // MINECRAFT
                        width += 3 + mcFont.getStringWidth(str);
                        break;
                    default:
                        width += 3 + fontRenderer.getStringWidth(str);
                        break;
                }
            }
        }
        return width;
    }

    private float getColorCycle(long long3, long long4) {
        long speed = (long) (3000.0 / Math.pow(Math.min(Math.max(0.5F, this.colorSpeed.getValue()), 1.5F), 3.0));
        return 1.0F - (float) (Math.abs(long3 - long4 * 300L) % speed) / (float) speed;
    }

    public HUD() {
        super("HUD", true, true, "Wdym It HUD u never know it :?");
        updateFontRenderer();
    }

    private void updateFontRenderer() {
        // Only update if font mode actually changed
        if (lastFontMode == fontMode.getValue()) {
            return;
        }

        CFontRenderer selectedFont;
        switch (fontMode.getValue()) {
            case 0: // SANS
                selectedFont = FontProcess.getFont("sans");
                break;
            case 1: // MINECRAFT - handled separately
                selectedFont = FontProcess.getFont("sans");
                break;
            case 2: // NUNITO
                selectedFont = getNunitoFontRenderer();
                break;
            default:
                selectedFont = FontProcess.getFont("sans");
                break;
        }

        if (selectedFont == null) {
            System.err.println("[Kereviz] Failed to resolve HUD font mode: " + fontMode.getModeString());
            fontRenderer = FontProcess.getFont("sans");
            lastFontMode = -1;
            return;
        }

        fontRenderer = selectedFont;
        lastFontMode = fontMode.getValue();
    }

    private CFontRenderer getNunitoFontRenderer() {
        if (nunitoFontRenderer == null) {
            nunitoFontRenderer = new CFontRenderer("nunito", 18, Font.PLAIN, true, false);
            System.out.println("[Kereviz] HUD Nunito loaded as: " + nunitoFontRenderer.getFont().getFontName());
        }
        return nunitoFontRenderer;
    }

    public Color getColor(long time) {
        return this.getColor(time, 0L);
    }

    public Color getColor(long time, long offset) {
        Color color = Color.white;
        switch (this.colorMode.getValue()) {
            case 0:
                color = ColorUtil.fromHSB(this.getColorCycle(time, offset), 1.0F, 1.0F);
                break;
            case 1:
                color = ColorUtil.fromHSB(this.getColorCycle(time / 3L, 0L), 1.0F, 1.0F);
                break;
            case 2:
                float cycle = this.getColorCycle(time, offset);
                if (cycle % 1.0F < 0.5F) {
                    cycle = 1.0F - cycle % 1.0F;
                }
                color = ColorUtil.fromHSB(cycle, 1.0F, 1.0F);
                break;
            case 3:
                color = new Color(this.custom1.getValue());
                break;
            case 4:
                double cycle1 = this.getColorCycle(time, offset);
                color = ColorUtil.interpolate(
                        (float) (2.0 * Math.abs(cycle1 - Math.floor(cycle1 + 0.5))),
                        new Color(this.custom1.getValue()),
                        new Color(this.custom2.getValue())
                );
                break;
            case 5:
                double cycle2 = this.getColorCycle(time, offset);
                float floor = (float) (2.0 * Math.abs(cycle2 - Math.floor(cycle2 + 0.5)));
                if (floor <= 0.5F) {
                    color = ColorUtil.interpolate(floor * 2.0F, new Color(this.custom1.getValue()), new Color(this.custom2.getValue()));
                } else {
                    color = ColorUtil.interpolate((floor - 0.5F) * 2.0F, new Color(this.custom2.getValue()), new Color(this.custom3.getValue()));
                }
        }
        if (this.colorSaturation.getValue() == 100 && this.colorBrightness.getValue() == 100) {
            return color;
        }
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(
                hsb[0],
                hsb[1] * (this.colorSaturation.getValue().floatValue() / 100.0F),
                hsb[2] * (this.colorBrightness.getValue().floatValue() / 100.0F)
        );
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            this.activeModules = Kereviz.moduleManager.modules.values().stream().filter(module -> module.isEnabled() && !module.isHidden()).sorted(Comparator.comparingInt(this::getModuleWidth).reversed()).collect(Collectors.<Module>toList());
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        // Update font renderer to ensure it uses current setting
        updateFontRenderer();
        
        if (this.chatOutline.getValue() && mc.currentScreen instanceof GuiChat) {
            String text = ((IAccessorGuiChat) mc.currentScreen).getInputField().getText().trim();
            if (Kereviz.commandManager != null && Kereviz.commandManager.isTypingCommand(text)) {
                RenderUtil.enableRenderState();
                RenderUtil.drawOutlineRect(
                        2.0F,
                        (float) (mc.currentScreen.height - 14),
                        (float) (mc.currentScreen.width - 2),
                        (float) (mc.currentScreen.height - 2),
                        1.5F,
                        0,
                        this.getColor(System.currentTimeMillis()).getRGB()
                );
                RenderUtil.disableRenderState();
            }
        }
        if (this.isEnabled() && !mc.gameSettings.showDebugInfo) {
            if (this.interfaceMode.getValue() == 1) {
                renderCreidaInterface();
            } else {
            float height = (float) fontRenderer.FONT_HEIGHT - 1.0F;
            float x = (float) this.offsetX.getValue()
                    + (1.0F + (this.showBar.getValue() ? (this.shadow.getValue() ? 2.0F : 1.0F) : 0.0F)) * this.scale.getValue();
            float y = (float) this.offsetY.getValue() + 1.0F * this.scale.getValue();
            if (this.posX.getValue() == 1) {
                x = (float) new ScaledResolution(mc).getScaledWidth() - x;
            }
            if (this.posY.getValue() == 1) {
                y = (float) new ScaledResolution(mc).getScaledHeight() - y - height * this.scale.getValue();
            }
            GlStateManager.pushMatrix();
            GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);


            long l = System.currentTimeMillis();
            long offset = 0L;
            for (Module module : this.activeModules) {
                String moduleName = this.getModuleName(module);
                String[] moduleSuffix = this.getModuleSuffix(module);
                float totalWidth = (float) (this.calculateStringWidth(moduleName, moduleSuffix) - (this.shadow.getValue() ? 0 : 1));
                int color = this.getColor(l, offset).getRGB();
                RenderUtil.enableRenderState();
                if (this.background.getValue() > 0) {
                    float pad = this.padding.getValue();
                    float bgX1 = x / this.scale.getValue() - 1.0F - pad - (this.posX.getValue() == 0 ? 0.0F : totalWidth);
                    float bgY1 = y / this.scale.getValue() - pad - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : (this.shadow.getValue() ? 1.0F : 0.0F));
                    float bgX2 = x / this.scale.getValue() + 1.0F + pad + (this.posX.getValue() == 0 ? totalWidth : 0.0F);
                    float bgY2 = y / this.scale.getValue() + height + pad + (this.posY.getValue() == 0 ? (this.shadow.getValue() ? 1.0F : 0.0F) : (offset == 0L ? 1.0F : 0.0F));
                    int bgColor = new Color(0.0F, 0.0F, 0.0F, this.background.getValue().floatValue() / 100.0F).getRGB();
                    if (this.rounded.getValue()) {
                        float bgW = bgX2 - bgX1;
                        float bgH = bgY2 - bgY1;
                        float rad = this.cornerRadius.getValue();
                        boolean isFirst = (offset == 0L);
                        boolean isLast = (offset == this.activeModules.size() - 1);
                        boolean sideLeft = this.posX.getValue() == 1;
                        boolean sideRight = this.posX.getValue() == 0;
                        boolean isTopEntry = (this.posY.getValue() == 0) ? isFirst : isLast;
                        boolean isBottomEntry = (this.posY.getValue() == 0) ? isLast : isFirst;
                        RenderUtil.drawRoundedRect(
                                bgX1, bgY1, bgW, bgH, rad, bgColor,
                                sideLeft && isTopEntry, sideRight && isTopEntry,
                                sideLeft && isBottomEntry, sideRight && isBottomEntry
                        );
                    } else {
                        RenderUtil.drawRect(bgX1, bgY1, bgX2, bgY2, bgColor);
                    }
                }
                if (this.showBar.getValue()) {
                    float pad = this.padding.getValue();
                    if (this.shadow.getValue()) {
                        RenderUtil.drawRect(
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -3.0F : 1.0F),
                                y / this.scale.getValue() - pad - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 2.0F),
                                y / this.scale.getValue() + height + pad + (this.posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                                color
                        );
                        RenderUtil.drawRect(
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 2.0F),
                                y / this.scale.getValue() - pad - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -1.0F : 3.0F),
                                y / this.scale.getValue() + height + pad + (this.posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                                (color & 16579836) >> 2 | color & 0xFF000000
                        );
                    } else {
                        RenderUtil.drawRect(
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 1.0F),
                                y / this.scale.getValue() - pad - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 0.0F),
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -1.0F : 2.0F),
                                y / this.scale.getValue() + height + pad + (this.posY.getValue() == 0 ? 0.0F : (offset == 0L ? 1.0F : 0.0F)),
                                color
                        );
                    }
                }
                RenderUtil.disableRenderState();
                GlStateManager.disableDepth();
                if (this.shadow.getValue()) {
                    if (fontMode.getValue() == 1) { // MINECRAFT
                        mcFont.drawStringWithShadow(moduleName, x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F), y / this.scale.getValue(), color);
                    } else {
                        fontRenderer.drawStringWithShadow(moduleName, x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F), y / this.scale.getValue(), color);
                    }
                } else {
                    if (fontMode.getValue() == 1) { // MINECRAFT
                        mcFont.drawString(
                                moduleName,
                                x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F),
                                y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                color,
                                false
                        );
                    } else {
                        fontRenderer.drawString(
                                moduleName,
                                x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F),
                                y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                color,
                                false
                        );
                    }
                }
                if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                    float width;
                    switch (fontMode.getValue()) {
                        case 1: // MINECRAFT
                            width = (float) mcFont.getStringWidth(moduleName) + 3.0F;
                            break;
                        default:
                            width = (float) fontRenderer.getStringWidth(moduleName) + 3.0F;
                            break;
                    }
                    for (String string : moduleSuffix) {
                        if (this.shadow.getValue()) {
                            if (fontMode.getValue() == 1) { // MINECRAFT
                                mcFont.drawStringWithShadow(
                                        string,
                                        x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                        y / this.scale.getValue(),
                                        ChatColors.GRAY.toAwtColor()
                                );
                            } else {
                                fontRenderer.drawStringWithShadow(
                                        string,
                                        x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                        y / this.scale.getValue(),
                                        ChatColors.GRAY.toAwtColor()
                                );
                            }
                        } else {
                            if (fontMode.getValue() == 1) { // MINECRAFT
                                mcFont.drawString(
                                        string,
                                        x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                        y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                        ChatColors.GRAY.toAwtColor(),
                                        false
                                );
                            } else {
                                fontRenderer.drawString(
                                        string,
                                        x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                        y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                        ChatColors.GRAY.toAwtColor(),
                                        false
                                );
                            }
                        }
                        width += (fontMode.getValue() == 1 ? (float) mcFont.getStringWidth(string) : (float) fontRenderer.getStringWidth(string)) + (this.shadow.getValue() ? 3.0F : 2.0F);
                    }
                }
                y += (height + (this.shadow.getValue() ? 1.0F : 0.0F) + this.padding.getValue() * 2.0F) * this.scale.getValue() * (this.posY.getValue() == 0 ? 1.0F : -1.0F);
                offset++;
            }
            if (this.blinkTimer.getValue()) {
                BlinkModules blinkingModule = Kereviz.blinkManager.getBlinkingModule();
                if (blinkingModule != BlinkModules.NONE && blinkingModule != BlinkModules.AUTO_BLOCK) {
                    long movementPacketSize = Kereviz.blinkManager.countMovement();
                    if (movementPacketSize > 0L) {
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        if (fontMode.getValue() == 1) { // MINECRAFT
                            mcFont.drawString(
                                    String.valueOf(movementPacketSize),
                                    (float) new ScaledResolution(mc).getScaledWidth() / 2.0F / this.scale.getValue()
                                            - (float) mcFont.getStringWidth(String.valueOf(movementPacketSize)) / 2.0F,
                                    (float) new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.scale.getValue(),
                                    this.getColor(l, offset).getRGB() & 16777215 | -1090519040,
                                    this.shadow.getValue()
                            );
                        } else {
                            fontRenderer.drawString(
                                    String.valueOf(movementPacketSize),
                                    (float) new ScaledResolution(mc).getScaledWidth() / 2.0F / this.scale.getValue()
                                            - (float) fontRenderer.getStringWidth(String.valueOf(movementPacketSize)) / 2.0F,
                                    (float) new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.scale.getValue(),
                                    this.getColor(l, offset).getRGB() & 16777215 | -1090519040,
                                    this.shadow.getValue()
                            );
                        }
                        GlStateManager.disableBlend();
                    }
                }
            }
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();


            }
        }
        renderNotifications();
    }

    public void renderEditorOverlay(int mouseX, int mouseY) {
        if (!this.isEnabled() || mc.gameSettings.showDebugInfo) {
            return;
        }

        float[] bounds = getEditorBounds();
        boolean hovered = isInside(bounds, mouseX, mouseY);
        int accent = this.getColor(System.currentTimeMillis()).getRGB();
        int fill = new Color(8, 12, 10, hovered || editorDragging ? 95 : 58).getRGB();
        int outline = (accent & 0x00FFFFFF) | ((hovered || editorDragging ? 220 : 150) << 24);

        RenderUtil.drawRoundedRect(bounds[0], bounds[1], bounds[2], bounds[3], 5.0F,
                fill, true, true, true, true);
        RenderUtil.drawRoundedRectOutline(bounds[0] + 0.5F, bounds[1] + 0.5F,
                bounds[2] - 1.0F, bounds[3] - 1.0F, 5.0F, 1.2F,
                outline, true, true, true, true);

        String label = "HUD";
        int labelColor = new Color(235, 255, 240, hovered || editorDragging ? 245 : 190).getRGB();
        drawHudText(label, bounds[0] + 6.0F, bounds[1] + 5.0F, labelColor);
    }

    public boolean mouseClickedEditor(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || !this.isEnabled()) {
            return false;
        }

        float[] bounds = getEditorBounds();
        if (!isInside(bounds, mouseX, mouseY)) {
            return false;
        }

        this.editorDragging = true;
        this.editorDragX = mouseX - bounds[0];
        this.editorDragY = mouseY - bounds[1];
        return true;
    }

    public void mouseReleasedEditor() {
        this.editorDragging = false;
    }

    public void mouseDraggedEditor(int mouseX, int mouseY) {
        if (!this.editorDragging) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int x = Math.max(0, Math.min(2000, Math.round(mouseX - this.editorDragX)));
        int y = Math.max(0, Math.min(2000, Math.round(mouseY - this.editorDragY)));
        x = Math.min(x, Math.max(0, sr.getScaledWidth() - 12));
        y = Math.min(y, Math.max(0, sr.getScaledHeight() - 12));

        this.posX.setValue(0);
        this.posY.setValue(0);
        this.offsetX.setValue(x);
        this.offsetY.setValue(y);
    }

    public boolean isEditorDragging() {
        return this.editorDragging;
    }

    private boolean isInside(float[] bounds, int mouseX, int mouseY) {
        return mouseX >= bounds[0] && mouseX <= bounds[0] + bounds[2]
                && mouseY >= bounds[1] && mouseY <= bounds[1] + bounds[3];
    }

    private float[] getEditorBounds() {
        updateFontRenderer();
        ScaledResolution sr = new ScaledResolution(mc);
        float hudScale = Math.max(0.5F, Math.min(1.5F, this.scale.getValue()));
        float x = Math.max(0.0F, this.offsetX.getValue());
        float y = Math.max(0.0F, this.offsetY.getValue());
        float width;
        float height;

        if (this.interfaceMode.getValue() == 1) {
            width = getCreidaWatermarkWidth() * hudScale;
            height = Math.max(16.0F, getCreidaTextHeight() + 8.0F) * hudScale;
        } else {
            float maxWidth = Math.max(getHudTextWidth("HUD"), 52.0F);
            for (Module module : this.activeModules) {
                maxWidth = Math.max(maxWidth, this.getModuleWidth(module));
            }
            float rowHeight = ((float) fontRenderer.FONT_HEIGHT - 1.0F
                    + (this.shadow.getValue() ? 1.0F : 0.0F)
                    + this.padding.getValue() * 2.0F) * hudScale;
            height = Math.max(18.0F, Math.max(1, this.activeModules.size()) * rowHeight + 4.0F);
            width = Math.max(70.0F, (maxWidth + this.padding.getValue() * 2.0F
                    + (this.showBar.getValue() ? 8.0F : 2.0F)) * hudScale);
        }

        if (this.posX.getValue() == 1) {
            x = sr.getScaledWidth() - x - width;
        }
        if (this.posY.getValue() == 1) {
            y = sr.getScaledHeight() - y - height;
        }

        x = Math.max(0.0F, Math.min(x, sr.getScaledWidth() - Math.min(width, sr.getScaledWidth())));
        y = Math.max(0.0F, Math.min(y, sr.getScaledHeight() - Math.min(height, sr.getScaledHeight())));
        return new float[]{x, y, width, height};
    }

    private float getCreidaWatermarkWidth() {
        return getCreidaTextWidth(getCreidaWatermarkText()) + 10.0F;
    }

    private void renderCreidaInterface() {
        ScaledResolution sr = new ScaledResolution(mc);
        float hudScale = Math.max(0.5F, Math.min(1.5F, this.scale.getValue()));
        float scaledWidth = sr.getScaledWidth() / hudScale;
        float edgeOffset = Math.max(3.0F, this.offsetX.getValue());
        float right = scaledWidth - edgeOffset;
        float y = Math.max(3.0F, this.offsetY.getValue() + 1.0F);
        float entryHeight = getCreidaEntryHeight();
        long time = System.currentTimeMillis();

        GlStateManager.pushMatrix();
        GlStateManager.scale(hudScale, hudScale, 1.0F);

        if (this.creidaWatermark.getValue()) {
            renderCreidaWatermark(time);
        }

        long offset = 0L;
        for (Module module : this.activeModules) {
            renderCreidaModule(module, right, y, time, offset, entryHeight);
            y += entryHeight;
            offset++;
        }

        GlStateManager.popMatrix();
    }

    private void renderCreidaModule(Module module, float right, float y, long time, long offset, float entryHeight) {
        String moduleName = this.getModuleName(module);
        String[] moduleSuffix = this.getModuleSuffix(module);
        float nameWidth = this.getCreidaTextWidth(moduleName);
        float tagWidth = this.getCreidaTagWidth(moduleSuffix);
        float boxWidth = nameWidth + tagWidth + 7.0F;
        float boxHeight = entryHeight;
        float x = right - boxWidth;
        float textY = y + getCreidaTextOffset(entryHeight);
        int accent = this.getColor(time, offset).getRGB();
        int backgroundColor = new Color(0, 0, 0, 110).getRGB();
        int depthColor = new Color(0, 0, 0, 65).getRGB();

        RenderUtil.enableRenderState();
        RenderUtil.drawRect(x - 1.0F, y, x + boxWidth, y + boxHeight, depthColor);
        RenderUtil.drawRect(x - 1.0F, y, x + boxWidth, y + boxHeight, backgroundColor);
        RenderUtil.drawRect(x + boxWidth - 1.0F, y, x + boxWidth, y + boxHeight, accent);
        RenderUtil.disableRenderState();

        GlStateManager.disableDepth();
        drawCreidaStringWithShadow(moduleName, x + 1.0F, textY, accent);

        if (this.suffixes.getValue() && moduleSuffix.length > 0) {
            float suffixX = x + 1.0F + nameWidth + 4.0F;
            for (String suffix : moduleSuffix) {
                drawCreidaStringWithShadow(suffix, suffixX, textY, 0xFFCCCCCC);
                suffixX += this.getCreidaTextWidth(suffix) + 3.0F;
            }
        }
        GlStateManager.enableDepth();
    }

    private void renderCreidaWatermark(long time) {
        String text = getCreidaWatermarkText();
        float textWidth = getCreidaTextWidth(text);
        float boxWidth = textWidth + 8.0F;
        float boxHeight = Math.max(15.0F, getCreidaTextHeight() + 6.0F);
        float x = 2.0F;
        float y = 3.0F;

        RenderUtil.drawRoundedRect(x, y, boxWidth, boxHeight, 4.0F,
                new Color(0, 0, 0, 100).getRGB(), true, true, true, true);

        GlStateManager.resetColor();
        GlStateManager.disableDepth();

        float currentX = x + 4.0F;
        float textY = y + (boxHeight - getCreidaTextHeight()) / 2.0F + 1.0F;
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            int color = this.getColor(time, (long) (i * this.colorDistance.getValue())).getRGB();
            drawCreidaStringWithShadow(character, currentX, textY, color);
            currentX += getCreidaTextWidth(character);
        }

        GlStateManager.enableDepth();
    }

    private String getCreidaWatermarkText() {
        String playerName = mc.thePlayer == null ? "Player" : mc.thePlayer.getName();
        String versionText = Kereviz.version == null ? "dev" : Kereviz.version;
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        return "Kereviz Client | " + versionText + " | " + playerName + " | " + time;
    }

    private float getCreidaModuleWidth(Module module) {
        return getCreidaTextWidth(this.getModuleName(module)) + getCreidaTagWidth(this.getModuleSuffix(module)) + 7.0F;
    }

    private float getCreidaTagWidth(String[] suffixes) {
        if (!this.suffixes.getValue() || suffixes.length == 0) {
            return 1.0F;
        }

        float width = 0.0F;
        for (String suffix : suffixes) {
            width += this.getCreidaTextWidth(suffix) + 3.0F;
        }
        return width + 1.0F;
    }

    private float getCreidaEntryHeight() {
        return Math.max(14.0F, this.getCreidaTextHeight() + 5.0F);
    }

    private float getCreidaTextOffset(float entryHeight) {
        return Math.max(2.0F, (entryHeight - this.getCreidaTextHeight()) / 2.0F);
    }

    private float getCreidaTextWidth(String text) {
        if (isCreidaMinecraftFont()) {
            return mcFont.getStringWidth(text);
        }
        return fontRenderer.getStringWidth(text);
    }

    private float getCreidaTextHeight() {
        if (isCreidaMinecraftFont()) {
            return mcFont.FONT_HEIGHT;
        }
        return fontRenderer.FONT_HEIGHT;
    }

    private void drawCreidaStringWithShadow(String text, float x, float y, int color) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (isCreidaMinecraftFont()) {
            mcFont.drawStringWithShadow(text, x, y, color);
        } else {
            fontRenderer.drawStringWithShadow(text, x, y, color);
        }
        GlStateManager.disableBlend();
    }

    private boolean isCreidaMinecraftFont() {
        return !this.creidaFont.getValue() || this.fontMode.getValue() == 1;
    }

    private void renderNotifications() {
        try {
            if (Kereviz.notificationManager == null) return;

            java.util.List<kereviz.management.NotificationManager.NotificationEntry> entries = Kereviz.notificationManager.getActive();
            if (entries.isEmpty()) return;

            float notificationScale = Math.max(0.5F, Math.min(1.5F, this.scale.getValue()));
            ScaledResolution sr = new ScaledResolution(mc);
            float scaledWidth = sr.getScaledWidth() / notificationScale;
            float scaledHeight = sr.getScaledHeight() / notificationScale;
            float margin = 8.0F;
            float paddingX = 8.0F;
            float paddingY = 5.0F;
            float spacing = 4.0F;
            float y = scaledHeight - margin;

            GlStateManager.pushMatrix();
            GlStateManager.scale(notificationScale, notificationScale, 1.0F);

            for (int i = entries.size() - 1; i >= 0; i--) {
                kereviz.management.NotificationManager.NotificationEntry entry = entries.get(i);
                float alpha = notificationAlpha(entry);
                if (alpha <= 0.01F) continue;

                String text = modernNotificationText(entry.message);
                float textWidth = getHudTextWidth(text);
                float textHeight = getHudTextHeight();
                float boxWidth = Math.max(86.0F, textWidth + paddingX * 2.0F + 2.0F);
                float boxHeight = textHeight + paddingY * 2.0F + 3.0F;
                float x = scaledWidth - margin - boxWidth;
                y -= boxHeight;

                drawModernNotification(entry, text, x, y, boxWidth, boxHeight, paddingX, paddingY, alpha);
                y -= spacing;
            }

            GlStateManager.popMatrix();
        } catch (Exception ignored) {
        }
    }

    private void drawModernNotification(kereviz.management.NotificationManager.NotificationEntry entry, String text,
                                        float x, float y, float boxWidth, float boxHeight,
                                        float paddingX, float paddingY, float alpha) {
        float motion = notificationMotion(entry);
        float slide = (1.0F - motion) * 14.0F + (1.0F - alpha) * 5.0F;
        float renderX = x + slide;
        int statusColor = notificationStatusColor(text, alpha);
        int glass = new Color(10, 12, 16, (int) (92 * alpha)).getRGB();
        int hoverLayer = new Color(255, 255, 255, (int) (9 * alpha)).getRGB();
        int border = new Color(255, 255, 255, (int) (24 * alpha)).getRGB();
        int depth = new Color(0, 0, 0, (int) (28 * alpha)).getRGB();
        int neutralText = new Color(238, 241, 245, (int) (242 * alpha)).getRGB();
        float radius = 6.0F;

        RenderUtil.drawRoundedRect(renderX + 1.0F, y + 1.5F, boxWidth, boxHeight, radius + 1.0F,
                depth, true, true, true, true);
        RenderUtil.drawRoundedRect(renderX, y, boxWidth, boxHeight, radius,
                glass, true, true, true, true);
        RenderUtil.drawRoundedRect(renderX + 1.0F, y + 1.0F, boxWidth - 2.0F, boxHeight - 2.0F, radius - 1.0F,
                hoverLayer, true, true, true, true);
        RenderUtil.drawRoundedRectOutline(renderX + 0.5F, y + 0.5F, boxWidth - 1.0F, boxHeight - 1.0F,
                radius, 1.0F, border, true, true, true, true);

        float progress = notificationProgress(entry);
        float progressX = renderX + 8.0F;
        float progressY = y + boxHeight - 2.0F;
        float progressW = boxWidth - 16.0F;
        RenderUtil.drawRoundedRect(progressX, progressY, progressW, 1.0F, 0.5F,
                new Color(255, 255, 255, (int) (10 * alpha)).getRGB(), true, true, true, true);
        RenderUtil.drawRoundedRect(progressX, progressY, Math.max(1.0F, progressW * progress), 1.0F, 0.5F,
                statusColor, true, true, true, true);

        drawNotificationText(text, renderX + paddingX + 1.0F, y + paddingY + 1.0F, neutralText, statusColor);
    }

    private float notificationAlpha(kereviz.management.NotificationManager.NotificationEntry entry) {
        if (entry.durationMillis <= 0) return 1.0F;

        float age = entry.getAge();
        float remaining = entry.durationMillis - age;
        float fade = Math.min(220.0F, entry.durationMillis / 3.0F);
        float alpha = Math.min(1.0F, Math.min(age / fade, remaining / fade));
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        return alpha * alpha * (3.0F - 2.0F * alpha);
    }

    private float notificationProgress(kereviz.management.NotificationManager.NotificationEntry entry) {
        if (entry.durationMillis <= 0) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F, 1.0F - entry.getAge() / (float) entry.durationMillis));
    }

    private float notificationMotion(kereviz.management.NotificationManager.NotificationEntry entry) {
        if (entry.durationMillis <= 0) return 1.0F;

        float age = entry.getAge();
        float remaining = entry.durationMillis - age;
        float in = Math.max(0.0F, Math.min(1.0F, age / 260.0F));
        float out = Math.max(0.0F, Math.min(1.0F, remaining / 220.0F));
        float motion = Math.min(in, out);
        return motion * motion * (3.0F - 2.0F * motion);
    }

    private String modernNotificationText(String message) {
        if (message == null) return "";
        return message
                .replace(" was toggled successfully", " enabled")
                .replace(" was untoggled successfully", " disabled");
    }

    private int softenColor(int rgb, float amount) {
        amount = Math.max(0.0F, Math.min(1.0F, amount));
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r += (int) ((255 - r) * amount);
        g += (int) ((255 - g) * amount);
        b += (int) ((255 - b) * amount);
        return (r << 16) | (g << 8) | b;
    }

    private int notificationStatusColor(String text, float alpha) {
        String lower = text.toLowerCase(Locale.ROOT);
        int rgb = lower.endsWith(" enabled") ? 0x41D982 : lower.endsWith(" disabled") ? 0xFF5C6C : 0xE5E9F0;
        return colorWithAlpha(rgb, (int) (245 * alpha));
    }

    private int colorWithAlpha(int rgb, int alpha) {
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF,
                Math.max(0, Math.min(255, alpha))).getRGB();
    }

    private float getHudTextWidth(String text) {
        return fontMode.getValue() == 1 ? mcFont.getStringWidth(text) : fontRenderer.getStringWidth(text);
    }

    private float getHudTextHeight() {
        return fontMode.getValue() == 1 ? mcFont.FONT_HEIGHT : fontRenderer.FONT_HEIGHT;
    }

    private void drawHudText(String text, float x, float y, int color) {
        if (fontMode.getValue() == 1) {
            mcFont.drawString(text, x, y, color, false);
        } else {
            fontRenderer.drawString(text, x, y, color, false);
        }
    }

    private void drawNotificationText(String text, float x, float y, int neutralColor, int statusColor) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.endsWith(" enabled")) {
            drawSplitNotificationText(text, " enabled", x, y, neutralColor, statusColor);
        } else if (lower.endsWith(" disabled")) {
            drawSplitNotificationText(text, " disabled", x, y, neutralColor, statusColor);
        } else {
            drawHudText(text, x, y, neutralColor);
        }
    }

    private void drawSplitNotificationText(String text, String suffix, float x, float y, int neutralColor, int statusColor) {
        String main = text.substring(0, text.length() - suffix.length());
        drawHudText(main, x, y, neutralColor);
        drawHudText(suffix.trim(), x + getHudTextWidth(main + " "), y, statusColor);
    }
}
