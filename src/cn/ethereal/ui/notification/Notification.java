package cn.ethereal.ui.notification;

import cn.ethereal.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;

public class Notification {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final FontRenderer font = mc.fontRendererObj;

    // ==================== 类型 ====================
    public enum Type {
        SUCCESS(0xFF4ADE80),
        ERROR  (0xFFEF4444),
        WARN   (0xFFFBBA24),
        INFO   (0xFF4A9EFF);

        public final int color;
        Type(int color) { this.color = color; }
    }

    private final String title;
    private final String message;
    private final Type type;

    private final long startTime;
    private final long duration;

    private float currentY;
    private float stackY;
    private boolean stackInit = false;
    private float animationAlpha;

    // ==================== 常量 ====================
    private static final float MOVE_SPEED = 0.30F;
    private static final float ALPHA_SPEED = 0.25F;
    private static final long DEFAULT_DURATION = 2200;

    private static final long FADE_IN_TIME = 180;
    private static final long FADE_OUT_TIME = 400;
    private static final float ENTRY_OFFSET = 24F;

    // 布局
    public static final int PADDING_X = 9;
    public static final int PADDING_Y = 6;
    public static final int GAP = 3;
    public static final int LINE_SPACING = 5;
    public static final int RADIUS = 6;

    public static final int ICON_SIZE = 14;
    public static final int ICON_MARGIN = 6;
    public static final int ICON_INNER = 6;

    // 颜色
    private static final int BG_COLOR = 0xF0181818;
    private static final int BORDER_COLOR = 0xFF2A2A2A;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int ICON_BG_ALPHA = 0x33;
    private static final int MSG_COLOR = 0xFFB0B0B0;

    // ==================== 构造 ====================

    public Notification(String title, boolean enabled) {
        this(title, enabled ? "Enabled" : "Disabled",
                enabled ? Type.SUCCESS : Type.ERROR, DEFAULT_DURATION);
    }

    public Notification(String title, String message, boolean enabled, long duration) {
        this(title, message,
                enabled ? Type.SUCCESS : Type.ERROR, duration);
    }

    public Notification(String title, String message, Type type) {
        this(title, message, type, DEFAULT_DURATION);
    }

