package cn.ethereal.util.key;

import cn.ethereal.util.Util;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

public class KeyUtil extends Util {

    // 正确的字母键映射
    private static final Map<Character, Integer> LETTER_KEYS = new HashMap<>();

    static {
        LETTER_KEYS.put('A', Keyboard.KEY_A);
        LETTER_KEYS.put('B', Keyboard.KEY_B);
        LETTER_KEYS.put('C', Keyboard.KEY_C);
        LETTER_KEYS.put('D', Keyboard.KEY_D);
        LETTER_KEYS.put('E', Keyboard.KEY_E);
        LETTER_KEYS.put('F', Keyboard.KEY_F);
        LETTER_KEYS.put('G', Keyboard.KEY_G);
        LETTER_KEYS.put('H', Keyboard.KEY_H);
        LETTER_KEYS.put('I', Keyboard.KEY_I);
        LETTER_KEYS.put('J', Keyboard.KEY_J);
        LETTER_KEYS.put('K', Keyboard.KEY_K);
        LETTER_KEYS.put('L', Keyboard.KEY_L);
        LETTER_KEYS.put('M', Keyboard.KEY_M);
        LETTER_KEYS.put('N', Keyboard.KEY_N);
        LETTER_KEYS.put('O', Keyboard.KEY_O);
        LETTER_KEYS.put('P', Keyboard.KEY_P);
        LETTER_KEYS.put('Q', Keyboard.KEY_Q);
        LETTER_KEYS.put('R', Keyboard.KEY_R);
        LETTER_KEYS.put('S', Keyboard.KEY_S);
        LETTER_KEYS.put('T', Keyboard.KEY_T);
        LETTER_KEYS.put('U', Keyboard.KEY_U);
        LETTER_KEYS.put('V', Keyboard.KEY_V);
        LETTER_KEYS.put('W', Keyboard.KEY_W);
        LETTER_KEYS.put('X', Keyboard.KEY_X);
        LETTER_KEYS.put('Y', Keyboard.KEY_Y);
        LETTER_KEYS.put('Z', Keyboard.KEY_Z);
    }

