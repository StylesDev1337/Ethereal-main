package cn.ethereal.command.commands;

import cn.ethereal.command.Command;
import cn.ethereal.module.Module;
import cn.ethereal.module.ModuleManager;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.stream.Collectors;

public class ModulesCommand extends Command {

    public ModulesCommand() {
        super("modules", "查看所有模块", ".modules");
        addAlias("mods");
    }

    @Override
    public void execute(String[] args) {
        List<Module> modules = ModuleManager.getInstance().getModules();

        sendMessage("§6=== 模块列表 (§f" + modules.size() + "§6) ===");

        for (Module module : modules) {
            String status = module.isEnabled() ? "§a[开]" : "§c[关]";
            String keyBind = module.getKey() != 0 ? " §7[" + Keyboard.getKeyName(module.getKey()) + "]" : "";
            sendMessage(status + " §f" + module.getName() + keyBind);
        }
    }
}