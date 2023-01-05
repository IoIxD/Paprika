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
        super(name.toLowerCase());
        this.functionName = "MinecraftCommand"+name;
        this.lua = lua;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        try {
            this.lua.functionExecute(this.functionName);
            return true;
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED+""+ChatColor.BOLD+"Error executing "+this.functionName);
            sender.sendMessage(ChatColor.RED+e.getMessage());
            return false;
        }
    }
}
