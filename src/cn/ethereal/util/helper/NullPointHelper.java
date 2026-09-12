package cn.ethereal.util.helper;

import cn.ethereal.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class NullPointHelper extends Util {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isPlayerInWorld() {
        return mc.thePlayer != null && mc.theWorld != null;
    }

    public static EntityPlayer getPlayer() {
        return mc.thePlayer;
    }

    public static World getWorld() {
        return mc.theWorld;
    }

    public static void ifPlayerInWorld(Runnable action) {
        if (isPlayerInWorld()) {
            action.run();
        }
    }
}
