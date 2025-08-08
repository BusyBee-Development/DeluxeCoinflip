/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage.handler.impl;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.storage.handler.StorageHandler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteHandler implements StorageHandler {

    private DeluxeCoinflipPlugin plugin;
    private File file;

    @Override
    public boolean onEnable(final DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "database.db");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Error occurred while creating the database file.", e);
                return false;
            }
        }

        createTable();
        return true;
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Saving player data to database...");

        Map<UUID, PlayerData> playerDataMap = DeluxeCoinflipPlugin.getInstance().getStorageManager().getPlayerDataMap();

        for (PlayerData player : new ArrayList<>(playerDataMap.values())) {
            savePlayer(player);
        }

        playerDataMap.clear();
    }

    public Connection getConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + file);
        } catch (SQLException | ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while setting up the database connection.", ex);
            return null;
        }
    }

    private synchronized void createTable() {
        try (Connection tableConnection = getConnection();
             Statement statement = tableConnection.createStatement()) {
            String TABLE_NAME = "players";
            String createPlayersTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "uuid VARCHAR(255) NOT NULL PRIMARY KEY, " +
                    "wins INTEGER, " +
                    "losses INTEGER, " +
                    "profit BIGINT," +
                    "total_loss BIGINT," +
                    "total_gambled BIGINT," +
                    "broadcasts BOOLEAN);";
            statement.execute(createPlayersTable);

            String createGamesTable = "CREATE TABLE IF NOT EXISTS games (" +
                    "uuid VARCHAR(255) NOT NULL PRIMARY KEY, " +
                    "provider VARCHAR(255)," +
                    "amount BIGINT);";
            statement.execute(createGamesTable);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while creating database tables.", e);
        }
    }

    @Override
    public synchronized PlayerData getPlayer(final UUID uuid) {
        String sql = "SELECT wins, losses, profit, total_loss, total_gambled, broadcasts FROM players WHERE uuid=?;";
        try (Connection playerConnection = getConnection();
             PreparedStatement preparedStatement = playerConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, uuid.toString());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    PlayerData playerData = new PlayerData(uuid);
                    playerData.setWins(resultSet.getInt("wins"));
                    playerData.setLosses(resultSet.getInt("losses"));
                    playerData.setProfit(resultSet.getLong("profit"));
                    playerData.setTotalLosses(resultSet.getLong("total_loss"));
                    playerData.setTotalGambled(resultSet.getLong("total_gambled"));
                    playerData.setDisplayBroadcastMessages(resultSet.getBoolean("broadcasts"));

                    return playerData;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to get a player's data.", e);
        }

        return new PlayerData(uuid);
    }

    @Override
    public synchronized void savePlayer(final PlayerData player) {
        String sql = "REPLACE INTO players (uuid, wins, losses, profit, total_loss, total_gambled, broadcasts) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection playerConnection = getConnection();
             PreparedStatement preparedStatement = playerConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, player.getUUID().toString());
            preparedStatement.setInt(2, player.getWins());
            preparedStatement.setInt(3, player.getLosses());
            preparedStatement.setLong(4, player.getProfit());
            preparedStatement.setLong(5, player.getTotalLosses());
            preparedStatement.setLong(6, player.getTotalGambled());
            preparedStatement.setBoolean(7, player.isDisplayBroadcastMessages());
            preparedStatement.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to save a player's data.", e);
        }
    }

    @Override
    public synchronized void saveCoinflip(CoinflipGame game) {
        String sql = "REPLACE INTO games (uuid, provider, amount) VALUES (?, ?, ?)";
        try (Connection coinflipConnection = getConnection();
             PreparedStatement preparedStatement = coinflipConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, game.getPlayerUUID().toString());
            preparedStatement.setString(2, game.getProvider());
            preparedStatement.setLong(3, game.getAmount());
            preparedStatement.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to save a coinflip game.", e);
        }
    }

    @Override
    public synchronized void deleteCoinfip(UUID uuid) {
        String sql = "DELETE FROM games WHERE uuid=?;";
        try (Connection coinflipConnection = getConnection();
             PreparedStatement preparedStatement = coinflipConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to delete a coinflip game.", e);
        }
    }

    @Override
    public synchronized Map<UUID, CoinflipGame> getGames() {
        Map<UUID, CoinflipGame> games = new HashMap<>();
        String sql = "SELECT uuid, provider, amount FROM games;";
        try (Connection gamesConnection = getConnection();
             PreparedStatement preparedStatement = gamesConnection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                String provider = resultSet.getString("provider");
                long amount = resultSet.getLong("amount");
                games.put(uuid, new CoinflipGame(uuid, provider, amount));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to get all coinflip games.", e);
        }

        return games;
    }

    @Override
    public CoinflipGame getCoinflipGame(@NotNull UUID uuid) {
        final String SQL = "SELECT * FROM games WHERE uuid = ?";
        try (Connection GAME_CONNECTION = getConnection();
             PreparedStatement preparedStatement = GAME_CONNECTION.prepareStatement(SQL)) {
            preparedStatement.setString(1, uuid.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String provider = resultSet.getString("provider");
                long amount = resultSet.getLong("amount");
                return new CoinflipGame(uuid, provider, amount);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to get a coinflip game.", e);
        }

        return null;
    }
}
