package cn.ethereal.hud.impl;

import cn.ethereal.hud.HudElement;
import cn.ethereal.hud.HudManager;
import cn.ethereal.module.movement.Scaffold;
import cn.ethereal.module.render.HUD;
import cn.ethereal.ui.notification.Notification;
import cn.ethereal.ui.notification.NotificationManager;
import cn.ethereal.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

import java.util.List;

/**
 * Modern 模式水印。
 * 模式优先级：NOTI > TARGET > EATING > SCAFFOLD > IDLE
 */
public class ModernWatermarkHud {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final FontRenderer font = mc.fontRendererObj;

    // ==================== 布局 ====================
    private static final int PADDING      = 6;
    private static final int RADIUS       = 6;
    private static final int HEIGHT_IDLE  = 24;
    private static final int HEIGHT_TARGET = 46;   // TargetHud 模式高度
    private static final int ROW_H        = 22;
    private static final int MAX_NOTI     = 2;
    private static final int LOGO_SIZE    = 16;
    private static final int LOGO_GAP     = 6;
    private static final int SEGMENT_GAP  = 10;
    private static final int TOP_MARGIN   = 4;

    // 进度条
    private static final int BAR_H        = 4;
    private static final int BAR_W        = 60;

    // ==================== 颜色 ====================
    private static final int BG           = 0xC0101010;
    private static final int BORDER       = 0x40FFFFFF;
    private static final int TEXT         = 0xFFFFFFFF;
    private static final int TEXT_DIM     = 0xFFB0B0B0;
    private static final int TEXT_FAINT   = 0xFF808080;
    private static final int LOGO_BG_A    = 0x33;
    private static final int BAR_BG       = 0x40FFFFFF;

    // ==================== 动画 ====================
    private static final float WIDTH_SPEED   = 0.22F;
    private static final float HEIGHT_SPEED  = 0.22F;
    private static final float ALPHA_SPEED   = 0.28F;
    private static final float CONTENT_SPEED = 0.30F;

    private float animatedWidth  = -1F;
    private float animatedHeight = -1F;
    private float animatedAlpha  = 0F;
    private float contentAlpha   = 1F;

    private enum Mode { IDLE, NOTI, TARGET, SCAFFOLD, EATING }
    private Mode currentMode = Mode.IDLE;
    private Mode pendingMode = Mode.IDLE;

    private int targetWidth  = 0;
    private int targetHeight = HEIGHT_IDLE;

    // ==================== measure ====================

    public int[] measure() {
        Mode newMode = decideMode();

        if (newMode != currentMode && contentAlpha >= 0.99F) {
            pendingMode = newMode;
        }

        switch (currentMode) {
            case NOTI: {
                List<Notification> active =
                        NotificationManager.getInstance().getActiveNotifications(MAX_NOTI);
                targetWidth  = measureNotiWidth(active);
                targetHeight = HEIGHT_IDLE + Math.max(0, active.size() - 1) * ROW_H;
                break;
            }
            case TARGET: {
                targetWidth  = TargetHud.WIDTH;
                targetHeight = HEIGHT_TARGET;
                break;
            }
            case SCAFFOLD: {
                targetWidth  = measureScaffoldWidth();
                targetHeight = HEIGHT_IDLE;
                break;
            }
            case EATING: {
                targetWidth  = measureEatingWidth();
                targetHeight = HEIGHT_IDLE;
                break;
            }
            case IDLE:
            default: {
                targetWidth  = measureIdleWidth();
                targetHeight = HEIGHT_IDLE;
                break;
            }
        }

        int w = (int) animatedWidth;
        int h = (int) animatedHeight;
        return new int[] { w, h };
    }