    public Notification(String title, String message, Type type, long duration) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
        this.animationAlpha = 0F;
        this.currentY = 0F;
        this.stackY = 0F;
    }

    // ==================== 更新 ====================

    public void update() {
        long elapsed = System.currentTimeMillis() - startTime;

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

        animationAlpha += (targetAlpha - animationAlpha) * ALPHA_SPEED;

        if (!stackInit) {
            currentY = stackY + ENTRY_OFFSET;
            stackInit = true;
        }
        currentY += (stackY - currentY) * MOVE_SPEED;
    }

    // ==================== 绘制（右下角竖版，Classic 模式用） ====================

    public void draw() {
        if (animationAlpha < 0.02F) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();

        float a = animationAlpha;

        int titleW = font.getStringWidth(title);
        int msgW = font.getStringWidth(message);
        int contentW = Math.max(titleW, msgW);
        int boxW = contentW + PADDING_X * 2 + ICON_SIZE + ICON_MARGIN * 2;
        int boxH = getHeight();

        int x = screenWidth - boxW - 4;
        int y = Math.round(currentY);

        RenderUtil.drawRoundedRect(x, y, boxW, boxH, RADIUS, applyAlpha(BG_COLOR, a));
        RenderUtil.drawRoundedOutline(x, y, boxW, boxH, RADIUS, 1, applyAlpha(BORDER_COLOR, a));

        int iconX = x + ICON_MARGIN;
        int iconY = y + (boxH - ICON_SIZE) / 2;
        int accent = type.color;

        int iconBg = applyAlpha((ICON_BG_ALPHA << 24) | (accent & 0x00FFFFFF), a);
        RenderUtil.drawRoundedRect(iconX, iconY, ICON_SIZE, ICON_SIZE, 3, iconBg);

        drawIcon(type, iconX, iconY, ICON_SIZE, accent, a);

        int titleColor = applyAlpha(TITLE_COLOR, a);
        int msgColor = applyAlpha(MSG_COLOR, a);

        int textX = iconX + ICON_SIZE + ICON_MARGIN + PADDING_X - 2;
        int titleY = y + PADDING_Y;
        int msgY = titleY + font.FONT_HEIGHT + GAP;

        font.drawStringWithShadow(title, textX, titleY, titleColor);
        font.drawString(message, textX, msgY, msgColor);
    }

    /**
     * ★ 横向绘制：图标 + 标题 + 消息，一行铺满 (x, y, w, h)。
     * 供 Modern Watermark 使用。
     * 高度推荐 22（ICON_SIZE 14 + 上下各 4 余量）。
     */
    public void renderInline(int x, int y, int w, int h, float globalAlpha) {
        if (globalAlpha < 0.02F) return;

        float a = globalAlpha;
        int accent = type.color;

        // 图标方块
        int iconX = x + 4;
        int iconY = y + (h - ICON_SIZE) / 2;
        int iconBg = applyAlpha((ICON_BG_ALPHA << 24) | (accent & 0x00FFFFFF), a);
        RenderUtil.drawRoundedRect(iconX, iconY, ICON_SIZE, ICON_SIZE, 3, iconBg);
        drawIcon(type, iconX, iconY, ICON_SIZE, accent, a);

        // 文字（标题 + 消息在一行，左右分布）
        int textY = y + (h - font.FONT_HEIGHT) / 2 + 1;
        int titleX = iconX + ICON_SIZE + 8;
        int titleColor = applyAlpha(TITLE_COLOR, a);
        int msgColor = applyAlpha(MSG_COLOR, a);

        // 标题左对齐
        String titleStr = title;
        int titleW = font.getStringWidth(titleStr);

        // 消息右对齐（如果空间不够就裁掉）
        int msgMaxX = x + w - 8;   // 右侧留 8 边距
        int msgW = font.getStringWidth(message);
        int msgX = msgMaxX - msgW;

        // 如果标题和消息重叠，裁掉消息
        if (msgX < titleX + titleW + 6) {
            font.drawStringWithShadow(titleStr, titleX, textY, titleColor);
        } else {
            font.drawStringWithShadow(titleStr, titleX, textY, titleColor);
            font.drawStringWithShadow(message, msgX, textY, msgColor);
        }
    }

    private static void drawIcon(Type type, int x, int y, int size, int accent, float alpha) {
        int color = applyAlpha(accent, alpha);
        int cx = x + size / 2;
        int cy = y + size / 2;
        int t = 2;
        int r = ICON_INNER / 2;

        switch (type) {
            case SUCCESS:
                drawThickLine(cx - r, cy, cx - 1, cy + r - 1, t, color);
                drawThickLine(cx - 1, cy + r - 1, cx + r, cy - r + 1, t, color);
                break;

            case ERROR:
                drawThickLine(cx - r, cy - r, cx + r, cy + r, t, color);
                drawThickLine(cx - r, cy + r, cx + r, cy - r, t, color);
                break;

            case WARN:
                RenderUtil.drawRect(cx - 1, cy - r, 2, r + 1, color);
                RenderUtil.drawRect(cx - 1, cy + r - 1, 2, 2, color);
                break;

            case INFO:
                RenderUtil.drawRect(cx - 1, cy - r, 2, 2, color);
                RenderUtil.drawRect(cx - 1, cy - r + 3, 2, r + 1, color);
                break;
        }
    }

    private static void drawThickLine(int x1, int y1, int x2, int y2, int thickness, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            RenderUtil.drawRect(x1, y1, thickness, thickness, color);
            return;
        }
        float sx = (x2 - x1) / (float) steps;
        float sy = (y2 - y1) / (float) steps;
        for (int i = 0; i <= steps; i++) {
            float px = x1 + sx * i;
            float py = y1 + sy * i;
            RenderUtil.drawRect(px - thickness / 2F, py - thickness / 2F, thickness, thickness, color);
        }
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

    public Type getType() {
        return type;
    }

    // ==================== 新增 getter（供 Watermark 用） ====================

    /** 当前动画透明度（0~1） */
    public float getAnimationAlpha() {
        return animationAlpha;
    }

    /** 消息内容 */
    public String getMessage() {
        return message;
    }

    // ==================== 工具 ====================

    private static int applyAlpha(int argb, float factor) {
        int alpha = argb >>> 24;
        int newAlpha = MathHelper.clamp_int((int) (alpha * factor), 0, 255);
        return (newAlpha << 24) | (argb & 0x00FFFFFF);
    }
}