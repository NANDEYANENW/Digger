package org.hotal.digger.mysql;



import org.hotal.digger.Digger;

import java.sql.*;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.Location;
public class MySQLDatabase {
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

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("config.properties");
}   public void savePlayerData(Map<UUID, Digger.PlayerData> blockCount, List<Location> placedBlocks) {
        String query = "INSERT INTO player_data (UUID, PlayerName, BlocksMined) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE BlocksMined = ?;";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, playerId.toString());
            stmt.setString(2, playerName);
            stmt.setInt(3, blocksMined);
            stmt.setInt(4, blocksMined);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (Map.Entry<UUID, Digger.PlayerData> entry : blockCount.entrySet()) {
            UUID playerId = entry.getKey();
            Digger.PlayerData playerData = entry.getValue();
            // 現在の savePlayerData メソッドを呼び出してデータを保存
            savePlayerData(playerId, playerData.getPlayerName(), playerData.getBlocksMined());
        }
        for (Map.Entry<UUID, Digger.PlayerData> entry : blockCount.entrySet()) {
            UUID playerId = entry.getKey();
           blockCount = entry.getValue();
            // 現在の savePlayerData メソッドを呼び出してデータを保存
            savePlayerData(playerId, playerData.blockCount,playerData.blockCount());
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
}