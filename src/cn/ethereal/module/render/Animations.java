package cn.ethereal.module.render;

import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.ModeValue;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import static org.lwjgl.opengl.GL11.glTranslated;
import static org.lwjgl.opengl.GL11.glTranslatef;

public class Animations extends Module {

    public static Animations INSTANCE;

    private final ModeValue mode;
    private final BooleanValue oddSwing;
    private final ModeValue swingSpeed;

    public Animations() {
        super("Animations", Keyboard.KEY_NONE, Category.RENDER, false, true);
        INSTANCE = this;

        mode = addModeValue("Mode",
                new String[]{"OneSeven", "Old", "OldPushdown", "NewPushdown",
                        "Helium", "Argon", "Cesium", "Sulfur"},
                "NewPushdown");
        oddSwing = addBooleanValue("OddSwing", false);
        swingSpeed = addModeValue("SwingSpeed",
                new String[]{"0", "1", "2", "5", "10", "15", "20"},
                "15");
    }

    /**
     * 由原版 ItemRenderer 调用，用于替换 doBlockTransformations。
     */
    public void applyAnimation(float equippedProgress, float swingProgress, AbstractClientPlayer player) {
        Animation animation = getAnimation();
        if (animation == null) return;
        animation.transform(swingProgress, equippedProgress, player);
    }

    public Animation getAnimation() {
        String name = mode.getValue();
        switch (name) {
            case "OneSeven":    return new OneSevenAnimation();
            case "Old":         return new OldAnimation();
            case "OldPushdown": return new OldPushdownAnimation();
            case "NewPushdown": return new NewPushdownAnimation();
            case "Helium":      return new HeliumAnimation();
            case "Argon":       return new ArgonAnimation();
            case "Cesium":      return new CesiumAnimation();
            case "Sulfur":      return new SulfurAnimation();
        }
        return null;
    }

    public boolean isOddSwing() {
        return oddSwing.getValue();
    }

    public int getSwingSpeed() {
        try {
            return Integer.parseInt(swingSpeed.getValue());
        } catch (NumberFormatException e) {
            return 15;
        }
    }

    // ================================================================
    //  Animation 抽象类 + 8 种实现
    // ================================================================

    public static abstract class Animation {
        public final String name;

        public Animation(String name) {
            this.name = name;
        }

        /** 主变换入口。f1 = swingProgress, f = equipProgress */
        public abstract void transform(float f1, float f, AbstractClientPlayer player);

        // ================== 原版抄过来的两个基础变换 ==================

        /** 剑格挡姿势（原版 ItemRenderer.doBlockTransformations） */
        protected void doBlockTransformations() {
            GlStateManager.translate(-0.5F, 0.2F, 0.0F);
            GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
        }

        /** 第一人称手部基础变换（原版 transformFirstPersonItem） */
        protected void transformFirstPersonItem(float equipProgress, float swingProgress) {
            GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
            GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
            GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);

            float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
            float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);

            GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(0.4F, 0.4F, 0.4F);
        }
    }

    // ==================== 各动画实现 ====================

    public static class OneSevenAnimation extends Animation {
        public OneSevenAnimation() { super("OneSeven"); }

        @Override
        public void transform(float f1, float f, AbstractClientPlayer player) {
            transformFirstPersonItem(f, f1);
            doBlockTransformations();
            GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        }
    }

    public static class OldAnimation extends Animation {
        public OldAnimation() { super("Old"); }

        @Override
        public void transform(float f1, float f, AbstractClientPlayer player) {
            transformFirstPersonItem(f, f1);
            doBlockTransformations();
        }
    }

    public static class OldPushdownAnimation extends Animation {
        public OldPushdownAnimation() { super("OldPushdown"); }

        @Override
        public void transform(float f1, float f, AbstractClientPlayer player) {
            GlStateManager.translate(0.56F, -0.52F, -0.5F);
            GlStateManager.translate(0.0F, -f * 0.3F, 0.0F);
            GlStateManager.rotate(45.5F, 0.0F, 1.0F, 0.0F);

            float var3 = MathHelper.sin(0.0F);
            float var4 = MathHelper.sin(0.0F);

            GlStateManager.rotate(var3 * -20.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(var4 * -20.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(var4 * -80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(0.32F, 0.32F, 0.32F);

            float var15 = MathHelper.sin(MathHelper.sqrt_float(f1) * (float) Math.PI);
            GlStateManager.rotate(-var15 * 125.0F / 1.75F, 3.95F, 0.35F, 8.0F);
            GlStateManager.rotate(-var15 * 35.0F, 0.0F, var15 / 100.0F, -10.0F);

            GlStateManager.translate(-1.0F, 0.6F, 0.0F);
            GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
            glTranslated(1.05, 0.35, 0.4);
            glTranslatef(-1.0F, 0.0F, 0.0F);
        }
    }

    public static class NewPushdownAnimation extends Animation {
        public NewPushdownAnimation() { super("NewPushdown"); }

        @Override
        public void transform(float f1, float f, AbstractClientPlayer player) {
            GlStateManager.translate(-0.08F, 0.12F, 0.0F);

            float var9 = MathHelper.sin(MathHelper.sqrt_float(f1) * (float) Math.PI);

            transformFirstPersonItem(f / 1.4F, 0.0F);

            GlStateManager.rotate(-var9 * 65.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
            GlStateManager.rotate(-var9 * 60.0F, 1.0F, var9 / 3.0F, 0.0F);

            doBlockTransformations();

            GlStateManager.scale(1.0F, 1.0F, 1.0F);
        }
    }

    public static class HeliumAnimation extends Animation {
        public HeliumAnimation() { super("Helium"); }

        @Override
        public void transform(float f1, float f, AbstractClientPlayer player) {
            transformFirstPersonItem(f, 0.0F);

            float c0 = MathHelper.sin(f1 * f * (float) Math.PI);
            float c1 = MathHelper.sin(MathHelper.sqrt_float(f1) * (float) Math.PI);

            GlStateManager.rotate(-c1 * 55.0F, 30.0F, c0 / 5.0F, 0.0F);
            doBlockTransformations();
        }
    }

    public static class ArgonAnimation extends Animation {
        public ArgonAnimation() { super("Argon"); }

        @Override
        public void transform(float f1, float f, AbstractClientPlayer player) {
            transformFirstPersonItem(f / 2.5F, f1);

            float c2 = MathHelper.sin(MathHelper.sqrt_float(f1) * (float) Math.PI);
            float c3 = MathHelper.cos(MathHelper.sqrt_float(f) * (float) Math.PI);

            GlStateManager.rotate(c3 * 50.0F / 10.0F, -c2, 0.0F, 100.0F);
            GlStateManager.rotate(c2 * 50.0F, 200.0F, -c2 / 2.0F, 0.0F);
            GlStateManager.translate(0.0F, 0.3F, 0.0F);
            doBlockTransformations();
        }
    }

    public static class CesiumAnimation extends Animation {
        public CesiumAnimation() { super("Cesium"); }

        @Override
        public void transform(float f1, float f, AbstractClientPlayer player) {
            float c4 = MathHelper.sin(MathHelper.sqrt_float(f1) * (float) Math.PI);

            transformFirstPersonItem(f, 0.0F);

            GlStateManager.rotate(-c4 * 10.0F / 20.0F, c4 / 2.0F, 0.0F, 4.0F);
            GlStateManager.rotate(-c4 * 30.0F, 0.0F, c4 / 3.0F, 0.0F);
            GlStateManager.rotate(-c4 * 10.0F, 1.0F, c4 / 10.0F, 0.0F);
            GlStateManager.translate(0.0F, 0.2F, 0.0F);

            doBlockTransformations();
        }
    }

    public static class SulfurAnimation extends Animation {
        public SulfurAnimation() { super("Sulfur"); }

        @Override
        public void transform(float f1, float f, AbstractClientPlayer player) {
            float c5 = MathHelper.sin(MathHelper.sqrt_float(f1) * (float) Math.PI);
            float c6 = MathHelper.cos(MathHelper.sqrt_float(f1) * (float) Math.PI);

            transformFirstPersonItem(f, 0.0F);

            GlStateManager.rotate(-c5 * 30.0F, c5 / 10.0F, c6 / 10.0F, 0.0F);
            GlStateManager.translate(c5 / 1.5F, 0.2F, 0.0F);

            doBlockTransformations();
        }
    }
}