package org.hotal.digger;

import org.bukkit.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.Material;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.FileConfiguration;
import org.hotal.digger.sql.PointsDatabase;

import java.sql.SQLException;
import java.util.*;

import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;

public class Digger extends JavaPlugin implements Listener {

    private PointsDatabase pointsDatabase;
    private FileConfiguration dataConfig;
    private File dataFile;

    public boolean isToolRewardEnabled = true;

    private boolean useToolMoney;

    private final Map<UUID, Boolean> scoreboardToggles = new HashMap<>();
    private static Digger instance;
    private Boolean currentSetting = null;

    public static double rewardProbability = 0.03;


    public ToolMoney toolMoney = new ToolMoney(getConfig(), this);

    private Scoreboard scoreboard;
    private Economy economy;
    private long scoreboardUpdateInterval = 20L;
    private Objective objective;
    private final Map<Material, Integer> rewardMap = new HashMap<>();
    public final Map<UUID, PlayerData> blockCount = new HashMap<>();

    private final List<Location> placedBlocks = new ArrayList<>();
    private List<String> worldBlacklist = new ArrayList<>();
    private Material toolType;

    public Digger() {
        instance = this;
    }

    public static Digger getInstance() {
        return instance;
    }

    public boolean getCurrentSetting() {
        if (currentSetting == null) {
            currentSetting = Digger.getInstance().isToolRewardEnabled;
        }
        return currentSetting;
    }

