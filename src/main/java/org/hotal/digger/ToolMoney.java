package org.hotal.digger;


import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class ToolMoney {

    private final FileConfiguration config;

    public ToolMoney(FileConfiguration config) {
        this.config = config;
    }

    public boolean isToolMoneyEnabled() {
        return config.getBoolean("use-tool-money", true);
    }

    public int getMoneyForTool(Material material) {
        int reward = config.getInt("tool-money." + material.name().toLowerCase(), 50);
        System.out.println("[DEBUG] Material: " + material.name() + ", Reward: " + reward);
        return reward;
    }


    public void setToolMoneyEnabled(boolean enabled) {
        config.set("use-tool-money", enabled);
    }
}
