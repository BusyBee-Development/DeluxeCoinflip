/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.exception.InvalidStorageHandlerException;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.GameManager;
import net.zithium.deluxecoinflip.storage.handler.StorageHandler;
import net.zithium.deluxecoinflip.storage.handler.impl.SQLiteHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class StorageManager {

    private final DeluxeCoinflipPlugin plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private StorageHandler storageHandler;

    public StorageManager(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
    }

    public void onEnable() {
        if (plugin.getConfig().getString("storage.type").equalsIgnoreCase("SQLITE")) {
            storageHandler = new SQLiteHandler();
        } else {
            throw new InvalidStorageHandlerException("Invalid storage handler specified");
        }

        if (!storageHandler.onEnable(plugin)) {
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        Stream.of(
                new Listener() {
                    @EventHandler(priority = EventPriority.MONITOR)
                    public void onPlayerJoin(final PlayerJoinEvent event) {
                        loadPlayerData(event.getPlayer().getUniqueId());
                    }

            }, new Listener() {
                @EventHandler(priority = EventPriority.MONITOR)
                public void onPlayerQuit(final PlayerQuitEvent event) {
                    UUID quitterId = event.getPlayer().getUniqueId();

                    getPlayer(quitterId).ifPresent(data -> savePlayerData(data, true));

                    GameManager gameManager = plugin.getGameManager();
                    CoinflipGame quitterGame = gameManager.getCoinflipGames().get(quitterId);
                    if (quitterGame != null) {
                        refundBothAndCancel(quitterGame);
                        return;
                    }

                    for (CoinflipGame g : gameManager.getCoinflipGames().values()) {
                        if (!g.getPlayerUUID().equals(quitterId) &&
                            g.getOfflinePlayer().getUniqueId().equals(quitterId)) {
                            refundBothAndCancel(g);
                            break;
                        }
                    }
                }
            }
        ).forEach(listener -> plugin.getServer().getPluginManager().registerEvents(listener, plugin));

        Bukkit.getOnlinePlayers().forEach(player -> loadPlayerData(player.getUniqueId()));
    }

    public void onDisable(boolean shutdown) {
        if (shutdown) {
            storageHandler.onDisable();
        }
    }

    public Optional<PlayerData> getPlayer(UUID uuid) {
        return Optional.ofNullable(playerDataMap.get(uuid));
    }

    public void updateOfflinePlayerWin(UUID uuid, long profit, long beforeTax) {
        PlayerData playerData = storageHandler.getPlayer(uuid);
        playerData.updateWins();
        playerData.updateProfit(profit);
        playerData.updateGambled(beforeTax);
        savePlayerData(playerData, false);
    }

    public void updateOfflinePlayerLoss(UUID uuid, long beforeTax) {
        PlayerData playerData = storageHandler.getPlayer(uuid);
        playerData.updateLosses();
        playerData.updateLosses(beforeTax);
        playerData.updateGambled(beforeTax);
        savePlayerData(playerData, false);
    }

    public void loadPlayerData(UUID uuid) {
        plugin.getScheduler().runTaskAsynchronously(() -> {
            PlayerData data = storageHandler.getPlayer(uuid);
            playerDataMap.put(uuid, data);

            CoinflipGame game = storageHandler.getCoinflipGame(uuid);
            if (game != null) {
                plugin.getScheduler().runTask(() -> {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    plugin.getEconomyManager()
                            .getEconomyProvider(game.getProvider())
                            .deposit(player, game.getAmount());

                    if (player.isOnline()) {
                        Messages.GAME_REFUNDED.send(player.getPlayer(),
                                "{AMOUNT}", game.getAmount(),
                                "{PROVIDER}", game.getProvider());
                    }

                    plugin.getGameManager().removeCoinflipGame(uuid);
                    storageHandler.deleteCoinfip(uuid);
                });
            }
        });
    }

    public void savePlayerData(PlayerData player, boolean removeCache) {
        UUID uuid = player.getUUID();
        plugin.getScheduler().runTaskAsynchronously(() -> {
            storageHandler.savePlayer(player);
            if (removeCache) playerDataMap.remove(uuid);
        });
    }

    public Map<UUID, PlayerData> getPlayerDataMap() {
        return playerDataMap;
    }

    public StorageHandler getStorageHandler() {
        return storageHandler;
    }

    private void refundBothAndCancel(CoinflipGame game) {

        // Refund creator
        plugin.getEconomyManager()
                .getEconomyProvider(game.getProvider())
                .deposit(game.getOfflinePlayer(), game.getAmount());

        // Find opponent
        OfflinePlayer opponent = null;
        for (CoinflipGame g : plugin.getGameManager().getCoinflipGames().values()) {
            if (!g.getPlayerUUID().equals(game.getPlayerUUID())) {
                opponent = g.getOfflinePlayer();
                break;
            }
        }

        if (opponent != null) {
            plugin.getEconomyManager()
                    .getEconomyProvider(game.getProvider())
                    .deposit(opponent, game.getAmount());
        }

        // Remove both games
        plugin.getGameManager().removeCoinflipGame(game.getPlayerUUID());
        if (opponent != null) {
            plugin.getGameManager().removeCoinflipGame(opponent.getUniqueId());
        }

        // Notify players
        if (game.getOfflinePlayer().isOnline()) {
            Messages.GAME_REFUNDED.send(
                    game.getOfflinePlayer().getPlayer(),
                    "{AMOUNT}", game.getAmount(),
                    "{PROVIDER}", game.getProvider()
            );
        }

        if (opponent != null && opponent.isOnline()) {
            Messages.GAME_REFUNDED.send(
                    opponent.getPlayer(),
                    "{AMOUNT}", game.getAmount(),
                    "{PROVIDER}", game.getProvider()
            );
        }
    }
}
