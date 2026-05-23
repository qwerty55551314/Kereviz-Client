package kereviz.module.modules;

import kereviz.event.EventTarget;
import kereviz.event.types.EventType;
import kereviz.event.types.Priority;
import kereviz.events.TickEvent;
import kereviz.events.UpdateEvent;
import kereviz.mixin.IAccessorPlayerControllerMP;
import kereviz.module.Module;
import kereviz.property.properties.BooleanProperty;
import kereviz.property.properties.FloatProperty;
import kereviz.property.properties.IntProperty;
import kereviz.util.BlockUtil;
import kereviz.util.PacketUtil;
import kereviz.util.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WaterMLG extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int ROTATION_PRIORITY = 9;
    private static final double[] AIM_OFFSETS = new double[]{0.5D, 0.35D, 0.65D, 0.2D, 0.8D};

    public final IntProperty fallDistance = new IntProperty("fall-distance", 3, 2, 20);
    public final IntProperty scanDepth = new IntProperty("scan-depth", 14, 4, 32);
    public final FloatProperty range = new FloatProperty("range", 5.0F, 3.0F, 6.0F);
    public final IntProperty placeDelay = new IntProperty("place-delay", 8, 0, 20);
    public final IntProperty rotationStep = new IntProperty("rotation-step", 180, 15, 180);
    public final BooleanProperty silentAim = new BooleanProperty("silent-aim", true);
    public final BooleanProperty itemSpoof = new BooleanProperty("item-spoof", true);

    private PlacementTarget pendingTarget;
    private int placeCooldown;

    public WaterMLG() {
        super("WaterMLG", false, false, "Places a water bucket below you while falling.");
    }

    @EventTarget(Priority.HIGHEST)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            this.pendingTarget = null;
            return;
        }

        if (event.getType() == EventType.POST) {
            this.placePendingTarget();
            return;
        }

        if (event.getType() != EventType.PRE) {
            return;
        }

        this.pendingTarget = null;
        if (!this.canAttemptMLG()) {
            return;
        }

        int waterSlot = this.findWaterBucketSlot();
        if (waterSlot == -1) {
            return;
        }

        PlacementTarget target = this.findPlacementTarget(event, waterSlot);
        if (target == null) {
            return;
        }

        if (!this.silentAim.getValue()) {
            mc.thePlayer.rotationYaw = target.yaw;
            mc.thePlayer.rotationPitch = target.pitch;
        }

        event.setRotation(target.yaw, target.pitch, ROTATION_PRIORITY);
        event.setPervRotation(this.silentAim.getValue() ? mc.thePlayer.rotationYaw : target.yaw, ROTATION_PRIORITY);
        this.pendingTarget = target;
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }

        if (this.placeCooldown > 0) {
            this.placeCooldown--;
        }
        if (mc.thePlayer != null && (mc.thePlayer.onGround || mc.thePlayer.isInWater() || mc.thePlayer.isInLava())) {
            this.pendingTarget = null;
            this.placeCooldown = 0;
        }
    }

    private boolean canAttemptMLG() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.playerController == null || mc.getNetHandler() == null) {
            return false;
        }
        if (mc.currentScreen != null || this.placeCooldown > 0) {
            return false;
        }
        if (mc.thePlayer.onGround
                || mc.thePlayer.isInWater()
                || mc.thePlayer.isInLava()
                || mc.thePlayer.isOnLadder()
                || mc.thePlayer.ridingEntity != null) {
            return false;
        }
        if (mc.thePlayer.capabilities.allowFlying || mc.thePlayer.capabilities.isFlying) {
            return false;
        }
        return mc.thePlayer.motionY < -0.08D && mc.thePlayer.fallDistance >= this.fallDistance.getValue();
    }

    private int findWaterBucketSlot() {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        ItemStack currentStack = mc.thePlayer.inventory.getStackInSlot(currentSlot);
        if (this.isWaterBucket(currentStack)) {
            return currentSlot;
        }
        if (!this.itemSpoof.getValue()) {
            return -1;
        }
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (this.isWaterBucket(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isWaterBucket(ItemStack stack) {
        return stack != null && stack.stackSize > 0 && stack.getItem() == Items.water_bucket;
    }

    private PlacementTarget findPlacementTarget(UpdateEvent event, int waterSlot) {
        List<BlockPos> supports = this.collectSupportBlocks();
        PlacementTarget best = null;
        for (BlockPos support : supports) {
            PlacementTarget target = this.getAimTarget(event, support, waterSlot);
            if (target != null && (best == null || target.score < best.score)) {
                best = target;
            }
        }
        return best;
    }

    private List<BlockPos> collectSupportBlocks() {
        List<BlockPos> supports = new ArrayList<BlockPos>();
        Set<BlockPos> visited = new HashSet<BlockPos>();
        double[][] columns = this.getSampleColumns();
        int startY = MathHelper.floor_double(mc.thePlayer.posY) - 1;
        int minY = Math.max(0, startY - this.scanDepth.getValue());

        for (double[] column : columns) {
            int x = MathHelper.floor_double(column[0]);
            int z = MathHelper.floor_double(column[1]);
            for (int y = startY; y >= minY; y--) {
                BlockPos support = new BlockPos(x, y, z);
                if (visited.contains(support)) {
                    continue;
                }
                visited.add(support);
                if (this.isValidSupport(support)) {
                    supports.add(support);
                    break;
                }
            }
        }
        return supports;
    }

    private double[][] getSampleColumns() {
        double halfWidth = Math.max(0.12D, (double) mc.thePlayer.width / 2.0D - 0.03D);
        double predictedX = mc.thePlayer.posX + mc.thePlayer.motionX * 1.5D;
        double predictedZ = mc.thePlayer.posZ + mc.thePlayer.motionZ * 1.5D;
        double currentX = mc.thePlayer.posX;
        double currentZ = mc.thePlayer.posZ;

        return new double[][]{
                {predictedX, predictedZ},
                {predictedX + halfWidth, predictedZ + halfWidth},
                {predictedX + halfWidth, predictedZ - halfWidth},
                {predictedX - halfWidth, predictedZ + halfWidth},
                {predictedX - halfWidth, predictedZ - halfWidth},
                {currentX, currentZ},
                {currentX + halfWidth, currentZ + halfWidth},
                {currentX + halfWidth, currentZ - halfWidth},
                {currentX - halfWidth, currentZ + halfWidth},
                {currentX - halfWidth, currentZ - halfWidth}
        };
    }

    private boolean isValidSupport(BlockPos support) {
        if (support.getY() < 0 || support.getY() > 255) {
            return false;
        }
        if (BlockUtil.isReplaceable(support)) {
            return false;
        }

        Block block = mc.theWorld.getBlockState(support).getBlock();
        Material material = block.getMaterial();
        if (material.isLiquid() || !material.blocksMovement()) {
            return false;
        }
        return this.isWaterPlaceable(support.up());
    }

    private boolean isWaterPlaceable(BlockPos waterPos) {
        if (waterPos.getY() < 0 || waterPos.getY() > 255) {
            return false;
        }
        Block block = mc.theWorld.getBlockState(waterPos).getBlock();
        Material material = block.getMaterial();
        return !material.isLiquid() && BlockUtil.isReplaceable(waterPos);
    }

    private PlacementTarget getAimTarget(UpdateEvent event, BlockPos support, int waterSlot) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        double reach = Math.max(3.0D, Math.min(6.0D, this.range.getValue()));
        double reachSq = reach * reach;
        float serverYaw = event.getYaw();
        float serverPitch = event.getPitch();
        float cameraYaw = mc.thePlayer.rotationYaw;
        float cameraPitch = mc.thePlayer.rotationPitch;

        PlacementTarget best = null;
        for (double xOffset : AIM_OFFSETS) {
            for (double zOffset : AIM_OFFSETS) {
                Vec3 hitVec = new Vec3(
                        (double) support.getX() + xOffset,
                        (double) support.getY() + 1.0D - 0.001D,
                        (double) support.getZ() + zOffset
                );
                double dx = hitVec.xCoord - eyes.xCoord;
                double dy = hitVec.yCoord - eyes.yCoord;
                double dz = hitVec.zCoord - eyes.zCoord;
                if (dx * dx + dy * dy + dz * dz > reachSq) {
                    continue;
                }

                float[] rotations = RotationUtil.getRotationsTo(dx, dy, dz, serverYaw, serverPitch);
                rotations = this.applyRotationStep(rotations, serverYaw, serverPitch);
                rotations = RotationUtil.gcd(rotations, new float[]{serverYaw, serverPitch});

                MovingObjectPosition mop = RotationUtil.rayTrace(rotations[0], rotations[1], reach, 1.0F);
                if (mop == null
                        || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                        || !support.equals(mop.getBlockPos())
                        || mop.sideHit != EnumFacing.UP) {
                    continue;
                }

                float cameraDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - cameraYaw))
                        + Math.abs(rotations[1] - cameraPitch);
                float serverDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - serverYaw))
                        + Math.abs(rotations[1] - serverPitch);
                double horizontal = mc.thePlayer.getDistance(
                        (double) support.getX() + 0.5D,
                        mc.thePlayer.posY,
                        (double) support.getZ() + 0.5D
                );
                float score = (float) (cameraDiff + serverDiff * 0.25F + horizontal * 2.0D);
                if (best == null || score < best.score) {
                    best = new PlacementTarget(support, rotations[0], rotations[1], waterSlot, score);
                }
            }
        }
        return best;
    }

    private float[] applyRotationStep(float[] rotations, float serverYaw, float serverPitch) {
        float maxStep = this.rotationStep.getValue().floatValue();
        float yawDelta = MathHelper.wrapAngleTo180_float(rotations[0] - serverYaw);
        float pitchDelta = rotations[1] - serverPitch;
        float yaw = serverYaw + MathHelper.clamp_float(yawDelta, -maxStep, maxStep);
        float pitch = MathHelper.clamp_float(serverPitch + MathHelper.clamp_float(pitchDelta, -maxStep, maxStep), -90.0F, 90.0F);
        return new float[]{yaw, pitch};
    }

    private void placePendingTarget() {
        if (this.pendingTarget == null || mc.thePlayer == null || mc.theWorld == null) {
            this.pendingTarget = null;
            return;
        }

        PlacementTarget target = this.pendingTarget;
        this.pendingTarget = null;
        if (!this.isValidSupport(target.support)) {
            return;
        }

        int originalSlot = mc.thePlayer.inventory.currentItem;
        boolean switched = originalSlot != target.slot;
        if (switched) {
            if (!this.itemSpoof.getValue()) {
                return;
            }
            mc.thePlayer.inventory.currentItem = target.slot;
            this.syncCurrentItem();
        }

        ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
        if (this.isWaterBucket(stack)) {
            this.syncCurrentItem();
            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(stack.copy()));
            mc.thePlayer.swingItem();
            this.placeCooldown = Math.max(1, this.placeDelay.getValue());
        }

        if (switched) {
            mc.thePlayer.inventory.currentItem = originalSlot;
            this.syncCurrentItem();
        }
    }

    private void syncCurrentItem() {
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
    }

    @Override
    public void onDisabled() {
        this.pendingTarget = null;
        this.placeCooldown = 0;
    }

    @Override
    public String[] getSuffix() {
        return this.silentAim.getValue() ? new String[]{"Silent"} : new String[]{"Visible"};
    }

    private static class PlacementTarget {
        private final BlockPos support;
        private final float yaw;
        private final float pitch;
        private final int slot;
        private final float score;

        private PlacementTarget(BlockPos support, float yaw, float pitch, int slot, float score) {
            this.support = support;
            this.yaw = yaw;
            this.pitch = pitch;
            this.slot = slot;
            this.score = score;
        }
    }
}
