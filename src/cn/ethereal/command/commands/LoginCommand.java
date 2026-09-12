package cn.ethereal.command.commands;

import cn.ethereal.account.MicrosoftLoginManager;
import cn.ethereal.command.Command;

public class LoginCommand extends Command {

    public LoginCommand() {
        super("login", "微软正版登录", ".login");
        addAlias("mslogin");
    }

    @Override
    public void execute(String[] args) {
        if (MicrosoftLoginManager.isLoggingIn()) {
            sendMessage("§e[正版登录] 已经有一个登录流程在进行了");
            return;
        }
        sendMessage("§e[正版登录] 正在启动...");
        MicrosoftLoginManager.startLogin();
    }
}