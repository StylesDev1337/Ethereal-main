package cn.ethereal.config;

import cn.ethereal.event.EventBus;
import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.TickEvent;

public class AutoSaveManager {
    private static AutoSaveManager instance;
    private int tickCounter = 0;
    private static final int SAVE_INTERVAL = 1200; // 每 60 秒保存一次（20 ticks = 1 秒）

    private AutoSaveManager() {
        // 注册事件监听
        EventBus.register(this);
    }

    public static AutoSaveManager getInstance() {
        if (instance == null) {
            instance = new AutoSaveManager();
        }
        return instance;
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (event.getPhase() != TickEvent.Phase.POST) return;

        tickCounter++;

        // 每 60 秒自动保存一次
        if (tickCounter >= SAVE_INTERVAL) {
            tickCounter = 0;
            ConfigManager.getInstance().saveConfig();
        }
    }

    /**
     * 立即保存配置
     */
    public void saveNow() {
        tickCounter = 0;
        ConfigManager.getInstance().saveConfig();
    }
}