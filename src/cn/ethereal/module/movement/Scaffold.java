package cn.ethereal.module.movement;

import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.Render2DEvent;
import cn.ethereal.event.events.UpdateEvent;
import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.module.combat.KillAura;
import cn.ethereal.module.player.ThrowableAura;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.util.helper.NullPointHelper;
import cn.ethereal.util.rotation.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Scaffold extends Module {

    public static Scaffold INSTANCE;

    private final BooleanValue debug;
    private final BooleanValue silent;
    private final BooleanValue autoSwitchFromInv;
    private final BooleanValue disableConflicts;   // ★ 新增开关
    private final NumberValue delay;

    private int prevSlot = -1;
    private int placeTimer = 0;
    private int baseY = -1;
    private int currentTick = 0;
    private final Map<BlockPos, Integer> recentlyPlaced = new HashMap<>();
    private final Random random = new Random();

    // ★ 记录开启 Scaffold 之前哪些模块是开着的
    private boolean wasKillAuraEnabled = false;
    private boolean wasThrowableAuraEnabled = false;

    private static final List<Block> INVALID_BLOCKS = Arrays.asList(
            Blocks.enchanting_table, Blocks.chest, Blocks.end_portal_frame,
            Blocks.trapped_chest, Blocks.anvil, Blocks.sand, Blocks.web,
            Blocks.torch, Blocks.crafting_table, Blocks.furnace,
            Blocks.waterlily, Blocks.dispenser, Blocks.noteblock,
            Blocks.dropper, Blocks.tnt, Blocks.standing_banner,
            Blocks.wall_banner, Blocks.redstone_torch
    );

    public Scaffold() {
        super("Scaffold", Keyboard.KEY_NONE, Category.MOVEMENT, false, true);
        INSTANCE = this;
        debug             = addBooleanValue("Debug", false);
        silent            = addBooleanValue("Silent", false);
        autoSwitchFromInv = addBooleanValue("AutoSwitchFromInv", true);
        disableConflicts  = addBooleanValue("DisableConflicts", true);
        delay             = addNumberValue("Delay", 0.0, 0.0, 5.0, 1.0);
    }

    // ==================== 生命周期 ====================

    @Override
    public void onEnable() {
        super.onEnable();
        prevSlot = -1;
        placeTimer = 0;
        baseY = -1;
        currentTick = 0;
        recentlyPlaced.clear();

        // ★ 关闭冲突模块并记录状态
        wasKillAuraEnabled = false;
        wasThrowableAuraEnabled = false;

        if (disableConflicts.getValue()) {
            if (KillAura.INSTANCE != null && KillAura.INSTANCE.isEnabled()) {
                wasKillAuraEnabled = true;
                KillAura.INSTANCE.setEnabled(false);
            }
            if (ThrowableAura.INSTANCE != null && ThrowableAura.INSTANCE.isEnabled()) {
                wasThrowableAuraEnabled = true;
                ThrowableAura.INSTANCE.setEnabled(false);
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (prevSlot != -1 && mc.thePlayer != null
                && prevSlot != mc.thePlayer.inventory.currentItem) {
            switchSlot(prevSlot);
        }
        prevSlot = -1;
        recentlyPlaced.clear();

        // ★ 恢复之前关闭的模块
        if (wasKillAuraEnabled && KillAura.INSTANCE != null) {
            KillAura.INSTANCE.setEnabled(true);
            wasKillAuraEnabled = false;
        }
        if (wasThrowableAuraEnabled && ThrowableAura.INSTANCE != null) {
            ThrowableAura.INSTANCE.setEnabled(true);
            wasThrowableAuraEnabled = false;
        }
    }

    // ==================== 主逻辑 ====================

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;
        if (event.getPhase() != UpdateEvent.Phase.PRE) return;
        if (!NullPointHelper.isPlayerInWorld()) return;

        currentTick++;

        // baseY 缓存：只在落地时更新
        if (mc.thePlayer.onGround) {
            baseY = (int) Math.floor(mc.thePlayer.posY) - 1;
        }
        if (baseY == -1) {
            baseY = (int) Math.floor(mc.thePlayer.posY) - 1;
        }

        if (placeTimer > 0) {
            placeTimer--;
            return;
        }

        BlockPos below = new BlockPos(
                (int) Math.floor(mc.thePlayer.posX),
                baseY,
                (int) Math.floor(mc.thePlayer.posZ)
        );

        if (!isReplaceable(below)) return;

        BlockData data = getBlockData(below);
        if (data == null) {
            if (debug.getValue()) System.out.println("[Scaffold] 找不到邻居方块 @ " + below);
            return;
        }

        int slot = findBlockSlot();
        if (slot == -1) return;

        if (mc.thePlayer.inventory.currentItem != slot) {
            if (prevSlot == -1) prevSlot = mc.thePlayer.inventory.currentItem;
            switchSlot(slot);
            placeTimer = Math.max(1, delay.getValue().intValue());
            return;
        }

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) return;

        float[] rot = getRotationsToBlock(data.pos, data.facing);
        applyRotation(rot[0], rot[1]);

        Vec3 hit = getVec3(data.pos, data.facing);
        boolean ok = mc.playerController.onPlayerRightClick(
                mc.thePlayer, mc.theWorld, held,
                data.pos, data.facing, hit
        );

        if (ok) {
            mc.thePlayer.swingItem();
            placeTimer = delay.getValue().intValue();
            BlockPos placedPos = data.pos.offset(data.facing);
            recentlyPlaced.put(placedPos, currentTick);
            if (debug.getValue()) System.out.println("[Scaffold] 成功 @ " + placedPos);
        } else {
            placeTimer = 2;
            if (debug.getValue()) System.out.println("[Scaffold] 失败 @ " + data.pos);
        }
    }

    @EventListener
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled()) return;
        if (!NullPointHelper.isPlayerInWorld()) return;

        String text = "Blocks: " + getBlockCount();
        ScaledResolution scaledResolution = new ScaledResolution(mc);

        int x = scaledResolution.getScaledWidth() / 2 + 10;
        int y = scaledResolution.getScaledHeight() / 2;
        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFFFFF);
    }

    private void applyRotation(float yaw, float pitch) {
        if (silent.getValue()) {
            RotationUtil.silentRotate(yaw, pitch);
        } else {
            mc.thePlayer.rotationYaw = yaw;
            mc.thePlayer.rotationPitch = pitch;
            mc.thePlayer.prevRotationYaw = yaw;
            mc.thePlayer.prevRotationPitch = pitch;
        }
    }

    // ==================== 找邻居 ====================

    private BlockData getBlockData(BlockPos placePos) {
        BlockData direct = getPos(placePos);
        if (direct != null) return direct;

        BlockPos playerPos = new BlockPos(
                (int) Math.floor(mc.thePlayer.posX),
                (int) Math.floor(mc.thePlayer.posY),
                (int) Math.floor(mc.thePlayer.posZ)
        );

        BlockPos[] neighbors = {
                placePos.add(1, 0, 0), placePos.add(-1, 0, 0),
                placePos.add(0, 0, 1), placePos.add(0, 0, -1),
                placePos.add(1, 0, 1), placePos.add(1, 0, -1),
                placePos.add(-1, 0, 1), placePos.add(-1, 0, -1),
        };

        List<BlockData> candidates = new ArrayList<>();
        for (BlockPos n : neighbors) {
            if (!isReplaceable(n)) continue;
            if (n.equals(playerPos)) continue;
            BlockData d = getPos(n);
            if (d != null) candidates.add(d);
        }

        if (candidates.isEmpty()) return null;

        candidates.sort(Comparator.comparingDouble(d ->
                mc.thePlayer.getDistanceSq(d.pos.getX() + 0.5, d.pos.getY() + 0.5, d.pos.getZ() + 0.5)));
        return candidates.get(0);
    }

    private BlockData getPos(BlockPos pos) {
        if (isPosSolid(pos.add(0, -1, 0))) return new BlockData(pos.add(0, -1, 0), EnumFacing.UP);
        if (isPosSolid(pos.add(-1, 0, 0))) return new BlockData(pos.add(-1, 0, 0), EnumFacing.EAST);
        if (isPosSolid(pos.add(1, 0, 0)))  return new BlockData(pos.add(1, 0, 0), EnumFacing.WEST);
        if (isPosSolid(pos.add(0, 0, 1)))  return new BlockData(pos.add(0, 0, 1), EnumFacing.NORTH);
        if (isPosSolid(pos.add(0, 0, -1))) return new BlockData(pos.add(0, 0, -1), EnumFacing.SOUTH);
        return null;
    }

    // ==================== 点击位置 ====================

    private Vec3 getVec3(BlockPos pos, EnumFacing face) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        double r1 = -0.3 + random.nextDouble() * 0.6;
        double r2 = -0.3 + random.nextDouble() * 0.6;

        switch (face) {
            case UP:    y = pos.getY() + 1.0; x += r1; z += r2; break;
            case DOWN:  y = pos.getY() + 0.0; x += r1; z += r2; break;
            case EAST:  x = pos.getX() + 1.0; y += r1; z += r2; break;
            case WEST:  x = pos.getX() + 0.0; y += r1; z += r2; break;
            case SOUTH: z = pos.getZ() + 1.0; x += r1; y += r2; break;
            case NORTH: z = pos.getZ() + 0.0; x += r1; y += r2; break;
        }

        return new Vec3(x, y, z);
    }

    // ==================== 工具 ====================

    private boolean isReplaceable(BlockPos pos) {
        if (isInRecent(pos)) return false;
        if (mc.theWorld == null) return false;
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block.getMaterial().isReplaceable();
    }

    private boolean isPosSolid(BlockPos pos) {
        if (isInRecent(pos)) return true;
        if (mc.theWorld == null) return false;
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block == Blocks.air) return false;
        if (block == Blocks.water || block == Blocks.flowing_water) return false;
        if (block == Blocks.lava || block == Blocks.flowing_lava) return false;
        if (block == Blocks.fire) return false;
        if (block == Blocks.skull) return false;
        return !INVALID_BLOCKS.contains(block);
    }

    private boolean isInRecent(BlockPos pos) {
        Integer t = recentlyPlaced.get(pos);
        if (t == null) return false;
        if (currentTick - t > 10) {
            recentlyPlaced.remove(pos);
            return false;
        }
        return true;
    }

    private int findBlockSlot() {
        if (isValidBlock(mc.thePlayer.getHeldItem())) {
            return mc.thePlayer.inventory.currentItem;
        }
        for (int i = 0; i < 9; i++) {
            if (isValidBlock(mc.thePlayer.inventory.getStackInSlot(i))) return i;
        }
        if (autoSwitchFromInv.getValue()) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (!isValidBlock(stack)) continue;
                int target = findEmptyHotbarSlot();
                if (target == -1) target = mc.thePlayer.inventory.currentItem;
                mc.playerController.windowClick(
                        mc.thePlayer.inventoryContainer.windowId,
                        i, target, 2, mc.thePlayer
                );
                return target;
            }
        }
        return -1;
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) return i;
        }
        return -1;
    }

    private boolean isValidBlock(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return false;
        if (!(stack.getItem() instanceof ItemBlock)) return false;
        return !INVALID_BLOCKS.contains(((ItemBlock) stack.getItem()).getBlock());
    }

    private void switchSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.thePlayer.inventory.currentItem = slot;
        mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(slot));
    }

    private float[] getRotationsToBlock(BlockPos pos, EnumFacing side) {
        double cx = pos.getX() + 0.5 + side.getFrontOffsetX() * 0.5;
        double cy = pos.getY() + 0.5 + side.getFrontOffsetY() * 0.5;
        double cz = pos.getZ() + 0.5 + side.getFrontOffsetZ() * 0.5;

        double dx = cx - mc.thePlayer.posX;
        double dy = cy - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = cz - mc.thePlayer.posZ;

        double horizontal = MathHelper.sqrt_double(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-Math.atan2(dy, horizontal) * 180.0 / Math.PI);

        return new float[]{RotationUtil.clampYaw(yaw), RotationUtil.clampPitch(pitch)};
    }

    public int getBlockCount() {
        int blockCount = 0;
        for (int i = 9; i < 45; ++i) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
                ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (isValidBlock(is)) {
                    blockCount += is.stackSize;
                }
            }
        }
        return blockCount;
    }

    private static class BlockData {
        final BlockPos pos;
        final EnumFacing facing;

        BlockData(BlockPos pos, EnumFacing facing) {
            this.pos = pos;
            this.facing = facing;
        }
    }
}