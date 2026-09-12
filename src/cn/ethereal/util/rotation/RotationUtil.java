package cn.ethereal.util.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class RotationUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // 当前目标角度
    private static float targetYaw = 0;
    private static float targetPitch = 0;
    private static boolean rotating = false;

    /**
     * 静默转头（只发送数据包，本地视角不变）
     */
    public static void silentRotate(Entity entity) {
        float[] rotations = getRotations(entity);
        silentRotate(rotations[0], rotations[1]);
    }

    /**
     * 静默转头（只发送数据包，本地视角不变）
     */
    public static void silentRotate(float yaw, float pitch) {
        targetYaw = yaw;
        targetPitch = pitch;
        rotating = true;

        // 发送转头数据包到服务器（本地不转动）
        mc.thePlayer.sendQueue.addToSendQueue(
                new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, mc.thePlayer.onGround)
        );
    }

    /**
     * 静默转头到指定坐标点
     */
    public static void silentRotate(Vec3 target) {
        float[] rotations = getRotations(target);
        silentRotate(rotations[0], rotations[1]);
    }

    /**
     * 静默转头 + 保持持续旋转（每 tick 发送）
     */
    public static void silentRotateKeep(Entity entity) {
        float[] rotations = getRotations(entity);
        silentRotateKeep(rotations[0], rotations[1]);
    }

    /**
     * 静默转头 + 保持持续旋转
     */
    public static void silentRotateKeep(float yaw, float pitch) {
        targetYaw = yaw;
        targetPitch = pitch;
        rotating = true;

        // 发送转头数据包
        mc.thePlayer.sendQueue.addToSendQueue(
                new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, mc.thePlayer.onGround)
        );
    }

    /**
     * 平滑旋转（本地旋转）
     */
    public static void smoothRotate(Entity entity, float speed) {
        float[] rotations = getRotations(entity);
        smoothRotate(rotations[0], rotations[1], speed);
    }

    /**
     * 平滑旋转（本地旋转）
     */
    public static void smoothRotate(float targetYaw, float targetPitch, float speed) {
        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;

        float yawDiff = getAngleDifference(currentYaw, targetYaw);
        float pitchDiff = getAngleDifference(currentPitch, targetPitch);

        // 限制旋转速度
        float maxRotate = speed;
        if (Math.abs(yawDiff) > maxRotate) {
            targetYaw = currentYaw + (yawDiff > 0 ? maxRotate : -maxRotate);
        }
        if (Math.abs(pitchDiff) > maxRotate) {
            targetPitch = currentPitch + (pitchDiff > 0 ? maxRotate : -maxRotate);
        }

        mc.thePlayer.rotationYaw = targetYaw;
        mc.thePlayer.rotationPitch = targetPitch;
    }

    /**
     * 重置旋转状态
     */
    public static void reset() {
        rotating = false;
        targetYaw = 0;
        targetPitch = 0;
    }

    /**
     * 检查是否正在静默旋转
     */
    public static boolean isRotating() {
        return rotating;
    }

    /**
     * 获取当前目标角度
     */
    public static float[] getTargetRotations() {
        return new float[]{targetYaw, targetPitch};
    }

    /**
     * 获取到实体的角度（瞄准眼睛）
     */
    public static float[] getRotations(Entity entity) {
        return getRotations(entity, entity.getEyeHeight());
    }

    /**
     * 获取到实体指定高度的角度（相对于实体脚部）
     */
    public static float[] getRotations(Entity entity, double height) {
        double x = entity.posX - mc.thePlayer.posX;
        double y = entity.posY + height - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = entity.posZ - mc.thePlayer.posZ;

        return calculateRotations(x, y, z);
    }

    /**
     * 获取到实体下半身的角度（投掷物用）
     */
    public static float[] getRotationsToFeet(Entity entity) {
        return getRotations(entity, 0.5);
    }

    /**
     * 获取到实体指定部位的角度
     * @param entity 目标实体
     * @param part 部位：0 = 脚部, 1 = 身体中部, 2 = 头部
     */
    public static float[] getRotationsToPart(Entity entity, int part) {
        double height;
        switch (part) {
            case 0: // 脚部
                height = 0.0;
                break;
            case 1: // 身体中部
                height = entity.height / 2.0;
                break;
            case 2: // 头部
                height = entity.getEyeHeight();
                break;
            default:
                height = entity.getEyeHeight();
                break;
        }
        return getRotations(entity, height);
    }

    /**
     * 获取到实体指定Y坐标的旋转角度（绝对Y坐标）
     */
    public static float[] getRotationsToY(Entity entity, double absoluteY) {
        double x = entity.posX - mc.thePlayer.posX;
        double y = absoluteY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = entity.posZ - mc.thePlayer.posZ;

        return calculateRotations(x, y, z);
    }

    /**
     * 获取到世界坐标点的角度
     */
    public static float[] getRotations(Vec3 target) {
        double x = target.xCoord - mc.thePlayer.posX;
        double y = target.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = target.zCoord - mc.thePlayer.posZ;

        return calculateRotations(x, y, z);
    }

    /**
     * 获取到世界坐标的角度
     */
    public static float[] getRotations(double x, double y, double z) {
        double diffX = x - mc.thePlayer.posX;
        double diffY = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = z - mc.thePlayer.posZ;

        return calculateRotations(diffX, diffY, diffZ);
    }

    /**
     * 获取带预判的旋转角度（瞄准下半身）
     */
    public static float[] getRotationsWithPrediction(Entity entity, double projectileSpeed) {
        return getRotationsWithPrediction(entity, projectileSpeed, 0.5);
    }

    /**
     * 获取带预判的旋转角度（可指定瞄准高度）
     * @param entity 目标实体
     * @param projectileSpeed 投掷物速度
     * @param aimHeight 瞄准高度（相对于实体脚部）
     */
    public static float[] getRotationsWithPrediction(Entity entity, double projectileSpeed, double aimHeight) {
        double targetX = entity.posX;
        double targetY = entity.posY + aimHeight;
        double targetZ = entity.posZ;

        if (entity instanceof net.minecraft.entity.EntityLivingBase) {
            net.minecraft.entity.EntityLivingBase living = (net.minecraft.entity.EntityLivingBase) entity;

            // 获取速度（每tick位移）
            double motionX = living.posX - living.prevPosX;
            double motionY = living.posY - living.prevPosY;
            double motionZ = living.posZ - living.prevPosZ;

            // 计算水平距离
            double dx = targetX - mc.thePlayer.posX;
            double dz = targetZ - mc.thePlayer.posZ;
            double horizontalDistance = MathHelper.sqrt_double(dx * dx + dz * dz);

            // 估算飞行时间（秒）
            double flightTime = horizontalDistance / projectileSpeed;

            // 转换为 tick 数
            double ticks = flightTime * 20.0;

            // 预测目标未来位置
            targetX += motionX * ticks;
            targetY += motionY * ticks;
            targetZ += motionZ * ticks;
        }

        return getRotations(targetX, targetY, targetZ);
    }

    /**
     * 核心旋转计算
     */
    private static float[] calculateRotations(double x, double y, double z) {
        double distance = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-Math.atan2(y, distance) * 180.0 / Math.PI);

        // 规范化角度
        yaw = clampYaw(yaw);
        pitch = clampPitch(pitch);

        return new float[]{yaw, pitch};
    }

    /**
     * 计算角度差
     */
    public static float getAngleDifference(float base, float target) {
        float diff = target - base;
        while (diff < -180.0F) diff += 360.0F;
        while (diff >= 180.0F) diff -= 360.0F;
        return diff;
    }

    /**
     * 限制角度范围
     */
    public static float clampYaw(float yaw) {
        while (yaw < -180.0F) yaw += 360.0F;
        while (yaw >= 180.0F) yaw -= 360.0F;
        return yaw;
    }

    /**
     * 限制俯仰角范围（-90° ~ 90°）
     */
    public static float clampPitch(float pitch) {
        return MathHelper.clamp_float(pitch, -90.0F, 90.0F);
    }
}