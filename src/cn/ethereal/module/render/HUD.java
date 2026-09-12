package cn.ethereal.module.render;

import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.Render2DEvent;
import cn.ethereal.hud.HudManager;
import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.ModeValue;
import cn.ethereal.ui.values.NumberValue;
import org.lwjgl.input.Keyboard;

public class HUD extends Module {

    private static HUD INSTANCE;

    // ==================== 主题 ====================
    public final ModeValue theme;

    // ==================== Watermark ====================
    public final BooleanValue watermarkShow;
    public final NumberValue watermarkPosX;
    public final NumberValue watermarkPosY;
    public final ModeValue    watermarkMode;
    public final BooleanValue watermarkParentheses;
    public final BooleanValue watermarkFps;

    // ==================== Username ====================
    public final BooleanValue usernameShow;
    public final NumberValue usernamePosX;
    public final NumberValue usernamePosY;

    // ==================== ArrayList ====================
    public final BooleanValue arrayListShow;
    public final NumberValue arrayListPosX;
    public final NumberValue arrayListPosY;
    public final BooleanValue arrayListBackground;

    // ==================== Scoreboard ====================
    public final BooleanValue scoreboardShow;
    public final NumberValue scoreboardPosX;
    public final NumberValue scoreboardPosY;

    // ==================== TargetHud ====================
    public final BooleanValue targetHudShow;
    public final NumberValue targetHudPosX;
    public final NumberValue targetHudPosY;

    public HUD() {
        super("HUD", Keyboard.KEY_NONE, Category.RENDER, true, true);
        INSTANCE = this;

        // ---------- 主题 ----------
        theme = addModeValue("Theme",
                new String[]{"White", "Red", "Blue", "Green", "Pink", "Yellow", "Cyan", "Purple"},
                "Blue");

        // ---------- Watermark ----------
        watermarkShow        = addBooleanValue("Watermark", true);
        watermarkPosX        = addNumberValue("Watermark-X", -1.0, -1.0, 3000.0, 1.0);
        watermarkPosY        = addNumberValue("Watermark-Y", -1.0, -1.0, 3000.0, 1.0);
        watermarkMode        = addModeValue("Watermark-Mode",
                new String[]{"Classic", "Modern"}, "Classic");
        watermarkParentheses = addBooleanValue("Watermark-Parentheses", false);
        watermarkFps         = addBooleanValue("Watermark-FPS", true);

        // ---------- Username ----------
        usernameShow = addBooleanValue("Username", true);
        usernamePosX = addNumberValue("Username-X", -1.0, -1.0, 3000.0, 1.0);
        usernamePosY = addNumberValue("Username-Y", -1.0, -1.0, 3000.0, 1.0);

        // ---------- ArrayList ----------
        arrayListShow       = addBooleanValue("ArrayList", true);
        arrayListPosX       = addNumberValue("ArrayList-X", -1.0, -1.0, 3000.0, 1.0);
        arrayListPosY       = addNumberValue("ArrayList-Y", -1.0, -1.0, 3000.0, 1.0);
        arrayListBackground = addBooleanValue("ArrayList-Background", true);

        // ---------- Scoreboard ----------
        scoreboardShow = addBooleanValue("Scoreboard", true);
        scoreboardPosX = addNumberValue("Scoreboard-X", -1.0, -1.0, 3000.0, 1.0);
        scoreboardPosY = addNumberValue("Scoreboard-Y", -1.0, -1.0, 3000.0, 1.0);

        // ---------- TargetHud ----------
        targetHudShow = addBooleanValue("TargetHud", true);
        targetHudPosX = addNumberValue("TargetHud-X", -1.0, -1.0, 3000.0, 1.0);
        targetHudPosY = addNumberValue("TargetHud-Y", -1.0, -1.0, 3000.0, 1.0);
    }

    public static HUD getInstance() {
        return INSTANCE;
    }

    @EventListener
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled()) return;
        HudManager.getInstance();
    }

    public int getThemeColor() {
        String mode = theme.getValue();
        if (mode == null) return 0xFF4A9EFF;
        switch (mode) {
            case "Red":    return 0xFFFF5555;
            case "Blue":   return 0xFF4A9EFF;
            case "Green":  return 0xFF4ADE80;
            case "Pink":   return 0xFFFF80C0;
            case "Yellow": return 0xFFFBBA24;
            case "Cyan":   return 0xFF00E5FF;
            case "Purple": return 0xFFB080FF;
            case "White":
            default:       return 0xFFFFFFFF;
        }
    }
}