    /** 优先级：NOTI > TARGET > EATING > SCAFFOLD > IDLE */
    private Mode decideMode() {
        // 1. NOTI
        List<Notification> active =
                NotificationManager.getInstance().getActiveNotifications(MAX_NOTI);
        if (!active.isEmpty()) return Mode.NOTI;

        // 2. TARGET（有目标）
        if (hasTarget()) return Mode.TARGET;

        // 3. EATING
        if (isEatingOrDrinking()) return Mode.EATING;

        // 4. SCAFFOLD
        if (Scaffold.INSTANCE != null && Scaffold.INSTANCE.isEnabled()) return Mode.SCAFFOLD;

        // 5. IDLE
        return Mode.IDLE;
    }

    private boolean hasTarget() {
        try {
            cn.ethereal.module.combat.Target tm = cn.ethereal.module.combat.Target.INSTANCE;
            if (tm == null || tm.getTarget() == null) return false;
            cn.ethereal.module.combat.KillAura ka = cn.ethereal.module.combat.KillAura.INSTANCE;
            return ka != null && ka.isAttacking();
        } catch (Throwable t) {
            return false;
        }
    }

    // ==================== 宽度计算 ====================

    private int measureIdleWidth() {
        int w = PADDING * 2;
        w += LOGO_SIZE + LOGO_GAP;
        w += font.getStringWidth("Ethereal");
        w += SEGMENT_GAP + font.getStringWidth(getFpsText());
        String ip = getServerIp();
        if (!ip.isEmpty()) w += SEGMENT_GAP + font.getStringWidth(ip);
        String player = getPlayerName();
        if (!player.isEmpty()) w += SEGMENT_GAP + font.getStringWidth(player);
        return w;
    }

    private int measureNotiWidth(List<Notification> list) {
        int maxW = 0;
        for (Notification n : list) {
            int titleW = font.getStringWidth(n.getTitle());
            int msgW   = font.getStringWidth(n.getMessage());
            int rowW = 4 + Notification.ICON_SIZE + 8 + titleW + 6 + msgW + 8;
            if (rowW > maxW) maxW = rowW;
        }
        if (maxW < 120) maxW = 120;
        return maxW;
    }

    private int measureScaffoldWidth() {
        int w = PADDING * 2;
        w += 14 + 8;
        w += font.getStringWidth("Scaffold");
        w += 8 + font.getStringWidth(String.valueOf(getBlockCount()));
        w += SEGMENT_GAP + BAR_W;
        return w;
    }

    private int measureEatingWidth() {
        int w = PADDING * 2;
        w += 14 + 8;
        w += font.getStringWidth("Eating");
        w += 8 + font.getStringWidth(getEatingTimeText());
        w += SEGMENT_GAP + BAR_W;
        return w;
    }

    // ==================== 位置 ====================

    public int getDefaultX(ScaledResolution sr, int currentWidth) {
        int w = currentWidth > 0 ? currentWidth : targetWidth;
        return (sr.getScaledWidth() - w) / 2;
    }

    public int getDefaultY(ScaledResolution sr) {
        return TOP_MARGIN;
    }

    // ==================== update ====================

    public void update() {
        NotificationManager.getInstance().tickAll();

        if (animatedWidth < 0F) {
            animatedWidth = targetWidth;
            animatedHeight = targetHeight;
        } else {
            animatedWidth  += (targetWidth  - animatedWidth)  * WIDTH_SPEED;
            animatedHeight += (targetHeight - animatedHeight) * HEIGHT_SPEED;
            if (Math.abs(animatedWidth  - targetWidth)  < 0.5F) animatedWidth  = targetWidth;
            if (Math.abs(animatedHeight - targetHeight) < 0.5F) animatedHeight = targetHeight;
        }

        animatedAlpha += (1F - animatedAlpha) * ALPHA_SPEED;
        if (animatedAlpha > 0.99F) animatedAlpha = 1F;

        if (pendingMode != currentMode) {
            contentAlpha += (0F - contentAlpha) * CONTENT_SPEED;
            if (contentAlpha < 0.02F) {
                contentAlpha = 0F;
                currentMode = pendingMode;
            }
        } else {
            contentAlpha += (1F - contentAlpha) * CONTENT_SPEED;
            if (contentAlpha > 0.99F) contentAlpha = 1F;
        }

        NotificationManager.getInstance().purgeExpired();
    }

