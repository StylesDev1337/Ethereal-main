package cn.ethereal;

import cn.ethereal.command.CommandManager;
import cn.ethereal.config.AutoSaveManager;
import cn.ethereal.config.ConfigManager;
import cn.ethereal.hud.HudManager;
import cn.ethereal.hud.impl.*;
import cn.ethereal.module.ModuleManager;
import cn.ethereal.ui.notification.NotificationManager;
import org.lwjgl.opengl.Display;

public class Ethereal {
    public static final String NAME = "Ethereal";
    public static final String VERSION = "Beta";
    public static final String PREMIX = "§fE§9t§3h§ae§2r§6e§ea§fl";

    public static void start() {
        NotificationManager.getInstance();

        ModuleManager manager = ModuleManager.getInstance();
        manager.registerModules();

        CommandManager commandManager = CommandManager.getInstance();
        commandManager.registerCommand();

        ConfigManager configManager = ConfigManager.getInstance();
        configManager.loadConfig();

        HudManager hudManager = HudManager.getInstance();
        hudManager.register(new ArrayListHud());
        hudManager.register(new WatermarkHud());
        hudManager.register(new UsernameHud());
        hudManager.register(new ScoreboardHud());
        hudManager.register(new TargetHud());

        AutoSaveManager.getInstance();

        System.out.println("Loaded " + manager.getModuleCount() + " modules");

        Display.setTitle(NAME + " " + getLauncherModByInt(0));
    }

    public static void stop() {
        // 保存配置
        ConfigManager.getInstance().saveConfig();
        System.out.println("Stopped.");
    }

    public static String getLauncherModByInt(int mode) {
        if (mode == 0) {
            return "Local Test";
        } else if (mode == 1) {
            return "Release Version";
        } else {
            return "Invilad Code";
        }
    }
}