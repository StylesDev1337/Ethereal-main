package cn.ethereal.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

public class RenderUtil {

    // ==================== 基础矩形 ====================
    public static void drawRect(float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0) return;
        float a = (color >> 24 & 255) / 255.0F;
        if (a <= 0) return;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(x, y + h, 0).endVertex();
        wr.pos(x + w, y + h, 0).endVertex();
        wr.pos(x + w, y, 0).endVertex();
        wr.pos(x, y, 0).endVertex();
        tess.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    // ==================== 渐变矩形（纵向） ====================
    public static void drawGradientRect(float x, float y, float w, float h, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        float a1 = (top >> 24 & 255) / 255.0F;
        float r1 = (top >> 16 & 255) / 255.0F;
        float g1 = (top >> 8 & 255) / 255.0F;
        float b1 = (top & 255) / 255.0F;
        float a2 = (bottom >> 24 & 255) / 255.0F;
        float r2 = (bottom >> 16 & 255) / 255.0F;
        float g2 = (bottom >> 8 & 255) / 255.0F;
        float b2 = (bottom & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x + w, y, 0).color(r1, g1, b1, a1).endVertex();
        wr.pos(x, y, 0).color(r1, g1, b1, a1).endVertex();
        wr.pos(x, y + h, 0).color(r2, g2, b2, a2).endVertex();
        wr.pos(x + w, y + h, 0).color(r2, g2, b2, a2).endVertex();
        tess.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawRoundedRect(float x, float y, float w, float h, float radius, int color) {
        if (w <= 0 || h <= 0) return;
        radius = Math.min(radius, Math.min(w / 2F, h / 2F));
        if (radius <= 0.5F) {
            drawRect(x, y, w, h, color);
            return;
        }

        float a = (color >> 24 & 255) / 255.0F;
        if (a <= 0) return;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        float r2 = radius * radius;
        int rows = (int) Math.ceil(h);

        for (int dy = 0; dy < rows; dy++) {
            float rowTop = y + dy;
            float rowBottom = Math.min(rowTop + 1, y + h);
            if (rowTop >= y + h) break;

            float offsetX = 0;

            // 上圆角
            if (dy < radius) {
                float dyFromCenter = radius - (dy + 0.5F);
                float cos = (float) Math.sqrt(Math.max(0, r2 - dyFromCenter * dyFromCenter));
                offsetX = radius - cos;
            }
            // 下圆角
            else if (dy >= h - radius) {
                float dyFromCenter = (dy + 0.5F) - (h - radius);
                float cos = (float) Math.sqrt(Math.max(0, r2 - dyFromCenter * dyFromCenter));
                offsetX = radius - cos;
            }

            float x1 = x + offsetX;
            float x2 = x + w - offsetX;

            wr.pos(x1, rowTop, 0).endVertex();
            wr.pos(x1, rowBottom, 0).endVertex();
            wr.pos(x2, rowBottom, 0).endVertex();
            wr.pos(x2, rowTop, 0).endVertex();
        }

        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedOutline(float x, float y, float w, float h, float radius, float thickness, int color) {
        if (w <= 0 || h <= 0 || thickness <= 0) return;
        radius = Math.min(radius, Math.min(w / 2F, h / 2F));

        // 四条直线
        drawRect(x + radius, y, w - radius * 2, thickness, color);
        drawRect(x + radius, y + h - thickness, w - radius * 2, thickness, color);
        drawRect(x, y + radius, thickness, h - radius * 2, color);
        drawRect(x + w - thickness, y + radius, thickness, h - radius * 2, color);

        // 四角弧线
        drawArc(x + radius, y + radius, radius, thickness, 180, 270, color);
        drawArc(x + w - radius, y + radius, radius, thickness, 270, 360, color);
        drawArc(x + w - radius, y + h - radius, radius, thickness, 0, 90, color);
        drawArc(x + radius, y + h - radius, radius, thickness, 90, 180, color);
    }

    // ==================== 渐变圆角矩形（纵向） ====================
    public static void drawRoundedRectGradient(float x, float y, float w, float h,
                                               float radius, int topColor, int bottomColor) {
        if (w <= 0 || h <= 0) return;
        radius = Math.min(radius, Math.min(w / 2F, h / 2F));
        if (radius <= 0.5F) {
            drawGradientRect(x, y, w, h, topColor, bottomColor);
            return;
        }

        float a1 = (topColor >> 24 & 255) / 255.0F;
        float r1 = (topColor >> 16 & 255) / 255.0F;
        float g1 = (topColor >> 8  & 255) / 255.0F;
        float b1 = (topColor & 255) / 255.0F;

        float a2 = (bottomColor >> 24 & 255) / 255.0F;
        float r2 = (bottomColor >> 16 & 255) / 255.0F;
        float g2 = (bottomColor >> 8  & 255) / 255.0F;
        float b2 = (bottomColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        float rSq = radius * radius;
        int rows = (int) Math.ceil(h);

        for (int dy = 0; dy < rows; dy++) {
            float rowTop = y + dy;
            float rowBottom = Math.min(rowTop + 1, y + h);
            if (rowTop >= y + h) break;

            float offsetX = 0;
            if (dy < radius) {
                float dyFromCenter = radius - (dy + 0.5F);
                float cos = (float) Math.sqrt(Math.max(0, rSq - dyFromCenter * dyFromCenter));
                offsetX = radius - cos;
            } else if (dy >= h - radius) {
                float dyFromCenter = (dy + 0.5F) - (h - radius);
                float cos = (float) Math.sqrt(Math.max(0, rSq - dyFromCenter * dyFromCenter));
                offsetX = radius - cos;
            }

            float x1 = x + offsetX;
            float x2 = x + w - offsetX;

            // 该行 y 对应的渐进步（0 = 顶，1 = 底）
            float tTop = (rowTop - y) / h;
            float tBottom = (rowBottom - y) / h;
            tTop = MathHelper.clamp_float(tTop, 0F, 1F);
            tBottom = MathHelper.clamp_float(tBottom, 0F, 1F);

            float ra = a1 + (a2 - a1) * tTop;
            float rr = r1 + (r2 - r1) * tTop;
            float rg = g1 + (g2 - g1) * tTop;
            float rb = b1 + (b2 - b1) * tTop;

            float ba = a1 + (a2 - a1) * tBottom;
            float br = r1 + (r2 - r1) * tBottom;
            float bg = g1 + (g2 - g1) * tBottom;
            float bb = b1 + (b2 - b1) * tBottom;

            // 顶边
            wr.pos(x1, rowTop, 0).color(rr, rg, rb, ra).endVertex();
            wr.pos(x2, rowTop, 0).color(rr, rg, rb, ra).endVertex();
            // 底边
            wr.pos(x2, rowBottom, 0).color(br, bg, bb, ba).endVertex();
            wr.pos(x1, rowBottom, 0).color(br, bg, bb, ba).endVertex();
        }

        tess.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private static void drawArc(float cx, float cy, float radius, float thickness, int startAngle, int endAngle, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        if (a <= 0) return;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION);

        float inner = radius - thickness;
        int segments = 16;   // ★ 从 10 提到 16，弧线更平滑
        for (int i = 0; i <= segments; i++) {
            double ang = Math.toRadians(startAngle + (endAngle - startAngle) * i / (double) segments);
            float cos = (float) Math.cos(ang);
            float sin = (float) Math.sin(ang);
            wr.pos(cx + cos * radius, cy + sin * radius, 0).endVertex();
            wr.pos(cx + cos * inner, cy + sin * inner, 0).endVertex();
        }
        tess.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    // ==================== 裁剪（Scissor） ====================
    /**
     * 开启 scissor 裁剪区域。
     * 注意：坐标是 GUI 缩放坐标，内部会乘 scaleFactor 转成物理像素。
     * 必须在绘制前调用 enableScissor，绘制后调用 disableScissor。
     */
    public static void enableScissor(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();

        int sx = x * scale;
        int sy = (sr.getScaledHeight() - y - height) * scale;   // OpenGL 原点在左下
        int sw = width * scale;
        int sh = height * scale;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
    }

    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}