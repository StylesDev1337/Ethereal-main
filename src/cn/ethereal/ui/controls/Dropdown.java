package cn.ethereal.ui.controls;

import cn.ethereal.ui.values.ModeValue;
import net.minecraft.client.gui.Gui;

public class Dropdown extends Control {
    private final ModeValue value;
    private boolean expanded = false;

    public Dropdown(String name, ModeValue value) {
        super(name);
        this.value = value;
        this.width = 100;
        this.height = 15;
    }

    public boolean isExpanded() {
        return expanded;
    }

    /**
     * 检查点是否落在 dropdown 的“整个可见区域”里
     * 包括收起的按钮本身 + 展开的下拉列表
     */
    public boolean isInFullArea(int mouseX, int mouseY) {
        if (isHovered(mouseX, mouseY)) return true;

        if (expanded) {
            int dropdownHeight = value.getModes().length * 15;
            int dropdownY = y + height;
            if (mouseX >= x && mouseX <= x + width
                    && mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        // 绘制名称
        font.drawStringWithShadow(name, x, y + 3, 0xFFFFFF);

        // 绘制当前值
        String currentValue = value.getValue();
        int textWidth = font.getStringWidth(currentValue);
        font.drawStringWithShadow(currentValue, x + width - textWidth, y + 3, 0xFF00FF00);

        // 绘制下拉背景
        if (expanded) {
            int dropdownHeight = value.getModes().length * 15;
            Gui.drawRect(x, y + height, x + width, y + height + dropdownHeight, 0xFF222222);

            // 绘制选项
            int optionY = y + height;
            for (String mode : value.getModes()) {
                int color = mode.equals(value.getValue()) ? 0xFF00FF00 : 0xFFFFFF;
                font.drawStringWithShadow(mode, x + 3, optionY + 3, color);
                optionY += 15;
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        if (isHovered(mouseX, mouseY)) {
            expanded = !expanded;
        } else if (expanded) {
            // 检查是否点击了下拉选项
            int dropdownY = y + height;
            int dropdownHeight = value.getModes().length * 15;

            if (mouseX >= x && mouseX <= x + width && mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                int index = (mouseY - dropdownY) / 15;
                if (index < value.getModes().length) {
                    value.setValue(value.getModes()[index]);
                }
                expanded = false;
            } else {
                expanded = false;
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}

    public ModeValue getValue() {
        return value;
    }
}