package net.ioixd.paprika;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class Paprika extends JavaPlugin {

    public void onEnable() {
        UnsafeLibrary lib = new UnsafeLibrary();
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
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled Paprika");
    }

}