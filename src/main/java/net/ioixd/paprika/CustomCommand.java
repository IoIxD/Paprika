package net.ioixd.paprika;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.LuaError;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class CustomCommand extends Command {
    Lua lua;
    String functionName;

    protected CustomCommand(@NotNull String name, Lua lua) {
        super(name);
        String funcName = name.substring(0, 1).toUpperCase() + name.substring(1);
        this.functionName = "MinecraftCommand"+funcName;
        this.lua = lua;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        try {
            this.lua.functionExecute(functionName);
            return true;
        } catch (LuaError e) {
            sender.sendMessage(ChatColor.RED+e.getMessage());
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