    // ==================== render ====================

    public void render(int x, int y, int width, int height) {
        int w = (int) animatedWidth;
        int h = (int) animatedHeight;
        if (w <= 0 || h <= 0) return;

        float a = animatedAlpha;
        int bgCol     = applyAlpha(BG, a);
        int borderCol = applyAlpha(BORDER, a);

        RenderUtil.drawRoundedRect(x, y, w, h, RADIUS, bgCol);
        RenderUtil.drawRoundedOutline(x, y, w, h, RADIUS, 1, borderCol);

        float contentA = a * contentAlpha;

        switch (currentMode) {
            case IDLE:     renderIdle(x, y, w, h, contentA);     break;
            case NOTI:     renderNoti(x, y, w, h, contentA);     break;
            case TARGET:   renderTarget(x, y, w, h, contentA);   break;
            case SCAFFOLD: renderScaffold(x, y, w, h, contentA); break;
            case EATING:   renderEating(x, y, w, h, contentA);   break;
        }
    }

    private void renderIdle(int x, int y, int w, int h, float a) {
        if (a < 0.02F) return;

        int textCol  = applyAlpha(TEXT, a);
        int dimCol   = applyAlpha(TEXT_DIM, a);
        int faintCol = applyAlpha(TEXT_FAINT, a);

        int cursorX = x + PADDING;
        int centerY = y + (h - font.FONT_HEIGHT) / 2 + 1;

        int logoY = y + (h - LOGO_SIZE) / 2;
        drawLogo(cursorX, logoY, LOGO_SIZE, a);
        cursorX += LOGO_SIZE + LOGO_GAP;

        int theme = HUD.getInstance().getThemeColor();
        int themeCol = applyAlpha(theme, a);
        font.drawStringWithShadow("Ethereal", cursorX, centerY, themeCol);
        cursorX += font.getStringWidth("Ethereal");

        drawSeparator(cursorX, centerY, faintCol);
        cursorX += SEGMENT_GAP;
        String fps = getFpsText();
        font.drawStringWithShadow(fps, cursorX, centerY, dimCol);
        cursorX += font.getStringWidth(fps);

        String ip = getServerIp();
        if (!ip.isEmpty()) {
            drawSeparator(cursorX, centerY, faintCol);
            cursorX += SEGMENT_GAP;
            font.drawStringWithShadow(ip, cursorX, centerY, dimCol);
            cursorX += font.getStringWidth(ip);
        }

        String player = getPlayerName();
        if (!player.isEmpty()) {
            drawSeparator(cursorX, centerY, faintCol);
            cursorX += SEGMENT_GAP;
            font.drawStringWithShadow(player, cursorX, centerY, textCol);
        }
    }

    private void renderNoti(int x, int y, int w, int h, float a) {
        if (a < 0.02F) return;

        List<Notification> list = NotificationManager.getInstance().getActiveNotifications(MAX_NOTI);
        if (list.isEmpty()) return;

        int rowY = y + (HEIGHT_IDLE - ROW_H) / 2;
        for (int i = 0; i < list.size(); i++) {
            Notification n = list.get(i);
            float itemAlpha = a * n.getAnimationAlpha();
            n.renderInline(x, rowY, w, ROW_H, itemAlpha);
            rowY += ROW_H;
        }
    }

    /** ★ TARGET 模式：委托给 TargetHud.renderInline */
    private void renderTarget(int x, int y, int w, int h, float a) {
        if (a < 0.02F) return;

        HudElement el = HudManager.getInstance().getElement("TargetHud");
        if (el instanceof TargetHud) {
            ((TargetHud) el).renderInline(x, y, w, h, a);
        }
    }

