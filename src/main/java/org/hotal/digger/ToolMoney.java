package org.hotal.digger;


import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
public class ToolMoney {

    private final FileConfiguration config;
    private final JavaPlugin plugin;  // JavaPluginのインスタンスへの参照

    public ToolMoney(FileConfiguration config, JavaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    public boolean isToolMoneyEnabled() {
        return config.getBoolean("use-tool-money", false);
    }

    public int getMoneyForTool(Material material) {
        int reward = config.getInt("tool-money." + material.name(), 50);
        return reward;
    }

    public void setToolMoneyEnabled(boolean enabled) {
        config.set("use-tool-money", false);
    }
}
