package cn.ethereal.ui.notification;

import cn.ethereal.event.EventBus;
import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.Render2DEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    private static NotificationManager instance;

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int BOTTOM_MARGIN = 4;
    private static final int MAX_VISIBLE = 8;

    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    private NotificationManager() {
        EventBus.register(this);
    }

    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    // ==================== 旧 API（兼容） ====================

    public void notify(String title, boolean enabled) {
        Notification notification = new Notification(title, enabled);
        notifications.add(0, notification);
        trimOverflow();
    }

    public void notify(String title, String message, boolean enabled) {
        Notification notification = new Notification(title, message, enabled, 2000);
        notifications.add(0, notification);
        trimOverflow();
    }

    // ==================== 新 API ====================

    public void notify(String title, String message, Notification.Type type) {
        notifications.add(0, new Notification(title, message, type));
        trimOverflow();
    }

    public void notify(String title, String message, Notification.Type type, long duration) {
        notifications.add(0, new Notification(title, message, type, duration));
        trimOverflow();
    }

    private void trimOverflow() {
        while (notifications.size() > MAX_VISIBLE) {
            notifications.remove(notifications.size() - 1);
        }
    }

    // ==================== 渲染（Classic 右下角） ====================

    @EventListener
    public void onRender2D(Render2DEvent event) {
        // ★ Modern Watermark 接管时，右下角这一套跳过
        if (isModernWatermarkActive()) return;

        if (notifications.isEmpty()) return;

        updateStackPositions();

        for (Notification n : notifications) {
            n.update();
            n.draw();
        }

        notifications.removeIf(Notification::isExpired);
    }

    private void updateStackPositions() {
        ScaledResolution sr = new ScaledResolution(mc);
        int screenHeight = sr.getScaledHeight();

        int cursor = screenHeight - BOTTOM_MARGIN;

        for (Notification n : notifications) {
            int h = n.getHeight();
            int targetY = cursor - h;
            n.setStackY(targetY);
            cursor = targetY - Notification.LINE_SPACING;
        }
    }

    // ==================== 供 Modern Watermark 使用 ====================

    /**
     * 返回当前活跃（未过期且还在显示）的通知，最新在前，最多 limit 条。
     * 只返回 animationAlpha 还有意义的。
     */
    public List<Notification> getActiveNotifications(int limit) {
        List<Notification> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Notification n : notifications) {
            if (result.size() >= limit) break;
            result.add(n);
        }
        return result;
    }

    /**
     * 是否还有未过期的通知。
     */
    public boolean hasActiveNotifications() {
        if (notifications.isEmpty()) return false;
        for (Notification n : notifications) {
            if (!n.isExpired()) return true;
        }
        return false;
    }

    /** 清理过期（Watermark 渲染循环也要调一次，避免 Classic 模式不渲染时列表不清理） */
    public void purgeExpired() {
        notifications.removeIf(Notification::isExpired);
    }

    /** 每帧 tick 所有通知（Watermark 模式下调） */
    public void tickAll() {
        for (Notification n : notifications) {
            n.update();
        }
    }

    /** 判断 Modern Watermark 是否在接管通知渲染 */
    private boolean isModernWatermarkActive() {
        try {
            cn.ethereal.hud.HudManager hm = cn.ethereal.hud.HudManager.getInstance();
            if (hm == null) return false;
            cn.ethereal.module.render.HUD hud = cn.ethereal.module.render.HUD.getInstance();
            if (hud == null || !hud.isEnabled()) return false;
            if (!hud.watermarkShow.getValue()) return false;
            return "Modern".equals(hud.watermarkMode.getValue());
        } catch (Throwable t) {
            return false;
        }
    }
}