package cn.ethereal.ui.controls;

import cn.ethereal.ui.values.StringValue;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class InputField extends Control {
    private final StringValue value;

    // 光标位置
    private int cursorPosition = 0;
    private int selectionEnd = 0;

    // 最大输入长度
    private int maxLength = 128;

    // 是否可编辑
    private boolean editable = true;

    // 颜色
    private int borderColor = 0xFF555555;
    private int borderColorFocused = 0xFF00AAFF;
    private int backgroundColor = 0xFF1A1A1A;
    private int textColor = 0xFFFFFFFF;
    private int placeholderColor = 0xFF666666;

    // 内边距
    private int padding = 4;

    // 焦点状态
    private boolean focused = false;

    // 光标闪烁
    private long cursorBlinkTime = System.currentTimeMillis();
    private boolean cursorVisible = true;

    // 选择状态
    private boolean selecting = false;

    // 上一次的值（用于撤销）
    private String lastValue = "";

    public InputField(String name, StringValue value) {
        super(name);
        this.value = value;
        this.width = 120;
        this.height = 18;
    }

    public InputField(String name, StringValue value, int width, int height) {
        super(name);
        this.value = value;
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        // ★ 修复：正确处理键盘事件
        if (focused && editable) {
            handleKeyboardInput();
        }

        // 更新光标闪烁
        long currentTime = System.currentTimeMillis();
        if (currentTime - cursorBlinkTime > 500) {
            cursorBlinkTime = currentTime;
            cursorVisible = !cursorVisible;
        }

        // 绘制背景
        drawRect(x, y, x + width, y + height, backgroundColor);

        // 绘制边框
        int border = focused ? borderColorFocused : borderColor;
        drawRect(x, y, x + width, y + 1, border);
        drawRect(x, y + height - 1, x + width, y + height, border);
        drawRect(x, y, x + 1, y + height, border);
        drawRect(x + width - 1, y, x + width, y + height, border);

        // 绘制选择高亮（在文本下方）
        if (hasSelection()) {
            drawSelection();
        }

        // 绘制文本
        drawText();

        // 绘制光标（在文本上方）
        if (focused && editable && cursorVisible) {
            drawCursor();
        }
    }

    /**
     * ★ 修复：正确处理键盘输入
     */
    private void handleKeyboardInput() {
        // 必须使用 while (Keyboard.next()) 循环处理所有按键事件
        while (Keyboard.next()) {
            // 只处理按下事件（忽略释放）
            if (!Keyboard.getEventKeyState()) {
                continue;
            }

            char typedChar = Keyboard.getEventCharacter();
            int keyCode = Keyboard.getEventKey();

            // 特殊处理：Ctrl 组合键不通过 keyTyped 处理
            if (isCtrlDown()) {
                handleCtrlCombination(keyCode);
                continue;
            }

            // 处理普通按键
            processKeyTyped(typedChar, keyCode);
        }
    }

    /**
     * 处理 Ctrl 组合键
     */
    private void handleCtrlCombination(int keyCode) {
        switch (keyCode) {
            case Keyboard.KEY_A:
                cursorPosition = 0;
                selectionEnd = value.getValue().length();
                break;
            case Keyboard.KEY_C:
                copySelection();
                break;
            case Keyboard.KEY_X:
                cutSelection();
                break;
            case Keyboard.KEY_V:
                pasteClipboard();
                break;
            case Keyboard.KEY_Z:
                undo();
                break;
        }
    }

    /**
     * 处理按键输入
     */
    private void processKeyTyped(char typedChar, int keyCode) {
        if (!focused || !editable) return;

        // ESC 取消焦点
        if (keyCode == Keyboard.KEY_ESCAPE) {
            setFocused(false);
            return;
        }

        // Enter 确认
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            setFocused(false);
            return;
        }

        // 退格
        if (keyCode == Keyboard.KEY_BACK) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursorPosition > 0) {
                String text = value.getValue();
                value.setValue(text.substring(0, cursorPosition - 1) + text.substring(cursorPosition));
                cursorPosition--;
                selectionEnd = cursorPosition;
            }
            return;
        }

        // 删除
        if (keyCode == Keyboard.KEY_DELETE) {
            if (hasSelection()) {
                deleteSelection();
            } else {
                String text = value.getValue();
                if (cursorPosition < text.length()) {
                    value.setValue(text.substring(0, cursorPosition) + text.substring(cursorPosition + 1));
                    selectionEnd = cursorPosition;
                }
            }
            return;
        }

        // 左箭头
        if (keyCode == Keyboard.KEY_LEFT) {
            if (isShiftDown()) {
                if (cursorPosition > 0) cursorPosition--;
            } else {
                cursorPosition--;
                selectionEnd = cursorPosition;
                selecting = false;
            }
            if (cursorPosition < 0) cursorPosition = 0;
            return;
        }

        // 右箭头
        if (keyCode == Keyboard.KEY_RIGHT) {
            String text = value.getValue();
            if (isShiftDown()) {
                if (cursorPosition < text.length()) cursorPosition++;
            } else {
                cursorPosition++;
                selectionEnd = cursorPosition;
                selecting = false;
            }
            if (cursorPosition > text.length()) cursorPosition = text.length();
            return;
        }

        // Home
        if (keyCode == Keyboard.KEY_HOME) {
            cursorPosition = 0;
            if (!isShiftDown()) selectionEnd = 0;
            return;
        }

        // End
        if (keyCode == Keyboard.KEY_END) {
            cursorPosition = value.getValue().length();
            if (!isShiftDown()) selectionEnd = cursorPosition;
            return;
        }

        // 普通字符输入
        if (isCharValid(typedChar)) {
            if (hasSelection()) {
                deleteSelection();
            }

            String text = value.getValue();
            if (text.length() < maxLength) {
                String newText = text.substring(0, cursorPosition) + typedChar + text.substring(cursorPosition);
                value.setValue(newText);
                cursorPosition++;
                selectionEnd = cursorPosition;
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        // 保留此方法以兼容外部调用
        processKeyTyped(typedChar, keyCode);
    }

    private void drawText() {
        String text = value.getValue();
        String displayText = text.isEmpty() ? value.getPlaceholder() : text;
        int color = text.isEmpty() ? placeholderColor : this.textColor;

        String visibleText = getVisibleText(displayText);
        int textX = x + padding;
        int textY = y + (height - font.FONT_HEIGHT) / 2;

        font.drawStringWithShadow(visibleText, textX, textY, color);
    }

    private String getVisibleText(String text) {
        int maxWidth = width - padding * 2;
        if (font.getStringWidth(text) <= maxWidth) {
            return text;
        }

        // 如果文本太长，截断并添加 "..."
        String result = "";
        for (char c : text.toCharArray()) {
            if (font.getStringWidth(result + c) > maxWidth - 6) {
                break;
            }
            result += c;
        }
        return result + "...";
    }

    private void drawCursor() {
        String text = value.getValue();
        int cursorX = getCursorX(text);
        int cursorY = y + padding + 1;
        int cursorHeight = height - padding * 2 - 2;

        drawRect(cursorX, cursorY, cursorX + 1, cursorY + cursorHeight, 0xFFFFFFFF);
    }

    private int getCursorX(String text) {
        int textX = x + padding;
        String beforeCursor = text.substring(0, Math.min(cursorPosition, text.length()));
        return textX + font.getStringWidth(beforeCursor);
    }

    private void drawSelection() {
        if (!hasSelection()) return;

        String text = value.getValue();
        int start = Math.min(cursorPosition, selectionEnd);
        int end = Math.max(cursorPosition, selectionEnd);

        String beforeStart = text.substring(0, start);
        String selectedText = text.substring(start, end);

        int startX = x + padding + font.getStringWidth(beforeStart);
        int endX = startX + font.getStringWidth(selectedText);
        int y1 = y + padding + 1;
        int y2 = y + height - padding - 1;

        drawRect(startX, y1, endX, y2, 0x4400AAFF);
    }

    private void drawRect(int left, int top, int right, int bottom, int color) {
        if (left >= right || top >= bottom) return;

        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        if (isHovered(mouseX, mouseY)) {
            setFocused(true);

            // 点击设置光标位置
            String text = value.getValue();
            int clickX = mouseX - x - padding;
            int newCursor = 0;

            for (int i = 0; i <= text.length(); i++) {
                String sub = text.substring(0, i);
                int width = font.getStringWidth(sub);
                if (width >= clickX) {
                    newCursor = i;
                    break;
                }
                if (i == text.length()) {
                    newCursor = i;
                }
            }

            cursorPosition = newCursor;
            selectionEnd = newCursor;
            selecting = false;
        } else {
            setFocused(false);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        // 不需要处理
    }

    // 鼠标拖动选择（需要在父级中调用 mouseClickMove）
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (focused && clickedMouseButton == 0) {
            String text = value.getValue();
            int clickX = mouseX - x - padding;
            int newCursor = 0;

            for (int i = 0; i <= text.length(); i++) {
                String sub = text.substring(0, i);
                int width = font.getStringWidth(sub);
                if (width >= clickX) {
                    newCursor = i;
                    break;
                }
                if (i == text.length()) {
                    newCursor = i;
                }
            }

            cursorPosition = newCursor;
            selecting = true;
        }
    }

    private void deleteSelection() {
        if (!hasSelection()) return;

        int start = Math.min(cursorPosition, selectionEnd);
        int end = Math.max(cursorPosition, selectionEnd);
        String text = value.getValue();

        value.setValue(text.substring(0, start) + text.substring(end));
        cursorPosition = start;
        selectionEnd = start;
    }

    private boolean hasSelection() {
        return cursorPosition != selectionEnd;
    }

    private void copySelection() {
        if (!hasSelection()) return;

        int start = Math.min(cursorPosition, selectionEnd);
        int end = Math.max(cursorPosition, selectionEnd);
        String text = value.getValue();
        String selected = text.substring(start, end);

        setClipboard(selected);
    }

    private void cutSelection() {
        if (!hasSelection()) return;
        copySelection();
        deleteSelection();
    }

    private void pasteClipboard() {
        String clipboard = getClipboard();
        if (clipboard == null || clipboard.isEmpty()) return;

        if (hasSelection()) {
            deleteSelection();
        }

        String text = value.getValue();
        if (text.length() + clipboard.length() > maxLength) {
            clipboard = clipboard.substring(0, maxLength - text.length());
        }

        String newText = text.substring(0, cursorPosition) + clipboard + text.substring(cursorPosition);
        value.setValue(newText);
        cursorPosition += clipboard.length();
        selectionEnd = cursorPosition;
    }

    private void undo() {
        String current = value.getValue();
        if (!current.equals(lastValue)) {
            value.setValue(lastValue);
            cursorPosition = value.getValue().length();
            selectionEnd = cursorPosition;
        }
        lastValue = current;
    }

    private boolean isCharValid(char c) {
        return c >= 32 && c != 127;
    }

    private boolean isCtrlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    private boolean isShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    private void setClipboard(String text) {
        try {
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(text);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        } catch (Exception e) {
            // 忽略
        }
    }

    private String getClipboard() {
        try {
            java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            java.awt.datatransfer.Transferable contents = clipboard.getContents(null);
            if (contents != null && contents.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                return (String) contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
            }
        } catch (Exception e) {
            // 忽略
        }
        return "";
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) {
            selecting = false;
        } else {
            cursorBlinkTime = System.currentTimeMillis();
            cursorVisible = true;
            lastValue = value.getValue();
        }
    }

    public boolean isFocused() {
        return focused;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public String getValue() {
        return value.getValue();
    }

    public void setValue(String text) {
        value.setValue(text);
        cursorPosition = text.length();
        selectionEnd = cursorPosition;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public void setBorderColorFocused(int borderColorFocused) {
        this.borderColorFocused = borderColorFocused;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setPlaceholderColor(int placeholderColor) {
        this.placeholderColor = placeholderColor;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }
}