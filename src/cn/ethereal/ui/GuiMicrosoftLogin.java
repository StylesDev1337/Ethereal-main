package cn.ethereal.ui;

import cn.ethereal.account.MicrosoftLoginManager;
import cn.ethereal.util.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * 微软设备代码流登录窗口。
 * 状态由 MicrosoftLoginManager 提供，本类只负责绘制和交互。
 */
public class GuiMicrosoftLogin extends GuiScreen {

    // ==================== 布局 ====================
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 200;
    private static final int RADIUS  = 8;

    // ==================== 颜色 ====================
    private static final int MASK       = 0x99000000;
    private static final int BG         = 0xF0181818;
    private static final int BORDER     = 0xFF2A2A2A;
    private static final int ACCENT     = 0xFF4A9EFF;
    private static final int SUCCESS    = 0xFF4ADE80;
    private static final int ERROR      = 0xFFEF4444;
    private static final int WARN       = 0xFFFBBA24;
    private static final int TEXT       = 0xFFFFFFFF;
    private static final int TEXT_DIM   = 0xFFB0B0B0;
    private static final int CODE_BG    = 0xFF252525;
    private static final int BTN_BG     = 0xFF252525;
    private static final int BTN_HOVER  = 0xFF333333;
    private static final int BTN_DISABLED = 0xFF1A1A1A;

    private final GuiScreen parent;
    private int panelX, panelY;

    // 按钮
    private static final int BTN_W = 100;
    private static final int BTN_H = 22;
    private int primaryX, primaryY;
    private int secondaryX, secondaryY;

    // 成功/失败后自动关闭的计时
    private long successAt = 0L;
    private static final long AUTO_CLOSE_DELAY = 1500L;

    public GuiMicrosoftLogin(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;

        secondaryX = panelX + PANEL_W / 2 - BTN_W - 4;
        secondaryY = panelY + PANEL_H - BTN_H - 12;
        primaryX   = panelX + PANEL_W / 2 + 4;
        primaryY   = secondaryY;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 遮罩
        drawRect(0, 0, width, height, MASK);

        // 面板
        RenderUtil.drawRoundedRect(panelX, panelY, PANEL_W, PANEL_H, RADIUS, BG);
        RenderUtil.drawRoundedOutline(panelX, panelY, PANEL_W, PANEL_H, RADIUS, 1, BORDER);

        // 标题
        fontRendererObj.drawStringWithShadow("Microsoft Login",
                panelX + 16, panelY + 12, TEXT);

        MicrosoftLoginManager.State state = MicrosoftLoginManager.getState();

        // 状态点
        int dotColor = stateColor(state);
        RenderUtil.drawRoundedRect(panelX + PANEL_W - 24, panelY + 14, 8, 8, 4, dotColor);

        switch (state) {
            case IDLE:       drawIdle(mouseX, mouseY); break;
            case REQUESTING: drawRequesting(); break;
            case WAITING:    drawWaiting(mouseX, mouseY); break;
            case SUCCESS:    drawSuccess(); break;
            case FAILED:     drawFailed(mouseX, mouseY); break;
        }

        // 自动关闭
        if (state == MicrosoftLoginManager.State.SUCCESS) {
            if (successAt == 0L) successAt = System.currentTimeMillis();
            if (System.currentTimeMillis() - successAt > AUTO_CLOSE_DELAY) {
                mc.displayGuiScreen(parent);
            }
        }
    }

    // ==================== 各状态绘制 ====================

    private void drawIdle(int mouseX, int mouseY) {
        fontRendererObj.drawStringWithShadow("Ready to start login.",
                panelX + 16, panelY + 44, TEXT_DIM);
        fontRendererObj.drawStringWithShadow("Press \"Login\" to begin.",
                panelX + 16, panelY + 58, TEXT_DIM);

        drawButton(primaryX, primaryY, "Login", true, mouseX, mouseY);
        drawButton(secondaryX, secondaryY, "Back", true, mouseX, mouseY);
    }

    private void drawRequesting() {
        String text = "Requesting device code...";
        int w = fontRendererObj.getStringWidth(text);
        fontRendererObj.drawStringWithShadow(text,
                panelX + (PANEL_W - w) / 2,
                panelY + PANEL_H / 2 - 4,
                TEXT_DIM);
    }

    private void drawWaiting(int mouseX, int mouseY) {
        fontRendererObj.drawStringWithShadow("1. Open this link in your browser:",
                panelX + 16, panelY + 40, TEXT_DIM);

        String link = MicrosoftLoginManager.getVerificationUri();
        if (link == null) link = "...";
        fontRendererObj.drawStringWithShadow(link,
                panelX + 16, panelY + 54, ACCENT);

        fontRendererObj.drawStringWithShadow("2. Enter this code:",
                panelX + 16, panelY + 76, TEXT_DIM);

        // 代码框
        int codeX = panelX + 16;
        int codeY = panelY + 90;
        int codeW = PANEL_W - 32;
        int codeH = 42;
        RenderUtil.drawRoundedRect(codeX, codeY, codeW, codeH, 6, CODE_BG);
        RenderUtil.drawRoundedOutline(codeX, codeY, codeW, codeH, 6, 1, BORDER);

        String code = MicrosoftLoginManager.getDeviceCode();
        if (code == null) code = "----";
        // 放大：手动间距
        int codeTextW = getSpacedWidth(code, 2);
        drawSpacedString(code, codeX + (codeW - codeTextW) / 2, codeY + (codeH - 8) / 2, 2, ACCENT);

        // 提示
        String hint = "Waiting for authorization...";
        int hintW = fontRendererObj.getStringWidth(hint);
        fontRendererObj.drawStringWithShadow(hint,
                panelX + (PANEL_W - hintW) / 2,
                codeY + codeH + 8,
                TEXT_DIM);

        // 按钮：复制代码 / 取消
        drawButton(primaryX, primaryY, "Copy Code", true, mouseX, mouseY);
        drawButton(secondaryX, secondaryY, "Cancel", true, mouseX, mouseY);
    }

