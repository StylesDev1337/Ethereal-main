package cn.ethereal.module.movement;

import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.TickEvent;
import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.ModeValue;
import cn.ethereal.util.helper.NullPointHelper;
import org.lwjgl.input.Keyboard;

public class Sprint extends Module {
    private static Sprint INSTANCE;

    private ModeValue mode;
    private BooleanValue blocking;

    // ★ 用容差代替硬阈值，避免斜走抖动
    private static final float MOVE_THRESHOLD = 0.6F;

    public Sprint() {
        super("Sprint", Keyboard.KEY_NONE, Category.MOVEMENT, true, true);
        INSTANCE = this;

        mode = addModeValue("Mode", new String[]{"Normal", "Auto", "Toggle"}, "Auto");
        blocking = addBooleanValue("Blocking", false);
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;
        if (event.getPhase() != TickEvent.Phase.POST) return;
        if (!NullPointHelper.isPlayerInWorld()) return;

        boolean currentlySprinting = mc.thePlayer.isSprinting();

        // ★ 1. 一次性算出"最终想要的状态"
        boolean wantSprint = computeWantSprint();

        // ★ 2. 只在状态真的需要变化时才调 setSprinting
        if (wantSprint && !currentlySprinting) {
            mc.thePlayer.setSprinting(true);
        } else if (!wantSprint && currentlySprinting) {
            mc.thePlayer.setSprinting(false);
        }
    }

    /**
     * 计算当前是否应该冲刺。
     */
    private boolean computeWantSprint() {
        // 硬性禁用条件
        if (mc.thePlayer.isSneaking()) return false;
        if (mc.thePlayer.isDead) return false;
        if (mc.thePlayer.getFoodStats().getFoodLevel() <= 6) return false;
        if (!mc.gameSettings.keyBindForward.isKeyDown()) return false;
        if (!isMoving()) return false;

        // 物品使用判断
        boolean usingItem = mc.thePlayer.isUsingItem();
        if (usingItem) {
            // Noslow 开了且允许冲刺 → 覆盖原版限制
            if (Noslow.isEnabledStatic() && Noslow.allowSprint()) {
                // 允许
            } else {
                return false;
            }
        }

        // 格挡时的额外判断
        if (mc.thePlayer.isBlocking() && !canSprintOnBlock()) {
            return false;
        }

        return true;
    }

    /**
     * 用容差判断"在移动"，兼容斜走（W+A / W+D）
     * 原版斜走时 moveForward ≈ 0.707，所以阈值定在 0.6
     */
    private static boolean isMoving() {
        return mc.thePlayer.moveForward >= MOVE_THRESHOLD;
    }

    private static boolean canSprintOnBlock() {
        return INSTANCE.blocking.getValue();
    }

    public static boolean isEnable() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        // 开启时不要立刻 setSprinting，交给 onTick 处理
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null && mc.thePlayer.isSprinting()) {
            mc.thePlayer.setSprinting(false);
        }
        super.onDisable();
    }
}