    @Override
    public void onEnable() { //起動時の初期化処理
        // Configファイルをロードまたは作成
        saveDefaultConfig();
        pointsDatabase = new PointsDatabase();

        // データベース接続のオープン
        try {
            pointsDatabase.openConnection(getDataFolder().getAbsolutePath());
        } catch (SQLException e) {
            getLogger().severe("データベース接続時にエラーが発生しました: " + e.getMessage());
            // 必要に応じてプラグインを無効化
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        try {
            pointsDatabase.openConnection(getDataFolder().getAbsolutePath());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            pointsDatabase.saveData(blockCount, placedBlocks);
        } catch (SQLException e) {
            getLogger().severe("§aデータベースへの保存中にエラーが発生しました。" + e.getMessage());
        }


        useToolMoney = getConfig().getBoolean("use-tool-money", false);

        // ツール報酬をロード
        loadToolRewards();

        rewardMap.put(Material.NETHERITE_PICKAXE, 250);
        rewardMap.put(Material.NETHERITE_SHOVEL, 250);
        rewardMap.put(Material.DIAMOND_PICKAXE, 200);
        rewardMap.put(Material.DIAMOND_SHOVEL, 200);
        rewardMap.put(Material.GOLDEN_PICKAXE, 175);
        rewardMap.put(Material.GOLDEN_SHOVEL, 175);
        rewardMap.put(Material.IRON_PICKAXE, 150);
        rewardMap.put(Material.IRON_SHOVEL, 150);
        rewardMap.put(Material.STONE_PICKAXE, 100);
        rewardMap.put(Material.STONE_SHOVEL, 100);
        rewardMap.put(Material.WOODEN_PICKAXE, 50);
        rewardMap.put(Material.WOODEN_SHOVEL, 50);

        FileConfiguration dataConfig;
        dataFile = new File(getDataFolder(), "player-data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            saveResource("player-data.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        File configFile = new File(getDataFolder(), "config.yml");

        reloadConfig();  // すでに存在する config.yml の内容を読み込む

        toolMoney = new ToolMoney(getConfig(), this);

        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(dataFile);
        this.dataConfig = yamlConfiguration;
        scoreboardUpdateInterval = getConfig().getLong("update-interval", scoreboardUpdateInterval);
        worldBlacklist = getConfig().getStringList("world-blacklist");

        if (!setupEconomy()) { // 起動時のVault関係があるかどうか
            getLogger().severe("エラー：Vaultプラグインが見つかりませんでした。プラグインを無効化します。");

            if (getServer().getPluginManager().getPlugin("Vault") == null) {
                getLogger().severe("エラー：Vaultプラグインが見つかりません。");
            } else {
                getLogger().severe("エラー：Economyサービスプロバイダが見つかりません。");
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

        Digger.rewardProbability = this.getConfig().getDouble("rewardProbability", 0.03); //3%

        ToolMoney toolMoneyInstance = new ToolMoney(getConfig(), this);
        Commands commandExecutor = new Commands(this, toolMoneyInstance);
        getCommand("updatescoreboard").setExecutor(commandExecutor);
        getCommand("setprobability").setExecutor(commandExecutor);
        getCommand("reload").setExecutor(commandExecutor);
        getCommand("set").setExecutor(commandExecutor);

        if (this.getConfig().contains("scoreboardUpdateInterval")) {
            scoreboardUpdateInterval = this.getConfig().getLong("scoreboardUpdateInterval");

        }

    }

    // データの保存
    public void savePlayerData(Map<UUID, Integer> blockCount, List<Location> placedBlocks) {
        Map<UUID, Digger.PlayerData> playerDataMap = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : blockCount.entrySet()) {
            // プレイヤーのUUIDを取得
            UUID playerId = entry.getKey();
            // プレイヤーの名前を取得
            Player player = Bukkit.getPlayer(playerId);
            String playerName = (player != null) ? player.getName() : "Unknown";
            // PlayerDataオブジェクトを作成
            playerDataMap.put(playerId, new PlayerData(playerName, entry.getValue()));
        }


         // データを保存する
        try {
            pointsDatabase.saveData(playerDataMap, placedBlocks); // playerDataMapを使用
        } catch (SQLException e) {
            // エラーハンドリング
            getLogger().severe("データベースへの保存中にエラーが発生しました。YAMLファイルに変更しています...: " + e.getMessage());
            saveToYaml(blockCount, placedBlocks);
        }

    }



    private void saveToYaml(Map<UUID, Integer> blockCount, List<Location> placedBlocks) {
        // blockCount の保存
        for (Map.Entry<UUID, Integer> entry : blockCount.entrySet()) {
            dataConfig.set("blockCount." + entry.getKey().toString(), entry.getValue());
        }

        // placedBlocks の保存 (リスト内の各 Location オブジェクトを文字列に変換して保存)
        List<String> blockLocStrings = placedBlocks.stream()
                .map(loc -> loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ())
                .collect(Collectors.toList());
        dataConfig.set("placedBlocks", blockLocStrings);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("YAMLファイルへの保存中にエラーが発生しました: " + e.getMessage());
        }
    }


    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("§4エラー：Vaultプラグインが見つかりませんでした。");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("§4エラー:Economyサービスプロバイダが登録されていません。");
            return false;
        }
        economy = rsp.getProvider();
        if (economy == null) {
            getLogger().warning("§4エラー：Economyサービスが見つかりません。");
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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーからのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("setprobability")) {
            if (!player.hasPermission("digger.setprobability")) {
                player.sendMessage("§cあなたにはこのコマンドを実行する権限がありません。");
                return true;
            }
            double newProbability;
            if (args.length == 0) {
                player.sendMessage("§c 確率を指定してください。例: /digger:setprobability 0.5");
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
                this.getConfig().set("rewardProbability", newProbability);
                this.saveConfig();
                player.sendMessage("§a確率を更新しました: " + Digger.rewardProbability);
                return true;
            }

        } else if (cmd.getName().equalsIgnoreCase("reload")) {
            if (!player.hasPermission("digger.reload")) {
                player.sendMessage("§cあなたにはこのコマンドを実行する権限がありません。");
                return true;
            }

            this.reloadConfig();
            Digger.rewardProbability = this.getConfig().getDouble("rewardProbability", 0.03);
            player.sendMessage("§a config.ymlを再読み込みしました。");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみが実行できます。");
            return true;
        }

        // コマンドが'digger:scoreboard'であるかを確認
        if (cmd.getName().equalsIgnoreCase("sb")) {
            // 引数が'on'または'off'であるかを確認
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("on")) {
                    // スコアボードをオンにする
                    scoreboardToggles.put(player.getUniqueId(), true);
                    updateScoreboard(player); // これはあなたのスコアボードを更新するメソッドです
                    player.sendMessage(ChatColor.GREEN + "スコアボードを表示しました。");
                } else if (args[0].equalsIgnoreCase("off")) {
                    // スコアボードをオフにする
                    scoreboardToggles.put(player.getUniqueId(), false);
                    player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                    player.sendMessage(ChatColor.GREEN + "スコアボードを非表示にしました。");
                } else {
                    player.sendMessage(ChatColor.RED + "無効な引数です。使用法: /digger:scoreboard <on/off>");
                }
                return true;
            }
        }
        if (cmd.getName().equalsIgnoreCase("sutm")) {
            if (!(sender.hasPermission("digger.sutm"))) {
                sender.sendMessage(ChatColor.RED + "このコマンドを実行する権限がありません。");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "使用法: /sutm <true/false>");
                return true;
            }

            boolean newSetting = Boolean.parseBoolean(args[0]);
            isToolRewardEnabled = newSetting;
            useToolMoney = newSetting;
            getConfig().set("use-tool-money", newSetting);
            saveConfig();

            sender.sendMessage(ChatColor.GREEN + "ツール報酬の使用が " + newSetting + " に設定されました。");
            return true;
        }
        return false;
    }


    private void saveUpdateIntervalToConfig(long interval) {
        this.getConfig().set("scoreboardUpdateInterval", interval);
        this.saveConfig();
    }


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        placedBlocks.add(event.getBlock().getLocation());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (worldBlacklist.contains(player.getWorld().getName()) ||
                placedBlocks.remove(event.getBlock().getLocation()) ||
                isBlockBlacklisted(event.getBlock().getType())) {
            return;
        }

        updateBlockCount(player);
        giveReward(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        saveData();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            saveData();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            loadData();
            Bukkit.getScheduler().runTask(this, () -> {

            });
        });
    }

    public void updateBlockCount(Player player) {
        UUID playerID = player.getUniqueId();
        // プレイヤーデータの取得、なければ新しいプレイヤーデータを作成
        PlayerData data = blockCount.getOrDefault(playerID, new PlayerData(player.getName(), 0));
        // 採掘数を1増やす
        data.setBlocksMined(data.getBlocksMined() + 1);
        // 更新されたプレイヤーデータをマップに戻す
        blockCount.put(playerID, data);
    }



    private void giveReward(Player player) {
        Material toolType = player.getInventory().getItemInMainHand().getType();
        Integer toolReward = isToolRewardEnabled ? rewardMap.getOrDefault(toolType, 50) : 50;
        if (Math.random() < rewardProbability) {
            economy.depositPlayer(player, toolReward);
            player.sendMessage("§a " + toolReward + "NANDEを手に入れました。");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        }
    }

    private boolean isBlockBlacklisted(Material material) {
        List<String> blacklist = getConfig().getStringList("block-blacklist");
        return blacklist.contains(material.name());
    }


    public void updateAllPlayersScoreboard () {
        // すべてのプレイヤー（オンライン・オフライン）のUUIDを使用してスコアボードを更新
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }

    public void updateScoreboard(Player viewingPlayer) {
        Boolean showScoreboard = scoreboardToggles.getOrDefault(viewingPlayer.getUniqueId(), true);
        if (showScoreboard) {
            // スコアボードのセットアップ
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("stats", "dummy", ChatColor.LIGHT_PURPLE + "整地の順位");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            // ソートされたリストを取得
            List<Map.Entry<UUID, PlayerData>> sortedList = blockCount.entrySet().stream()
                    .sorted((entry1, entry2) -> Integer.compare(entry2.getValue().getBlocksMined(), entry1.getValue().getBlocksMined()))
                    .limit(10)
                    .collect(Collectors.toList());

            // プレイヤーのランクを決定
            int viewerScore = blockCount.getOrDefault(viewingPlayer.getUniqueId(), new PlayerData("", 0)).getBlocksMined();
            int viewerRankIndex = sortedList.stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList())
                    .indexOf(viewingPlayer.getUniqueId());
            String rankDisplayText = viewerRankIndex < 0 || viewerRankIndex >= 10 ? "ランキング外" : (viewerRankIndex + 1) + "位";


            // トップ10プレイヤーをスコアボードに表示
            for (int i = 0; i < sortedList.size(); i++) {
                Map.Entry<UUID, PlayerData> entry = sortedList.get(i);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
                String listedPlayerName = offlinePlayer.getName() == null ? "Unknown" : offlinePlayer.getName();
                objective.getScore(listedPlayerName).setScore(entry.getValue().getBlocksMined());
            }

                 // 空行
            objective.getScore(" ").setScore(1);
            objective.getScore(ChatColor.GOLD + "あなたの順位: " + rankDisplayText).setScore(0);
            objective.getScore(ChatColor.GREEN + "掘ったブロック数: " + ChatColor.WHITE + viewerScore + "ブロック").setScore(-1);
            // プレイヤーの座標を表示
            Location location = viewingPlayer.getLocation();
            String locationDisplay = ChatColor.WHITE + "座標: " + ChatColor.RED +
                    "X:" + location.getBlockX() +
                    " Y:" + location.getBlockY() +
                    " Z:" + location.getBlockZ();
            objective.getScore(locationDisplay).setScore(-2);

            // スコアボードをプレイヤーに適用
            viewingPlayer.setScoreboard(scoreboard);
        }
    }



        @Override
    public void onDisable() {
        saveData();
        getConfig().set("update-interval", scoreboardUpdateInterval);
        getConfig().set("world-blacklist", worldBlacklist);
        getConfig().set("use-tool-money", useToolMoney);
        saveConfig();

    }

    public void loadData() {
        // データベースから読み込む試み
        try {
            // データベース接続の確認とデータの取得
            if (pointsDatabase.checkConnection()) {
                Map<UUID, PlayerData> dataFromDatabase = pointsDatabase.getData();
                if (dataFromDatabase != null) {
                    blockCount.clear();
                    blockCount.putAll(dataFromDatabase);
                    getLogger().info("データがデータベースから読み込まれました。");
                    return;
                }
            }
        } catch (SQLException e) {
            getLogger().severe("データベースからの読み込み中にエラーが発生しました: " + e.getMessage());
            // エラーが発生した場合はYAMLからの読み込みにフォールバック
        }

        // YAMLファイルからの読み込みにフォールバック
        dataFile = new File(getDataFolder(), "player-data.yml");
        if (!dataFile.exists()) {
            getLogger().info("player-data.ymlファイルが見つかりませんでした。新しく作成します。");
            saveResource("player-data.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("blockCount")) {
            blockCount.clear();
            for (String uuidString : dataConfig.getConfigurationSection("blockCount").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                String playerName = dataConfig.getString("blockCount." + uuidString + ".playerName");
                int blocksMined = dataConfig.getInt("blockCount." + uuidString + ".blocksMined");
                PlayerData playerData = new PlayerData(playerName, blocksMined);
                blockCount.put(uuid, playerData);
            }
            getLogger().info("データがYAMLファイルから読み込まれました。");
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

    public void saveData() {
        // データベースに保存を試みる
        try {
            // データ変換
            Map<UUID, Integer> simpleBlockCount = blockCount.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getBlocksMined()));

            // データベースに保存
            pointsDatabase.saveData(blockCount, placedBlocks);
            getLogger().info("データがデータベースに保存されました。");

            // YAMLファイルに保存
            saveToYAML();
        } catch (SQLException | IOException e) {
            getLogger().severe("データの保存中にエラーが発生しました。YAMLファイルにフォールバックしています: " + e.getMessage());

        }
    }

        private void saveToYAML() throws IOException {
            if (dataConfig != null) {
                for (Map.Entry<UUID, PlayerData> entry : blockCount.entrySet()) {
                    UUID uuid = entry.getKey();
                    PlayerData playerData = entry.getValue();
                    dataConfig.set("blockCount." + uuid.toString() + ".blocksMined", playerData.getBlocksMined());
                    dataConfig.set("blockCount." + uuid.toString() + ".playerName", playerData.getPlayerName());
                }

                // placedBlocks の保存
                List<String> blockLocStrings = placedBlocks.stream()
                        .map(loc -> loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ())
                        .collect(Collectors.toList());
                dataConfig.set("placedBlocks", blockLocStrings);


                dataConfig.save(dataFile);
                getLogger().info("データがYAMLファイルに保存されました。");
            }
        }





    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private void loadToolRewards() {
        // "tools"セクションを取得
        ConfigurationSection toolsSection = getConfig().getConfigurationSection("tools");

        if (toolsSection == null) {

            return;
        }

        // セクション内の全てのキーと値を取得してrewardMapに保存
        for (String key : toolsSection.getKeys(false)) {
            Material material = Material.getMaterial(key);
            int reward = toolsSection.getInt(key);

            if (material != null) {
                rewardMap.put(material, reward);
            } else {

            }
        }
    }


    public static class PlayerData {
        private String playerName;
        private int blocksMined;

        public PlayerData(String playerName, int blocksMined) {
            this.playerName = playerName;
            this.blocksMined = blocksMined;
        }

        // ゲッターとセッターを追加
        public String getPlayerName() {
            return playerName;
        }

        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        public int getBlocksMined() {
            return blocksMined;
        }

        public void setBlocksMined(int blocksMined) {
            this.blocksMined = blocksMined;
        }
    }
}


