    private void renderScaffold(int x, int y, int w, int h, float a) {
        if (a < 0.02F) return;

        int textCol = applyAlpha(TEXT, a);
        int theme   = applyAlpha(HUD.getInstance().getThemeColor(), a);
        int centerY = y + (h - font.FONT_HEIGHT) / 2 + 1;

        int cursorX = x + PADDING;

        int iconY = y + (h - 14) / 2;
        drawBlockIcon(cursorX, iconY, 14, a);
        cursorX += 14 + 8;

        font.drawStringWithShadow("Scaffold", cursorX, centerY, theme);
        cursorX += font.getStringWidth("Scaffold");

        cursorX += 8;
        int count = getBlockCount();
        String countStr = String.valueOf(count);
        font.drawStringWithShadow(countStr, cursorX, centerY, textCol);

        int barX = x + w - PADDING - BAR_W;
        int barY = y + (h - BAR_H) / 2;
        drawProgressBar(barX, barY, BAR_W, BAR_H, getBlockPercent(), a);
    }

    private void renderEating(int x, int y, int w, int h, float a) {
        if (a < 0.02F) return;

        int textCol = applyAlpha(TEXT, a);
        int theme   = applyAlpha(HUD.getInstance().getThemeColor(), a);
        int centerY = y + (h - font.FONT_HEIGHT) / 2 + 1;

        int cursorX = x + PADDING;

        int iconY = y + (h - 14) / 2;
        drawFoodIcon(cursorX, iconY, 14, a);
        cursorX += 14 + 8;

        font.drawStringWithShadow("Eating", cursorX, centerY, theme);
        cursorX += font.getStringWidth("Eating");

        cursorX += 8;
        String timeStr = getEatingTimeText();
        font.drawStringWithShadow(timeStr, cursorX, centerY, textCol);

        int barX = x + w - PADDING - BAR_W;
        int barY = y + (h - BAR_H) / 2;
        drawProgressBar(barX, barY, BAR_W, BAR_H, getEatingPercent(), a);
    }

    // ==================== 图标 ====================

    private void drawBlockIcon(int x, int y, int size, float a) {
        int color = getHeldBlockColor();
        int fill = applyAlpha((0xAA << 24) | (color & 0x00FFFFFF), a);
        int edge = applyAlpha((0xFF << 24) | (color & 0x00FFFFFF), a);

        RenderUtil.drawRoundedRect(x, y, size, size, 2, fill);

        RenderUtil.drawRect(x + 3, y + 3, size - 6, 1, edge);
        RenderUtil.drawRect(x + 3, y + size - 4, size - 6, 1, edge);
        RenderUtil.drawRect(x + 3, y + 3, 1, size - 6, edge);
        RenderUtil.drawRect(x + size - 4, y + 3, 1, size - 6, edge);
    }

    private void drawFoodIcon(int x, int y, int size, float a) {
        ItemStack held = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        boolean potion = held != null && held.getItem() instanceof ItemPotion;

        int color = potion ? 0xFFD040D0 : 0xFFC08050;
        int fill = applyAlpha((0xAA << 24) | (color & 0x00FFFFFF), a);
        int edge = applyAlpha((0xFF << 24) | (color & 0x00FFFFFF), a);

        RenderUtil.drawRoundedRect(x, y, size, size, 2, fill);

        if (potion) {
            RenderUtil.drawRect(x + size / 2 - 1, y + 3, 2, size - 6, edge);
        } else {
            RenderUtil.drawRect(x + 3, y + size / 2 - 1, size - 6, 2, edge);
        }
    }

    // ==================== 进度条 ====================

    private void drawProgressBar(int x, int y, int w, int h, float percent, float a) {
        percent = MathHelper.clamp_float(percent, 0F, 1F);

        int bg = applyAlpha(BAR_BG, a);
        RenderUtil.drawRoundedRect(x, y, w, h, h / 2F, bg);

        int fillW = (int) (w * percent);
        if (fillW > 1) {
            int theme = HUD.getInstance().getThemeColor();
            int fillCol = applyAlpha(theme, a);
            RenderUtil.drawRoundedRect(x, y, fillW, h, h / 2F, fillCol);
        }
    }

