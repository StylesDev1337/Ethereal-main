package cn.ethereal.ui.values;

public class BooleanValue extends Value<Boolean> {

    public BooleanValue(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public void toggle() {
        value = !value;
    }

    public boolean isEnabled() {
        return value;
    }
}