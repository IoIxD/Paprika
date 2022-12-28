package net.ioixd.paprika;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class Paprika extends JavaPlugin {

    public void onEnable() {
        // Use our own tricks to display the 'enabled' message to make sure things are ok.
        Bridge lib = new Bridge();
        try {
            lib.callFunction(
                    "org.bukkit.plugin.PluginLogger",
                    "info",
                    "{'msg': 'Paprika is enabled'}",
                    this
            );
        } catch(Exception ex) {
            String stack = Arrays.toString(ex.getStackTrace()).replaceAll(",",",\n");
            getLogger().severe(ex.toString()+"\n"+stack);
        }

        // start the Lua interpreter
        Lua lua = new Lua(this);

        // command for executing lua commands.
        this.getCommand("lua").setExecutor(new LuaFunctionCommand(lua));

    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled Paprika");
    }

}