    // ==================== Logo ====================

    private void drawLogo(int x, int y, int size, float alpha) {
        int theme = HUD.getInstance().getThemeColor();

        int bg = applyAlpha((LOGO_BG_A << 24) | (theme & 0x00FFFFFF), alpha);
        RenderUtil.drawRoundedRect(x, y, size, size, 4, bg);

        int glyph = applyAlpha(0xFFFFFFFF, alpha);
        int pad = 4;
        int ix = x + pad;
        int iy = y + pad;
        int iw = size - pad * 2;
        int ih = size - pad * 2;

        int barH = 2;
        int stemW = 2;

        RenderUtil.drawRect(ix, iy, stemW, ih, glyph);
        RenderUtil.drawRect(ix, iy, iw, barH, glyph);
        RenderUtil.drawRect(ix, iy + (ih - barH) / 2, (int) (iw * 0.75F), barH, glyph);
        RenderUtil.drawRect(ix, iy + ih - barH, iw, barH, glyph);
    }

    private void drawSeparator(int x, int centerY, int color) {
        int r = 2;
        RenderUtil.drawRoundedRect(x + 3, centerY + 2, r, r, r / 2F, color);
    }

    // ==================== 数据 ====================

    private String getFpsText() {
        return mc.getDebugFPS() + " FPS";
    }

    private String getServerIp() {
        if (mc.getCurrentServerData() != null) {
            String ip = mc.getCurrentServerData().serverIP;
            return ip == null ? "" : ip;
        }
        if (mc.isSingleplayer()) return "Singleplayer";
        return "";
    }

    private String getPlayerName() {
        if (mc.thePlayer == null) return "";
        String n = mc.thePlayer.getName();
        return n == null ? "" : n;
    }

    private int getBlockCount() {
        if (Scaffold.INSTANCE == null) return 0;
        return Scaffold.INSTANCE.getBlockCount();
    }

    private float getBlockPercent() {
        return MathHelper.clamp_float(getBlockCount() / 576F, 0F, 1F);
    }

    private boolean isEatingOrDrinking() {
        if (mc.thePlayer == null) return false;
        if (!mc.thePlayer.isUsingItem()) return false;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return false;
        return held.getItem() instanceof ItemFood || held.getItem() instanceof ItemPotion;
    }

    private float getEatingPercent() {
        if (mc.thePlayer == null) return 0F;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return 0F;
        int max = held.getMaxItemUseDuration();
        if (max <= 0) return 0F;
        int remaining = mc.thePlayer.getItemInUseCount();
        return MathHelper.clamp_float(1F - remaining / (float) max, 0F, 1F);
    }

    private String getEatingTimeText() {
        if (mc.thePlayer == null) return "0.0s";
        int remaining = mc.thePlayer.getItemInUseCount();
        float seconds = remaining / 20F;
        return String.format("%.1fs", seconds);
    }

    private int getHeldBlockColor() {
        if (mc.thePlayer == null) return 0xFF888888;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof net.minecraft.item.ItemBlock)) {
            return 0xFF888888;
        }
        net.minecraft.block.Block block =
                ((net.minecraft.item.ItemBlock) held.getItem()).getBlock();
        try {
            int color = block.getMaterial().getMaterialMapColor().colorValue;
            if (color == 0) return 0xFF888888;
            return 0xFF000000 | (color & 0x00FFFFFF);
        } catch (Throwable t) {
            return 0xFF888888;
        }
    }

    // ==================== 工具 ====================

    private static int applyAlpha(int color, float factor) {
        factor = MathHelper.clamp_float(factor, 0F, 1F);
        int a = (color >>> 24);
        int na = MathHelper.clamp_int((int) (a * factor), 0, 255);
        return (na << 24) | (color & 0x00FFFFFF);
    }
}