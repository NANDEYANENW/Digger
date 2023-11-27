package org.hotal.digger.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class PointsDatabase {
    private Connection connection;

    public void openConnection(String path) throws SQLException {
        // データベース接続の確立
        String url = "jdbc:sqlite:" + path + "/Database.db";
        connection = DriverManager.getConnection(url);

        // テーブルの作成
        try (Statement statement = connection.createStatement()) {
            String tableCreationQuery = "CREATE TABLE IF NOT EXISTS player_data ("
                    + "MCID VARCHAR(255) NOT NULL,"
                    + "PlayerName VARCHAR(255),"
                    + "BlocksMined INT DEFAULT 0,"
                    + "PRIMARY KEY (MCID));";
            statement.execute(tableCreationQuery);
        }
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
