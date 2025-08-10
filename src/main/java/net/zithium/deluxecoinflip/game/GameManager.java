/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.game;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.storage.StorageManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final DeluxeCoinflipPlugin plugin;
    private final Map<UUID, CoinflipGame> coinflipGames;
    private final StorageManager storageManager;

    public GameManager(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.coinflipGames = new HashMap<>();
        this.storageManager = plugin.getStorageManager();
    }

    /**
     * Add a coinflip game
     *
     * @param uuid The UUID of the player creating the game
     * @param game The coinflip game object
     */
    public void addCoinflipGame(UUID uuid, CoinflipGame game) {
        coinflipGames.put(uuid, game);
        plugin.getScheduler().runTaskAsynchronously(() -> storageManager.getStorageHandler().saveCoinflip(game));
    }

    /**
     * Delete an existing coinflip game
     *
     * @param uuid The UUID of the player removing the game
     */
    public void removeCoinflipGame(@NotNull UUID uuid) {
        coinflipGames.remove(uuid);

        // If the plugin is disabled (e.g., during onDisable), DON'T schedule - run inline
        if (!plugin.isEnabled()) {
            try {
                storageManager.getStorageHandler().deleteCoinflip(uuid);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to delete coinflip for " + uuid + " during shutdown: " + ex.getMessage());
            }

            return;
        }

        plugin.getScheduler().runTaskAsynchronously(() -> {
            try {
                storageManager.getStorageHandler().deleteCoinflip(uuid);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to delete coinflip for " + uuid + ": " + ex.getMessage());
            }
        });
    }

    /**
     * Get all coinflip games
     *
     * @return Map of UUID and CoinflipGame object
     */
    public Map<UUID, CoinflipGame> getCoinflipGames() {
        return coinflipGames;
    }

    public CoinflipGame getCoinflipGame(@NotNull UUID playerUUID) {
        return coinflipGames.get(playerUUID);
    }
}
