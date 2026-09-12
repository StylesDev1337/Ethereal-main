package cn.ethereal.module.render;

import cn.ethereal.event.annotation.EventListener;
import cn.ethereal.event.events.TickEvent;
import cn.ethereal.module.Category;
import cn.ethereal.module.Module;
import cn.ethereal.ui.values.NumberValue;
import org.lwjgl.input.Keyboard;

public class FullBright extends Module {
    private float normalSetting = 1.0f;
    private NumberValue lightValue;

    public FullBright() {
        super("FullBright", Keyboard.KEY_NONE, Category.RENDER, true, true);
        lightValue = addNumberValue("Value", 1.0, 1.0, 20.0, 1.0);
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getPhase() != TickEvent.Phase.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null || lightValue == null) return;

        Double val = lightValue.getValue();
        if (val != null) {
            mc.gameSettings.gammaSetting = val.floatValue();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        normalSetting = mc.gameSettings.gammaSetting;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        mc.gameSettings.gammaSetting = normalSetting;
    }
}