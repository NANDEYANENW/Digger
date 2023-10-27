package org.hotal.digger;


import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {
    private final Digger plugin;
    public double rewardProbability = 0.02; //デフォルトは2%


    public Commands(Digger plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーからのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;
        String cmdName = command.getName();

        if (cmdName.equalsIgnoreCase("updatescoreboard")) {
            if (!player.hasPermission("digger.debug")) {
                player.sendMessage("§cあなたにはこのコマンドを実行する権限がありません。");
                return true;
            }
            plugin.updateAllPlayersScoreboard(); // こちらのメソッドは既存のクラス内に存在すると仮定しています。
            player.sendMessage("§aスコアボードをアップデートしました。");
            return true;
        } else if (cmdName.equalsIgnoreCase("setprobability")) {
            plugin.saveConfig();
            plugin.reloadConfig();
            FileConfiguration config = plugin.getConfig();  // getConfigの戻り値を利用する場合は変数に格納
            double newProbability;
            if (args.length == 0) {
                player.sendMessage("§c確率を指定してください。例: /digger:setprobability 0.5");
                return true;
            }
            try {
                newProbability = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c不正な確率の形式です。0.0から1.0の間の数値を指定してください。");
                return true;
            }
            if (newProbability >= 0.0 && newProbability <= 1.0) {
                Digger.rewardProbability = newProbability;
                config.set("rewardProbability", newProbability);  // 以前のthis.getConfig()をconfigに変更
                plugin.saveConfig();  // こちらも変更
                Bukkit.getLogger().info("Updated rewardProbability: " + Digger.rewardProbability);
                player.sendMessage("§a確率を更新しました: " + Digger.rewardProbability);
                return true;
            }
        } else if (cmdName.equalsIgnoreCase("reloadconfig")) {
            if (!player.hasPermission("digger.reloadconfig")) {
                player.sendMessage("§cあなたにはこのコマンドを実行する権限がありません。");
                return true;
            }
            plugin.reloadConfig();  // こちらも変更
            Digger.rewardProbability = plugin.getConfig().getDouble("rewardProbability", 0.5);  // こちらも変更
            player.sendMessage("§aconfig.ymlを再読み込みしました。");
            return true;
        }
        return false;
    }
}


















