package org.hotal.digger;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {
    private final Digger diggerPlugin;
    public static double rewardProbability = 0.03; //デフォルトは3%
    public Commands(Digger diggerPlugin) {
        this.diggerPlugin = diggerPlugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーからのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("updatescoreboard")) {
            if (!player.hasPermission("digger.debug")) {
                player.sendMessage("§cあなたにはこのコマンドを実行する権限がありません。");
                return true;
            }
            diggerPlugin.updateAllPlayersScoreboard(); // こちらのメソッドは既存のクラス内に存在すると仮定しています。
            player.sendMessage("§aスコアボードをアップデートしました。");
            return true;
        } else if (command.getName().equalsIgnoreCase("setprobability")) {
            if (!player.hasPermission("digger.setprobability")) {
                player.sendMessage("§cあなたにはこのコマンドを実行する権限がありません。");
                return true;
            }
            if (args.length == 1) {
                try {
                    double newProbability = Double.parseDouble(args[0]);

                    // 確率が0.0から1.0の間にあるかどうかをチェック
                    if (newProbability >= 0.0 && newProbability <= 1.0) {
                        Commands.rewardProbability = newProbability;
                        Bukkit.getLogger().info("[Debug] Setting new probability: " + newProbability); // デバッグのログを追加
                        sender.sendMessage("確率が " + newProbability + " に設定されました。");
                    } else {
                        sender.sendMessage("エラー: 確率は0.0から1.0の間で指定してください。"); // 0%~100%
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("エラー: 無効な確率が指定されました。");
                }
            } else {
                sender.sendMessage("使用方法: /setprobability <確率>");
            }
            return true;
    }


        return false;
    }
}

