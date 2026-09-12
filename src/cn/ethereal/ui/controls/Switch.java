package cn.ethereal.ui.controls;

import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.util.render.RenderUtil;

public class Switch extends Control {
    private final BooleanValue value;

    public Switch(String name, BooleanValue value) {
        super(name);
        this.value = value;
        this.width = 100;
        this.height = 16;
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        boolean on = value.getValue();

        // 名字
        font.drawStringWithShadow(name, x, y + 3, on ? 0xFFFFFFFF : 0xFFAAAAAA);

        // 开关
        int swW = 22;
        int swH = 10;
        int swX = x + width - swW;
        int swY = y + 3;

        int trackColor = on ? 0xFF4A9EFF : 0xFF333333;
        RenderUtil.drawRoundedRect(swX, swY, swW, swH, swH / 2F, trackColor);

        // 滑块
        int knobSize = swH - 2;
        int knobX = on ? swX + swW - knobSize - 1 : swX + 1;
        RenderUtil.drawRoundedRect(knobX, swY + 1, knobSize, knobSize, knobSize / 2F, 0xFFFFFFFF);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            value.toggle();
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}

    public BooleanValue getValue() {
        return value;
    }
}