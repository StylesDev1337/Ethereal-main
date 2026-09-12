package cn.ethereal.ui.controls;

import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.util.render.RenderUtil;
import net.minecraft.util.MathHelper;

public class Slider extends Control {
    private final NumberValue value;
    private boolean dragging = false;

    public Slider(String name, NumberValue value) {
        super(name);
        this.value = value;
        this.width = 100;
        this.height = 22;
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        // 名字 + 值
        String displayName = name;
        String displayValue = String.format("%.1f", value.getValue());

        font.drawStringWithShadow(displayName, x, y, 0xFFFFFFFF);
        font.drawStringWithShadow(displayValue,
                x + width - font.getStringWidth(displayValue), y, 0xFF4A9EFF);

        // 轨道
        int barY = y + height - 7;
        int barH = 3;
        RenderUtil.drawRoundedRect(x, barY, width, barH, barH / 2F, 0xFF2A2A2A);

        // 进度
        float percent = (float) ((value.getValue() - value.getMin()) / (value.getMax() - value.getMin()));
        percent = MathHelper.clamp_float(percent, 0F, 1F);
        int fillW = (int) (width * percent);
        if (fillW > 0) {
            RenderUtil.drawRoundedRect(x, barY, fillW, barH, barH / 2F, 0xFF4A9EFF);
        }

        // 滑块
        int knobX = x + fillW;
        int knobSize = 10;
        int knobY = barY + barH / 2 - knobSize / 2;

        // hover/drag 放大
        boolean active = dragging || isHovered(mouseX, mouseY);
        int actualKnobSize = active ? 12 : 10;
        int adjust = (actualKnobSize - knobSize) / 2;
        RenderUtil.drawRoundedRect(knobX - actualKnobSize / 2F, knobY - adjust,
                actualKnobSize, actualKnobSize, actualKnobSize / 2F, 0xFFFFFFFF);
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