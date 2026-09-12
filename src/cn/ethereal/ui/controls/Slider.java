package cn.ethereal.ui.controls;

import cn.ethereal.ui.values.NumberValue;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.MathHelper;

public class Slider extends Control {
    private final NumberValue value;
    private boolean dragging = false;

    public Slider(String name, NumberValue value) {
        super(name);
        this.value = value;
        this.width = 100;
        this.height = 20;
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        // 绘制名称和当前值
        String displayText = name + ": " + String.format("%.1f", value.getValue());
        font.drawStringWithShadow(displayText, x, y + 2, 0xFFFFFF);

        // 绘制滑条背景
        int sliderY = y + height - 5;
        Gui.drawRect(x, sliderY, x + width, sliderY + 4, 0xFF444444);

        // 计算滑块位置
        float percent = (float) ((value.getValue() - value.getMin()) / (value.getMax() - value.getMin()));
        int sliderX = x + (int) (percent * width);

        // 绘制滑块
        Gui.drawRect(sliderX - 2, sliderY - 2, sliderX + 2, sliderY + 6, 0xFF00FF00);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            dragging = true;
            updateValue(mouseX);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        dragging = false;
    }

    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragging) {
            updateValue(mouseX);
        }
    }

    private void updateValue(int mouseX) {
        float percent = MathHelper.clamp_float((float) (mouseX - x) / width, 0.0F, 1.0F);
        double newValue = value.getMin() + (value.getMax() - value.getMin()) * percent;
        value.setValue(newValue);
    }

    public NumberValue getValue() {
        return value;
    }
}