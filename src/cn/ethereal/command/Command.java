package cn.ethereal.command;

import cn.ethereal.util.chat.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import java.util.ArrayList;
import java.util.List;

public abstract class Command {
    private final String name;
    private final String description;
    private final String usage;
    private final List<String> aliases = new ArrayList<>();

    public Command(String name, String description, String usage) {
        this.name = name;
        this.description = description;
        this.usage = usage;
    }

    public abstract void execute(String[] args);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void addAlias(String alias) {
        aliases.add(alias);
    }

    // 发送消息给玩家
    protected void sendMessage(String message) {
        ChatUtil.chat(message);
    }

    // 发送错误消息
    protected void sendError(String message) {
        ChatUtil.chat(message);
    }
}