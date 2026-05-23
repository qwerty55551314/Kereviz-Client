package kereviz.module.modules;

import kereviz.Kereviz;
import kereviz.event.EventTarget;
import kereviz.events.TickEvent;
import kereviz.module.Module;
import kereviz.property.properties.BooleanProperty;
import kereviz.util.ChatUtil;
import kereviz.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.network.play.client.*;
import org.lwjgl.input.Keyboard;

public class AutoHypixel extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty hypixelinv = new BooleanProperty("InvDisabler", true);
    public final BooleanProperty policiesaccept = new BooleanProperty("AutoPolicies", true);
    public final BooleanProperty policiesacceptsilent = new BooleanProperty("Silent", false, this.policiesaccept::getValue);
    private boolean policiesacceptboolean;

    public AutoHypixel() {
        super("AutoHypixel", false, false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            if (this.hypixelinv.getValue() && mc.currentScreen instanceof GuiInventory) {
                boolean moving = (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()) ||
                        Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()) ||
                        Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()) ||
                        Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
                if (mc.thePlayer.isPotionActive(net.minecraft.potion.Potion.moveSpeed)) {
                    if (mc.thePlayer.ticksExisted % 4 == 0 && moving) {
                        PacketUtil.sendPacket(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
                    } else if (mc.thePlayer.ticksExisted % 4 == 1 && moving) {
                        PacketUtil.sendPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
                    }
                } else {
                    if (mc.thePlayer.ticksExisted % 3 == 0 && moving) {
                        PacketUtil.sendPacket(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
                    } else if (mc.thePlayer.ticksExisted % 3 == 1 && moving) {
                        PacketUtil.sendPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
                    }
                }
            }
            if (this.policiesaccept.getValue() && Minecraft.getMinecraft().getCurrentServerData() != null && Minecraft.getMinecraft().getCurrentServerData().serverIP.toLowerCase().contains("hypixel")) {
                if (mc.currentScreen instanceof GuiScreenBook) {
                    if (!policiesacceptboolean) {
                        PacketUtil.sendPacket(new C01PacketChatMessage("/policies accept"));
                        ChatUtil.sendFormatted(String.format("%s&rAutoPolicies", Kereviz.clientName));
                        if (policiesacceptsilent.getValue()) mc.displayGuiScreen(null);
                        policiesacceptboolean = true;
                    }
                } else {
                    policiesacceptboolean = false;
                }
            }
        }
    }
}