    private void drawSuccess() {
        String name = MicrosoftLoginManager.getSuccessName();
        String text = "Welcome, " + (name == null ? "player" : name) + "!";
        int w = fontRendererObj.getStringWidth(text);
        fontRendererObj.drawStringWithShadow(text,
                panelX + (PANEL_W - w) / 2,
                panelY + PANEL_H / 2 - 4,
                SUCCESS);
    }

    private void drawFailed(int mouseX, int mouseY) {
        fontRendererObj.drawStringWithShadow("Login failed",
                panelX + 16, panelY + 44, ERROR);

        String err = MicrosoftLoginManager.getErrorMessage();
        if (err == null) err = "Unknown error";
        // 简单换行
        drawWrapped(err, panelX + 16, panelY + 62, PANEL_W - 32, TEXT_DIM);

        drawButton(primaryX, primaryY, "Retry", true, mouseX, mouseY);
        drawButton(secondaryX, secondaryY, "Back", true, mouseX, mouseY);
    }

    // ==================== 按钮 ====================

    private void drawButton(int x, int y, String label, boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && isInRect(mouseX, mouseY, x, y, BTN_W, BTN_H);
        int bg = !enabled ? BTN_DISABLED : (hover ? BTN_HOVER : BTN_BG);
        RenderUtil.drawRoundedRect(x, y, BTN_W, BTN_H, 4, bg);

        int tw = fontRendererObj.getStringWidth(label);
        int color = enabled ? TEXT : TEXT_DIM;
        fontRendererObj.drawStringWithShadow(label,
                x + (BTN_W - tw) / 2,
                y + (BTN_H - 8) / 2,
                color);
    }

    // ==================== 交互 ====================

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;

        MicrosoftLoginManager.State state = MicrosoftLoginManager.getState();

        if (state == MicrosoftLoginManager.State.IDLE) {
            if (isInRect(mouseX, mouseY, primaryX, primaryY, BTN_W, BTN_H)) {
                MicrosoftLoginManager.startLogin();
                return;
            }
            if (isInRect(mouseX, mouseY, secondaryX, secondaryY, BTN_W, BTN_H)) {
                mc.displayGuiScreen(parent);
                return;
            }
        } else if (state == MicrosoftLoginManager.State.WAITING) {
            if (isInRect(mouseX, mouseY, primaryX, primaryY, BTN_W, BTN_H)) {
                String code = MicrosoftLoginManager.getDeviceCode();
                if (code != null) setClipboardString(code);
                return;
            }
            if (isInRect(mouseX, mouseY, secondaryX, secondaryY, BTN_W, BTN_H)) {
                // 取消：现在没有 cancel API，只能退回菜单（后台线程还在跑）
                mc.displayGuiScreen(parent);
                return;
            }
        } else if (state == MicrosoftLoginManager.State.FAILED) {
            if (isInRect(mouseX, mouseY, primaryX, primaryY, BTN_W, BTN_H)) {
                MicrosoftLoginManager.reset();
                MicrosoftLoginManager.startLogin();
                return;
            }
            if (isInRect(mouseX, mouseY, secondaryX, secondaryY, BTN_W, BTN_H)) {
                MicrosoftLoginManager.reset();
                mc.displayGuiScreen(parent);
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            // ESC 直接退回，不管状态（后台线程继续跑）
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ==================== 工具 ====================

    private int stateColor(MicrosoftLoginManager.State s) {
        switch (s) {
            case SUCCESS: return SUCCESS;
            case FAILED:  return ERROR;
            case WAITING: return WARN;
            case REQUESTING: return ACCENT;
            default: return TEXT_DIM;
        }
    }

    /** 画带字间距的字符串，返回总宽 */
    private int getSpacedWidth(String s, int spacing) {
        if (s.isEmpty()) return 0;
        return fontRendererObj.getStringWidth(s) + (s.length() - 1) * spacing;
    }

    private void drawSpacedString(String s, int x, int y, int spacing, int color) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            fontRendererObj.drawStringWithShadow(ch, cx, y, color);
            cx += fontRendererObj.getStringWidth(ch) + spacing;
        }
    }

    /** 简单按宽度换行 */
    private void drawWrapped(String text, int x, int y, int maxW, int color) {
        StringBuilder line = new StringBuilder();
        int cy = y;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (fontRendererObj.getStringWidth(line.toString() + c) > maxW) {
                fontRendererObj.drawStringWithShadow(line.toString(), x, cy, color);
                line = new StringBuilder();
                cy += fontRendererObj.FONT_HEIGHT + 2;
            }
            line.append(c);
        }
        if (line.length() > 0) {
            fontRendererObj.drawStringWithShadow(line.toString(), x, cy, color);
        }
    }

    private static boolean isInRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}