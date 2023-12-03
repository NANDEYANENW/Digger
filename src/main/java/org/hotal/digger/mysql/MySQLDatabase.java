package org.hotal.digger.mysql;


import org.bukkit.Location;
import org.hotal.digger.Digger;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
public class MySQLDatabase {

        private String url;
        private String user;
        private String password;

        public MySQLDatabase(Properties prop) {
            prop = new Properties();
            try {
                // config.properties ファイルを読み込む
                prop.load(new FileInputStream("config.properties"));
                this.url = prop.getProperty("db.url");
                this.user = prop.getProperty("db.user");
                this.password = prop.getProperty("db.password");
            } catch (IOException e) {
                e.printStackTrace();
                // エラー処理...
            }
        }

        private Connection getConnection() throws SQLException {
            // データベースへの接続を確立
            return DriverManager.getConnection(url, user, password);
        }





    public static void main(String[] args) {
        Properties prop = new Properties();
        try {
            // 設定ファイルを読み込む
            prop.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String url = prop.getProperty("db.url");
        String user = prop.getProperty("db.user");
        String password = prop.getProperty("db.password");

        // 最初のクエリ
        String query1 = "SELECT * FROM player_data;";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query1);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // 結果の処理...
                String uuid = rs.getString("UUID");
                String playerName = rs.getString("PlayerName");
                int blocksMined = rs.getInt("BlocksMined");

                // 取得したデータを使用して処理（例: コンソールに出力）
                System.out.println("UUID: " + uuid + ", Player Name: " + playerName + ", Blocks Mined: " + blocksMined);            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2番目のクエリ
        String query2 = "SELECT * FROM placed_blocks;";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query2);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // 結果の処理...
                String world = rs.getString("World");
                int x = rs.getInt("X");
                int y = rs.getInt("Y");
                int z = rs.getInt("Z");

                // 取得したデータを使用して処理（例: コンソールに出力）
                System.out.println("World: " + world + ", X: " + x + ", Y: " + y + ", Z: " + z);            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void savePlayerData(Map<UUID, Digger.PlayerData> blockCount, List<Location> placedBlocks, Map<Location, UUID> placedBlocksWithUUID) {
        // プレイヤーデータの保存
        String playerDataQuery = "INSERT INTO player_data (UUID, PlayerName, BlocksMined) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE BlocksMined = ?;";
        for (Map.Entry<UUID, Digger.PlayerData> entry : blockCount.entrySet()) {
            UUID playerId = entry.getKey();
            Digger.PlayerData playerData = entry.getValue();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(playerDataQuery)) {

                stmt.setString(1, playerId.toString());
                stmt.setString(2, playerData.getPlayerName());
                stmt.setInt(3, playerData.getBlocksMined());
                stmt.setInt(4, playerData.getBlocksMined());

                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
// placedBlocks の保存
        // placedBlocks の保存
        String placedBlocksQuery = "INSERT INTO placed_blocks (UUID, World, X, Y, Z) VALUES (?, ?, ?, ?, ?);";

        for (Location loc : placedBlocks) {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(placedBlocksQuery)) {

                UUID playerId = placedBlocksWithUUID.get(loc); // ここでUUIDを取得

                stmt.setString(1, playerId.toString());
                stmt.setString(2, loc.getWorld().getName());
                stmt.setInt(3, loc.getBlockX());
                stmt.setInt(4, loc.getBlockY());
                stmt.setInt(5, loc.getBlockZ());

                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

        public boolean connect() {
        try (Connection conn = getConnection()) {
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<UUID, Digger.PlayerData> loadData() {
        Map<UUID, Digger.PlayerData> dataMap = new HashMap<>();
        String query = "SELECT UUID, PlayerName, BlocksMined FROM player_data;";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("UUID"));
                String playerName = rs.getString("PlayerName");
                int blocksMined = rs.getInt("BlocksMined");

                dataMap.put(uuid, new Digger.PlayerData(playerName, blocksMined));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dataMap;
    }

    public void savePlacedBlock(UUID playerId, Location loc) {
        String insertQuery = "INSERT INTO placed_blocks (World, X, Y, Z) VALUES (?, ?, ?, ?);";

        try (Connection conn = getSQLiteConnection(); // SQLiteデータベースへの接続を取得
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {

            stmt.setString(1, loc.getWorld().getName());
            stmt.setInt(2, loc.getBlockX());
            stmt.setInt(3, loc.getBlockY());
            stmt.setInt(4, loc.getBlockZ());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getSQLiteConnection() {
        return null;
    }
}


