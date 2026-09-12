package cn.ethereal.module.combat;

import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.UpdateEvent;
import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.ModeValue;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.util.helper.NullPointHelper;
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
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 全局目标管理模块。
 * KillAura / ThrowableAura 等所有需要"锁定一个敌人"的模块都从这拿。
 *
 * 注意：这个模块始终在 tick 里更新目标，不受 isEnabled() 影响。
 * 开关只控制 UI 显示，不影响目标选择。
 */
public class Target extends Module {

    public static Target INSTANCE;
    private static final Minecraft mc = Minecraft.getMinecraft();

    // ==================== 过滤 / 排序设置 ====================
    private final ModeValue sortMode;
    private final ModeValue switchMode;
    private final NumberValue fov;
    private final NumberValue switchDelay;
    private final BooleanValue throughWalls;
    private final BooleanValue players;
    private final BooleanValue mobs;
    private final BooleanValue animals;
    private final BooleanValue villagers;
    private final BooleanValue bosses;
    private final BooleanValue silverfish;

    // ==================== 状态 ====================
    private EntityLivingBase target = null;
    private long switchTime = 0L;
    private int lastTick = -1;

    public Target() {
        super("Target", Keyboard.KEY_NONE, Category.COMBAT, true, true);
        INSTANCE = this;

        sortMode    = addModeValue("Sort",        new String[]{"Distance", "Health", "HurtTime", "FOV"}, "Distance");
        switchMode  = addModeValue("SwitchMode",  new String[]{"Single", "Switch"}, "Single");
        fov         = addNumberValue("FOV",         360.0, 30.0, 360.0, 5.0);
        switchDelay = addNumberValue("SwitchDelay", 150.0, 0.0, 1000.0, 10.0);

        throughWalls = addBooleanValue("ThroughWalls", false);
        players      = addBooleanValue("Players",      true);
        mobs         = addBooleanValue("Mobs",         true);
        animals      = addBooleanValue("Animals",      false);
        villagers    = addBooleanValue("Villagers",    false);
        bosses       = addBooleanValue("Bosses",       false);
        silverfish   = addBooleanValue("Silverfish",   false);
    }

    // ==================== 主逻辑 ====================

    @EventListener(priority = 100)   // 保证在 KillAura 之前跑
    public void onUpdate(UpdateEvent event) {
        if (event.getPhase() != UpdateEvent.Phase.PRE) return;
        if (!NullPointHelper.isPlayerInWorld()) {
            target = null;
            return;
        }

        // 同一 tick 只跑一次（多个模块可能触发，实际只有一份监听器，这里保险）
        int currentTick = mc.thePlayer.ticksExisted;
        if (currentTick == lastTick) return;
        lastTick = currentTick;

        updateTarget();
    }

    private void updateTarget() {
        // ★ 1. 每 tick 都扫描所有候选（不再依赖 needReselect 短路）
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
        List<EntityLivingBase> playerOnly = new ArrayList<>();
        for (EntityLivingBase e : candidates) {
            if (e instanceof EntityPlayer) playerOnly.add(e);
        }
        if (!playerOnly.isEmpty()) candidates = playerOnly;

        candidates.sort(getComparator());
        EntityLivingBase best = candidates.get(0);

        // ★ 2. 当前目标无效 → 直接采用最优
        if (target == null || !isValidTarget(target)) {
            target = best;
            switchTime = System.currentTimeMillis();
            return;
        }

        // ★ 3. 最优就是当前目标 → 不动
        if (best == target) return;

        // ★ 4. 最优不是当前目标 → 按模式决定是否切换
        if (switchMode.getValue().equals("Switch")) {
            // Switch：按时间轮流切换
            if (System.currentTimeMillis() - switchTime >= switchDelay.getValue().longValue()) {
                target = best;
                switchTime = System.currentTimeMillis();
            }
        } else {
            // Single：新目标明显更优才切（避免抖动）
            if (isSignificantlyBetter(best, target)) {
                target = best;
                switchTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * a 是否比 b 明显更优。
     * 用"距离近 0.5 格以上"作为阈值，避免两目标距离接近时反复横跳。
     */
    private boolean isSignificantlyBetter(EntityLivingBase a, EntityLivingBase b) {
        return distanceTo(a) < distanceTo(b) - 0.5;
    }

    /** 强制下一 tick 重选目标（KillAura Switch 模式用） */
    public void forceReselect() {
        switchTime = 0L;
        target = null;
    }

    // ==================== 过滤 ====================

    public boolean isValidTarget(EntityLivingBase living) {
        if (living == null || living == mc.thePlayer) return false;
        if (living.isDead || living.deathTime > 0) return false;
        if (!living.isEntityAlive()) return false;
        if (living.getHealth() <= 0.0F) return false;
        if (living == mc.thePlayer.ridingEntity) return false;
        if (living == mc.getRenderViewEntity() || living == mc.getRenderViewEntity().ridingEntity) return false;

        if (fov.getValue() < 360.0 && angleTo(living) > fov.getValue().floatValue()) return false;
        if (!throughWalls.getValue() && !mc.thePlayer.canEntityBeSeen(living)) return false;

        if (living instanceof EntityPlayer) return players.getValue();
        if (living instanceof EntityDragon || living instanceof EntityWither) return bosses.getValue();
        if (living instanceof EntitySilverfish) return silverfish.getValue();
        if (living instanceof EntityIronGolem) return mobs.getValue();
        if (living instanceof EntityMob || living instanceof EntitySlime) return mobs.getValue();
        if (living instanceof EntityVillager) return villagers.getValue();
        if (living instanceof EntityAnimal
                || living instanceof EntityBat
                || living instanceof EntitySquid) {
            return animals.getValue();
        }
        return false;
    }

    // ==================== 排序 ====================

    private Comparator<EntityLivingBase> getComparator() {
        String sort = sortMode.getValue();
        if (sort.equals("Health")) {
            return (a, b) -> Float.compare(a.getHealth(), b.getHealth());
        }
        if (sort.equals("HurtTime")) {
            return (a, b) -> Integer.compare(a.hurtResistantTime, b.hurtResistantTime);
        }
        if (sort.equals("FOV")) {
            return (a, b) -> Float.compare(angleTo(a), angleTo(b));
        }
        return (a, b) -> Double.compare(distanceTo(a), distanceTo(b));
    }

    // ==================== 工具 ====================

    public static double distanceTo(EntityLivingBase living) {
        return mc.thePlayer.getDistanceToEntity(living);
    }

    public static float angleTo(EntityLivingBase living) {
        float[] rot = RotationUtil.getRotations(living);
        float yawDiff = Math.abs(RotationUtil.getAngleDifference(mc.thePlayer.rotationYaw, rot[0]));
        float pitchDiff = Math.abs(RotationUtil.getAngleDifference(mc.thePlayer.rotationPitch, rot[1]));
        return Math.max(yawDiff, pitchDiff);
    }

    // ==================== 对外接口 ====================

    public EntityLivingBase getTarget() {
        return target;
    }

    public boolean isThroughWalls() {
        return throughWalls.getValue();
    }

    public void clear() {
        target = null;
        switchTime = 0L;
    }
}