package kereviz.module.modules;

import kereviz.event.EventTarget;
import kereviz.event.types.EventType;
import kereviz.event.types.Priority;
import kereviz.events.MoveInputEvent;
import kereviz.events.TickEvent;
import kereviz.events.UpdateEvent;
import kereviz.module.Module;
import kereviz.util.BlockUtil;
import kereviz.util.ItemUtil;
import kereviz.util.MoveUtil;
import kereviz.util.PlayerUtil;
import kereviz.property.properties.BooleanProperty;
import kereviz.property.properties.IntProperty;
import kereviz.util.RotationUtil;
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
    private static final double[] PLACE_OFFSETS = new double[]{
            0.03125, 0.09375, 0.15625, 0.21875,
            0.28125, 0.34375, 0.40625, 0.46875,
            0.53125, 0.59375, 0.65625, 0.71875,
            0.78125, 0.84375, 0.90625, 0.96875
    };

    private int sneakDelay = 0;
    private int placeDelay = 0;
    private BlockData pendingBlockData = null;
    private Vec3 pendingHitVec = null;
    public final IntProperty minDelay = new IntProperty("min-delay", 2, 0, 10);
    public final IntProperty maxDelay = new IntProperty("max-delay", 3, 0, 10);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty jumpCheck = new BooleanProperty("jump-check", true);
    public final BooleanProperty pitchCheck = new BooleanProperty("pitch-check", true);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", true);
    public final BooleanProperty sneakOnly = new BooleanProperty("sneaking-only", false);
    public final BooleanProperty silentAim = new BooleanProperty("silent-aim", false);

    private boolean canMoveSafely() {
        double[] offset = MoveUtil.predictMovement();
        return PlayerUtil.canMove(mc.thePlayer.motionX + offset[0], mc.thePlayer.motionZ + offset[1]);
    }

    private boolean canAutoPlace() {
        if (!this.silentAim.getValue() || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
            return false;
        }
        if (this.jumpCheck.getValue() && mc.gameSettings.keyBindJump.isKeyDown()) {
            return false;
        }
        return mc.thePlayer.onGround && MoveUtil.isForwardPressed() && ItemUtil.isHoldingBlock() && this.canMoveSafely();
    }

    private boolean shouldSneak() {
        if (this.silentAim.getValue()) {
            return false;
        } else if (this.directionCheck.getValue() && mc.gameSettings.keyBindForward.isKeyDown()) {
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

    public Eagle() {
        super("Eagle", false);
    }

    private BlockPos getPlaceTargetPos() {
        double[] offset = MoveUtil.predictMovement();
        return new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX + mc.thePlayer.motionX + offset[0]),
                MathHelper.floor_double(mc.thePlayer.posY) - 1,
                MathHelper.floor_double(mc.thePlayer.posZ + mc.thePlayer.motionZ + offset[1])
        );
    }

    private BlockData getBlockData() {
        BlockPos targetPos = this.getPlaceTargetPos();
        if (!BlockUtil.isReplaceable(targetPos)) {
            return null;
        }

        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing == EnumFacing.DOWN) {
                continue;
            }
            BlockPos support = targetPos.offset(facing.getOpposite());
            if (!BlockUtil.isReplaceable(support)
                    && !BlockUtil.isInteractable(support)
                    && mc.thePlayer.getDistance(
                    (double) support.getX() + 0.5,
                    (double) support.getY() + 0.5,
                    (double) support.getZ() + 0.5
            ) <= (double) mc.playerController.getBlockReachDistance()) {
                return new BlockData(support, facing);
            }
        }
        return null;
    }

    private AimData getAimData(UpdateEvent event, BlockData blockData) {
        double[] xOffsets = PLACE_OFFSETS;
        double[] yOffsets = PLACE_OFFSETS;
        double[] zOffsets = PLACE_OFFSETS;
        switch (blockData.facing) {
            case NORTH:
                zOffsets = new double[]{0.0};
                break;
            case EAST:
                xOffsets = new double[]{1.0};
                break;
            case SOUTH:
                zOffsets = new double[]{1.0};
                break;
            case WEST:
                xOffsets = new double[]{0.0};
                break;
            case DOWN:
                yOffsets = new double[]{0.0};
                break;
            case UP:
                yOffsets = new double[]{1.0};
                break;
        }

        AimData best = null;
        for (double dx : xOffsets) {
            for (double dy : yOffsets) {
                for (double dz : zOffsets) {
                    double relX = (double) blockData.pos.getX() + dx - mc.thePlayer.posX;
                    double relY = (double) blockData.pos.getY() + dy - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                    double relZ = (double) blockData.pos.getZ() + dz - mc.thePlayer.posZ;
                    float[] rotations = RotationUtil.getRotationsTo(relX, relY, relZ, event.getYaw(), event.getPitch());
                    MovingObjectPosition mop = RotationUtil.rayTrace(rotations[0], rotations[1], mc.playerController.getBlockReachDistance(), 1.0F);
                    if (mop == null
                            || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                            || !mop.getBlockPos().equals(blockData.pos)
                            || mop.sideHit != blockData.facing) {
                        continue;
                    }

                    float diff = Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - event.getYaw()))
                            + Math.abs(MathHelper.wrapAngleTo180_float(rotations[1] - event.getPitch()));
                    if (best == null || diff < best.diff) {
                        best = new AimData(rotations[0], rotations[1], mop.hitVec, diff);
                    }
                }
            }
        }
        return best;
    }

    private boolean place(BlockData blockData, Vec3 hitVec) {
        if (ItemUtil.isHoldingBlock()
                && mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                mc.thePlayer.inventory.getCurrentItem(),
                blockData.pos,
                blockData.facing,
                hitVec
        )) {
            mc.thePlayer.swingItem();
            return true;
        }
        return false;
    }

    @EventTarget(Priority.HIGH)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }

        if (event.getType() == EventType.POST) {
            this.placePendingBlock();
            return;
        }

        if (event.getType() != EventType.PRE) {
            return;
        }

        this.pendingBlockData = null;
        this.pendingHitVec = null;
        if (this.placeDelay > 0) {
            this.placeDelay--;
        }

        if (!this.canAutoPlace()) {
            return;
        }

        BlockData blockData = this.getBlockData();
        if (blockData == null) {
            return;
        }

        AimData aimData = this.getAimData(event, blockData);
        if (aimData == null) {
            return;
        }

        event.setRotation(aimData.yaw, aimData.pitch, 2);
        if (this.placeDelay <= 0) {
            this.pendingBlockData = blockData;
            this.pendingHitVec = aimData.hitVec;
        }
    }

    private void placePendingBlock() {
        if (!this.silentAim.getValue() || this.pendingBlockData == null || this.pendingHitVec == null) {
            return;
        }

        if (this.place(this.pendingBlockData, this.pendingHitVec)) {
            this.placeDelay = VANILLA_PLACE_DELAY;
        }
        this.pendingBlockData = null;
        this.pendingHitVec = null;
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && !this.silentAim.getValue() && event.getType() == EventType.PRE) {
            if (this.sneakDelay > 0) {
                this.sneakDelay--;
            }
            if (this.sneakDelay == 0 && this.canMoveSafely()) {
                this.sneakDelay = RandomUtils.nextInt(this.minDelay.getValue(), this.maxDelay.getValue() + 1);
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled() && !this.silentAim.getValue() && mc.currentScreen == null) {

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
        this.pendingBlockData = null;
        this.pendingHitVec = null;
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
        return this.silentAim.getValue() ? new String[]{delay, "Silent"} : new String[]{delay};
    }

    private static class BlockData {
        private final BlockPos pos;
        private final EnumFacing facing;

        private BlockData(BlockPos pos, EnumFacing facing) {
            this.pos = pos;
            this.facing = facing;
        }
    }

    private static class AimData {
        private final float yaw;
        private final float pitch;
        private final Vec3 hitVec;
        private final float diff;

        private AimData(float yaw, float pitch, Vec3 hitVec, float diff) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.hitVec = hitVec;
            this.diff = diff;
        }
    }
}
