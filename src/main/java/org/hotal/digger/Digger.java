package org.hotal.digger;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;

public class Digger extends JavaPlugin implements Listener {

    private final Map<UUID, Integer> blockCount = new HashMap<>();
    private Scoreboard scoreboard;
    private Economy economy;

    private Objective objective;

    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() { //起動時の初期化処理

        if (!setupEconomy()) { // 起動時のVault関係があるかどうか
            getLogger().severe("エラー：Vaultプラグインが見つかりませんでした。プラグインを無効化します。");

            if (getServer().getPluginManager().getPlugin("Vault") == null) {
                getLogger().severe("エラー：Vaultプラグインが見つかりません！！");
            } else {
                getLogger().severe("エラー：Economyサービスプロバイダが見つかりません！！");
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.getServer().getPluginManager().registerEvents(this, this);
        new BukkitRunnable() { //スコアボードの表示を1秒遅延させる
            @Override
            public void run() {
                // スコアボードの初期化
                scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                objective = scoreboard.registerNewObjective("トップ10", "dummy", "あなたの順位");
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                loadData(); // player-data.ymlの中身を読み込む。
            }
        }.runTaskLater(this, 20L); //1秒遅延（20tick=1秒）
    } //起動時の初期化処理ここまで

    private boolean setupEconomy() { //Vaultのセットアップ、各種エラー
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("エラー：Vaultプラグインが見つかりませんでした。");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("エラー:Economyサービスプロバイダが登録されていません");
            return false;
        }
        economy = rsp.getProvider();
        if (economy == null) {
            getLogger().warning("エラー：Economyサービスが見つかりません");
            return false;
        }
        return true;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // スコアボードやobjectiveがnullかどうかを確認
        if (scoreboard == null || objective == null) {
            return; // スコアボードやobjectiveがnullなら処理を終了
        }

        UUID playerID = event.getPlayer().getUniqueId();
        blockCount.put(playerID, blockCount.getOrDefault(playerID, 0) + 1);


        if (Math.random() < 0.02) { //2%(1.00で100%)。
            economy.depositPlayer(event.getPlayer(), 50);
            event.getPlayer().sendMessage("§a 50NANNDEを手に入れました");
        }
        updateScoreboard(playerID); // playerID を引数として渡す
        event.getPlayer().setScoreboard(scoreboard);

    }

    private void updateScoreboard(UUID playerUUID) {
        if (scoreboard == null || objective == null) return; // ここにnullチェックを追加

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
            Player player = Bukkit.getPlayer(entry.getKey());
            if(player == null) continue;
            playerName = player.getName();
            int score = entry.getValue();
            objective.getScore(playerName).setScore(score);
        }


        // プレイヤー自身のランキングとスコアを表示
        int playerRank = 1;
        int playerScore = blockCount.getOrDefault(playerUUID, 0);
        for (Map.Entry<UUID, Integer> entry : sortedList) {
            if (entry.getKey().equals(playerUUID)) {
                break;
            }
            playerRank++;
        }


        if (playerRank > sortedList.size()) {

            return;
        }

        String rankDisplay = "あなたの順位: " + playerRank + "位";
        objective.getScore(rankDisplay).setScore(playerScore);

    }
        @Override
        public void onDisable () { //終了処理
            saveData();
        }

        private void loadData () { //データ保存メソッド（読み込み）
            dataFile = new File(getDataFolder(), "player-data.yml");
            if (!dataFile.exists()) {
                saveResource("player-data.yml", false);
            }
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);

            if (dataConfig.contains("blockCount")) {
                blockCount.clear();
                for (String uuidString : dataConfig.getConfigurationSection("blockCount").getKeys(false)) {
                    UUID uuid = UUID.fromString(uuidString);
                    int count = dataConfig.getInt("blockCount." + uuidString);
                    blockCount.put(uuid, count);
                }
            }
        } //読み込みメソッドここまで

        private void saveData(){ //保存メソッドここから
            for (UUID uuid : blockCount.keySet()) {
                dataConfig.set("blockCount." + uuid.toString(), blockCount.get(uuid));
            }
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                getLogger().severe("データファイルの保存中にエラーが発生しました。 " + e.getMessage());
            }
        } //データ保存メソッドここまで
}
