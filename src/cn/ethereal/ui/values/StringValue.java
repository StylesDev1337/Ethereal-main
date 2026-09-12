package cn.ethereal.ui.values;

public class StringValue extends Value<String> {
    private String placeholder;

    public StringValue(String name, String defaultValue) {
        super(name, defaultValue);
        this.placeholder = "";
    }

    public StringValue(String name, String defaultValue, String placeholder) {
        super(name, defaultValue);
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    public void setValue(String value) {
        if (value == null) {
            value = "";
        }
        super.setValue(value);
    }
}