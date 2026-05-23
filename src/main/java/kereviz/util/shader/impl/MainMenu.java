package kereviz.util.shader.impl;

import kereviz.util.ShaderUtil;
import net.minecraft.client.gui.ScaledResolution;

import static kereviz.config.Config.mc;

public class MainMenu {
    private static final ShaderUtil mainmenu = new ShaderUtil("mainmenu");

    public static void draw(long initTime) {
        ScaledResolution sr = new ScaledResolution(mc);
        mainmenu.init();
        mainmenu.setUniformf("TIME", (float) (System.currentTimeMillis() - initTime) / 1000);
        mainmenu.setUniformf("RESOLUTION", (float) ((double) sr.getScaledWidth() * sr.getScaleFactor()), (float) ((double) sr.getScaledHeight() * sr.getScaleFactor()));
        ShaderUtil.drawFixedQuads();
        mainmenu.unload();
    }
}