package cn.ethereal.hud.impl;

import cn.ethereal.hud.HudElement;
import cn.ethereal.module.render.HUD;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.ModeValue;
import cn.ethereal.util.render.RenderUtil;
import net.minecraft.client.gui.ScaledResolution;

public class WatermarkHud extends HudElement {

    private static final int PADDING = 5;
    private static final int RADIUS = 4;
    private static final int BAR_WIDTH = 2;
    private static final int BG = 0xB0101010;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DIM = 0xFF888888;

    private final ModernWatermarkHud modern = new ModernWatermarkHud();

    public WatermarkHud() {
        super("Watermark", "客户端水印",
                HUD.getInstance().watermarkShow,
                HUD.getInstance().watermarkPosX,
                HUD.getInstance().watermarkPosY);
    }

    private BooleanValue parentheses() { return HUD.getInstance().watermarkParentheses; }
    private BooleanValue showFps()     { return HUD.getInstance().watermarkFps; }
    private ModeValue    mode()        { return HUD.getInstance().watermarkMode; }

    @Override
    public int[] measure() {
        if (isModern()) {
            int[] m = modern.measure();
            width = m[0];
            height = m[1];
            return m;
        }
        return measureClassic();
    }

    private int[] measureClassic() {
        String mainText = (parentheses().getValue() ? "[Ethereal]" : "Ethereal");
        String fpsText = showFps().getValue() ? (mc.getDebugFPS() + " FPS") : "";
        int mainW = font.getStringWidth(mainText);
        int fpsW = fpsText.isEmpty() ? 0 : font.getStringWidth(fpsText) + 6;
        width = mainW + fpsW + PADDING * 2 + 4;
        height = font.FONT_HEIGHT + PADDING * 2 - 2;
        return new int[] { width, height };
    }

    @Override
    public int getDefaultX(ScaledResolution sr) {
        if (isModern()) return modern.getDefaultX(sr, width);
        return 4;
    }

    @Override
    public int getDefaultY(ScaledResolution sr) {
        if (isModern()) return modern.getDefaultY(sr);
        return 4;
    }

    @Override
    public void update() {
        if (isModern()) modern.update();
    }

    @Override
    public void render(int x, int y) {
        if (isModern()) {
            modern.render(x, y, width, height);
            return;
        }
        renderClassic(x, y);
    }

    private void renderClassic(int x, int y) {
        int themeColor = HUD.getInstance().getThemeColor();

        RenderUtil.drawRoundedRect(x, y, width, height, RADIUS, BG);
        RenderUtil.drawRoundedRect(x + 2, y + 3, BAR_WIDTH, height - 6, BAR_WIDTH / 2F, themeColor);

        String mainText = (parentheses().getValue() ? "[Ethereal]" : "Ethereal");
        String fpsText = showFps().getValue() ? (mc.getDebugFPS() + " FPS") : "";

        int textX = x + 2 + BAR_WIDTH + 4;
        int textY = y + (height - font.FONT_HEIGHT) / 2 + 1;

        font.drawStringWithShadow(mainText, textX, textY, TEXT);

        if (!fpsText.isEmpty()) {
            int fpsX = x + width - PADDING - font.getStringWidth(fpsText);
            font.drawStringWithShadow(fpsText, fpsX, textY, DIM);
        }
    }

    private boolean isModern() {
        return "Modern".equals(mode().getValue());
    }
}