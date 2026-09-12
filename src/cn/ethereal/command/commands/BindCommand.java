package cn.ethereal.command.commands;

import cn.ethereal.command.Command;
import cn.ethereal.module.Module;
import cn.ethereal.module.ModuleManager;
import cn.ethereal.util.key.KeyUtil;

public class BindCommand extends Command {

    public BindCommand() {
        super("bind", "绑定按键到模块", ".bind <模块名> <按键>");
        addAlias("b");
        addAlias("keybind");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            sendUsage();
            return;
        }

        switch (args.length) {
            case 1:
                if (args[0].equalsIgnoreCase("list")) {
                    listBinds();
                } else {
                    showModuleBind(args[0]);
                }
                break;

            case 2:
                if (args[1].equalsIgnoreCase("none") || args[1].equalsIgnoreCase("clear")) {
                    clearBind(args[0]);
                } else {
                    setBind(args[0], args[1]);
                }
                break;

            default:
                sendUsage();
                break;
        }
    }

    private void setBind(String moduleName, String keyName) {
        Module module = ModuleManager.getInstance().getModule(moduleName);
        if (module == null) {
            sendError("§c模块 §f'" + moduleName + "' §c不存在!");
            return;
        }

        int keyCode = KeyUtil.parseKey(keyName);
        if (keyCode == -1) {
            sendError("§c无效的按键: §f" + keyName);
            return;
        }

        // ★ 不再强制解绑，只提醒
        Module existingModule = ModuleManager.getInstance().getModuleByKey(keyCode);
        if (existingModule != null && existingModule != module) {
            sendMessage("§e提示: 按键 §f" + KeyUtil.getKeyName(keyCode)
                    + " §e已被 §f'" + existingModule.getName() + "' §e占用");
        }

        module.setKey(keyCode);
        String displayKey = KeyUtil.getKeyName(keyCode);
        sendMessage("§a模块 §f'" + module.getName() + "' §a已绑定到按键 §f" + displayKey);
    }

    private void clearBind(String moduleName) {
        Module module = ModuleManager.getInstance().getModule(moduleName);
        if (module == null) {
            sendError("§c模块 §f'" + moduleName + "' §c不存在!");
            return;
        }

        int oldKey = module.getKey();
        module.setKey(0);
        sendMessage("§a模块 §f'" + module.getName() + "' §a的按键绑定已清除 §7(原按键: " + KeyUtil.getKeyName(oldKey) + ")");
    }

    private void showModuleBind(String moduleName) {
        Module module = ModuleManager.getInstance().getModule(moduleName);
        if (module == null) {
            sendError("§c模块 §f'" + moduleName + "' §c不存在!");
            return;
        }

        if (module.getKey() == 0) {
            sendMessage("§e模块 §f'" + module.getName() + "' §e未绑定按键");
        } else {
            sendMessage("§a模块 §f'" + module.getName() + "' §a绑定在按键 §f" + module.getKeyName());
        }
    }

    private void listBinds() {
        java.util.List<Module> modules = ModuleManager.getInstance().getModules();
        sendMessage("§6=== 按键绑定列表 ===");

        boolean hasBinds = false;
        for (Module module : modules) {
            if (module.getKey() != 0) {
                hasBinds = true;
                sendMessage("§f" + module.getName() + " §7-> §b" + module.getKeyName());
            }
        }

        if (!hasBinds) {
            sendMessage("§e当前没有模块绑定按键");
        }
    }

    private void sendUsage() {
        sendMessage("§6用法:");
        sendMessage("§f.bind <模块名> <按键> §7- 绑定按键");
        sendMessage("§f.bind <模块名> §7- 查看绑定");
        sendMessage("§f.bind <模块名> none §7- 清除绑定");
        sendMessage("§f.bind list §7- 查看所有绑定");
    }
}