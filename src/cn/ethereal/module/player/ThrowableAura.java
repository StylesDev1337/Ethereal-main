package cn.ethereal.module.player;

import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.UpdateEvent;
import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.module.client.Rotations;
import cn.ethereal.module.combat.KillAura;
import cn.ethereal.module.combat.Target;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.util.helper.NullPointHelper;
import cn.ethereal.util.rotation.RotationUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;

public class ThrowableAura extends Module {
    public static ThrowableAura INSTANCE;

    private final NumberValue range;
    private final BooleanValue debug;
    private final BooleanValue autoSwitch;

    private EntityLivingBase target = null;
    private int throwCooldown = 0;
    private static final int THROW_INTERVAL_TICKS = 6;
    private static final double PROJECTILE_SPEED_TICKS = 1.5;

    private int lastThrowableSlot = -1;

    public ThrowableAura() {
        super("ThrowableAura", Keyboard.KEY_NONE, Category.PLAYER, false, true);
        range = addNumberValue("Range", 15.0, 1.0, 30.0, 1.0);
        debug = addBooleanValue("Debug", false);
        autoSwitch = addBooleanValue("AutoSwitch", true);
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;
        if (event.getPhase() != UpdateEvent.Phase.PRE) return;
        if (!NullPointHelper.isPlayerInWorld()) return;

        if (throwCooldown > 0) throwCooldown--;

        // 从 Target 模块取目标
        target = Target.INSTANCE.getTarget();

        if (target == null) {
            Rotations.clearTarget();
            return;
        }

        float distance = (float) Target.distanceTo(target);
        if (distance <= getKillAuraRange()) return;   // 近距离交给 KillAura

        if (distance > range.getValue()) {            // 超出投掷范围
            Rotations.clearTarget();
            return;
        }

        Vec3 predictedPos = getPredictedPosition(target);
        if (isBlocked(predictedPos)) return;

        int throwableSlot = findThrowableSlot();
        if (throwableSlot == -1) return;

        float[] rotations = RotationUtil.getRotations(predictedPos);

        RotationUtil.silentRotate(rotations[0], rotations[1]);
        Rotations.setTarget(rotations[0], rotations[1]);

        if (throwCooldown <= 0) {
            if (throwItem(throwableSlot, rotations[0], rotations[1])) {
                throwCooldown = THROW_INTERVAL_TICKS;
                if (debug.getValue()) debug("Threw at " + target.getName());
            }
        }
    }

    /**
     * 切换 → 投掷 → 切回，全部在同一 tick 内完成（本地视角无感）
     */
    private boolean throwItem(int throwableSlot, float yaw, float pitch) {
        int prevSlot = mc.thePlayer.inventory.currentItem;
        boolean needSwitch = autoSwitch.getValue() && prevSlot != throwableSlot;

        float oldYaw = mc.thePlayer.rotationYaw;
        float oldPitch = mc.thePlayer.rotationPitch;

        try {
            if (needSwitch) {
                mc.thePlayer.inventory.currentItem = throwableSlot;
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(throwableSlot));
            }

            mc.thePlayer.rotationYaw = yaw;
            mc.thePlayer.rotationPitch = pitch;

            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null) return false;

            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, held);
            return true;
        } finally {
            mc.thePlayer.rotationYaw = oldYaw;
            mc.thePlayer.rotationPitch = oldPitch;

            if (needSwitch) {
                mc.thePlayer.inventory.currentItem = prevSlot;
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(prevSlot));
            }
        }
    }

    // ==================== 投掷物槽位 ====================

    private int findThrowableSlot() {
        if (lastThrowableSlot >= 0 && lastThrowableSlot < 9) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(lastThrowableSlot);
            if (s != null && isThrowableItem(s)) return lastThrowableSlot;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (s != null && s.getItem() == Items.egg) {
                lastThrowableSlot = i;
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (s != null && s.getItem() == Items.snowball) {
                lastThrowableSlot = i;
                return i;
            }
        }
        return -1;
    }

    private boolean isThrowableItem(ItemStack s) {
        return s.getItem() == Items.egg || s.getItem() == Items.snowball;
    }

    // ==================== 预判 ====================

    private Vec3 getPredictedPosition(EntityLivingBase entity) {
        double eyeX = mc.thePlayer.posX;
        double eyeY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double eyeZ = mc.thePlayer.posZ;

        double aimX = entity.posX;
        double aimY = entity.posY + 0.5;
        double aimZ = entity.posZ;

        double motionX = entity.posX - entity.prevPosX;
        double motionY = entity.posY - entity.prevPosY;
        double motionZ = entity.posZ - entity.prevPosZ;

        for (int iter = 0; iter < 3; iter++) {
            double dx = aimX - eyeX;
            double dy = aimY - eyeY;
            double dz = aimZ - eyeZ;
            double totalDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double flightTicks = totalDist / PROJECTILE_SPEED_TICKS;
            double gravityDrop = 0.5 * 0.03 * flightTicks * flightTicks * 0.85;

            aimX = entity.posX + motionX * flightTicks;
            aimY = entity.posY + 0.5 + motionY * flightTicks + gravityDrop;
            aimZ = entity.posZ + motionZ * flightTicks;
        }

        return new Vec3(aimX, aimY, aimZ);
    }

    private boolean isBlocked(Vec3 targetPoint) {
        Vec3 start = new Vec3(
                mc.thePlayer.posX,
                mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ
        );
        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(start, targetPoint, false, true, false);
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return result.hitVec.distanceTo(start) < targetPoint.distanceTo(start);
        }
        return false;
    }

    // ==================== 工具 ====================

    private float getKillAuraRange() {
        KillAura ka = KillAura.INSTANCE;
        if (ka != null && ka.isEnabled()) {
            return ka.getRange();
        }
        return 3.5f;
    }

    // ==================== 生命周期 ====================

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        throwCooldown = 0;
        lastThrowableSlot = -1;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Rotations.clearTarget();
        target = null;
        throwCooldown = 0;
    }
}