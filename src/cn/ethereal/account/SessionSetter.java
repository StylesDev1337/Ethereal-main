package cn.ethereal.account;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.lang.reflect.Field;

public class SessionSetter {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static Field sessionField;

    static {
        try {
            sessionField = Minecraft.class.getDeclaredField("session");
            sessionField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (f.getType() == Session.class) {
                    f.setAccessible(true);
                    sessionField = f;
                    break;
                }
            }
        }
    }

    public static boolean setSession(String username, String accessToken, String uuid) {
        if (sessionField == null) {
            System.err.println("[Ethereal] 无法找到 session 字段");
            return false;
        }
        try {
            Session session = new Session(username, uuid, accessToken, "mojang");
            sessionField.set(mc, session);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}