package cn.ethereal.hud.impl;

import cn.ethereal.hud.HudElement;
import cn.ethereal.module.render.HUD;
import cn.ethereal.util.render.RenderUtil;
import net.minecraft.client.gui.ScaledResolution;

public class UsernameHud extends HudElement {

    private static final int PADDING = 5;
    private static final int RADIUS = 4;
    private static final int BAR_WIDTH = 2;

    private static final int BG = 0xB0101010;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DIM = 0xFF888888;

    public UsernameHud() {
        super("Username", "用户名 + 服务器",
                HUD.getInstance().usernameShow,
                HUD.getInstance().usernamePosX,
                HUD.getInstance().usernamePosY);
    }

    @Override
    public int[] measure() {
        // ★ 防 NPE
        if (mc.thePlayer == null) {
            width = 0;
            height = 0;
            return new int[] { 0, 0 };
        }
        String name = mc.thePlayer.getName();
        String server = getServerIp();
        int maxW = Math.max(font.getStringWidth(name), font.getStringWidth(server));
        width = maxW + BAR_WIDTH + 4 + PADDING * 2;
        height = font.FONT_HEIGHT * 2 + 8;
        return new int[] { width, height };
    }

    @Override
    public int getDefaultX(ScaledResolution sr) {
        return 4;
    }

    @Override
    public int getDefaultY(ScaledResolution sr) {
        return sr.getScaledHeight() - height - 4;
    }

    @Override
    public void render(int x, int y) {
        if (mc.thePlayer == null) return;

        int themeColor = HUD.getInstance().getThemeColor();

        RenderUtil.drawRoundedRect(x, y, width, height, RADIUS, BG);
        RenderUtil.drawRoundedRect(x + 2, y + 3, BAR_WIDTH, height - 6, BAR_WIDTH / 2F, themeColor);

        String name = mc.thePlayer.getName();
        String server = getServerIp();

        int textX = x + BAR_WIDTH + 6;
        font.drawStringWithShadow(name, textX, y + 4, TEXT);
        font.drawStringWithShadow(server, textX, y + 4 + font.FONT_HEIGHT, DIM);
    }

    private String getServerIp() {
        if (mc.getCurrentServerData() == null) return "Singleplayer";
        return mc.getCurrentServerData().serverIP;
    }
}