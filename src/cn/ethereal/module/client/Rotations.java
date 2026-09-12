package cn.ethereal.module.client;

import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.UpdateEvent;
import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.util.helper.NullPointHelper;
import cn.ethereal.util.rotation.RotationUtil;
import org.lwjgl.input.Keyboard;

public class Rotations extends Module {

    private final BooleanValue realistic;
    private final BooleanValue body;
    private final BooleanValue smoothRotations;
    private final NumberValue smoothingFactor;

    // 由 KillAura 等写入
    private static float targetYaw = 0f;
    private static float targetPitch = 0f;
    private static boolean hasTarget = false;

    // 平滑用
    private float lastYaw = 0f;
    private float lastPitch = 0f;
    private boolean firstFrame = true;

    public Rotations() {
        super("Rotations", Keyboard.KEY_NONE, Category.RENDER, false, true);
        realistic = addBooleanValue("Realistic", true);
        body = addBooleanValue("Body", true);
        smoothRotations = addBooleanValue("SmoothRotations", false);
        smoothingFactor = addNumberValue("SmoothFactor", 0.15, 0.1, 0.9, 0.01);
    }

    // ================= 对外接口：静默转头模块调这两个 =================

    public static void setTarget(float yaw, float pitch) {
        targetYaw = yaw;
        targetPitch = pitch;
        hasTarget = true;
    }

    public static void clearTarget() {
        hasTarget = false;
    }

    public static boolean hasTarget() {
        return hasTarget;
    }

    // ================= 主逻辑 =================

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;
        if (!NullPointHelper.isPlayerInWorld()) return;
        if (event.getPhase() != UpdateEvent.Phase.POST) return;

        // ★ 无目标 → 让头平滑回到身体朝向
        if (!hasTarget) {
            resetHeadSmooth();
            return;
        }

        float yaw = targetYaw;
        float pitch = targetPitch;

        if (smoothRotations.getValue()) {
            if (firstFrame) {
                lastYaw = yaw;
                lastPitch = pitch;
                firstFrame = false;
            }
            float f = smoothingFactor.getValue().floatValue();
            lastYaw += (yaw - lastYaw) * f;
            lastPitch += (pitch - lastPitch) * f;
            yaw = lastYaw;
            pitch = lastPitch;
        } else {
            firstFrame = true;
        }

        mc.thePlayer.rotationYawHead = yaw;
        if (body.getValue() && !realistic.getValue()) {
            mc.thePlayer.renderYawOffset = yaw;
        }
    }

    /**
     * 头平滑回到身体朝向（避免 clearTarget 后卡住）
     */
    private void resetHeadSmooth() {
        float bodyYaw = mc.thePlayer.rotationYaw;
        float headYaw = mc.thePlayer.rotationYawHead;
        float renderYaw = mc.thePlayer.renderYawOffset;

        float headDiff = RotationUtil.getAngleDifference(headYaw, bodyYaw);
        float renderDiff = RotationUtil.getAngleDifference(renderYaw, bodyYaw);

        if (Math.abs(headDiff) < 0.5F && Math.abs(renderDiff) < 0.5F) {
            mc.thePlayer.rotationYawHead = bodyYaw;
            mc.thePlayer.renderYawOffset = bodyYaw;
            return;
        }

        float f = 0.35F;
        mc.thePlayer.rotationYawHead = headYaw + headDiff * f;
        mc.thePlayer.renderYawOffset = renderYaw + renderDiff * f;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearTarget();
        firstFrame = true;
    }
}