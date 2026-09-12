package cn.ethereal.ui.controls;

import cn.ethereal.ui.values.BooleanValue;
import net.minecraft.client.gui.Gui;

public class Switch extends Control {
    private final BooleanValue value;

    public Switch(String name, BooleanValue value) {
        super(name);
        this.value = value;
        this.width = 100;
        this.height = 15;
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        // 绘制名称
        font.drawStringWithShadow(name, x, y + 3, 0xFFFFFF);

        // 绘制开关背景
        int switchX = x + width - 20;
        int switchY = y + 2;
        int switchWidth = 16;
        int switchHeight = 8;

        // 开关背景
        int bgColor = value.getValue() ? 0xFF00FF00 : 0xFF444444;
        Gui.drawRect(switchX, switchY, switchX + switchWidth, switchY + switchHeight, bgColor);

        // 开关滑块
        int sliderX = value.getValue() ? switchX + switchWidth - 6 : switchX + 1;
        int sliderColor = value.getValue() ? 0xFFFFFFFF : 0xFFAAAAAA;
        Gui.drawRect(sliderX, switchY + 1, sliderX + 5, switchY + switchHeight - 1, sliderColor);
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