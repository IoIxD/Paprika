package net.ioixd.paprika;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import net.ioixd.paprika.Lua;

public class LuaFunctionCommand implements CommandExecutor {
    Lua lua;

    LuaFunctionCommand(Lua lua) {
        this.lua = lua;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0) {
            sender.sendMessage("Just do '/lua [function_name]'. Or '/lua list' to list functions");
            return true;
        }
        if(args[0].matches("list")) {
            sender.sendMessage(this.lua.listFunctions());
            return true;
        }

        try {
            lua.functionExecute(args[0]);
        } catch(Exception ex) {
            sender.sendMessage(ex.getMessage());
            return false;
        }
        return true;
    }
}
