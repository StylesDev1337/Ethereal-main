package cn.ethereal.hud.impl;

import cn.ethereal.hud.HudElement;
import cn.ethereal.module.combat.KillAura;
import cn.ethereal.module.combat.Target;
import cn.ethereal.module.render.HUD;
import cn.ethereal.util.render.RenderUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

/**
 * TargetHud 从 Module 改成 HudElement。
 * Classic 模式：作为独立 HUD 元素显示。
 * Modern 模式：自身跳过渲染，内容由 ModernWatermarkHud 通过 renderInline 绘制。
 */
public class TargetHud extends HudElement {

    // ==================== 布局 ====================
    public static final int WIDTH      = 160;
    public static final int HEIGHT     = 46;
    private static final int PADDING    = 8;
    private static final int AVATAR     = 28;
    private static final int BAR_HEIGHT = 6;
    private static final int RADIUS     = 6;
    private static final int DEFAULT_BOTTOM_MARGIN = 40;

    // ==================== 颜色 ====================
    private static final int BG_TOP       = 0xF01A1A1A;
    private static final int BG_BOTTOM    = 0xF0141414;
    private static final int BORDER_COLOR = 0xFF2A2A2A;
    private static final int HOVER_BORDER = 0xFF4A9EFF;
    private static final int AVATAR_BG    = 0xFF252525;
    private static final int AVATAR_TEXT  = 0xFFFFFFFF;
    private static final int NAME_COLOR   = 0xFFFFFFFF;
    private static final int HP_TEXT_COLOR= 0xFFB0B0B0;
    private static final int BAR_BG_COLOR = 0xFF2A2A2A;
    private static final int BAR_HIGH_COLOR = 0xFF4ADE80;
    private static final int BAR_MID_COLOR  = 0xFFFBBA24;
    private static final int BAR_LOW_COLOR  = 0xFFEF4444;
    private static final int BAR_TRAIL_COLOR= 0xFFFF6B6B;

    // ==================== 动画 ====================
    private static final float TRAIL_SPEED = 0.06F;

    // 伤害残留
    private float trailPercent = 1F;
    private EntityLivingBase lastTarget = null;

    public TargetHud() {
        super("TargetHud", "目标信息",
                HUD.getInstance().targetHudShow,
                HUD.getInstance().targetHudPosX,
                HUD.getInstance().targetHudPosY);
    }

    // ==================== 显示条件 ====================

    /** 是否应该显示（有目标或聊天打开） */
    private boolean shouldShow() {
        if (mc.currentScreen instanceof GuiChat) return true;
        if (KillAura.INSTANCE == null || !KillAura.INSTANCE.isAttacking()) return false;
        Target targetModule = Target.INSTANCE;
        return targetModule != null && targetModule.getTarget() != null;
    }

    /** Modern 模式下由 Watermark 接管渲染 */
    private boolean isModernWatermarkActive() {
        try {
            HUD hud = HUD.getInstance();
            if (hud == null || !hud.isEnabled()) return false;
            if (!hud.watermarkShow.getValue()) return false;
            return "Modern".equals(hud.watermarkMode.getValue());
        } catch (Throwable t) {
            return false;
        }
    }

    // ==================== measure ====================

    @Override
    public int[] measure() {
        // Modern 模式：由 Watermark 接管，本元素尺寸归零
        if (isModernWatermarkActive()) {
            width = 0;
            height = 0;
            return new int[] { 0, 0 };
        }
        // 没目标：尺寸归零
        if (!shouldShow()) {
            width = 0;
            height = 0;
            return new int[] { 0, 0 };
        }
        width = WIDTH;
        height = HEIGHT;
        return new int[] { WIDTH, HEIGHT };
    }

    @Override
    public int getDefaultX(ScaledResolution sr) {
        return (sr.getScaledWidth() - WIDTH) / 2;
    }

    @Override
    public int getDefaultY(ScaledResolution sr) {
        return sr.getScaledHeight() - HEIGHT - DEFAULT_BOTTOM_MARGIN;
    }

    // ==================== update ====================

    @Override
    public void update() {
        // 伤害残留的推进在这里做（保证即使不渲染也能推进，
        // 但实际 target 变化时在 renderInline / render 里处理）
    }

    // ==================== render（Classic 独立显示） ====================

    @Override
    public void render(int x, int y) {
        if (isModernWatermarkActive()) return;
        if (!shouldShow()) return;

        Target targetModule = Target.INSTANCE;
        EntityLivingBase target = targetModule == null ? null : targetModule.getTarget();

        renderPanel(x, y, WIDTH, HEIGHT, target, false, 1F);
    }

    // ==================== renderInline（Modern Watermark 调用） ====================

    /**
     * 供 Modern Watermark 使用：在 (x, y, w, h) 里画一个内嵌的 TargetHud。
     * 高度建议 46（WIDTH 160 时）。宽度自适应 w。
     */
    public void renderInline(int x, int y, int w, int h, float alpha) {
        Target targetModule = Target.INSTANCE;
        EntityLivingBase target = targetModule == null ? null : targetModule.getTarget();

        renderPanel(x, y, w, h, target, false, alpha);
    }

