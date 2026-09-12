package cn.ethereal.module;

import cn.ethereal.event.EventBus;
import cn.ethereal.ui.controls.Control;
import cn.ethereal.ui.controls.InputField;
import cn.ethereal.ui.notification.NotificationManager;
import cn.ethereal.ui.values.BooleanValue;
import cn.ethereal.ui.values.ModeValue;
import cn.ethereal.ui.values.NumberValue;
import cn.ethereal.ui.values.StringValue;
import cn.ethereal.util.chat.ChatUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class Module {
    private final String name;
    private boolean enabled;
    private int key;
    private final Category category;

    public boolean isVisible() {
        return visible;
    }

    private final boolean visible;
    protected static final Minecraft mc = Minecraft.getMinecraft();

    // 值列表
    private final List<BooleanValue> booleanValues = new ArrayList<>();
    private final List<NumberValue> numberValues = new ArrayList<>();
    private final List<ModeValue> modeValues = new ArrayList<>();
    private final List<StringValue> stringValues = new ArrayList<>();

    // 控件列表
    private final List<Control> controls = new ArrayList<>();

    public Module(String name, int key, Category category, boolean defaultEnable, boolean visible) {
        this.name = name;
        this.key = key;
        this.category = category;
        this.enabled = defaultEnable;
        this.visible = visible;

        EventBus.register(this);
    }

    // 添加值的方法

    protected StringValue addStringValue(String name, String defaultValue) {
        StringValue value = new StringValue(name, defaultValue);
        stringValues.add(value);
        controls.add(new InputField(name, value));
        return value;
    }

    protected StringValue addStringValue(String name, String defaultValue, String placeholder) {
        StringValue value = new StringValue(name, defaultValue, placeholder);
        stringValues.add(value);
        controls.add(new InputField(name, value));
        return value;
    }

    protected StringValue addStringValue(String name, String defaultValue, int width, int height) {
        StringValue value = new StringValue(name, defaultValue);
        stringValues.add(value);
        controls.add(new InputField(name, value, width, height));
        return value;
    }

    public List<StringValue> getStringValues() {
        return stringValues;
    }

    protected BooleanValue addBooleanValue(String name, boolean defaultValue) {
        BooleanValue value = new BooleanValue(name, defaultValue);
        booleanValues.add(value);
        controls.add(new cn.ethereal.ui.controls.Switch(name, value));
        return value;
    }

    protected NumberValue addNumberValue(String name, double defaultValue, double min, double max, double increment) {
        NumberValue value = new NumberValue(name, defaultValue, min, max, increment);
        numberValues.add(value);
        controls.add(new cn.ethereal.ui.controls.Slider(name, value));
        return value;
    }

    protected ModeValue addModeValue(String name, String[] modes, String defaultValue) {
        ModeValue value = new ModeValue(name, modes, defaultValue);
        modeValues.add(value);
        controls.add(new cn.ethereal.ui.controls.Dropdown(name, value));
        return value;
    }

    public List<Control> getControls() {
        return controls;
    }

    public List<BooleanValue> getBooleanValues() {
        return booleanValues;
    }

    public List<NumberValue> getNumberValues() {
        return numberValues;
    }

    public List<ModeValue> getModeValues() {
        return modeValues;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
            NotificationManager.getInstance().notify(this.name, enabled);
        }
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public Category getCategory() {
        return category;
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    public String getKeyName() {
        if (key == 0) {
            return "None";
        }
        return Keyboard.getKeyName(key);
    }

    protected void debug(String msg) {
        ChatUtil.normalChat(this.getName() + " " + msg);
    }

    public void onEnable() {}
    public void onDisable() {}
}