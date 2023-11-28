package org.hotal.digger.sql;

import java.sql.*;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;

public class PointsDatabase {
    private Connection connection;

    public void openConnection(String path) throws SQLException {
        // データベース接続の確立
        String url = "jdbc:sqlite:" + path + "/Database.db";
        connection = DriverManager.getConnection(url);

        // テーブルの作成
        try (Statement statement = connection.createStatement()) {
            String playerDataTableCreationQuery = "CREATE TABLE IF NOT EXISTS player_data ("
                    + "UUID VARCHAR(255) NOT NULL,"
                    + "PlayerName VARCHAR(255),"
                    + "BlocksMined INT DEFAULT 0,"
                    + "PRIMARY KEY (UUID));";
            statement.execute(playerDataTableCreationQuery);

            String placedBlocksTableCreationQuery = "CREATE TABLE IF NOT EXISTS placed_blocks ("
                    + "BlockID INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "World VARCHAR(255),"
                    + "X INT,"
                    + "Y INT,"
                    + "Z INT);";
            statement.execute(placedBlocksTableCreationQuery);
        }
    }

    public void saveData(Map<UUID, Integer> blockCount, Iterable<Location> placedBlocks) throws SQLException {
        // blockCount の保存
        saveBlockCount(blockCount);

        // placedBlocks の保存
        savePlacedBlocks(placedBlocks);
    }

    private void saveBlockCount(Map<UUID, Integer> blockCount) throws SQLException {
        String blockCountQuery = "INSERT INTO player_data (UUID, BlocksMined) VALUES (?, ?) "
                + "ON CONFLICT(UUID) DO UPDATE SET BlocksMined = excluded.BlocksMined;";

        try (PreparedStatement pstmt = connection.prepareStatement(blockCountQuery)) {
            for (Map.Entry<UUID, Integer> entry : blockCount.entrySet()) {
                pstmt.setString(1, entry.getKey().toString());
                pstmt.setInt(2, entry.getValue());
                pstmt.executeUpdate();
            }
        }
    }

    private void savePlacedBlocks(Iterable<Location> placedBlocks) throws SQLException {
        String placedBlocksQuery = "INSERT INTO placed_blocks (World, X, Y, Z) VALUES (?, ?, ?, ?);";

        try (PreparedStatement pstmt = connection.prepareStatement(placedBlocksQuery)) {
            for (Location loc : placedBlocks) {
                pstmt.setString(1, loc.getWorld().getName());
                pstmt.setInt(2, loc.getBlockX());
                pstmt.setInt(3, loc.getBlockY());
                pstmt.setInt(4, loc.getBlockZ());
                pstmt.executeUpdate();
            }
        }
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
