package net.ioixd.paprika;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class LuaFunctionCommand implements CommandExecutor {
    Lua lua;

    Map<String, Callable<String>> commands = new HashMap<>();
    LuaFunctionCommand(Lua lua) {
        this.lua = lua;

        this.commands.put("reload", this.lua::reload);
        this.commands.put("help", this::printNativeFunctions);
        this.commands.put("list", this::printListHelp);
        this.commands.put("list native", this.lua::listMinecraftFunctions);
        this.commands.put("list custom", this.lua::listCustomFunctions);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0) {
            sender.sendMessage(this.printNativeFunctions());
            return true;
        }

        // check if they're executing a standard command
        String fullArgs = String.join(" ",args);
        for(Map.Entry<String, Callable<String>> obj : this.commands.entrySet()) {
            if(fullArgs.equals(obj.getKey())) {
                try {
                    sender.sendMessage(obj.getValue().call());
                    return true;
                } catch(Exception ex) {
                    sender.sendMessage(ChatColor.RED+ex.getMessage());
                    return false;
                }
            }
        }

        // if that fails, try and search for a function basedd on what they provided.
        try {
            lua.functionExecute(args[0]);
        } catch(Exception ex) {
            sender.sendMessage(ex.getMessage());
            return false;
        }
        return true;
    }

    public String printListHelp() {
        String str = "/lua list native - print the functions that you can use in Lua that map to Spigot's functions.\n";
        str += "/lua list custom - print the functions you've made.\n";
        return str;
    }

    public String printNativeFunctions() {
        String str = "/lua <option>\n";
        str += "/lua help - print this\n";
        str += "/lua reload - reload the lua files.\n";
        str += printListHelp();
        str += "/lua <functionName> - execute the provided function name\n";
        return str;
    }
}
