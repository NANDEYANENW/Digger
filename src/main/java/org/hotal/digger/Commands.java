package org.hotal.digger;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {
    private final Digger diggerPlugin;
    public double rewardProbability = 0.02; // デフォルトは2%

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
            diggerPlugin.updateAllPlayersScoreboard();
            player.sendMessage("§aスコアボードをアップデートしました。");
            return true;
        }

        if (command.getName().equalsIgnoreCase("reloadprobabilityconfig")) {
            if (!player.hasPermission("digger.reloadconfig")) {
                player.sendMessage("§cあなたにはこのコマンドを実行する権限がありません。");
                return true;
            }
            diggerPlugin.reloadConfig();
            diggerPlugin.rewardProbability = diggerPlugin.getConfig().getDouble("rewardProbability", 0.5);
            player.sendMessage("§aconfig.ymlを再読み込みしました。");
            return true;
        }

        return false;
    }
}



