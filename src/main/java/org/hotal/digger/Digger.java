package org.hotal.digger;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Location;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;

public class Digger extends JavaPlugin implements Listener {

    private final Map<UUID, Integer> blockCount = new HashMap<>();
    private Scoreboard scoreboard;
    private Economy economy;
    private long scoreboardUpdateInterval = 1200L;
    private Objective objective;

    private File dataFile;
    private FileConfiguration dataConfig;

    private final List<Location> placedBlocks = new ArrayList<>();

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
        new BukkitRunnable() {
            @Override
            public void run() {
                // スコアボードの初期化
                scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                objective = scoreboard.registerNewObjective("整地の順位", "dummy", "あなたの順位");
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                loadData(); // player-data.ymlの中身を読み込む。
            }
        }.runTaskLater(this, 20L); //1秒遅延（20tick=1秒）
        startScoreboardUpdater();
        this.saveDefaultConfig();
    } //起動時の初期化処理ここまで

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("§4エラー：Vaultプラグインが見つかりませんでした。");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("§4エラー:Economyサービスプロバイダが登録されていません");
            return false;
        }
        economy = rsp.getProvider();
        if (economy == null) {
            getLogger().warning("§4エラー：Economyサービスが見つかりません");
            return false;
        }
        return true;
    }


    private void startScoreboardUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllPlayersScoreboard();
            }
        }.runTaskTimer(this, 20L, scoreboardUpdateInterval);  // 開始は1秒後、その後は指定された間隔で更新
    }

    private void updateAllPlayersScoreboard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player.getUniqueId());
            player.setScoreboard(scoreboard);
        }

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("digger")) {
            if (args.length == 1) {
                try {
                    int minutes = Integer.parseInt(args[0].replace("m", ""));
                    scoreboardUpdateInterval = minutes * 60L * 20L;  // 分をticksに変換
                    sender.sendMessage("§aスコアボードの更新間隔を " + minutes + " 分に設定しました。");
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c無効な時間が指定されました。例：/digger 1m");
                }
            } else {
                sender.sendMessage("§3使用方法: /digger [時間(分)]");
            }
        }
        return false;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        placedBlocks.add(event.getBlock().getLocation());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (placedBlocks.contains(event.getBlock().getLocation())) {
            placedBlocks.remove(event.getBlock().getLocation());
        }
        List<String> blacklist = this.getConfig().getStringList("block-blacklist");//ブラックリスト機能
        if (blacklist.contains(event.getBlock().getType().name())) {
            return;
        }
        if (scoreboard == null || objective == null) {
            return;
        }
        UUID playerID = event.getPlayer().getUniqueId();
        blockCount.put(playerID, blockCount.getOrDefault(playerID, 0) + 1);
        if (Math.random() < 0.03) { //3%
            economy.depositPlayer(event.getPlayer(), 50); //50NANNDE 追加
            event.getPlayer().sendMessage("§a 50NANNDEを手に入れました");
        }
    }

    private void updateScoreboard(UUID playerUUID) {
        if (scoreboard == null || objective == null) return;

        List<Map.Entry<UUID, Integer>> sortedList = blockCount.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(11)
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(10, sortedList.size()); i++) {
            Map.Entry<UUID, Integer> entry = sortedList.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;

            int score = entry.getValue();
            String playerName = player.getName();
            String display = playerName + ":" + score;
            objective.getScore(playerName).setScore(score);
        }

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

        String rankDisplay = "§6あなたの順位: " + playerRank + "位";
        objective.getScore(rankDisplay).setScore(playerScore);
    }

    @Override
    public void onDisable() {
        saveData();
    }
    private void loadData() {
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

                if (dataConfig.contains("placedBlocks")) {
                    placedBlocks.clear();
                    for (String blockLocString : dataConfig.getStringList("placedBlocks")) {
                        placedBlocks.add(stringToLocation(blockLocString));
                    }
                }
            }
        }
    }
    private Location stringToLocation(String s) {
        String[] parts = s.split(",");
        World world = Bukkit.getWorld(parts[0]);
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        return new Location(world, x, y, z);

    }
    private void saveData() {
            for (UUID uuid : blockCount.keySet()) {
                dataConfig.set("blockCount." + uuid.toString(), blockCount.get(uuid));
            }
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                getLogger().severe("§aデータファイルの保存中にエラーが発生しました。 " + e.getMessage());
            }
    List<String> blockLocStrings = placedBlocks.stream().map(this::locationToString).collect(Collectors.toList());
            dataConfig.set("placedBlocks",blockLocStrings);
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                getLogger().severe("データファイルの保存中にエラーが発生しました。"+ e.getMessage());
            }
     }
     private String locationToString(Location loc){
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
     }
}
