package net.ioixd.paprika;

import org.bukkit.plugin.java.JavaPlugin;

public class Paprika extends JavaPlugin {

    public void onEnable() {
        getLogger().info("Enabled Paprika");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled Paprika");
    }

}