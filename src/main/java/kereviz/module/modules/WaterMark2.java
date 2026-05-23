package kereviz.module.modules;

import kereviz.Kereviz;
import net.minecraft.client.Minecraft;
import kereviz.event.EventTarget;
import kereviz.events.Render2DEvent;
import kereviz.module.Module;
import kereviz.property.properties.BooleanProperty;
import kereviz.property.properties.IntProperty;
import kereviz.util.RenderUtil;
import kereviz.font.impl.UFontRenderer; // 必须导入自定义字体类

public class WaterMark2 extends Module {
    public final IntProperty rectLeft = new IntProperty("RectLeft", 2, 0, 20);
    public final IntProperty rectTop = new IntProperty("RectTop", 2, 0, 20);
    public final BooleanProperty shadow = new BooleanProperty("Shadow", true);

    public WaterMark2() {
        super("WaterMark2", false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        UFontRenderer fr = Kereviz.fontManagers.getFont(20);
        String text = "Kereviz Client";

        float textWidth = (float) fr.getStringWidth(text);
        float textHeight = (float) fr.getHeight();

        float padX = 6.0F;
        float padY = 4.0F;

        float startX = (float) rectLeft.getValue();
        float startY = (float) rectTop.getValue();

        float rectRight = startX + textWidth + (padX);
        float rectBottom = startY + textHeight + (padY);

        float radius = 4.0f;

        HUD hud = (HUD) Kereviz.moduleManager.modules.get(HUD.class);

        int fillColor = 0x80000000;
        int hudColor = hud.getColor(System.currentTimeMillis()).getRGB();

        RenderUtil.drawRoundedGradientOutlinedRectangle(
                startX, startY, rectRight, rectBottom,
                radius, fillColor, hudColor, hudColor
        );

        fr.drawString(
                text,
                startX + padX / 2,
                startY,
                hudColor,
                shadow.getValue()
        );
    }
}