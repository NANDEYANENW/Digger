package org.hotal.digger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugCommands implements CommandExecutor {
    private final Digger diggerPlugin;

    public DebugCommands(Digger diggerPlugin) {
        this.diggerPlugin = diggerPlugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("updatescoreboard")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cこのコマンドはプレイヤーからのみ実行できます。");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("digger.debug")) {
                player.sendMessage("§cあなたにはこのコマンドを実行する権限がありません。");
                return true;
            }

            diggerPlugin.updateAllPlayersScoreboard();
            player.sendMessage("§aスコアボードをアップデートしました。");
            return true;
        }
        return false;
    }
}