package net.ioixd.paprika;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class Paprika extends JavaPlugin {

    Lua lua;

    public void onEnable() {
        // start the Lua interpreter
        this.lua = new Lua(this);

        // command for executing lua commands.
        this.getCommand("lua").setExecutor(new LuaFunctionCommand(this));

        getLogger().info("Paprika is enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled Paprika");
    }

}