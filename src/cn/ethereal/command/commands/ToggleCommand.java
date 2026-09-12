package cn.ethereal.command.commands;

import cn.ethereal.command.Command;
import cn.ethereal.module.Module;
import cn.ethereal.module.ModuleManager;

public class ToggleCommand extends Command {

    public ToggleCommand() {
        super("toggle", "切换模块开关", ".toggle <模块名>");
        addAlias("t");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            sendMessage("§6用法: §f" + getUsage());
            return;
        }

        Module module = ModuleManager.getInstance().getModule(args[0]);
        if (module == null) {
            sendError("§c模块 §f'" + args[0] + "' §c不存在!");
            return;
        }

        module.toggle();
        sendMessage("§a模块 §f'" + module.getName() + "' §a已" + (module.isEnabled() ? "§2启用" : "§c禁用"));
    }
}