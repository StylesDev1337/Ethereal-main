package cn.ethereal.ui.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

public class Notification {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final FontRenderer font = mc.fontRendererObj;

    private final String title;
    private final String message;
    private final boolean enabled;

    private final long startTime;
    private final long duration;

    // 位置 / 透明度
    private float currentY;         // 当前屏幕 Y（平滑逼近 stackY）
    private float stackY;           // 目标堆叠 Y（由 Manager 每帧设置）
    private boolean stackInit = false;
    private float animationAlpha;

    // ==================== 常量 ====================
    private static final float MOVE_SPEED = 0.30F;    // Y 缓动速率
    private static final float ALPHA_SPEED = 0.25F;   // 透明缓动速率
    private static final long DEFAULT_DURATION = 2200;

    private static final long FADE_IN_TIME = 180;
    private static final long FADE_OUT_TIME = 400;

    private static final float ENTRY_OFFSET = 24F;    // 入场时在 stackY 下方多少像素

    // 布局（Manager 会用到 LINE_SPACING）
    public static final int PADDING_X = 7;
    public static final int PADDING_Y = 5;
    public static final int BAR_WIDTH = 2;
    public static final int GAP = 3;
    public static final int LINE_SPACING = 4;

    // 颜色
    private static final int BG_COLOR = 0xB0101010;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int ACCENT_ON = 0x55FF55;
    private static final int ACCENT_OFF = 0xFF5555;
    private static final int MSG_COLOR_ON = 0xAAAAAA;
    private static final int MSG_COLOR_OFF = 0xAA8888;

    // ==================== 构造 ====================

    public Notification(String title, boolean enabled) {
        this(title, enabled ? "Enabled" : "Disabled", enabled, DEFAULT_DURATION);
    }

    public Notification(String title, String message, boolean enabled, long duration) {
        this.title = title;
        this.message = message;
        this.enabled = enabled;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
        this.animationAlpha = 0F;
        this.currentY = 0F;
        this.stackY = 0F;
    }

    // ==================== 更新 ====================

    /**
     * 每渲染帧调用一次（不是每 tick）。
     * 动画基于帧率，比每 tick 更新要顺滑得多。
     */
    public void update() {
        long elapsed = System.currentTimeMillis() - startTime;

        // ---------- 透明度 ----------
        float targetAlpha;
        if (elapsed < duration) {
            if (elapsed < FADE_IN_TIME) {
                targetAlpha = (float) elapsed / FADE_IN_TIME;
            } else if (elapsed > duration - FADE_OUT_TIME) {
                targetAlpha = (float) (duration - elapsed) / FADE_OUT_TIME;
            } else {
                targetAlpha = 1F;
            }
        } else {
            targetAlpha = 0F;
        }
        targetAlpha = MathHelper.clamp_float(targetAlpha, 0F, 1F);

        // 缓动 —— 用 float，不用 (int) 截断
        animationAlpha += (targetAlpha - animationAlpha) * ALPHA_SPEED;

        // ---------- 位置 ----------
        if (!stackInit) {
            // 首次拿到 stackY 时，从下方 ENTRY_OFFSET 处开始
            currentY = stackY + ENTRY_OFFSET;
            stackInit = true;
        }
        currentY += (stackY - currentY) * MOVE_SPEED;
    }

    // ==================== 绘制 ====================

    public void draw() {
        if (animationAlpha < 0.02F) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();

        float a = animationAlpha;

        // 宽高
        int titleW = font.getStringWidth(title);
        int msgW = font.getStringWidth(message);
        int contentW = Math.max(titleW, msgW);
        int boxW = contentW + PADDING_X * 2 + BAR_WIDTH;
        int boxH = getHeight();

        int x = screenWidth - boxW - 4;
        int y = Math.round(currentY);   // 只在最终落笔时取整

        // 背景
        drawRect(x, y, x + boxW, y + boxH, applyAlpha(BG_COLOR, a));

        // 左侧色条
        int accent = enabled ? ACCENT_ON : ACCENT_OFF;
        drawRect(x, y, x + BAR_WIDTH, y + boxH, applyAlpha(accent, a));

        // 文字
        int titleColor = applyAlpha(TITLE_COLOR, a);
        int msgColor = applyAlpha(enabled ? MSG_COLOR_ON : MSG_COLOR_OFF, a);

        int textX = x + BAR_WIDTH + PADDING_X;
        int titleY = y + PADDING_Y;
        int msgY = titleY + font.FONT_HEIGHT + GAP;

        font.drawStringWithShadow(title, textX, titleY, titleColor);
        font.drawString(message, textX, msgY, msgColor);
    }

    // ==================== 对外状态 ====================

    public int getHeight() {
        return PADDING_Y * 2 + font.FONT_HEIGHT * 2 + GAP;
    }

    public void setStackY(float y) {
        this.stackY = y;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > duration + 200;
    }

    public String getTitle() {
        return title;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ==================== 工具 ====================

    private static int applyAlpha(int argb, float factor) {
        int alpha = argb >>> 24;
        int newAlpha = MathHelper.clamp_int((int) (alpha * factor), 0, 255);
        return (newAlpha << 24) | (argb & 0x00FFFFFF);
    }

    private static void drawRect(int left, int top, int right, int bottom, int color) {
        if (left >= right || top >= bottom) return;

        float alpha = (color >> 24 & 255) / 255.0F;
        if (alpha <= 0F) return;

        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(left, bottom, 0.0D).endVertex();
        wr.pos(right, bottom, 0.0D).endVertex();
        wr.pos(right, top, 0.0D).endVertex();
        wr.pos(left, top, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}