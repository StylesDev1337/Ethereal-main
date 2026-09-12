package cn.ethereal.hud.impl;

import cn.ethereal.hud.HudElement;
import cn.ethereal.module.Module;
import cn.ethereal.module.ModuleManager;
import cn.ethereal.module.render.HUD;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.util.render.RenderUtil;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayListHud extends HudElement {

    private static final int SPACING = 2;
    private static final int ITEM_HEIGHT = 14;
    private static final int RADIUS = 3;
    private static final int BAR_WIDTH = 2;
    private static final int PADDING = 5;

    private static final int BG = 0xB0101010;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SUFFIX = 0xFF888888;

    private static final float SLIDE_SPEED = 0.25F;
    private static final float SLIDE_OFFSET = 20F;

    private final Map<String, Float> animCache = new HashMap<>();

    public ArrayListHud() {
        super("ArrayList", "模块列表",
                HUD.getInstance().arrayListShow,
                HUD.getInstance().arrayListPosX,
                HUD.getInstance().arrayListPosY);
    }

    private BooleanValue background() { return HUD.getInstance().arrayListBackground; }

    @Override
    public int[] measure() {
        int count = getSortedModules().size();
        width = 100;
        height = count > 0 ? count * (ITEM_HEIGHT + SPACING) - SPACING : 0;
        return new int[] { width, height };
    }

    @Override
    public int getDefaultX(ScaledResolution sr) {
        return sr.getScaledWidth() - 200;
    }

    @Override
    public int getDefaultY(ScaledResolution sr) {
        return 4;
    }

    @Override
    public void render(int x, int y) {
        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();

        List<Module> modules = getSortedModules();

        List<String> names = new ArrayList<>();
        for (Module m : modules) names.add(m.getName());
        animCache.keySet().removeIf(n -> !names.contains(n));

        int themeColor = HUD.getInstance().getThemeColor();
        int currentY = y;

        for (Module module : modules) {
            String name = module.getName();
            String suffix = getSuffix(module);
            int nameW = font.getStringWidth(name);
            int suffixW = suffix == null ? 0 : font.getStringWidth(suffix);
            int boxW = nameW + suffixW + PADDING * 2 + BAR_WIDTH + (suffix != null ? 6 : 0);

            int targetX = screenWidth - boxW - 4;

            float animX = animCache.getOrDefault(name, targetX + SLIDE_OFFSET);
            animX += (targetX - animX) * SLIDE_SPEED;
            animCache.put(name, animX);
            int drawX = Math.round(animX);

            if (background().getValue()) {
                RenderUtil.drawRoundedRect(drawX, currentY, boxW, ITEM_HEIGHT, RADIUS, BG);
            }

            RenderUtil.drawRoundedRect(drawX + 1, currentY + 3, BAR_WIDTH, ITEM_HEIGHT - 6, BAR_WIDTH / 2F, themeColor);

            int textY = currentY + (ITEM_HEIGHT - font.FONT_HEIGHT) / 2 + 1;

            font.drawStringWithShadow(name, drawX + BAR_WIDTH + PADDING, textY, TEXT);

            if (suffix != null) {
                int suffixX = drawX + boxW - PADDING - suffixW;
                font.drawStringWithShadow(suffix, suffixX, textY, SUFFIX);
            }

            currentY += ITEM_HEIGHT + SPACING;
        }
    }

    private List<Module> getSortedModules() {
        List<Module> list = new ArrayList<>(ModuleManager.getInstance().getEnabledModules());
        list.removeIf(m -> !m.isVisible());
        list.sort(Comparator.comparingInt(m -> -getFullWidth(m)));
        return list;
    }

    private static int getFullWidth(Module module) {
        String name = module.getName();
        String suffix = getSuffix(module);
        int nameW = font.getStringWidth(name);
        if (suffix == null) return nameW;
        return nameW + font.getStringWidth(suffix) + 6;
    }

    private static String getSuffix(Module module) {
        if (!module.getModeValues().isEmpty()) return module.getModeValues().get(0).getValue();
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
}