package cn.ethereal.module.render;

import cn.ethereal.Ethereal;
import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.Render2DEvent;
import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.module.ModuleManager;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.ModeValue;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.util.helper.NullPointHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HUD extends Module {
    private static final FontRenderer font = mc.fontRendererObj;
    private static final int WM_X = 2;
    private static final int WM_Y = 2;
    private static final int MARGIN = 2;

    // 颜色缓存
    private static int cachedColor = 0xFFFFFF;
    private static String lastColorInput = "";

    // 值
    private static BooleanValue watermark;
    private static BooleanValue parentheses;
    private static BooleanValue drawUserName;
    private static ModeValue colorMode;

    // 预定义颜色映射
    private static final Map<String, Integer> PREDEFINED_COLORS = new HashMap<>();

    static {
        PREDEFINED_COLORS.put("red", 0xFF0000);
        PREDEFINED_COLORS.put("blue", 0x0000FF);
        PREDEFINED_COLORS.put("black", 0x000000);
        PREDEFINED_COLORS.put("white", 0xFFFFFF);
        PREDEFINED_COLORS.put("grey", 0x808080);
        PREDEFINED_COLORS.put("gray", 0x808080);
        PREDEFINED_COLORS.put("pink", 0xFFC0CB);
        PREDEFINED_COLORS.put("yellow", 0xFFFF00);
        PREDEFINED_COLORS.put("green", 0x00FF00);
        PREDEFINED_COLORS.put("orange", 0xFFA500);
        PREDEFINED_COLORS.put("purple", 0x800080);
        PREDEFINED_COLORS.put("cyan", 0x00FFFF);
        PREDEFINED_COLORS.put("magenta", 0xFF00FF);
        PREDEFINED_COLORS.put("lime", 0x32CD32);
        PREDEFINED_COLORS.put("maroon", 0x800000);
        PREDEFINED_COLORS.put("navy", 0x000080);
        PREDEFINED_COLORS.put("olive", 0x808000);
        PREDEFINED_COLORS.put("teal", 0x008080);
        PREDEFINED_COLORS.put("silver", 0xC0C0C0);
        PREDEFINED_COLORS.put("gold", 0xFFD700);
        PREDEFINED_COLORS.put("brown", 0x8B4513);
        PREDEFINED_COLORS.put("indigo", 0x4B0082);
        PREDEFINED_COLORS.put("violet", 0xEE82EE);
        PREDEFINED_COLORS.put("aqua", 0x00FFFF);
        PREDEFINED_COLORS.put("coral", 0xFF7F50);
        PREDEFINED_COLORS.put("crimson", 0xDC143C);
        PREDEFINED_COLORS.put("darkred", 0x8B0000);
        PREDEFINED_COLORS.put("darkgreen", 0x006400);
        PREDEFINED_COLORS.put("darkblue", 0x00008B);
        PREDEFINED_COLORS.put("darkgrey", 0xA9A9A9);
        PREDEFINED_COLORS.put("darkgray", 0xA9A9A9);
        PREDEFINED_COLORS.put("lightgrey", 0xD3D3D3);
        PREDEFINED_COLORS.put("lightgray", 0xD3D3D3);
    }

    public HUD() {
        super("HUD", Keyboard.KEY_NONE, Category.RENDER, true, true);

        watermark = addBooleanValue("Draw WaterMark", true);
        parentheses = addBooleanValue("Draw Parentheses", false);
        drawUserName = addBooleanValue("Draw Username", false);
        colorMode = addModeValue("Themes", new String[]{"Red", "Blue", "Black", "White", "Grey", "Pink", "Yellow", "Green"}, "White");
    }

    @EventListener
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled() || !NullPointHelper.isPlayerInWorld()) return;

        drawWatermark();
        drawArraylist();
        drawUserName();
    }

    // ==================== 绘制方法 ====================

    /**
     * 绘制 Watermark，E 使用主题色
     */
    private static void drawWatermark() {
        if (!watermark.getValue()) return;

        int color = getColor();
        String prefix = parentheses.getValue() ? "[" : "";
        String suffix = parentheses.getValue() ? "] " : "";

        // 拆分绘制: "[Ethereal] " 或 "Ethereal"
        // E 用主题色，其余白色
        String beforeE = prefix;           // "[" 或 ""
        String eChar = "E";                // "E"
        String afterE = "thereal" + suffix; // "thereal] " 或 "thereal"

        int x = WM_X;
        int y = WM_Y;

        // 1. 绘制 "[" 或 "" (白色)
        if (!beforeE.isEmpty()) {
            font.drawStringWithShadow(beforeE, x, y, 0xFFFFFF);
            x += font.getStringWidth(beforeE);
        }

        // 2. 绘制 "E" (主题色)
        font.drawStringWithShadow(eChar, x, y, color);
        x += font.getStringWidth(eChar);

        // 3. 绘制 "thereal] " 或 "thereal" (白色)
        font.drawStringWithShadow(afterE + " " + mc.getDebugFPS(), x, y, 0xFFFFFF);
    }

    private static void drawUserName() {
        if (drawUserName.getValue()) {
            ScaledResolution sr = new ScaledResolution(mc);
            String displayName = "User: " + mc.thePlayer.getName();
            int textHeight = font.FONT_HEIGHT;
            int y = sr.getScaledHeight() - textHeight - MARGIN;
            font.drawStringWithShadow(displayName, MARGIN, y, getColor());
        }
    }

    private static void drawArraylist() {
        ScaledResolution sr = new ScaledResolution(mc);
        int width = sr.getScaledWidth();

        List<Module> sortedModules = ModuleManager.getInstance().getEnabledModules().stream()
                .filter(Module::isVisible)
                .sorted((m1, m2) -> {
                    String t1 = getDisplayText(m1);
                    String t2 = getDisplayText(m2);
                    return Integer.compare(font.getStringWidth(t2), font.getStringWidth(t1));
                })
                .collect(Collectors.toList());

        int y = 1;
        int color = getColor();
        for (Module module : sortedModules) {
            String displayText = getDisplayText(module);
            int x = width - font.getStringWidth(displayText) - MARGIN;
            font.drawStringWithShadow(displayText, x, y, color);
            y += font.FONT_HEIGHT + 3;
        }
    }

    // ==================== 辅助方法 ====================

    private static String getDisplayText(Module module) {
        String suffix = getModuleSuffix(module);
        return suffix != null && !suffix.isEmpty()
                ? module.getName() + " §7- §f" + suffix
                : module.getName();
    }

    private static String getModuleSuffix(Module module) {
        if (!module.getModeValues().isEmpty()) {
            return module.getModeValues().get(0).getValue();
        }

        if (!module.getNumberValues().isEmpty()) {
            NumberValue nv = module.getNumberValues().get(0);
            double val = nv.getValue();
            return val == (int) val ? String.valueOf((int) val) : String.format("%.1f", val);
        }

        if (!module.getBooleanValues().isEmpty()) {
            BooleanValue bv = module.getBooleanValues().get(0);
            if (bv.getValue()) return "ON";
        }

        return null;
    }

    // ==================== 颜色解析（带缓存） ====================

    private static int getColor() {
        if (colorMode.getValue() == null) return 0xFFFFFF;

        String input = colorMode.getValue().trim();
        if (input.isEmpty()) return 0xFFFFFF;

        if (input.equals(lastColorInput)) {
            return cachedColor;
        }

        lastColorInput = input;
        cachedColor = parseColor(input);
        return cachedColor;
    }

    private static int parseColor(String input) {
        // 1. 预定义颜色
        Integer predefined = PREDEFINED_COLORS.get(input.toLowerCase());
        if (predefined != null) return predefined;

        // 2. 十六进制 (0xFFFFFF / #FFFFFF)
        try {
            if (input.startsWith("0x") || input.startsWith("0X")) {
                return Integer.decode(input);
            }
            if (input.startsWith("#")) {
                return Integer.decode(input.replace("#", "0x"));
            }
            // 3. 纯数字
            return Integer.parseInt(input);
        } catch (NumberFormatException ignored) {}

        // 4. RGB (255,255,255)
        if (input.contains(",")) {
            try {
                String[] parts = input.split(",");
                if (parts.length == 3) {
                    int r = clamp(parts[0]);
                    int g = clamp(parts[1]);
                    int b = clamp(parts[2]);
                    return (r << 16) | (g << 8) | b;
                }
            } catch (NumberFormatException ignored) {}
        }

        return 0xFFFFFF;
    }

    private static int clamp(String value) {
        int v = Integer.parseInt(value.trim());
        return Math.max(0, Math.min(255, v));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastColorInput = "";
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}