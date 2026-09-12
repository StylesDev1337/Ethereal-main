package cn.ethereal.hud;

import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

/**
 * 所有 HUD 元素的抽象基类。
 * Value（enabled / posX / posY）不再自己 new，而是从 HUD 模块构造传入。
 */
public abstract class HudElement {

    protected static final Minecraft mc = Minecraft.getMinecraft();
    protected static final FontRenderer font = mc.fontRendererObj;

    // ==================== 元数据 ====================
    public final String name;
    public final String description;

    // ==================== 位置 / 尺寸（引用 HUD 模块里的 Value） ====================
    protected final NumberValue posX;
    protected final NumberValue posY;

    protected int width = 0;
    protected int height = 0;

    // ==================== 状态 ====================
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    // ==================== 开关（引用 HUD 模块里的 Value） ====================
    protected final BooleanValue enabled;

    public HudElement(String name, String description,
                      BooleanValue enabled, NumberValue posX, NumberValue posY) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.posX = posX;
        this.posY = posY;
    }

    // ================================================================
    //  子类必须实现
    // ================================================================

    public abstract int[] measure();

    public abstract void render(int x, int y);

    // ================================================================
    //  可选覆盖
    // ================================================================

    public int getDefaultX(ScaledResolution sr) {
        return (sr.getScaledWidth() - width) / 2;
    }

    public int getDefaultY(ScaledResolution sr) {
        return sr.getScaledHeight() - height - 40;
    }

    public void update() {}

    // ================================================================
    //  位置 / 拖拽
    // ================================================================

    public final int getX(ScaledResolution sr) {
        double v = posX.getValue();
        if (v < 0) return getDefaultX(sr);
        return (int) v;
    }

    public final int getY(ScaledResolution sr) {
        double v = posY.getValue();
        if (v < 0) return getDefaultY(sr);
        return (int) v;
    }

    public final void setX(double x) { posX.setValue(x); }
    public final void setY(double y) { posY.setValue(y); }

    public final boolean handleDrag(ScaledResolution sr, int x, int y) {
        boolean chatOpen = mc.currentScreen instanceof GuiChat;
        if (!chatOpen) {
            dragging = false;
            return false;
        }

        int mouseX = getScaledMouseX(sr);
        int mouseY = getScaledMouseY(sr);
        boolean hovered = isMouseOver(mouseX, mouseY, x, y);
        boolean leftDown = Mouse.isButtonDown(0);

        if (!dragging) {
            if (leftDown && hovered) {
                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
            }
        } else {
            if (!leftDown) {
                dragging = false;
            } else {
                int newX = Math.max(0, mouseX - dragOffsetX);
                int newY = Math.max(0, mouseY - dragOffsetY);
                posX.setValue((double) newX);
                posY.setValue((double) newY);
            }
        }
        return hovered || dragging;
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    // ================================================================
    //  工具
    // ================================================================

    protected final void drawHoverOutline(int x, int y, boolean highlighted) {
        if (!highlighted) return;
        RenderUtil.drawRoundedOutline(x - 1, y - 1, width + 2, height + 2, 4, 1, 0xFF4A9EFF);
    }

    protected static int getScaledMouseX(ScaledResolution sr) {
        return Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
    }

    protected static int getScaledMouseY(ScaledResolution sr) {
        return sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
    }

    // ================================================================
    //  Getter
    // ================================================================

    public boolean isEnabled() {
        return enabled.getValue();
    }

    public BooleanValue getEnabledValue() {
        return enabled;
    }

    public NumberValue getPosX() { return posX; }
    public NumberValue getPosY() { return posY; }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }
}