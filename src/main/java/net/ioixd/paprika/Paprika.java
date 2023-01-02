package net.ioixd.paprika;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class Paprika extends JavaPlugin {

    public void onEnable() {
        // start the Lua interpreter
        Lua lua = new Lua(this);

        // command for executing lua commands.
        this.getCommand("lua").setExecutor(new LuaFunctionCommand(lua));

        getLogger().info("Paprika is enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled Paprika");
    }

}