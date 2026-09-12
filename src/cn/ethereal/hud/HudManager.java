package cn.ethereal.hud;

import cn.ethereal.event.EventBus;
import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.Render2DEvent;
import cn.ethereal.module.render.HUD;
import cn.ethereal.util.helper.NullPointHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.List;

public class HudManager {

    private static HudManager instance;
    private final List<HudElement> elements = new ArrayList<>();

    private HudManager() {
        EventBus.register(this);
    }

    public static HudManager getInstance() {
        if (instance == null) instance = new HudManager();
        return instance;
    }

    public void register(HudElement element) {
        elements.add(element);
    }

    public List<HudElement> getElements() {
        return elements;
    }

    public HudElement getElement(String name) {
        for (HudElement e : elements) {
            if (e.name.equals(name)) return e;
        }
        return null;
    }

    @EventListener
    public void onRender(Render2DEvent event) {
        if (!NullPointHelper.isPlayerInWorld()) return;
        if (HUD.getInstance() == null || !HUD.getInstance().isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        boolean chatOpen = mc.currentScreen instanceof GuiChat;

        for (HudElement element : elements) {
            if (!element.isEnabled()) continue;

            // ★ 顺序调整：先 update（推进动画），再 measure（算尺寸）
            element.update();
            element.measure();

            // measure 返回 0 尺寸 → 跳过（如 TargetHud 无目标时）
            if (element.getWidth() <= 0 || element.getHeight() <= 0) continue;

            int x = element.getX(sr);
            int y = element.getY(sr);

            boolean highlighted = element.handleDrag(sr, x, y);

            x = element.getX(sr);
            y = element.getY(sr);

            element.render(x, y);

            if (chatOpen) {
                element.drawHoverOutline(x, y, highlighted);
            }
        }
    }
}