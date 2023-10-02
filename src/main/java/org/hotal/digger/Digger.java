package org.hotal.digger;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@SuppressWarnings("UsagesOfObsoleteApi")
public final class Digger extends JavaPlugin implements Listener {

    private final Map<UUID,Integer> blockCount = new HashMap<>();
    private Scoreboard scoreboard;

    @Override
    public void onEnable() {
        // Plugin startup logic
        if (!setupEconomy()) {
            getLogger().severe("&4Vaultプラグインが見つかりませんでした。プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

       this.getServer().getPluginManager().registerEvents(this,this);

       //スコアボードの初期化
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("トップ10","トップ10","あなたの順位");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();
        blockCount.put(playerID,blockCount.getOrDefault(playerID,0)+1);

        if (Math.random() < 0.02) {
            economy.depositPlayer(event.getPlayer(),50);
            event.getPlayer().sendMessage("&350NANNDEを手に入れました");

        }
        updateScoreboard();

    }

    private void updateScoreboard(){

    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null ) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

}

