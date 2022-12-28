package net.ioixd.paprika.lua_functions;

import org.bukkit.plugin.Plugin;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

public class print extends VarArgFunction {
    Plugin plugin;

    public print(Plugin plugin) {
        this.plugin = plugin;
    }

    public LuaValue call(LuaValue str) {
        plugin.getServer().broadcastMessage(str.toString());
        return LuaValue.valueOf(true);
    }
}
