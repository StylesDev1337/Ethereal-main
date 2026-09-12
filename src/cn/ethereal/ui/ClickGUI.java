package cn.ethereal.ui;

import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.module.ModuleManager;
import cn.ethereal.ui.controls.Control;
import cn.ethereal.ui.controls.Dropdown;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGUI extends GuiScreen {
    private final Map<Category, CategoryPanel> categoryPanels = new HashMap<>();
    private Category currentCategory = Category.COMBAT;
    private Module selectedModule = null;

    private int panelX = 100;
    private int panelY = 50;
    private int panelWidth = 120;
    private int panelHeight = 25;
    private int categoryPanelWidth = 80;

    public ClickGUI() {
        initCategories();
    }

    private void initCategories() {
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            CategoryPanel panel = new CategoryPanel(categories[i], panelX, panelY + i * panelHeight, categoryPanelWidth, panelHeight);
            categoryPanels.put(categories[i], panel);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDefaultBackground();

        // 绘制分类栏
        for (CategoryPanel panel : categoryPanels.values()) {
            panel.draw(mouseX, mouseY, currentCategory == panel.getCategory());
        }

        // 绘制模块列表
        drawModuleList(mouseX, mouseY);

        // 绘制模块设置
        if (selectedModule != null) {
            drawModuleSettings(mouseX, mouseY);
        }
    }

    private void drawModuleList(int mouseX, int mouseY) {
        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(currentCategory);
        int x = panelX + categoryPanelWidth + 10;
        int y = panelY;

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int moduleY = y + i * 20;

            // 背景颜色优先级：选中 > 悬停 > 启用 > 默认
            int bgColor;
            if (selectedModule == module) {
                bgColor = 0xFF0088FF; // 蓝色 - 选中
            } else if (isModuleHovered(mouseX, mouseY, x, moduleY)) {
                bgColor = 0xFF0055AA; // 深蓝 - 悬停
            } else if (module.isEnabled()) {
                bgColor = 0xFF00AA00; // 绿色 - 启用
            } else {
                bgColor = 0xFF333333; // 灰色 - 禁用
            }

            drawRect(x, moduleY, x + panelWidth, moduleY + 18, bgColor);
            fontRendererObj.drawStringWithShadow(module.getName(), x + 5, moduleY + 5, 0xFFFFFF);
        }
    }

    private boolean isModuleHovered(int mouseX, int mouseY, int x, int moduleY) {
        return mouseX >= x && mouseX <= x + panelWidth && mouseY >= moduleY && mouseY <= moduleY + 18;
    }

    private Module getModuleAt(int mouseX, int mouseY) {
        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(currentCategory);
        int x = panelX + categoryPanelWidth + 10;
        int y = panelY;

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int moduleY = y + i * 20;

            if (isModuleHovered(mouseX, mouseY, x, moduleY)) {
                return module;
            }
        }
        return null;
    }

    private void drawModuleSettings(int mouseX, int mouseY) {
        int x = panelX + categoryPanelWidth + panelWidth + 20;
        int y = panelY;
        int settingsWidth = 150;

        drawRect(x, y, x + settingsWidth, y + 200, 0xFF222222);
        fontRendererObj.drawStringWithShadow(selectedModule.getName(), x + 5, y + 5, 0xFFFF00);

        List<Control> controls = selectedModule.getControls();

        // 第一趟：统一设置位置
        int controlY = y + 25;
        for (Control control : controls) {
            control.setPosition(x + 5, controlY);
            control.setWidth(settingsWidth - 10);
            controlY += control.height + 5;
        }

        // 第二趟：先画“未展开”的控件
        for (Control control : controls) {
            if (isExpandedDropdown(control)) continue;
            control.draw(mouseX, mouseY, 0);
        }

        // 第三趟：最后画“已展开”的 dropdown（z-order 最上）
        for (Control control : controls) {
            if (!isExpandedDropdown(control)) continue;
            control.draw(mouseX, mouseY, 0);
        }
    }

    private boolean isExpandedDropdown(Control control) {
        return control instanceof Dropdown && ((Dropdown) control).isExpanded();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // 1. 展开的 dropdown 优先拦截
        if (selectedModule != null) {
            for (Control control : selectedModule.getControls()) {
                if (control instanceof Dropdown) {
                    Dropdown dd = (Dropdown) control;
                    if (dd.isExpanded() && dd.isInFullArea(mouseX, mouseY)) {
                        dd.mouseClicked(mouseX, mouseY, mouseButton);
                        return;   // ★ 拦截，不再向下传
                    }
                }
            }

            // 2. 其他控件
            for (Control control : selectedModule.getControls()) {
                control.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }

        // 3. 分类点击
        for (CategoryPanel panel : categoryPanels.values()) {
            if (panel.isHovered(mouseX, mouseY)) {
                currentCategory = panel.getCategory();
                selectedModule = null;
                return;
            }
        }

        // 4. 模块列表点击
        Module clickedModule = getModuleAt(mouseX, mouseY);
        if (clickedModule != null) {
            if (mouseButton == 0) {
                clickedModule.toggle();
            } else if (mouseButton == 1) {
                selectedModule = clickedModule;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (selectedModule != null) {
            for (Control control : selectedModule.getControls()) {
                control.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (selectedModule != null) {
            for (Control control : selectedModule.getControls()) {
                if (control instanceof cn.ethereal.ui.controls.Slider) {
                    ((cn.ethereal.ui.controls.Slider) control).mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
                }
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static class CategoryPanel {
        private final Category category;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        public CategoryPanel(Category category, int x, int y, int width, int height) {
            this.category = category;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void draw(int mouseX, int mouseY, boolean selected) {
            int color = selected ? 0xFF00AA00 : 0xFF333333;
            GuiScreen.drawRect(x, y, x + width, y + height, color);

            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                    category.name(),
                    x + 5,
                    y + 5,
                    selected ? 0xFFFF00 : 0xFFFFFF
            );
        }

        public boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        public Category getCategory() {
            return category;
        }
    }
}