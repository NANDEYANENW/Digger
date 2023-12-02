package org.hotal.digger.mysql;

import java.sql.*;

public class mysqldatabase {
    public static void main(String[] args) {

        // データベース接続情報は外部から取得する
        String url = "jdbc:mysql://localhost:3306/database.db";
        String user = "root";
        String password = "";

        String query = "SELECT * FROM player_data;";

        // try-with-resourcesを使用して自動的にリソースをクローズ
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String UUID = rs.getString("UUID");
                String PlayerName = rs.getString("PlayerName");
                String BlocksMined = rs.getString("BlocksMined");

                System.out.println(UUID + "、" + PlayerName + "、" + BlocksMined);
            }

        } catch (SQLException e) {
            // エラーの詳細を表示
            e.printStackTrace();
        }
    }
}
