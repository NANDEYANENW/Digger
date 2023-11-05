package org.hotal.digger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CoordinatesDisplay {

    private Digger plugin;
    private ConcurrentHashMap<UUID, Integer> blockCount; // 掘ったブロック数を追跡するためのマップ。

    public CoordinatesDisplay(Digger plugin) {
        this.plugin = plugin;
        this.blockCount = new ConcurrentHashMap<>(); // または既存のマップを使用する。
    }

    public void updateAndDisplayScoreboard() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerUUID = player.getUniqueId();
                    Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard(); // 新しいスコアボードを作成
                    Objective objective = scoreboard.registerNewObjective("stats", "dummy", ChatColor.GREEN + "整地と座標");
                    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

                    // 整地の順位とスコアを更新
                    List<Map.Entry<UUID, Integer>> sortedList = blockCount.entrySet().stream()
                            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                            .limit(10)
                            .collect(Collectors.toList());

                    for (Map.Entry<UUID, Integer> entry : sortedList) {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
                        if (offlinePlayer.getName() == null) continue; // 名前がnullの場合はスキップ

                        String listedPlayerName = offlinePlayer.getName();
                        int score = entry.getValue();
                        objective.getScore(listedPlayerName).setScore(score);
                    }

                    // プレイヤーの順位と掘ったブロック数を表示
                    int viewerScore = blockCount.getOrDefault(playerUUID, 0);
                    int viewerRank = sortedList.indexOf(new AbstractMap.SimpleEntry<>(playerUUID, viewerScore)) + 1;

                    objective.getScore(ChatColor.YELLOW + "あなたの順位: " + viewerRank + "位").setScore(-1);
                    objective.getScore(ChatColor.GREEN + "掘ったブロック数: " + viewerScore).setScore(-2);

                    // プレイヤーの座標を表示
                    Location location = player.getLocation();
                    objective.getScore(ChatColor.WHITE + "X: " + ChatColor.RED + location.getBlockX()).setScore(-3);
                    objective.getScore(ChatColor.WHITE + "Y: " + ChatColor.RED + location.getBlockY()).setScore(-4);
                    objective.getScore(ChatColor.WHITE + "Z: " + ChatColor.RED + location.getBlockZ()).setScore(-5);

                    // プレイヤーにスコアボードを設定
                    player.setScoreboard(scoreboard);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 20ティックごとに更新
    }
}