    // 解析按键名称到键码
    public static int parseKey(String keyName) {
        if (keyName == null || keyName.isEmpty()) {
            return 0;
        }

        String key = keyName.toUpperCase().trim();

        // 特殊处理
        if (key.equals("NONE") || key.equals("CLEAR") || key.equals("0")) {
            return 0;
        }

        // 尝试直接解析为数字
        try {
            int numKey = Integer.parseInt(key);
            if (numKey >= 0 && numKey <= 255) {
                return numKey;
            }
        } catch (NumberFormatException ignored) {}

        // 修复：使用正确的字母键映射
        if (key.length() == 1 && LETTER_KEYS.containsKey(key.charAt(0))) {
            return LETTER_KEYS.get(key.charAt(0));
        }

        // 数字键 0-9（这些在 LWJGL 中是连续的）
        if (key.length() == 1 && key.charAt(0) >= '0' && key.charAt(0) <= '9') {
            return Keyboard.KEY_0 + (key.charAt(0) - '0');
        }

        // 功能键 F1-F12（这些在 LWJGL 中是连续的）
        for (int i = 1; i <= 12; i++) {
            if (key.equals("F" + i)) {
                return Keyboard.KEY_F1 + (i - 1);
            }
        }

        // 特殊按键映射
        switch (key) {
            case "ESC":
            case "ESCAPE":
                return Keyboard.KEY_ESCAPE;
            case "ENTER":
            case "RETURN":
                return Keyboard.KEY_RETURN;
            case "SPACE":
            case "SPACEBAR":
                return Keyboard.KEY_SPACE;
            case "SHIFT":
            case "LSHIFT":
            case "LEFTSHIFT":
                return Keyboard.KEY_LSHIFT;
            case "RSHIFT":
            case "RIGHTSHIFT":
                return Keyboard.KEY_RSHIFT;
            case "CTRL":
            case "CONTROL":
            case "LCTRL":
            case "LCONTROL":
            case "LEFTCTRL":
                return Keyboard.KEY_LCONTROL;
            case "RCTRL":
            case "RCONTROL":
            case "RIGHTCTRL":
                return Keyboard.KEY_RCONTROL;
            case "ALT":
            case "LALT":
            case "LMENU":
            case "LEFTALT":
                return Keyboard.KEY_LMENU;
            case "RALT":
            case "RMENU":
            case "RIGHTALT":
                return Keyboard.KEY_RMENU;
            case "TAB":
                return Keyboard.KEY_TAB;
            case "BACKSPACE":
            case "BACK":
                return Keyboard.KEY_BACK;
            case "DELETE":
            case "DEL":
                return Keyboard.KEY_DELETE;
            case "INSERT":
            case "INS":
                return Keyboard.KEY_INSERT;
            case "HOME":
                return Keyboard.KEY_HOME;
            case "END":
                return Keyboard.KEY_END;
            case "PAGEUP":
            case "PGUP":
                return Keyboard.KEY_PRIOR;
            case "PAGEDOWN":
            case "PGDN":
                return Keyboard.KEY_NEXT;
            case "UP":
            case "ARROWUP":
                return Keyboard.KEY_UP;
            case "DOWN":
            case "ARROWDOWN":
                return Keyboard.KEY_DOWN;
            case "LEFT":
            case "ARROWLEFT":
                return Keyboard.KEY_LEFT;
            case "RIGHT":
            case "ARROWRIGHT":
                return Keyboard.KEY_RIGHT;
            case "CAPSLOCK":
            case "CAPS":
                return Keyboard.KEY_CAPITAL;
            case "NUMPAD0":
                return Keyboard.KEY_NUMPAD0;
            case "NUMPAD1":
                return Keyboard.KEY_NUMPAD1;
            case "NUMPAD2":
                return Keyboard.KEY_NUMPAD2;
            case "NUMPAD3":
                return Keyboard.KEY_NUMPAD3;
            case "NUMPAD4":
                return Keyboard.KEY_NUMPAD4;
            case "NUMPAD5":
                return Keyboard.KEY_NUMPAD5;
            case "NUMPAD6":
                return Keyboard.KEY_NUMPAD6;
            case "NUMPAD7":
                return Keyboard.KEY_NUMPAD7;
            case "NUMPAD8":
                return Keyboard.KEY_NUMPAD8;
            case "NUMPAD9":
                return Keyboard.KEY_NUMPAD9;
            case "ADD":
            case "PLUS":
                return Keyboard.KEY_ADD;
            case "SUBTRACT":
                return Keyboard.KEY_SUBTRACT;
            case "MULTIPLY":
                return Keyboard.KEY_MULTIPLY;
            case "DIVIDE":
                return Keyboard.KEY_DIVIDE;
            case "DECIMAL":
                return Keyboard.KEY_DECIMAL;
            case "PERIOD":
            case "DOT":
                return Keyboard.KEY_PERIOD;
            case "COMMA":
                return Keyboard.KEY_COMMA;
            case "SEMICOLON":
                return Keyboard.KEY_SEMICOLON;
            case "APOSTROPHE":
            case "QUOTE":
                return Keyboard.KEY_APOSTROPHE;
            case "LBRACKET":
            case "LEFTBRACKET":
                return Keyboard.KEY_LBRACKET;
            case "RBRACKET":
            case "RIGHTBRACKET":
                return Keyboard.KEY_RBRACKET;
            case "BACKSLASH":
                return Keyboard.KEY_BACKSLASH;
            case "SLASH":
                return Keyboard.KEY_SLASH;
            case "GRAVE":
            case "TILDE":
                return Keyboard.KEY_GRAVE;
            case "MINUS":
                return Keyboard.KEY_MINUS;
            case "EQUALS":
                return Keyboard.KEY_EQUALS;
        }

        return -1;
    }

    // 获取按键名称
    public static String getKeyName(int keyCode) {
        if (keyCode == 0) {
            return "None";
        }
        return Keyboard.getKeyName(keyCode);
    }

    // 检查是否是有效的按键
    public static boolean isValidKey(int keyCode) {
        return keyCode > 0 && keyCode < 256;
    }

    // 获取所有可用的按键名称
    public static String[] getAllKeyNames() {
        java.util.List<String> keyNames = new java.util.ArrayList<>();
        keyNames.add("None");

        // 字母键 - 使用正确的顺序
        for (char c = 'A'; c <= 'Z'; c++) {
            if (LETTER_KEYS.containsKey(c)) {
                keyNames.add(Keyboard.getKeyName(LETTER_KEYS.get(c)));
            }
        }

        // 数字键
        for (int i = Keyboard.KEY_0; i <= Keyboard.KEY_9; i++) {
            keyNames.add(Keyboard.getKeyName(i));
        }

        // 功能键
        for (int i = Keyboard.KEY_F1; i <= Keyboard.KEY_F12; i++) {
            keyNames.add(Keyboard.getKeyName(i));
        }

        // 特殊键
        int[] specialKeys = {
                Keyboard.KEY_ESCAPE, Keyboard.KEY_RETURN, Keyboard.KEY_SPACE,
                Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT,
                Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL,
                Keyboard.KEY_LMENU, Keyboard.KEY_RMENU,
                Keyboard.KEY_TAB, Keyboard.KEY_BACK, Keyboard.KEY_DELETE,
                Keyboard.KEY_INSERT, Keyboard.KEY_HOME, Keyboard.KEY_END,
                Keyboard.KEY_PRIOR, Keyboard.KEY_NEXT,
                Keyboard.KEY_UP, Keyboard.KEY_DOWN, Keyboard.KEY_LEFT, Keyboard.KEY_RIGHT
        };

        for (int key : specialKeys) {
            keyNames.add(Keyboard.getKeyName(key));
        }

        return keyNames.toArray(new String[0]);
    }
}