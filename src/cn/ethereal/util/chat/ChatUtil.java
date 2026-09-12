package cn.ethereal.util.chat;

import cn.ethereal.Ethereal;
import cn.ethereal.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public class ChatUtil extends Util {
    private static void buildChat(String chat) {
        Minecraft mc = Minecraft.getMinecraft();
        String msg = Ethereal.PREMIX + " " + chat;
        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(msg));
    }

    public static void chat(String msg) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        buildChat(msg);
    }

    public static void normalChat(String msg) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(msg));
    }
}
