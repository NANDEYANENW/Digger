package org.hotal.digger.mysql;

import java.sql.*;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

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
}
