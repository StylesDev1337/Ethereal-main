package cn.ethereal.command.commands;

import cn.ethereal.command.Command;
import cn.ethereal.config.ConfigManager;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "管理配置文件", ".config <save|load|reset>");
        addAlias("cfg");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            sendUsage();
            return;
        }

        ConfigManager configManager = ConfigManager.getInstance();

        switch (args[0].toLowerCase()) {
            case "save":
                configManager.saveConfig();
                sendMessage("§a配置已保存!");
                break;

            case "load":
                configManager.loadConfig();
                sendMessage("§a配置已加载!");
                break;

            case "reset":
                configManager.resetConfig();
                sendMessage("§e配置已重置!");
                break;

            case "reload":
                configManager.loadConfig();
                sendMessage("§a配置已重新加载!");
                break;

            default:
                sendUsage();
                break;
        }
    }

    private void sendUsage() {
        sendMessage("§6用法:");
        sendMessage("§f.config save §7- 保存配置");
        sendMessage("§f.config load §7- 加载配置");
        sendMessage("§f.config reset §7- 重置配置");
        sendMessage("§f.config reload §7- 重新加载配置");
    }
}