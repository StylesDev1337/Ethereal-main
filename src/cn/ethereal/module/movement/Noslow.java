package cn.ethereal.module.movement;

import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.ModeValue;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.util.helper.NullPointHelper;
import net.minecraft.item.*;
import org.lwjgl.input.Keyboard;

public class Noslow extends Module {

    private static Noslow INSTANCE;

    // ===== 模式选择 =====
    private ModeValue mode;

    // ===== 剑格挡减速 =====
    private NumberValue swordForward;
    private NumberValue swordStrafe;
    private BooleanValue swordSprint;

    // ===== 食物/药水减速 =====
    private NumberValue foodForward;
    private NumberValue foodStrafe;
    private BooleanValue foodSprint;

    // ===== 弓拉弓减速 =====
    private NumberValue bowForward;
    private NumberValue bowStrafe;
    private BooleanValue bowSprint;

    public Noslow() {
        super("Noslow", Keyboard.KEY_NONE, Category.MOVEMENT, false, true);
        INSTANCE = this;

        mode = addModeValue("Mode", new String[]{"Vanilla", "Legit"}, "Vanilla");

        swordForward = addNumberValue("Sword Forward", 5.0, 0.2, 5.0, 0.1);
        swordStrafe = addNumberValue("Sword Strafe", 5.0, 0.2, 5.0, 0.1);
        swordSprint = addBooleanValue("Sword Sprint", true);

        foodForward = addNumberValue("Food Forward", 5.0, 0.2, 5.0, 0.1);
        foodStrafe = addNumberValue("Food Strafe", 5.0, 0.2, 5.0, 0.1);
        foodSprint = addBooleanValue("Food Sprint", true);

        bowForward = addNumberValue("Bow Forward", 5.0, 0.2, 5.0, 0.1);
        bowStrafe = addNumberValue("Bow Strafe", 5.0, 0.2, 5.0, 0.1);
        bowSprint = addBooleanValue("Bow Sprint", true);
    }

    // ===== 静态方法供 EntityPlayerSP 调用 =====

    public static boolean isEnabledStatic() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static boolean shouldApply() {
        if (INSTANCE == null || !INSTANCE.isEnabledStatic()) return false;
        if (!NullPointHelper.isPlayerInWorld()) return false;
        if (!mc.thePlayer.isUsingItem()) return false;

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null) return false;

        Item item = heldItem.getItem();
        return item instanceof ItemSword ||
                item instanceof ItemFood ||
                item instanceof ItemPotion ||
                item instanceof ItemBucketMilk ||
                item instanceof ItemBow;
    }

    public static float getForwardMultiplier() {
        if (INSTANCE == null || !INSTANCE.isEnabledStatic()) return 1.0f;

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null) return 1.0f;

        Item item = heldItem.getItem();
        String currentMode = INSTANCE.mode.getValue();

        if (item instanceof ItemSword) {
            if (currentMode.equals("Vanilla")) {
                return INSTANCE.swordForward.getValue().floatValue();
            } else {
                return 5.0f; // Legit 模式也改为 5.0，否则 2.0*0.2=0.4 依然很慢
            }
        } else if (item instanceof ItemFood || item instanceof ItemPotion || item instanceof ItemBucketMilk) {
            if (currentMode.equals("Vanilla")) {
                return INSTANCE.foodForward.getValue().floatValue();
            } else {
                return 5.0f;
            }
        } else if (item instanceof ItemBow) {
            if (currentMode.equals("Vanilla")) {
                return INSTANCE.bowForward.getValue().floatValue();
            } else {
                return 5.0f;
            }
        }
        return 1.0f;
    }

    public static float getStrafeMultiplier() {
        if (INSTANCE == null || !INSTANCE.isEnabledStatic()) return 1.0f;

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null) return 1.0f;

        Item item = heldItem.getItem();
        String currentMode = INSTANCE.mode.getValue();

        if (item instanceof ItemSword) {
            if (currentMode.equals("Vanilla")) {
                return INSTANCE.swordStrafe.getValue().floatValue();
            } else {
                return 5.0f;
            }
        } else if (item instanceof ItemFood || item instanceof ItemPotion || item instanceof ItemBucketMilk) {
            if (currentMode.equals("Vanilla")) {
                return INSTANCE.foodStrafe.getValue().floatValue();
            } else {
                return 5.0f;
            }
        } else if (item instanceof ItemBow) {
            if (currentMode.equals("Vanilla")) {
                return INSTANCE.bowStrafe.getValue().floatValue();
            } else {
                return 5.0f;
            }
        }
        return 1.0f;
    }

    /**
     * ★ 新增：检查 Noslow 是否允许 Sprint
     * 供 Sprint 模块调用，判断当前使用物品时是否允许冲刺
     */
    public static boolean allowSprint() {
        if (INSTANCE == null || !INSTANCE.isEnabledStatic()) return true;

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null) return true;

        Item item = heldItem.getItem();

        if (item instanceof ItemSword) {
            return INSTANCE.swordSprint.getValue();
        } else if (item instanceof ItemFood || item instanceof ItemPotion || item instanceof ItemBucketMilk) {
            return INSTANCE.foodSprint.getValue();
        } else if (item instanceof ItemBow) {
            return INSTANCE.bowSprint.getValue();
        }
        return true;
    }

    /**
     * ★ 新增：检查是否因为使用物品而被 Noslow 禁用了冲刺
     */
    public static boolean isSprintBlockedByNoslow() {
        if (INSTANCE == null || !INSTANCE.isEnabledStatic()) return false;
        if (!NullPointHelper.isPlayerInWorld()) return false;
        if (!mc.thePlayer.isUsingItem()) return false;
        return !allowSprint();
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}