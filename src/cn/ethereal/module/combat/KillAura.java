package cn.ethereal.module.combat;

import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.TickEvent;
import cn.ethereal.event.events.UpdateEvent;
import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.module.client.Rotations;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.ModeValue;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.util.helper.NullPointHelper;
import cn.ethereal.util.rotation.RotationUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ThreadLocalRandom;

public class KillAura extends Module {

    public static KillAura INSTANCE;

    // ==================== 状态 ====================
    private EntityLivingBase target = null;
    private int attackCooldownTicks = 0;
    private boolean blocking = false;
    private boolean attacking;

    // ==================== 属性 ====================
    private final ModeValue autoBlockMode;
    private final ModeValue rotationMode;

    private final NumberValue attackRange;
    private final NumberValue swingRange;
    private final NumberValue minCPS;
    private final NumberValue maxCPS;
    private final NumberValue angleStep;

    private final BooleanValue throughWalls;
    private final BooleanValue requirePress;

    public KillAura() {
        super("KillAura", Keyboard.KEY_R, Category.COMBAT, false, true);
        INSTANCE = this;

        autoBlockMode = addModeValue("AutoBlock", new String[]{"None", "Vanilla", "Legit"}, "Vanilla");
        rotationMode  = addModeValue("Rotations", new String[]{"None", "Normal", "Packet"}, "Packet");

        attackRange = addNumberValue("AttackRange", 3.0, 1.0, 6.0, 0.1);
        swingRange  = addNumberValue("SwingRange",  3.5, 1.0, 6.0, 0.1);
        minCPS      = addNumberValue("MinCPS",     10.0, 1.0, 20.0, 1.0);
        maxCPS      = addNumberValue("MaxCPS",     14.0, 1.0, 20.0, 1.0);
        angleStep   = addNumberValue("AngleStep",   90.0, 10.0, 180.0, 5.0);

        throughWalls = addBooleanValue("ThroughWalls", false);
        requirePress = addBooleanValue("RequirePress", false);
    }

    // ==================== 生命周期 ====================

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        attackCooldownTicks = 0;
        blocking = false;
        attacking = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopBlocking();
        attacking = false;
        target = null;
        Rotations.clearTarget();
    }

    // ==================== 主逻辑 ====================

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;
        if (!NullPointHelper.isPlayerInWorld()) return;
        if (event.getPhase() != UpdateEvent.Phase.PRE) return;

        attacking = false;

        if (attackCooldownTicks > 0) attackCooldownTicks--;

        // 从 Target 模块取目标
        target = Target.INSTANCE.getTarget();

        if (target == null) {
            stopBlocking();
            Rotations.clearTarget();
            return;
        }

        // 超出自己的 swingRange → 交给投掷物光环处理
        if (Target.distanceTo(target) > swingRange.getValue()) {
            stopBlocking();
            Rotations.clearTarget();
            return;
        }

        handleRotations();
        tryAttack();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;
        if (event.getPhase() != TickEvent.Phase.POST) return;

        if (blocking && !mc.thePlayer.isUsingItem()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held != null && held.getItem() instanceof ItemSword) {
                mc.thePlayer.setItemInUse(held, held.getMaxItemUseDuration());
            }
        }
    }

    // ==================== 旋转 ====================

    private void handleRotations() {
        if (target == null) {
            Rotations.clearTarget();
            return;
        }
        String mode = rotationMode.getValue();
        if (mode.equals("None")) return;

        float[] rotations = RotationUtil.getRotations(target);

        if (mode.equals("Normal")) {
            RotationUtil.smoothRotate(rotations[0], rotations[1], angleStep.getValue().floatValue());
        } else if (mode.equals("Packet")) {
            RotationUtil.silentRotate(rotations[0], rotations[1]);
            Rotations.setTarget(rotations[0], rotations[1]);
        }
    }

    // ==================== 攻击 ====================

    private void tryAttack() {
        if (target == null) {
            Rotations.clearTarget();
            return;
        }

        if (Target.distanceTo(target) > attackRange.getValue()) {
            stopBlocking();
            return;
        }

        if (!throughWalls.getValue() && !mc.thePlayer.canEntityBeSeen(target)) {
            stopBlocking();
            return;
        }

        if (requirePress.getValue() && !mc.gameSettings.keyBindAttack.isKeyDown()) {
            stopBlocking();
            return;
        }

        boolean wantBlock = shouldAutoBlock();

        if (attackCooldownTicks > 0) {
            if (wantBlock) startBlocking();
            return;
        }

        if (blocking) stopBlocking();

        if (rotationMode.getValue().equals("None")) {
            performAttack();
        } else {
            doAttackWithRotation();
        }

        if (wantBlock && Target.INSTANCE.isValidTarget(target)) {
            startBlocking();
        }
    }

    private void doAttackWithRotation() {
        if (target == null) {
            performAttack();
            return;
        }

        float[] rotations = RotationUtil.getRotations(target);

        float oldYaw = mc.thePlayer.rotationYaw;
        float oldPitch = mc.thePlayer.rotationPitch;
        float oldPrevYaw = mc.thePlayer.prevRotationYaw;
        float oldPrevPitch = mc.thePlayer.prevRotationPitch;

        try {
            mc.thePlayer.rotationYaw = rotations[0];
            mc.thePlayer.rotationPitch = rotations[1];
            mc.thePlayer.prevRotationYaw = rotations[0];
            mc.thePlayer.prevRotationPitch = rotations[1];
            performAttack();
        } finally {
            mc.thePlayer.rotationYaw = oldYaw;
            mc.thePlayer.rotationPitch = oldPitch;
            mc.thePlayer.prevRotationYaw = oldPrevYaw;
            mc.thePlayer.prevRotationPitch = oldPrevPitch;
        }
    }

    private void performAttack() {
        mc.thePlayer.swingItem();
        mc.playerController.attackEntity(mc.thePlayer, target);

        attacking = true;

        int cps = randomInt(minCPS.getValue().intValue(), maxCPS.getValue().intValue());
        attackCooldownTicks = Math.max(1, 20 / cps);
    }

    private int randomInt(int min, int max) {
        if (max <= min) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    // ==================== 自动格挡 ====================

    private boolean shouldAutoBlock() {
        if (autoBlockMode.getValue().equals("None")) return false;
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && held.getItem() instanceof ItemSword;
    }

    private void startBlocking() {
        if (blocking) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemSword)) return;

        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(held));
        mc.thePlayer.setItemInUse(held, held.getMaxItemUseDuration());
        blocking = true;
    }

    private void stopBlocking() {
        if (!blocking) return;

        mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN,
                EnumFacing.DOWN
        ));
        mc.thePlayer.stopUsingItem();
        blocking = false;
    }

    // ==================== 对外接口 ====================

    public EntityLivingBase getTarget() {
        return target;
    }

    public boolean isBlockingState() {
        return blocking;
    }

    public float getRange() {
        return attackRange.getValue().floatValue();
    }

    public float getSwingRange() {
        return swingRange.getValue().floatValue();
    }

    public boolean isAttacking() {
        return attacking;
    }
}