package cn.ethereal.module.render;

import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.Render2DEvent;
import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.module.combat.Target;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.util.helper.NullPointHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class TargetHud extends Module {

    // ==================== 布局 ====================
    private static final int WIDTH = 140;
    private static final int HEIGHT = 28;
    private static final int PADDING = 5;
    private static final int BAR_HEIGHT = 5;
    private static final int DEFAULT_BOTTOM_MARGIN = 40;

    // ==================== 颜色 ====================
    private static final int BG_COLOR       = 0xA0101010;
    private static final int BORDER_COLOR   = 0xFF3A3A3A;
    private static final int HOVER_BORDER   = 0xFF00AAFF;   // 拖动/悬停时蓝色边框
    private static final int NAME_COLOR     = 0xFFFFFFFF;
    private static final int HP_TEXT_COLOR  = 0xFFBBBBBB;
    private static final int BAR_BG_COLOR   = 0xFF1A1A1A;
    private static final int BAR_HIGH_COLOR = 0xFF55FF55;
    private static final int BAR_MID_COLOR  = 0xFFFFAA00;
    private static final int BAR_LOW_COLOR  = 0xFFFF5555;

    // ==================== 位置 ====================
    // -1 表示"未设置" → 使用默认（底部居中）
    private final NumberValue posX;
    private final NumberValue posY;

    // ==================== 拖动状态 ====================
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public TargetHud() {
        super("TargetHud", Keyboard.KEY_NONE, Category.RENDER, true, true);
        posX = addNumberValue("PosX", -1.0, -1.0, 3000.0, 1.0);
        posY = addNumberValue("PosY", -1.0, -1.0, 3000.0, 1.0);
    }

    @EventListener
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled()) return;
        if (!NullPointHelper.isPlayerInWorld()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        boolean chatOpen = mc.currentScreen instanceof GuiChat;

        Target targetModule = Target.INSTANCE;
        EntityLivingBase target = targetModule == null ? null : targetModule.getTarget();

        // 没目标 且 不在聊天框 → 不画
        if (!chatOpen && target == null) return;

        int x = getX(sr);
        int y = getY(sr);

        // 拖动处理（仅聊天框打开时生效）
        boolean hovering = false;
        if (chatOpen) {
            int mx = getScaledMouseX(sr);
            int my = getScaledMouseY(sr);
            hovering = isMouseOver(mx, my, x, y);
            handleDrag(sr, x, y);
            x = getX(sr);
            y = getY(sr);
        } else {
            dragging = false;
        }

        drawHud(x, y, target, chatOpen && (dragging || hovering));
    }

    // ==================== 位置 ====================

    private int getX(ScaledResolution sr) {
        double v = posX.getValue();
        if (v < 0) return (sr.getScaledWidth() - WIDTH) / 2;
        return (int) v;
    }

    private int getY(ScaledResolution sr) {
        double v = posY.getValue();
        if (v < 0) return sr.getScaledHeight() - HEIGHT - DEFAULT_BOTTOM_MARGIN;
        return (int) v;
    }

    // ==================== 拖动 ====================

    private void handleDrag(ScaledResolution sr, int x, int y) {
        int mouseX = getScaledMouseX(sr);
        int mouseY = getScaledMouseY(sr);
        boolean leftDown = Mouse.isButtonDown(0);

        if (!dragging) {
            if (leftDown && isMouseOver(mouseX, mouseY, x, y)) {
                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
            }
        } else {
            if (!leftDown) {
                dragging = false;
            } else {
                int newX = mouseX - dragOffsetX;
                int newY = mouseY - dragOffsetY;
                posX.setValue((double) Math.max(0, newX));
                posY.setValue((double) Math.max(0, newY));
            }
        }
    }

    private static boolean isMouseOver(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + WIDTH
                && mouseY >= y && mouseY <= y + HEIGHT;
    }

    /** LWJGL 原始像素 → ScaledResolution 坐标（X） */
    private static int getScaledMouseX(ScaledResolution sr) {
        return Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
    }

    /** LWJGL 原始像素 → ScaledResolution 坐标（Y，注意 Y 轴翻转） */
    private static int getScaledMouseY(ScaledResolution sr) {
        return sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
    }

    // ==================== 绘制 ====================

    private void drawHud(int x, int y, EntityLivingBase target, boolean highlighted) {
        FontRenderer font = mc.fontRendererObj;

        // 背景 + 边框
        drawPanel(x, y, highlighted);

        // 血量
        float health = target != null ? target.getHealth() : 0F;
        float maxHealth = target != null ? target.getMaxHealth() : 20F;
        float percent = maxHealth <= 0F ? 0F : health / maxHealth;
        percent = MathHelper.clamp_float(percent, 0F, 1F);

        String name = target != null ? target.getName() : "No Target";
        String hpText = String.format("%.1f", health);
        String pctText = String.format("%.0f%%", percent * 100);
        String healthText = hpText + " (" + pctText + ")";

        // 顶部行：名字（左） + 血量（右）
        int textY = y + PADDING;
        font.drawStringWithShadow(name, x + PADDING, textY, NAME_COLOR);

        int hpWidth = font.getStringWidth(healthText);
        font.drawStringWithShadow(healthText, x + WIDTH - PADDING - hpWidth, textY, HP_TEXT_COLOR);

        // 底部血条
        int barX = x + PADDING;
        int barY = y + HEIGHT - PADDING - BAR_HEIGHT;
        int barW = WIDTH - PADDING * 2;

        drawRect(barX, barY, barX + barW, barY + BAR_HEIGHT, BAR_BG_COLOR);

        if (target != null) {
            int barColor = percent > 0.6F ? BAR_HIGH_COLOR
                    : percent > 0.3F ? BAR_MID_COLOR
                    : BAR_LOW_COLOR;
            int fillW = (int) (barW * percent);
            if (fillW > 0) {
                drawRect(barX, barY, barX + fillW, barY + BAR_HEIGHT, barColor);
            }
        }
    }

    private static void drawPanel(int x, int y, boolean highlighted) {
        int right = x + WIDTH;
        int bottom = y + HEIGHT;

        drawRect(x, y, right, bottom, BG_COLOR);

        int border = highlighted ? HOVER_BORDER : BORDER_COLOR;
        drawRect(x, y, right, y + 1, border);
        drawRect(x, bottom - 1, right, bottom, border);
        drawRect(x, y, x + 1, bottom, border);
        drawRect(right - 1, y, right, bottom, border);
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