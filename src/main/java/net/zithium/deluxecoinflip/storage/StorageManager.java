/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.exception.InvalidStorageHandlerException;
import net.zithium.deluxecoinflip.storage.handler.StorageHandler;
import net.zithium.deluxecoinflip.storage.handler.impl.SQLiteHandler;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager implements Listener {

    private final DeluxeCoinflipPlugin plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private StorageHandler storageHandler;

    public StorageManager(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.playerDataMap = new ConcurrentHashMap<>();
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

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

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
        plugin.getScheduler().runTaskAsynchronously(() -> {
            PlayerData playerData = storageHandler.getPlayer(uuid);
            playerData.updateWins();
            playerData.updateProfit(profit);
            playerData.updateGambled(beforeTax);
            storageHandler.savePlayer(playerData);
        });
    }

    public void updateOfflinePlayerLoss(UUID uuid, long beforeTax) {
        plugin.getScheduler().runTaskAsynchronously(() -> {
            PlayerData playerData = storageHandler.getPlayer(uuid);
            playerData.updateLosses();
            playerData.updateLosses(beforeTax);
            playerData.updateGambled(beforeTax);
            storageHandler.savePlayer(playerData);
        });
    }

    public void loadPlayerData(UUID uuid) {
        plugin.getScheduler().runTaskAsynchronously(() -> {
            final PlayerData data = storageHandler.getPlayer(uuid);
            this.playerDataMap.put(uuid, data);
        });
    }

    public void savePlayerData(PlayerData player, boolean removeCache) {
        UUID uuid = player.getUUID();
        plugin.getScheduler().runTaskAsynchronously(() -> {
            storageHandler.savePlayer(player);
            if (removeCache) {
                playerDataMap.remove(uuid);
            }
        });
    }

    public Map<UUID, PlayerData> getPlayerDataMap() {
        return playerDataMap;
    }

    public StorageHandler getStorageHandler() {
        return storageHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        loadPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        getPlayer(event.getPlayer().getUniqueId()).ifPresent(data -> savePlayerData(data, true));
    }
}
