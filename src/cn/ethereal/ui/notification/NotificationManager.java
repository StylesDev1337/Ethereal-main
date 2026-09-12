package cn.ethereal.ui.notification;

import cn.ethereal.event.EventBus;
import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.Render2DEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

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

    public void notify(String title, boolean enabled) {
        Notification notification = new Notification(title, enabled);
        notifications.add(0, notification);   // index 0 = 最新 = 最靠近屏幕底部
        trimOverflow();
    }

    public void notify(String title, String message, boolean enabled) {
        Notification notification = new Notification(title, message, enabled, 2000);
        notifications.add(0, notification);
        trimOverflow();
    }

    private void trimOverflow() {
        while (notifications.size() > MAX_VISIBLE) {
            notifications.remove(notifications.size() - 1);
        }
    }

    // ==================== 渲染 ====================

    @EventListener
    public void onRender2D(Render2DEvent event) {
        if (notifications.isEmpty()) return;

        // 1. 重算每条通知的目标堆叠位置
        updateStackPositions();

        // 2. 更新动画 + 绘制
        for (Notification n : notifications) {
            n.update();
            n.draw();
        }

        // 3. 清理过期（放在最后，避免清理时影响本帧的 updateStackPositions）
        notifications.removeIf(Notification::isExpired);
    }

    /**
     * 从下往上依次排列：index 0 最靠底，往上的 index 依次上移。
     */
    private void updateStackPositions() {
        ScaledResolution sr = new ScaledResolution(mc);
        int screenHeight = sr.getScaledHeight();

        int cursor = screenHeight - BOTTOM_MARGIN;   // 底边 y

        for (Notification n : notifications) {
            int h = n.getHeight();
            int targetY = cursor - h;
            n.setStackY(targetY);
            cursor = targetY - Notification.LINE_SPACING;
        }
    }
}