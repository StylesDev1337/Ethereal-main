package cn.ethereal.ui.values;

public class ModeValue extends Value<String> {
    private final String[] modes;

    public ModeValue(String name, String[] modes, String defaultValue) {
        super(name, defaultValue);
        this.modes = modes;
    }

    public String[] getModes() {
        return modes;
    }

    public void cycle() {
        int currentIndex = -1;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(value)) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = (currentIndex + 1) % modes.length;
        value = modes[nextIndex];
    }
}