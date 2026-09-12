package cn.ethereal.ui.controls;

import cn.ethereal.ui.values.ModeValue;
import cn.ethereal.util.render.RenderUtil;

public class Dropdown extends Control {
    private final ModeValue value;
    private boolean expanded = false;

    public Dropdown(String name, ModeValue value) {
        super(name);
        this.value = value;
        this.width = 100;
        this.height = 16;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isInFullArea(int mouseX, int mouseY) {
        if (isHovered(mouseX, mouseY)) return true;

        if (expanded) {
            int dropdownHeight = value.getModes().length * 14;
            int dropdownY = y + height;
            return mouseX >= x && mouseX <= x + width
                    && mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight;
        }
        return false;
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        // 名字
        font.drawStringWithShadow(name, x, y + 3, 0xFFFFFFFF);

        // 当前值 + 三角
        String current = value.getValue();
        int textW = font.getStringWidth(current);
        font.drawStringWithShadow(current, x + width - textW - 8, y + 3, 0xFF4A9EFF);

        // 小三角（用圆角矩形拼）
        int arrowX = x + width - 6;
        int arrowY = y + 6;
        int arrowColor = expanded ? 0xFF4A9EFF : 0xFF888888;
        RenderUtil.drawRoundedRect(arrowX, arrowY, 4, 4, 1, arrowColor);

        // 展开的下拉
        if (expanded) {
            int dropdownHeight = value.getModes().length * 14;
            int dropdownY = y + height;

            // 阴影 + 背景
            RenderUtil.drawRoundedRect(x, dropdownY, width, dropdownHeight, 4, 0xFF181818);
            RenderUtil.drawRoundedOutline(x, dropdownY, width, dropdownHeight, 4, 1, 0xFF2A2A2A);

            int optionY = dropdownY + 2;
            for (String mode : value.getModes()) {
                boolean hov = mouseX >= x && mouseX <= x + width
                        && mouseY >= optionY && mouseY <= optionY + 14;
                boolean selected = mode.equals(value.getValue());

                if (hov) {
                    RenderUtil.drawRoundedRect(x + 2, optionY, width - 4, 14, 3, 0xFF252525);
                }

                int color = selected ? 0xFF4A9EFF : (hov ? 0xFFFFFFFF : 0xFFAAAAAA);
                font.drawStringWithShadow(mode, x + 8, optionY + 3, color);
                optionY += 14;
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        if (isHovered(mouseX, mouseY)) {
            expanded = !expanded;
        } else if (expanded) {
            int dropdownY = y + height;
            int dropdownHeight = value.getModes().length * 14;

            if (mouseX >= x && mouseX <= x + width
                    && mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                int index = (mouseY - dropdownY - 2) / 14;
                if (index >= 0 && index < value.getModes().length) {
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