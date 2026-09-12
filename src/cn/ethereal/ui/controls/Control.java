package cn.ethereal.ui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public abstract class Control {
    protected static final Minecraft mc = Minecraft.getMinecraft();
    protected static final FontRenderer font = mc.fontRendererObj;

    protected final String name;
    protected int x, y;
    protected int width;
    public int height;
    protected boolean visible = true;

    public Control(String name) {
        this.name = name;
        this.width = 100;
        this.height = 15;
    }

    public abstract void draw(int mouseX, int mouseY, float partialTicks);

    public abstract void mouseClicked(int mouseX, int mouseY, int mouseButton);

    public abstract void mouseReleased(int mouseX, int mouseY, int mouseButton);

    public void keyTyped(char typedChar, int keyCode) {}

    public String getName() {
        return name;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}