    // ==================== 核心绘制 ====================

    private void renderPanel(int x, int y, int w, int h, EntityLivingBase target,
                             boolean highlighted, float fade) {
        FontRenderer font = mc.fontRendererObj;

        int alpha = (int) (0xFF * fade);
        if (alpha <= 0) return;

        int bgTop     = withAlpha(BG_TOP, alpha);
        int bgBottom  = withAlpha(BG_BOTTOM, alpha);
        int border    = withAlpha(highlighted ? HOVER_BORDER : BORDER_COLOR, alpha);
        int avatarBg  = withAlpha(AVATAR_BG, alpha);
        int avatarTxt = withAlpha(AVATAR_TEXT, alpha);
        int nameCol   = withAlpha(NAME_COLOR, alpha);
        int hpCol     = withAlpha(HP_TEXT_COLOR, alpha);
        int barBg     = withAlpha(BAR_BG_COLOR, alpha);
        int trailCol  = withAlpha(BAR_TRAIL_COLOR, alpha);

        // 背景 + 边框
        RenderUtil.drawRoundedRectGradient(x, y, w, h, RADIUS, bgTop, bgBottom);
        RenderUtil.drawRoundedOutline(x, y, w, h, RADIUS, 1, border);

        // 头像
        int avX = x + PADDING;
        int avY = y + (h - AVATAR) / 2;
        RenderUtil.drawRoundedRect(avX, avY, AVATAR, AVATAR, 4, avatarBg);

        String name = target != null ? target.getName() : "No Target";
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        int initialW = font.getStringWidth(initial);
        font.drawStringWithShadow(initial,
                avX + (AVATAR - initialW) / 2,
                avY + (AVATAR - 8) / 2,
                avatarTxt);

        int contentX = avX + AVATAR + PADDING;
        int contentW = w - (contentX - x) - PADDING;

        float health = target != null ? target.getHealth() : 0F;
        float maxHealth = target != null ? target.getMaxHealth() : 20F;
        float percent = maxHealth <= 0F ? 0F : health / maxHealth;
        percent = MathHelper.clamp_float(percent, 0F, 1F);

        String healthText = String.format("%.1f", health);

        int nameY = y + PADDING + 2;
        int nameMaxW = contentW - font.getStringWidth(healthText) - 6;
        String drawName = trimToWidth(font, name, nameMaxW);
        font.drawStringWithShadow(drawName, contentX, nameY, nameCol);

        int hpW = font.getStringWidth(healthText);
        font.drawStringWithShadow(healthText, x + w - PADDING - hpW, nameY, hpCol);

        int barX = contentX;
        int barY = y + h - PADDING - BAR_HEIGHT;
        int barW = contentW;

        RenderUtil.drawRoundedRect(barX, barY, barW, BAR_HEIGHT, BAR_HEIGHT / 2F, barBg);

        if (target != null) {
            if (target != lastTarget) {
                lastTarget = target;
                trailPercent = percent;
            }
            if (trailPercent < percent) {
                trailPercent = percent;
            } else {
                trailPercent += (percent - trailPercent) * TRAIL_SPEED;
                if (trailPercent - percent < 0.005F) trailPercent = percent;
            }

            float trailW = barW * trailPercent;
            if (trailW > 1.5F && trailPercent > percent) {
                RenderUtil.drawRoundedRect(barX, barY, trailW, BAR_HEIGHT, BAR_HEIGHT / 2F, trailCol);
            }

            int barColor = getHealthColor(percent, alpha);
            float fillW = barW * percent;
            if (fillW > 1.5F) {
                RenderUtil.drawRoundedRect(barX, barY, fillW, BAR_HEIGHT, BAR_HEIGHT / 2F, barColor);
            }
        }
    }

    private static int getHealthColor(float percent, int alpha) {
        int high = withAlpha(BAR_HIGH_COLOR, alpha);
        int mid  = withAlpha(BAR_MID_COLOR, alpha);
        int low  = withAlpha(BAR_LOW_COLOR, alpha);
        if (percent > 0.5F) {
            return lerpColor(mid, high, (percent - 0.5F) / 0.5F);
        } else {
            return lerpColor(low, mid, percent / 0.5F);
        }
    }

    private static String trimToWidth(FontRenderer font, String s, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.getStringWidth(s) <= maxWidth) return s;
        String ellipsis = "...";
        int ellipsisW = font.getStringWidth(ellipsis);
        if (ellipsisW >= maxWidth) return ellipsis;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (font.getStringWidth(sb.toString() + c) + ellipsisW > maxWidth) break;
            sb.append(c);
        }
        return sb + ellipsis;
    }

    private static int withAlpha(int color, int alpha) {
        alpha = MathHelper.clamp_int(alpha, 0, 255);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, float t) {
        t = MathHelper.clamp_float(t, 0F, 1F);
        int a1 = (from >> 24) & 0xFF, a2 = (to >> 24) & 0xFF;
        int r1 = (from >> 16) & 0xFF, r2 = (to >> 16) & 0xFF;
        int g1 = (from >> 8)  & 0xFF, g2 = (to >> 8)  & 0xFF;
        int b1 = from & 0xFF,         b2 = to & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}