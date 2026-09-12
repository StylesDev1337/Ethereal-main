package cn.ethereal.util.target;

import cn.ethereal.util.Util;
import cn.ethereal.util.rotation.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 全局目标管理器：所有需要"锁定一个敌人"的模块都从这里取。
 * 保证同一时刻全客户端只有一个敌人目标。
 */
public class TargetManager extends Util {

    private static TargetManager instance;
    private static final Minecraft mc = Minecraft.getMinecraft();

    // ==================== 目标 ====================
    private EntityLivingBase target = null;
    private long switchTime = 0L;

    // ==================== 过滤条件 ====================
    public float fov = 360F;
    public boolean throughWalls = false;
    public boolean players = true;
    public boolean mobs = true;
    public boolean animals = false;
    public boolean villagers = false;
    public boolean bosses = false;
    public boolean silverfish = false;

    // 排序方式
    public SortMode sortMode = SortMode.DISTANCE;

    // 目标切换冷却（毫秒）
    public long switchDelay = 0L;

    // ==================== 单例 ====================

    private TargetManager() {}

    public static TargetManager getInstance() {
        if (instance == null) instance = new TargetManager();
        return instance;
    }

    // ==================== 枚举 ====================

    public enum SortMode {
        DISTANCE, HEALTH, HURT_TIME, FOV
    }

    // ==================== 对外 ====================

    public EntityLivingBase getTarget() {
        return target;
    }

    public void clear() {
        target = null;
    }

    /**
     * 每 tick 调用一次。所有模块共用一份目标。
     */
    public void update() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            target = null;
            return;
        }

        // 目标仍有效 → 沿用
        boolean needReselect =
                target == null
                        || !isValidTarget(target)
                        || (switchDelay > 0
                        && System.currentTimeMillis() - switchTime >= switchDelay);

        if (!needReselect) return;

        // 重新扫描
        List<EntityLivingBase> candidates = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            EntityLivingBase living = (EntityLivingBase) entity;
            if (!isValidTarget(living)) continue;
            candidates.add(living);
        }

        if (candidates.isEmpty()) {
            target = null;
            return;
        }

        // 玩家优先
        List<EntityLivingBase> playersOnly = new ArrayList<>();
        for (EntityLivingBase e : candidates) {
            if (e instanceof EntityPlayer) playersOnly.add(e);
        }
        if (!playersOnly.isEmpty()) candidates = playersOnly;

        candidates.sort(getComparator());
        target = candidates.get(0);
        switchTime = System.currentTimeMillis();
    }

    // ==================== 过滤 ====================

    public boolean isValidTarget(EntityLivingBase living) {
        if (living == null || living == mc.thePlayer) return false;
        if (living.isDead || living.deathTime > 0) return false;
        if (!living.isEntityAlive()) return false;
        if (living.getHealth() <= 0F) return false;
        if (living == mc.thePlayer.ridingEntity) return false;
        if (living == mc.getRenderViewEntity() || living == mc.getRenderViewEntity().ridingEntity) return false;

        // FOV
        if (fov < 360F && angleTo(living) > fov) return false;

        // 视线
        if (!throughWalls && !mc.thePlayer.canEntityBeSeen(living)) return false;

        // 类型
        if (living instanceof EntityPlayer) return players;
        if (living instanceof EntityDragon || living instanceof EntityWither) return bosses;
        if (living instanceof EntitySilverfish) return silverfish;
        if (living instanceof EntityIronGolem) return mobs;
        if (living instanceof EntityMob || living instanceof EntitySlime) return mobs;
        if (living instanceof EntityVillager) return villagers;
        if (living instanceof EntityAnimal
                || living instanceof EntityBat
                || living instanceof EntitySquid) return animals;

        return false;
    }

    // ==================== 排序 ====================

    private Comparator<EntityLivingBase> getComparator() {
        switch (sortMode) {
            case HEALTH:
                return (a, b) -> Float.compare(a.getHealth(), b.getHealth());
            case HURT_TIME:
                return (a, b) -> Integer.compare(a.hurtResistantTime, b.hurtResistantTime);
            case FOV:
                return (a, b) -> Float.compare(angleTo(a), angleTo(b));
            case DISTANCE:
            default:
                return (a, b) -> Double.compare(distanceTo(a), distanceTo(b));
        }
    }

    // ==================== 工具 ====================

    public static double distanceTo(EntityLivingBase living) {
        return mc.thePlayer.getDistanceToEntity(living);
    }

    public static float angleTo(EntityLivingBase living) {
        float[] rot = RotationUtil.getRotations(living);
        float yawDiff   = Math.abs(RotationUtil.getAngleDifference(mc.thePlayer.rotationYaw,   rot[0]));
        float pitchDiff = Math.abs(RotationUtil.getAngleDifference(mc.thePlayer.rotationPitch, rot[1]));
        return Math.max(yawDiff, pitchDiff);
    }
}