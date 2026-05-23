package kereviz.module.modules;

import kereviz.event.EventTarget;
import kereviz.event.types.EventType;
import kereviz.event.types.Priority;
import kereviz.events.MoveInputEvent;
import kereviz.events.TickEvent;
import kereviz.module.Module;
import kereviz.property.properties.BooleanProperty;
import kereviz.property.properties.IntProperty;
import kereviz.util.BlockUtil;
import kereviz.util.ItemUtil;
import kereviz.util.MoveUtil;
import kereviz.util.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.input.Keyboard;

import java.util.Objects;

public class Eagle extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int VANILLA_PLACE_DELAY = 4;

    private int sneakDelay = 0;
    private int placeDelay = 0;
    public final IntProperty minDelay = new IntProperty("min-delay", 2, 0, 10);
    public final IntProperty maxDelay = new IntProperty("max-delay", 3, 0, 10);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty jumpCheck = new BooleanProperty("jump-check", true);
    public final BooleanProperty pitchCheck = new BooleanProperty("pitch-check", true);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", true);
    public final BooleanProperty sneakOnly = new BooleanProperty("sneaking-only", false);
    public final BooleanProperty silentAim = new BooleanProperty("silent-aim", false);

    public Eagle() {
        super("Eagle", false);
    }

    private boolean canMoveSafely() {
        double[] offset = MoveUtil.predictMovement();
        return PlayerUtil.canMove(mc.thePlayer.motionX + offset[0], mc.thePlayer.motionZ + offset[1]);
    }

    private boolean canUsePlaceAssist() {
        if (!this.silentAim.getValue() || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
            return false;
        }
        if (this.jumpCheck.getValue() && mc.gameSettings.keyBindJump.isKeyDown()) {
            return false;
        }
        return mc.thePlayer.onGround
                && mc.gameSettings.keyBindUseItem.isKeyDown()
                && MoveUtil.isForwardPressed()
                && ItemUtil.isHoldingBlock()
                && this.canMoveSafely();
    }

    private boolean shouldSneak() {
        if (this.directionCheck.getValue() && mc.gameSettings.keyBindForward.isKeyDown()) {
            return false;
        } else if (this.jumpCheck.getValue() && mc.gameSettings.keyBindJump.isKeyDown()) {
            return false;
        } else if (this.pitchCheck.getValue() && mc.thePlayer.rotationPitch < 69.0F) {
            return false;
        } else if (sneakOnly.getValue() && !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            return false;
        } else {
            return (!this.blocksOnly.getValue() || ItemUtil.isHoldingBlock()) && mc.thePlayer.onGround;
        }
    }

    private BlockPos getPlaceTargetPos() {
        double[] offset = MoveUtil.predictMovement();
        return new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX + mc.thePlayer.motionX + offset[0]),
                MathHelper.floor_double(mc.thePlayer.posY) - 1,
                MathHelper.floor_double(mc.thePlayer.posZ + mc.thePlayer.motionZ + offset[1])
        );
    }

    private BlockData getLookedBlockData() {
        MovingObjectPosition objectMouseOver = mc.objectMouseOver;
        if (objectMouseOver == null
                || objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || objectMouseOver.sideHit == null
                || objectMouseOver.hitVec == null) {
            return null;
        }

        BlockPos support = objectMouseOver.getBlockPos();
        EnumFacing facing = objectMouseOver.sideHit;
        BlockPos target = support.offset(facing);
        if (!target.equals(this.getPlaceTargetPos())) {
            return null;
        }
        if (!BlockUtil.isReplaceable(target) || BlockUtil.isReplaceable(support) || BlockUtil.isInteractable(support)) {
            return null;
        }
        if (mc.thePlayer.getDistance(
                (double) support.getX() + 0.5,
                (double) support.getY() + 0.5,
                (double) support.getZ() + 0.5
        ) > (double) mc.playerController.getBlockReachDistance()) {
            return null;
        }

        return new BlockData(support, facing, objectMouseOver.hitVec);
    }

    private boolean place(BlockData blockData) {
        if (ItemUtil.isHoldingBlock()
                && mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                mc.thePlayer.inventory.getCurrentItem(),
                blockData.pos,
                blockData.facing,
                blockData.hitVec
        )) {
            mc.thePlayer.swingItem();
            return true;
        }
        return false;
    }

    private void tryLegitPlaceAssist() {
        if (this.placeDelay > 0 || !this.canUsePlaceAssist()) {
            return;
        }

        BlockData blockData = this.getLookedBlockData();
        if (blockData != null && this.place(blockData)) {
            this.placeDelay = VANILLA_PLACE_DELAY;
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }

        if (this.sneakDelay > 0) {
            this.sneakDelay--;
        }
        if (this.sneakDelay == 0 && this.canMoveSafely()) {
            this.sneakDelay = RandomUtils.nextInt(this.minDelay.getValue(), this.maxDelay.getValue() + 1);
        }

        if (this.placeDelay > 0) {
            this.placeDelay--;
        }
        if (this.silentAim.getValue()) {
            this.tryLegitPlaceAssist();
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled() && mc.currentScreen == null) {

            if (sneakOnly.getValue() && Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()) && shouldSneak()) {
                mc.thePlayer.movementInput.sneak = false;
                mc.thePlayer.movementInput.moveForward /= 0.3F;
                mc.thePlayer.movementInput.moveStrafe /= 0.3F;
            }

            if (!mc.thePlayer.movementInput.sneak) {
                if (this.shouldSneak() && (this.sneakDelay > 0 || this.canMoveSafely())) {
                    mc.thePlayer.movementInput.sneak = true;
                    mc.thePlayer.movementInput.moveStrafe *= 0.3F;
                    mc.thePlayer.movementInput.moveForward *= 0.3F;
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        this.sneakDelay = 0;
        this.placeDelay = 0;
    }

    @Override
    public void verifyValue(String name) {
        switch (name) {
            case "min-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.maxDelay.setValue(this.minDelay.getValue());
                }
                break;
            case "max-delay":
                if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                    this.minDelay.setValue(this.maxDelay.getValue());
                }
        }
    }

    @Override
    public String[] getSuffix() {
        String delay = Objects.equals(this.minDelay.getValue(), this.maxDelay.getValue())
                ? this.minDelay.getValue().toString()
                : String.format("%d-%d", this.minDelay.getValue(), this.maxDelay.getValue());
        return this.silentAim.getValue() ? new String[]{delay, "LegitPlace"} : new String[]{delay};
    }

    private static class BlockData {
        private final BlockPos pos;
        private final EnumFacing facing;
        private final Vec3 hitVec;

        private BlockData(BlockPos pos, EnumFacing facing, Vec3 hitVec) {
            this.pos = pos;
            this.facing = facing;
            this.hitVec = hitVec;
        }
    }
}
