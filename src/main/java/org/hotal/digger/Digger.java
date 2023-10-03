package org.hotal.digger;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Digger extends JavaPlugin implements Listener {

    private final Map<UUID, Integer> blockCount = new HashMap<>();
    private Scoreboard scoreboard;
    private Economy economy;

    @Override
    public void onEnable() {
        // Plugin startup logic
        if (!setupEconomy()) {
            getLogger().severe("Vaultプラグインが見つかりませんでした。プラグインを無効化します。");

            if (getServer().getPluginManager().getPlugin("Vault") == null) {
                getLogger().severe("Vaultプラグインが見つかりません！！");
            } else {
                getLogger().severe("Economyサービスプロバイダが見つかりません！！");
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.getServer().getPluginManager().registerEvents(this, this);
        new BukkitRunnable() {
            @Override
            public void run() {
                // スコアボードの初期化
                scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                Objective objective = scoreboard.registerNewObjective("トップ10", "dummy", "あなたの順位");
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
        }.runTaskLater(this, 20L); // Run 1 second (20 ticks) after the plugin is enabled
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vaultプラグインが見つかりませんでした。");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("Economyサービスプロバイダが登録されていません");
            return false;
        }
        economy = rsp.getProvider();
        if (economy == null) {
            getLogger().warning("Economyサービスが見つかりません");
            return false;
        }
        return true;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();
        blockCount.put(playerID, blockCount.getOrDefault(playerID, 0) + 1);

        getLogger().info("Block broken by " + event.getPlayer().getName()); //デバッグ用

        if (Math.random() < 0.02) {
            economy.depositPlayer(event.getPlayer(), 50);
            event.getPlayer().sendMessage("50NANNDEを手に入れました");
        }
        updateScoreboard(playerID); // playerID を引数として渡す
    }

    private void updateScoreboard(UUID playerUUID) {
        Objective objective = scoreboard.getObjective("トップ10");

        // ブロックのカウントを降順にソート
        List<Map.Entry<UUID, Integer>> sortedList = blockCount.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(11)  // トップ10 + 現在のプレイヤー
                .collect(Collectors.toList());

        // トップ10のプレイヤーを表示
        for (int i = 0; i < Math.min(10, sortedList.size()); i++) {
            Map.Entry<UUID, Integer> entry = sortedList.get(i);
            String playerName = Bukkit.getPlayer(entry.getKey()).getName();
            int score = entry.getValue();
            objective.getScore(playerName).setScore(score);
        }

        getLogger().info("Updating scoreboard for " + Bukkit.getPlayer(playerUUID).getName());

// プレイヤー自身のランキングとスコアを表示
        int playerRank = 1;
        int playerScore = blockCount.get(playerUUID);
        for (Map.Entry<UUID, Integer> entry : sortedList) {
            if (entry.getKey().equals(playerUUID)) {
                break;
            }
            playerRank++;
        }
        getLogger().info("Updating scoreboard for " + Bukkit.getPlayer(playerUUID).getName());

// プレイヤー自身のランキングとスコアを表示
        playerRank = 1;
        playerScore = blockCount.get(playerUUID);
        for (Map.Entry<UUID, Integer> entry : sortedList) {
            if (entry.getKey().equals(playerUUID)) {
                break;
            }
            playerRank++;
        }

        String rankDisplay = "あなたの順位: " + playerRank + "位";
        if (objective.getScore(rankDisplay).getScore() != playerScore) {
            objective.getScore(rankDisplay).setScore(playerScore);
        }
    }
}

