package net.ioixd.paprika;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            this.lua.functionExecute(this.functionName, Bridge.fullCoerce(sender), Bridge.fullCoerce(commandLabel), Bridge.fullCoerce(args));
            return true;
        } catch (Exception e) {
            StringBuilder err = new StringBuilder();
            err.append(ChatColor.DARK_RED).append(ChatColor.BOLD).append("Error executing ").append(this.functionName).append(ChatColor.RESET).append(ChatColor.RED).append("\n");
            // is there an underlying cause?
            if(e.getCause() != null) {
                // if so, print that error instead.
                err.append(e.getCause().getMessage());
            } else {
                err.append(e.getMessage());
            }
            err.append("\n");

            // if there is a better way to get the file name i can't find it.
            String fileName;
            try {
                Matcher m = Pattern.compile("@(.*?) ").matcher(e.getMessage());
                m.find();
                fileName = m.group(1);
                String[] fileParts = fileName.split("/");
                fileName = fileParts[fileParts.length-1];
                err.append("at ").append(fileName).append("\n");
            } catch (Exception ex) {
                // just don't add the file name at all if any exceptions come up
            }

            sender.sendMessage(err.toString());
            return false;
        }
    }
}
