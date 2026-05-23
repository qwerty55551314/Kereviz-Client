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
import kereviz.property.properties.ModeProperty;
import kereviz.util.BlockUtil;
import kereviz.util.PacketUtil;
import kereviz.util.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
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

public class MLG extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int ROTATION_PRIORITY = 9;
    private static final double[] AIM_OFFSETS = new double[]{0.5D, 0.35D, 0.65D, 0.2D, 0.8D};
    private static final EnumFacing[] HORIZONTAL_FACINGS = new EnumFacing[]{
            EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST
    };
    private static final EnumFacing[] BLOCK_PLACE_FACES = new EnumFacing[]{
            EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST
    };

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"WATER", "LADDER"});
    public final IntProperty fallDistance = new IntProperty("fall-distance", 3, 2, 20);
    public final IntProperty scanDepth = new IntProperty("scan-depth", 16, 4, 32);
    public final FloatProperty range = new FloatProperty("range", 5.0F, 3.0F, 6.0F);
    public final IntProperty placeDelay = new IntProperty("place-delay", 8, 0, 20);
    public final IntProperty rotationStep = new IntProperty("rotation-step", 180, 15, 180);
    public final BooleanProperty silentAim = new BooleanProperty("silent-aim", true);
    public final BooleanProperty itemSpoof = new BooleanProperty("item-spoof", true);

    private PlacementTarget pendingTarget;
    private LadderPlan ladderPlan;
    private int placeCooldown;

    public MLG() {
        super("MLG", false, false, "Places water or a block-ladder clutch below you while falling.");
    }

    @EventTarget(Priority.HIGHEST)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            this.resetRuntime();
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

        PlacementTarget target = this.mode.getValue() == 1
                ? this.findLadderTarget(event)
                : this.findWaterTarget(event);
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
            this.resetRuntime();
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

    private PlacementTarget findWaterTarget(UpdateEvent event) {
        int waterSlot = this.findWaterBucketSlot();
        if (waterSlot == -1) {
            return null;
        }

        PlacementTarget best = null;
        for (BlockPos support : this.collectGroundSupports()) {
            PlacementTarget target = this.getWaterAimTarget(event, support, waterSlot);
            if (target != null && (best == null || target.score < best.score)) {
                best = target;
            }
        }
        return best;
    }

    private PlacementTarget findLadderTarget(UpdateEvent event) {
        if (this.ladderPlan != null) {
            PlacementTarget target = this.nextLadderTarget(event, this.ladderPlan);
            if (target != null) {
                return target;
            }
            this.ladderPlan = null;
        }

        int ladderSlot = this.findLadderSlot();
        if (ladderSlot == -1) {
            return null;
        }

        int blockSlot = this.findMLGBlockSlot();
        LadderCandidate best = null;
        for (BlockPos ladderPos : this.collectLadderPositions()) {
            for (EnumFacing ladderFacing : HORIZONTAL_FACINGS) {
                LadderCandidate candidate = this.getLadderCandidate(event, ladderPos, ladderFacing, ladderSlot, blockSlot);
                if (candidate != null && (best == null || candidate.score < best.score)) {
                    best = candidate;
                }
            }
        }

        if (best == null) {
            return null;
        }

        this.ladderPlan = best.plan;
        return best.target;
    }

    private LadderCandidate getLadderCandidate(UpdateEvent event, BlockPos ladderPos, EnumFacing ladderFacing, int ladderSlot, int blockSlot) {
        BlockPos backingPos = ladderPos.offset(ladderFacing.getOpposite());
        if (!this.isLadderPlaceable(ladderPos)) {
            return null;
        }

        boolean backingReady = this.isStableSupport(backingPos);
        if (backingReady) {
            LadderPlan plan = new LadderPlan(ladderPos, backingPos, ladderFacing, -1, ladderSlot, false);
            PlacementTarget ladderTarget = this.getPlacementAimTarget(event, backingPos, ladderFacing, ladderSlot, ActionType.LADDER, plan);
            if (ladderTarget == null) {
                return null;
            }
            return new LadderCandidate(plan, ladderTarget, ladderTarget.score + this.scoreLadderPosition(ladderPos, false));
        }

        if (blockSlot == -1 || !this.isBlockSpacePlaceable(backingPos)) {
            return null;
        }

        PlacementData blockPlacement = this.findBlockPlacement(backingPos);
        if (blockPlacement == null) {
            return null;
        }

        LadderPlan plan = new LadderPlan(ladderPos, backingPos, ladderFacing, blockSlot, ladderSlot, true);
        PlacementTarget blockTarget = this.getPlacementAimTarget(
                event,
                blockPlacement.clickPos,
                blockPlacement.facing,
                blockSlot,
                ActionType.BLOCK,
                plan
        );
        if (blockTarget == null) {
            return null;
        }
        return new LadderCandidate(plan, blockTarget, blockTarget.score + this.scoreLadderPosition(ladderPos, true));
    }

    private PlacementTarget nextLadderTarget(UpdateEvent event, LadderPlan plan) {
        if (this.isLadderAt(plan.ladderPos)) {
            this.placeCooldown = Math.max(1, this.placeDelay.getValue());
            this.ladderPlan = null;
            return null;
        }

        if (!this.isLadderPlaceable(plan.ladderPos)) {
            return null;
        }

        if (plan.needsBlock && !this.isStableSupport(plan.backingPos)) {
            if (!this.isBlockSpacePlaceable(plan.backingPos)) {
                return null;
            }
            PlacementData blockPlacement = this.findBlockPlacement(plan.backingPos);
            return blockPlacement == null
                    ? null
                    : this.getPlacementAimTarget(event, blockPlacement.clickPos, blockPlacement.facing, plan.blockSlot, ActionType.BLOCK, plan);
        }

        return this.getPlacementAimTarget(event, plan.backingPos, plan.ladderFacing, plan.ladderSlot, ActionType.LADDER, plan);
    }

    private double scoreLadderPosition(BlockPos ladderPos, boolean needsBlock) {
        double predictedX = mc.thePlayer.posX + mc.thePlayer.motionX * 1.5D;
        double predictedZ = mc.thePlayer.posZ + mc.thePlayer.motionZ * 1.5D;
        double dx = ((double) ladderPos.getX() + 0.5D) - predictedX;
        double dz = ((double) ladderPos.getZ() + 0.5D) - predictedZ;
        return Math.sqrt(dx * dx + dz * dz) * 6.0D + (needsBlock ? 4.0D : 0.0D);
    }

    private List<BlockPos> collectGroundSupports() {
        List<BlockPos> supports = new ArrayList<BlockPos>();
        Set<BlockPos> visited = new HashSet<BlockPos>();
        int startY = MathHelper.floor_double(mc.thePlayer.posY) - 1;
        int minY = Math.max(0, startY - this.scanDepth.getValue());

        for (double[] column : this.getSampleColumns()) {
            int x = MathHelper.floor_double(column[0]);
            int z = MathHelper.floor_double(column[1]);
            for (int y = startY; y >= minY; y--) {
                BlockPos support = new BlockPos(x, y, z);
                if (visited.contains(support)) {
                    continue;
                }
                visited.add(support);
                if (this.isStableSupport(support) && this.isWaterPlaceable(support.up())) {
                    supports.add(support);
                    break;
                }
            }
        }
        return supports;
    }

    private List<BlockPos> collectLadderPositions() {
        List<BlockPos> ladderPositions = new ArrayList<BlockPos>();
        Set<BlockPos> visited = new HashSet<BlockPos>();
        int startY = MathHelper.floor_double(mc.thePlayer.posY) - 1;
        int minY = Math.max(0, startY - this.scanDepth.getValue());

        for (double[] column : this.getSampleColumns()) {
            int x = MathHelper.floor_double(column[0]);
            int z = MathHelper.floor_double(column[1]);
            for (int y = startY; y >= minY; y--) {
                BlockPos ground = new BlockPos(x, y, z);
                if (this.isStableSupport(ground)) {
                    BlockPos ladderPos = ground.up();
                    if (!visited.contains(ladderPos) && this.isLadderPlaceable(ladderPos)) {
                        visited.add(ladderPos);
                        ladderPositions.add(ladderPos);
                    }
                    break;
                }
            }
        }
        return ladderPositions;
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

    private PlacementData findBlockPlacement(BlockPos targetPos) {
        for (EnumFacing facing : BLOCK_PLACE_FACES) {
            BlockPos clickPos = targetPos.offset(facing.getOpposite());
            if (this.isStableSupport(clickPos) && !BlockUtil.isInteractable(clickPos)) {
                return new PlacementData(clickPos, facing);
            }
        }
        return null;
    }

    private PlacementTarget getWaterAimTarget(UpdateEvent event, BlockPos support, int waterSlot) {
        PlacementTarget best = null;
        for (double xOffset : AIM_OFFSETS) {
            for (double zOffset : AIM_OFFSETS) {
                Vec3 hitVec = new Vec3(
                        (double) support.getX() + xOffset,
                        (double) support.getY() + 1.0D - 0.001D,
                        (double) support.getZ() + zOffset
                );
                PlacementTarget target = this.getAimTargetToVec(event, hitVec, waterSlot, ActionType.WATER, null);
                if (target == null) {
                    continue;
                }

                MovingObjectPosition mop = RotationUtil.rayTrace(target.yaw, target.pitch, this.clampedReach(), 1.0F);
                if (mop == null
                        || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                        || !support.equals(mop.getBlockPos())
                        || mop.sideHit != EnumFacing.UP) {
                    continue;
                }

                target.clickPos = support;
                target.facing = EnumFacing.UP;
                target.hitVec = mop.hitVec;
                if (best == null || target.score < best.score) {
                    best = target;
                }
            }
        }
        return best;
    }

    private PlacementTarget getPlacementAimTarget(UpdateEvent event, BlockPos clickPos, EnumFacing facing, int slot, ActionType action, LadderPlan plan) {
        PlacementTarget best = null;
        for (double u : AIM_OFFSETS) {
            for (double v : AIM_OFFSETS) {
                Vec3 hitVec = this.getFaceHitVec(clickPos, facing, u, v);
                PlacementTarget target = this.getAimTargetToVec(event, hitVec, slot, action, plan);
                if (target == null) {
                    continue;
                }

                MovingObjectPosition mop = RotationUtil.rayTrace(target.yaw, target.pitch, this.clampedReach(), 1.0F);
                if (mop == null
                        || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                        || !clickPos.equals(mop.getBlockPos())
                        || mop.sideHit != facing) {
                    continue;
                }

                target.clickPos = clickPos;
                target.facing = facing;
                target.hitVec = mop.hitVec;
                if (best == null || target.score < best.score) {
                    best = target;
                }
            }
        }
        return best;
    }

    private PlacementTarget getAimTargetToVec(UpdateEvent event, Vec3 hitVec, int slot, ActionType action, LadderPlan plan) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        double reach = this.clampedReach();
        double dx = hitVec.xCoord - eyes.xCoord;
        double dy = hitVec.yCoord - eyes.yCoord;
        double dz = hitVec.zCoord - eyes.zCoord;
        if (dx * dx + dy * dy + dz * dz > reach * reach) {
            return null;
        }

        float serverYaw = event.getYaw();
        float serverPitch = event.getPitch();
        float[] rotations = RotationUtil.getRotationsTo(dx, dy, dz, serverYaw, serverPitch);
        rotations = this.applyRotationStep(rotations, serverYaw, serverPitch);
        rotations = RotationUtil.gcd(rotations, new float[]{serverYaw, serverPitch});

        float cameraDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw))
                + Math.abs(rotations[1] - mc.thePlayer.rotationPitch);
        float serverDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - serverYaw))
                + Math.abs(rotations[1] - serverPitch);
        float score = cameraDiff + serverDiff * 0.25F;
        return new PlacementTarget(action, rotations[0], rotations[1], slot, score, plan);
    }

    private Vec3 getFaceHitVec(BlockPos blockPos, EnumFacing facing, double u, double v) {
        double x = (double) blockPos.getX() + 0.5D;
        double y = (double) blockPos.getY() + 0.5D;
        double z = (double) blockPos.getZ() + 0.5D;
        double inset = 0.001D;

        switch (facing) {
            case DOWN:
                y = (double) blockPos.getY() + inset;
                x = (double) blockPos.getX() + u;
                z = (double) blockPos.getZ() + v;
                break;
            case UP:
                y = (double) blockPos.getY() + 1.0D - inset;
                x = (double) blockPos.getX() + u;
                z = (double) blockPos.getZ() + v;
                break;
            case NORTH:
                z = (double) blockPos.getZ() + inset;
                x = (double) blockPos.getX() + u;
                y = (double) blockPos.getY() + v;
                break;
            case SOUTH:
                z = (double) blockPos.getZ() + 1.0D - inset;
                x = (double) blockPos.getX() + u;
                y = (double) blockPos.getY() + v;
                break;
            case WEST:
                x = (double) blockPos.getX() + inset;
                z = (double) blockPos.getZ() + u;
                y = (double) blockPos.getY() + v;
                break;
            case EAST:
                x = (double) blockPos.getX() + 1.0D - inset;
                z = (double) blockPos.getZ() + u;
                y = (double) blockPos.getY() + v;
                break;
        }
        return new Vec3(x, y, z);
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

        boolean placed;
        if (target.action == ActionType.WATER) {
            placed = this.placeWater(target);
        } else {
            placed = this.placeBlockLike(target);
        }

        if (target.action == ActionType.BLOCK && target.plan != null) {
            if (placed || this.isStableSupport(target.plan.backingPos)) {
                target.plan.blockPlaced = true;
                PlacementTarget ladderTarget = this.getImmediateLadderTarget(target.plan);
                if (ladderTarget != null && this.placeBlockLike(ladderTarget)) {
                    this.placeCooldown = Math.max(1, this.placeDelay.getValue());
                    this.ladderPlan = null;
                }
            }
            return;
        }

        if (target.action == ActionType.LADDER && target.plan != null) {
            if (placed || this.isLadderAt(target.plan.ladderPos)) {
                this.placeCooldown = Math.max(1, this.placeDelay.getValue());
                this.ladderPlan = null;
            }
        } else if (placed) {
            this.placeCooldown = Math.max(1, this.placeDelay.getValue());
        }
    }

    private boolean placeWater(PlacementTarget target) {
        if (target.clickPos == null || !this.isStableSupport(target.clickPos) || !this.isWaterPlaceable(target.clickPos.up())) {
            return false;
        }

        return this.withSlot(target.slot, new SlotAction() {
            @Override
            public boolean run(ItemStack stack) {
                if (!isWaterBucket(stack)) {
                    return false;
                }
                syncCurrentItem();
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(stack.copy()));
                mc.thePlayer.swingItem();
                return true;
            }
        });
    }

    private PlacementTarget getImmediateLadderTarget(LadderPlan plan) {
        if (plan == null || !this.isStableSupport(plan.backingPos) || !this.isLadderPlaceable(plan.ladderPos)) {
            return null;
        }

        Vec3 hitVec = this.getFaceHitVec(plan.backingPos, plan.ladderFacing, 0.5D, 0.5D);
        if (mc.thePlayer.getPositionEyes(1.0F).distanceTo(hitVec) > this.clampedReach()) {
            return null;
        }

        PlacementTarget target = new PlacementTarget(
                ActionType.LADDER,
                mc.thePlayer.rotationYaw,
                mc.thePlayer.rotationPitch,
                plan.ladderSlot,
                0.0F,
                plan
        );
        target.clickPos = plan.backingPos;
        target.facing = plan.ladderFacing;
        target.hitVec = hitVec;
        return target;
    }

    private boolean placeBlockLike(final PlacementTarget target) {
        if (target.clickPos == null || target.facing == null || target.hitVec == null) {
            return false;
        }
        if (!this.isStableSupport(target.clickPos)) {
            return false;
        }

        return this.withSlot(target.slot, new SlotAction() {
            @Override
            public boolean run(ItemStack stack) {
                if (target.action == ActionType.BLOCK && !isMLGBlock(stack)) {
                    return false;
                }
                if (target.action == ActionType.LADDER && !isLadder(stack)) {
                    return false;
                }
                boolean placed = mc.playerController.onPlayerRightClick(
                        mc.thePlayer,
                        mc.theWorld,
                        stack,
                        target.clickPos,
                        target.facing,
                        target.hitVec
                );
                if (placed) {
                    mc.thePlayer.swingItem();
                }
                return placed;
            }
        });
    }

    private boolean withSlot(int slot, SlotAction action) {
        if (slot < 0 || slot > 8) {
            return false;
        }

        int originalSlot = mc.thePlayer.inventory.currentItem;
        boolean switched = originalSlot != slot;
        if (switched) {
            if (!this.itemSpoof.getValue()) {
                return false;
            }
            mc.thePlayer.inventory.currentItem = slot;
            this.syncCurrentItem();
        }

        boolean result = false;
        try {
            result = action.run(mc.thePlayer.inventory.getCurrentItem());
        } finally {
            if (switched) {
                mc.thePlayer.inventory.currentItem = originalSlot;
                this.syncCurrentItem();
            }
        }
        return result;
    }

    private void syncCurrentItem() {
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
    }

    private int findWaterBucketSlot() {
        return this.findSlot(new SlotMatcher() {
            @Override
            public boolean matches(ItemStack stack) {
                return isWaterBucket(stack);
            }
        });
    }

    private int findLadderSlot() {
        return this.findSlot(new SlotMatcher() {
            @Override
            public boolean matches(ItemStack stack) {
                return isLadder(stack);
            }
        });
    }

    private int findMLGBlockSlot() {
        return this.findSlot(new SlotMatcher() {
            @Override
            public boolean matches(ItemStack stack) {
                return isMLGBlock(stack);
            }
        });
    }

    private int findSlot(SlotMatcher matcher) {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        ItemStack currentStack = mc.thePlayer.inventory.getStackInSlot(currentSlot);
        if (matcher.matches(currentStack)) {
            return currentSlot;
        }
        if (!this.itemSpoof.getValue()) {
            return -1;
        }
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (matcher.matches(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isWaterBucket(ItemStack stack) {
        return stack != null && stack.stackSize > 0 && stack.getItem() == Items.water_bucket;
    }

    private boolean isLadder(ItemStack stack) {
        return stack != null
                && stack.stackSize > 0
                && stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).getBlock() == Blocks.ladder;
    }

    private boolean isMLGBlock(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }

        Block block = ((ItemBlock) stack.getItem()).getBlock();
        return block != Blocks.ladder && !BlockUtil.isInteractable(block) && BlockUtil.isSolid(block);
    }

    private boolean isStableSupport(BlockPos pos) {
        if (pos.getY() < 0 || pos.getY() > 255 || BlockUtil.isReplaceable(pos)) {
            return false;
        }

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        Material material = block.getMaterial();
        return !material.isLiquid() && material.blocksMovement() && BlockUtil.isSolid(block);
    }

    private boolean isWaterPlaceable(BlockPos waterPos) {
        if (waterPos.getY() < 0 || waterPos.getY() > 255) {
            return false;
        }
        Block block = mc.theWorld.getBlockState(waterPos).getBlock();
        Material material = block.getMaterial();
        return !material.isLiquid() && BlockUtil.isReplaceable(waterPos);
    }

    private boolean isLadderPlaceable(BlockPos ladderPos) {
        if (ladderPos.getY() < 0 || ladderPos.getY() > 255) {
            return false;
        }
        Block block = mc.theWorld.getBlockState(ladderPos).getBlock();
        Material material = block.getMaterial();
        return !material.isLiquid() && BlockUtil.isReplaceable(ladderPos);
    }

    private boolean isBlockSpacePlaceable(BlockPos blockPos) {
        if (blockPos.getY() < 0 || blockPos.getY() > 255) {
            return false;
        }
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        Material material = block.getMaterial();
        return !material.isLiquid() && BlockUtil.isReplaceable(blockPos);
    }

    private boolean isLadderAt(BlockPos pos) {
        return pos.getY() >= 0 && pos.getY() <= 255 && mc.theWorld.getBlockState(pos).getBlock() == Blocks.ladder;
    }

    private double clampedReach() {
        return Math.max(3.0D, Math.min(6.0D, this.range.getValue()));
    }

    private void resetRuntime() {
        this.pendingTarget = null;
        this.ladderPlan = null;
        this.placeCooldown = 0;
    }

    @Override
    public void onDisabled() {
        this.resetRuntime();
    }

    @Override
    public String[] getSuffix() {
        String type = this.mode.getModeString();
        return this.silentAim.getValue() ? new String[]{type, "Silent"} : new String[]{type};
    }

    private enum ActionType {
        WATER,
        BLOCK,
        LADDER
    }

    private interface SlotAction {
        boolean run(ItemStack stack);
    }

    private interface SlotMatcher {
        boolean matches(ItemStack stack);
    }

    private static class PlacementData {
        private final BlockPos clickPos;
        private final EnumFacing facing;

        private PlacementData(BlockPos clickPos, EnumFacing facing) {
            this.clickPos = clickPos;
            this.facing = facing;
        }
    }

    private static class LadderPlan {
        private final BlockPos ladderPos;
        private final BlockPos backingPos;
        private final EnumFacing ladderFacing;
        private final int blockSlot;
        private final int ladderSlot;
        private final boolean needsBlock;
        private boolean blockPlaced;

        private LadderPlan(BlockPos ladderPos, BlockPos backingPos, EnumFacing ladderFacing, int blockSlot, int ladderSlot, boolean needsBlock) {
            this.ladderPos = ladderPos;
            this.backingPos = backingPos;
            this.ladderFacing = ladderFacing;
            this.blockSlot = blockSlot;
            this.ladderSlot = ladderSlot;
            this.needsBlock = needsBlock;
        }
    }

    private static class LadderCandidate {
        private final LadderPlan plan;
        private final PlacementTarget target;
        private final double score;

        private LadderCandidate(LadderPlan plan, PlacementTarget target, double score) {
            this.plan = plan;
            this.target = target;
            this.score = score;
        }
    }

    private static class PlacementTarget {
        private final ActionType action;
        private final float yaw;
        private final float pitch;
        private final int slot;
        private final float score;
        private final LadderPlan plan;
        private BlockPos clickPos;
        private EnumFacing facing;
        private Vec3 hitVec;

        private PlacementTarget(ActionType action, float yaw, float pitch, int slot, float score, LadderPlan plan) {
            this.action = action;
            this.yaw = yaw;
            this.pitch = pitch;
            this.slot = slot;
            this.score = score;
            this.plan = plan;
        }
    }
}
