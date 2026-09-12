package cn.ethereal.ui;

import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.module.ModuleManager;
import cn.ethereal.ui.controls.Control;
import cn.ethereal.ui.controls.Dropdown;
import cn.ethereal.ui.controls.Slider;
import cn.ethereal.util.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGUI extends GuiScreen {

    // ==================== 布局 ====================
    private static final int PANEL_WIDTH  = 520;
    private static final int PANEL_HEIGHT = 300;
    private static final int TAB_WIDTH    = 100;
    private static final int MODULE_WIDTH = 180;
    private static final int PADDING      = 8;
    private static final int ROW_HEIGHT   = 22;

    // ==================== 颜色 ====================
    private static final int BG            = 0xFF0F0F0F;
    private static final int BG_PANEL      = 0xFF181818;
    private static final int BG_HOVER      = 0xFF252525;
    private static final int BG_SELECTED   = 0xFF2E2E2E;
    private static final int BORDER        = 0xFF2A2A2A;
    private static final int ACCENT        = 0xFF4A9EFF;
    private static final int SUCCESS       = 0xFF4ADE80;
    private static final int OFF_COLOR     = 0xFF3A3A3A;
    private static final int TEXT          = 0xFFEEEEEE;
    private static final int TEXT_DIM      = 0xFF888888;

    // ==================== 动画 ====================
    private static final float OPEN_SPEED  = 0.28F;
    private static final float TAB_SPEED   = 0.35F;
    private static final float DOT_SPEED   = 0.30F;
    private static final float SCALE_MIN   = 0.80F;
    private static final int   MASK_ALPHA  = 0xB0;

    // ==================== 滚动 ====================
    private static final float SCROLL_SPEED = 28F;       // 每格滚轮滚动的像素
    private float settingsScroll = 0F;                   // 右侧设置区滚动偏移
    private float settingsScrollTarget = 0F;             // 目标（缓动用）
    private float maxSettingsScroll = 0F;                // 本帧算出来的最大滚动量
    private static final float SCROLL_LERP = 0.35F;      // 滚动缓动

    // ==================== 状态 ====================
    private float openProgress = 0F;
    private boolean closing = false;

    private float tabSliderY = -1F;

    private static final Map<Module, Float> DOT_CACHE = new HashMap<>();

    private int panelX, panelY;
    private Category currentCategory = Category.COMBAT;
    private Module selectedModule = null;

    @Override
    public void initGui() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;

        openProgress = 0F;
        closing = false;
        tabSliderY = -1F;
        DOT_CACHE.clear();

        // ★ 重置滚动
        settingsScroll = 0F;
        settingsScrollTarget = 0F;
        maxSettingsScroll = 0F;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // ---- 动画推进 ----
        if (closing) {
            openProgress += (0F - openProgress) * OPEN_SPEED;
            if (openProgress < 0.005F) {
                openProgress = 0F;
                mc.displayGuiScreen(null);
                return;
            }
        } else {
            openProgress += (1F - openProgress) * OPEN_SPEED;
            if (openProgress > 0.995F) openProgress = 1F;
        }

        float eased = easeOutCubic(openProgress);

        // ---- 滚动缓动 ----
        settingsScroll += (settingsScrollTarget - settingsScroll) * SCROLL_LERP;
        if (Math.abs(settingsScrollTarget - settingsScroll) < 0.5F) {
            settingsScroll = settingsScrollTarget;
        }

        // ---- 遮罩 ----
        int maskAlpha = (int) (MASK_ALPHA * eased);
        drawRect(0, 0, width, height, (maskAlpha << 24));

        // ---- 缩放 ----
        float scale = SCALE_MIN + (1F - SCALE_MIN) * eased;
        int scaledW = (int) (PANEL_WIDTH * scale);
        int scaledH = (int) (PANEL_HEIGHT * scale);
        int scaledX = panelX + (PANEL_WIDTH - scaledW) / 2;
        int scaledY = panelY + (PANEL_HEIGHT - scaledH) / 2;

        // ---- 颜色 ----
        int alpha = (int) (0xFF * eased);
        int bgColor          = withAlpha(BG, alpha);
        int panelColor       = withAlpha(BG_PANEL, alpha);
        int borderColor      = withAlpha(BORDER, alpha);
        int accentColor      = withAlpha(ACCENT, alpha);
        int successColor     = withAlpha(SUCCESS, alpha);
        int offColor         = withAlpha(OFF_COLOR, alpha);
        int textColor        = withAlpha(TEXT, alpha);
        int textDimColor     = withAlpha(TEXT_DIM, alpha);
        int bgHoverColor     = withAlpha(BG_HOVER, alpha);
        int bgSelectedColor  = withAlpha(BG_SELECTED, alpha);

        // ---- 面板 ----
        RenderUtil.drawRoundedRect(scaledX, scaledY, scaledW, scaledH, 6, bgColor);
        RenderUtil.drawRoundedOutline(scaledX, scaledY, scaledW, scaledH, 6, 1, borderColor);

        // 左侧栏
        RenderUtil.drawRoundedRect(scaledX + 1, scaledY + 1, TAB_WIDTH - 1, scaledH - 2, 5, panelColor);

        // ---- 三块内容 ----
        drawTabs(mouseX, mouseY, scaledX, scaledY, scaledH,
                accentColor, bgHoverColor, bgSelectedColor, textColor, textDimColor);
        drawModules(mouseX, mouseY, scaledX, scaledY,
                bgHoverColor, bgSelectedColor, textColor, textDimColor,
                successColor, offColor, accentColor);

        // ★ 设置区用 scissor 裁剪
        drawSettingsClipped(mouseX, mouseY, scaledX, scaledY,
                textColor, textDimColor, borderColor, panelColor);
    }

    // ==================== 左侧 Tab ====================

    private void drawTabs(int mouseX, int mouseY, int px, int py, int ph,
                          int accentColor, int bgHoverColor, int bgSelectedColor,
                          int textColor, int textDimColor) {
        fontRendererObj.drawStringWithShadow("ETHEREAL", px + 14, py + 14, accentColor);

        int baseTabY = py + 38;

        float targetSliderY = getTabY(currentCategory, baseTabY);
        if (tabSliderY < 0) {
            tabSliderY = targetSliderY;
        } else {
            tabSliderY += (targetSliderY - tabSliderY) * TAB_SPEED;
            if (Math.abs(tabSliderY - targetSliderY) < 0.05F) tabSliderY = targetSliderY;
        }

        RenderUtil.drawRoundedRect(px + 6, (int) tabSliderY, TAB_WIDTH - 12, ROW_HEIGHT, 3, bgSelectedColor);
        RenderUtil.drawRoundedRect(px + 6, (int) tabSliderY + 5, 2, ROW_HEIGHT - 10, 1, accentColor);

        int tabY = baseTabY;
        for (Category category : Category.values()) {
            boolean selected = currentCategory == category;
            boolean hovered = isInRect(mouseX, mouseY, px + 6, tabY, TAB_WIDTH - 12, ROW_HEIGHT);

            if (hovered && !selected) {
                RenderUtil.drawRoundedRect(px + 6, tabY, TAB_WIDTH - 12, ROW_HEIGHT, 3, bgHoverColor);
            }

            String name = capitalize(category.name());
            int color = selected ? textColor : textDimColor;
            fontRendererObj.drawStringWithShadow(name, px + 18, tabY + 7, color);

            tabY += ROW_HEIGHT + 2;
        }

        fontRendererObj.drawStringWithShadow("v1.0", px + 14, py + ph - 16, textDimColor);
    }

    private float getTabY(Category category, int baseY) {
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            if (cats[i] == category) {
                return baseY + i * (ROW_HEIGHT + 2);
            }
        }
        return baseY;
    }

    // ==================== 中间模块列表 ====================

    private void drawModules(int mouseX, int mouseY, int px, int py,
                             int bgHoverColor, int bgSelectedColor,
                             int textColor, int textDimColor,
                             int successColor, int offColor, int accentColor) {
        int x = px + TAB_WIDTH + PADDING;
        int y = py + PADDING;
        int w = MODULE_WIDTH - PADDING * 2;

        fontRendererObj.drawStringWithShadow("MODULES", x, y, textDimColor);
        y += 18;

        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(currentCategory);

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int rowY = y + i * (ROW_HEIGHT + 2);

            boolean hovered = isInRect(mouseX, mouseY, x, rowY, w, ROW_HEIGHT);
            boolean selected = selectedModule == module;

            int bgColor = 0;
            if (selected) bgColor = bgSelectedColor;
            else if (hovered) bgColor = bgHoverColor;

            if (bgColor != 0) {
                RenderUtil.drawRoundedRect(x, rowY, w, ROW_HEIGHT, 3, bgColor);
            }
            if (selected) {
                RenderUtil.drawRoundedRect(x, rowY + 5, 2, ROW_HEIGHT - 10, 1, accentColor);
            }

            int nameColor = module.isEnabled() ? textColor : textDimColor;
            fontRendererObj.drawStringWithShadow(module.getName(), x + 10, rowY + 7, nameColor);

            float dotProgress = DOT_CACHE.getOrDefault(module, module.isEnabled() ? 1F : 0F);
            float target = module.isEnabled() ? 1F : 0F;
            dotProgress += (target - dotProgress) * DOT_SPEED;
            if (Math.abs(dotProgress - target) < 0.01F) dotProgress = target;
            DOT_CACHE.put(module, dotProgress);

            int dotColor = lerpColor(offColor, successColor, dotProgress);
            RenderUtil.drawRoundedRect(x + w - 12, rowY + 8, 6, 6, 3, dotColor);
        }

        if (modules.isEmpty()) {
            fontRendererObj.drawStringWithShadow("No modules", x + 10, y + 6, textDimColor);
        }
    }

    // ==================== 右侧设置（带滚动 + 裁剪） ====================

    private void drawSettingsClipped(int mouseX, int mouseY, int px, int py,
                                     int textColor, int textDimColor, int borderColor,
                                     int panelColor) {
        int x = px + TAB_WIDTH + MODULE_WIDTH + PADDING;
        int y = py + PADDING;
        int w = PANEL_WIDTH - TAB_WIDTH - MODULE_WIDTH - PADDING * 2;
        int h = PANEL_HEIGHT - PADDING * 2;

        // 面板未选中模块
        if (selectedModule == null) {
            fontRendererObj.drawStringWithShadow("Select a module",
                    x + 8, py + PANEL_HEIGHT / 2 - 12, textDimColor);
            fontRendererObj.drawStringWithShadow("to edit settings",
                    x + 8, py + PANEL_HEIGHT / 2, textDimColor);
            maxSettingsScroll = 0F;
            settingsScroll = 0F;
            settingsScrollTarget = 0F;
            return;
        }

        // 头部（标题 + 分隔线）在裁剪区外，固定不动
        fontRendererObj.drawStringWithShadow(selectedModule.getName(), x + 8, y, textColor);
        String desc = selectedModule.getCategory().name();
        fontRendererObj.drawStringWithShadow(desc, x + 8, y + 12, textDimColor);
        RenderUtil.drawRect(x + 8, y + 26, w - 16, 1, borderColor);

        // 控件从 y + 34 开始
        int headerH = 34;
        int contentTop = y + headerH;
        int contentH = h - headerH - 4;   // 留 4px 底部余量
        int contentBottom = contentTop + contentH;

        List<Control> controls = selectedModule.getControls();

        // ★ 计算总内容高度，确定 maxScroll
        int totalH = 0;
        for (Control control : controls) {
            totalH += control.height + 6;
        }
        if (totalH > 0) totalH -= 6;   // 最后一行不留 gap

        maxSettingsScroll = Math.max(0F, totalH - contentH);

        // 夹住滚动值
        if (settingsScrollTarget < 0F) settingsScrollTarget = 0F;
        if (settingsScrollTarget > maxSettingsScroll) settingsScrollTarget = maxSettingsScroll;
        if (settingsScroll < 0F) settingsScroll = 0F;
        if (settingsScroll > maxSettingsScroll) settingsScroll = maxSettingsScroll;

        // ★ 开启裁剪
        RenderUtil.enableScissor(x, contentTop, w, contentH);

        // 布局控件（用滚动偏移）
        int controlY = contentTop - (int) settingsScroll;
        for (Control control : controls) {
            control.setPosition(x + 8, controlY);
            control.setWidth(w - 16);
            controlY += control.height + 6;
        }

        // 先画非展开 dropdown，再画展开的（保证展开的在上层）
        for (Control control : controls) {
            if (isExpandedDropdown(control)) continue;
            control.draw(mouseX, mouseY, 0);
        }
        for (Control control : controls) {
            if (!isExpandedDropdown(control)) continue;
            control.draw(mouseX, mouseY, 0);
        }

        // ★ 关闭裁剪
        RenderUtil.disableScissor();

        // ★ 画滚动条（如果有必要）
        if (maxSettingsScroll > 0F) {
            int barX = x + w - 3;
            int barTrackY = contentTop;
            int barTrackH = contentH;
            int barH = Math.max(20, (int) (barTrackH * (contentH / (float) totalH)));
            float progress = settingsScroll / maxSettingsScroll;
            int barY = barTrackY + (int) ((barTrackH - barH) * progress);

            RenderUtil.drawRoundedRect(barX, barTrackY, 2, barTrackH, 1, withAlpha(BORDER, 0x60));
            RenderUtil.drawRoundedRect(barX, barY, 2, barH, 1, withAlpha(ACCENT, 0xFF));
        }
    }

    private boolean isExpandedDropdown(Control control) {
        return control instanceof Dropdown && ((Dropdown) control).isExpanded();
    }

    // ==================== 交互 ====================

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing) return;

        // 1. 展开的 dropdown 优先拦截
        if (selectedModule != null) {
            for (Control control : selectedModule.getControls()) {
                if (control instanceof Dropdown) {
                    Dropdown dd = (Dropdown) control;
                    if (dd.isExpanded() && dd.isInFullArea(mouseX, mouseY)) {
                        dd.mouseClicked(mouseX, mouseY, mouseButton);
                        return;
                    }
                }
            }
            // 2. 其它控件（只有落在裁剪区内的才响应）
            if (isInSettingsContent(mouseX, mouseY)) {
                for (Control control : selectedModule.getControls()) {
                    control.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }

        // 3. Tab 点击
        int tabY = panelY + 38;
        for (Category category : Category.values()) {
            if (isInRect(mouseX, mouseY, panelX + 6, tabY, TAB_WIDTH - 12, ROW_HEIGHT)) {
                currentCategory = category;
                selectedModule = null;
                settingsScroll = 0F;
                settingsScrollTarget = 0F;
                return;
            }
            tabY += ROW_HEIGHT + 2;
        }

        // 4. 模块点击
        Module clicked = getModuleAt(mouseX, mouseY);
        if (clicked != null) {
            if (mouseButton == 0) {
                clicked.toggle();
            } else if (mouseButton == 1) {
                selectedModule = clicked;
                settingsScroll = 0F;
                settingsScrollTarget = 0F;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (closing) return;
        if (selectedModule != null) {
            for (Control control : selectedModule.getControls()) {
                control.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (closing) return;
        if (selectedModule != null) {
            for (Control control : selectedModule.getControls()) {
                if (control instanceof Slider) {
                    ((Slider) control).mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
                }
            }
        }
    }

    /**
     * ★ 鼠标滚轮
     */
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        if (closing) return;
        if (selectedModule == null) return;
        if (maxSettingsScroll <= 0F) return;

        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) return;

        // 鼠标必须在右侧设置区
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (!isInSettingsContent(mouseX, mouseY)) return;

        // 滚轮向上 = +120，向下 = -120
        float delta = dWheel > 0 ? -SCROLL_SPEED : SCROLL_SPEED;
        settingsScrollTarget += delta;
    }

    @Override
    public void onGuiClosed() {
        if (closing) return;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            startClosing();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    public void startClosing() {
        if (closing) return;
        closing = true;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ==================== 工具 ====================

    /** 判断点是否落在右侧设置区内容范围内 */
    private boolean isInSettingsContent(int mouseX, int mouseY) {
        int x = panelX + TAB_WIDTH + MODULE_WIDTH + PADDING;
        int y = panelY + PADDING;
        int w = PANEL_WIDTH - TAB_WIDTH - MODULE_WIDTH - PADDING * 2;
        int h = PANEL_HEIGHT - PADDING * 2;
        int contentTop = y + 34;
        int contentH = h - 34 - 4;
        return isInRect(mouseX, mouseY, x, contentTop, w, contentH);
    }

    private Module getModuleAt(int mouseX, int mouseY) {
        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(currentCategory);
        int x = panelX + TAB_WIDTH + PADDING;
        int y = panelY + PADDING + 18;
        int w = MODULE_WIDTH - PADDING * 2;

        for (int i = 0; i < modules.size(); i++) {
            int rowY = y + i * (ROW_HEIGHT + 2);
            if (isInRect(mouseX, mouseY, x, rowY, w, ROW_HEIGHT)) {
                return modules.get(i);
            }
        }
        return null;
    }

    private static boolean isInRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private static float easeOutCubic(float t) {
        return 1F - (float) Math.pow(1F - t, 3);
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