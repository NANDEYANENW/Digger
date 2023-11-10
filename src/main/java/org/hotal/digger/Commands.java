package org.hotal.digger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.UUID;

public class Commands implements CommandExecutor {
    private boolean isToolRewardEnabled = true;
    boolean currentSetting = Digger.getInstance().isToolRewardEnabled;
    private final Digger plugin;
    private final ToolMoney toolMoney;

    public Commands(Digger plugin, ToolMoney toolMoney) {
        this.plugin = plugin;
        this.toolMoney = toolMoney;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c このコマンドはプレイヤーからのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;
        String cmdName = command.getName();

        if (cmdName.equalsIgnoreCase("updatescoreboard")) {
            if (!player.hasPermission("digger.debug")) {
                player.sendMessage("§c あなたにはこのコマンドを実行する権限がありません。");
                return true;
            }
            plugin.updateAllPlayersScoreboard(); // こちらのメソッドは既存のクラス内に存在すると仮定しています。
            player.sendMessage("§a スコアボードをアップデートしました。");
            return true;
        } else if (cmdName.equalsIgnoreCase("setprobability")) {
            if (!player.hasPermission("digger.setprobability")) {
                player.sendMessage("§cあなたにはこのコマンドを実行する権限がありません。");
                return true;
            }
            plugin.saveConfig();
            plugin.reloadConfig();
            FileConfiguration config = plugin.getConfig();  // getConfigの戻り値を利用する場合は変数に格納
            double newProbability;
            if (args.length == 0) {
                player.sendMessage("§c 確率を指定してください。例: /digger:setprobability 0.5");
                return true;
            }
            try {
                newProbability = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c 不正な確率の形式です。0.0から1.0の間の数値を指定してください。");
                return true;
            }
            if (newProbability >= 0.0 && newProbability <= 1.0) {
                Digger.rewardProbability = newProbability;
                config.set("rewardProbability", newProbability);  // 以前のthis.getConfig()をconfigに変更
                plugin.saveConfig();  // こちらも変更
                player.sendMessage("§a 確率を更新しました: " + Digger.rewardProbability);
                return true;
            }

        } else if (cmdName.equalsIgnoreCase("reload")) {
            if (!player.hasPermission("digger.reload")) {
                player.sendMessage("§c あなたにはこのコマンドを実行する権限がありません。");
                return true;
            }
            plugin.reloadConfig();  // こちらも変更
            Digger.rewardProbability = plugin.getConfig().getDouble("rewardProbability", 0.04);  // こちらも変更
            player.sendMessage("§a config.ymlを再読み込みしました。");
            return true;
        } else if (cmdName.equalsIgnoreCase("tools")) {
            if (!player.hasPermission("digger.tools")) {
                player.sendMessage("§c あなたにはこのコマンドを実行する権限がありません。");
                return true;
            }
            if (args.length == 0) {
                player.sendMessage("§c 引数を指定してください。例: /digger:tools on");
                return true;
            }
            if (args[0].equalsIgnoreCase("on")) {
                Digger.getInstance().isToolRewardEnabled = true;
                sender.sendMessage("ツール別の報酬が有効になりました。");

                return true;
            } else if (args[0].equalsIgnoreCase("off")) {
                Digger.getInstance().isToolRewardEnabled = false;
                sender.sendMessage("ツール別の報酬が無効になりました。");

                return true;
            }
        }
        if (command.getName().equalsIgnoreCase("set")) {
            if (sender instanceof Player) {
                if (!player.hasPermission("digger.set")) {
                    player.sendMessage("§cあなたにはこのコマンドを実行する権限がありません。");
                    return true;
                }

                // コマンドが 'set' の場合に適切な引数が提供されているかをチェック
                if (args.length == 2) {
                    String playerName = args[0];
                    try {
                        int newScore = Integer.parseInt(args[1]);
                        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
                        if (targetPlayer == null) {
                            player.sendMessage("§c指定されたプレイヤーが見つかりません。");
                            return true;
                        }

                        UUID targetUUID = targetPlayer.getUniqueId();
                        // 新しいスコアを設定
                        plugin.blockCount.put(targetUUID, newScore);
                        // 全プレイヤーのスコアボードを更新
                        plugin.updateAllPlayersScoreboard();
                        player.sendMessage("§a" + playerName + "のスコアを" + newScore + "に設定しました。");
                    } catch (NumberFormatException e) {
                        player.sendMessage("§c無効なスコアです。数字を入力してください。");
                    }
                    return true;
                } else {
                    player.sendMessage("§c使用方法: /set <プレイヤー名> <新しいスコア>");
                    return true;
                }
            } else {
                sender.sendMessage("§cこのコマンドはプレイヤーのみが実行できます。");
                return true;
            }
        }

        return false;
    }
}



































