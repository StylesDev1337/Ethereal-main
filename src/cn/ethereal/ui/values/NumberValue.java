package cn.ethereal.ui.values;

public class NumberValue extends Value<Double> {
    private final double min;
    private final double max;
    private final double increment;

    public NumberValue(String name, double defaultValue, double min, double max, double increment) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getIncrement() {
        return increment;
    }

    @Override
    public void setValue(Double value) {
        // 确保值在范围内
        value = Math.max(min, Math.min(max, value));
        // 按增量取整
        value = Math.round(value / increment) * increment;
        super.setValue(value);
    }
}