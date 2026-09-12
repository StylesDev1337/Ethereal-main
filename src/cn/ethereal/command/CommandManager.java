package cn.ethereal.command;

import cn.ethereal.command.commands.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommandManager {
    private static CommandManager instance;
    private final List<Command> commands = new ArrayList<>();

    private CommandManager() {}

    public static CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    public void registerCommand() {
        commands.add(new BindCommand());
        commands.add(new ModulesCommand());
        commands.add(new ToggleCommand());
        commands.add(new ConfigCommand());
        commands.add(new LoginCommand());
    }

    public List<Command> getCommands() {
        return commands;
    }

    public Optional<Command> getCommand(String name) {
        return commands.stream()
                .filter(cmd -> cmd.getName().equalsIgnoreCase(name) ||
                        cmd.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(name)))
                .findFirst();
    }

    // 处理命令
    public boolean handleCommand(String message) {
        if (!message.startsWith(".")) return false;

        String[] parts = message.substring(1).split(" ");
        if (parts.length == 0) return false;

        String commandName = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, parts.length - 1);

        Optional<Command> command = getCommand(commandName);
        if (command.isPresent()) {
            try {
                command.get().execute(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        return false;
    }
}