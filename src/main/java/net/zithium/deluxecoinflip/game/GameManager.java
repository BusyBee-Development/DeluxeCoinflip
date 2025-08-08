/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.game;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.storage.StorageManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GameManager {

    private final DeluxeCoinflipPlugin plugin;
    private final ConcurrentMap<UUID, CoinflipGame> coinflipGames;
    private final StorageManager storageManager;
    private final ConcurrentMap<UUID, UUID> pairings;

    private boolean canStartGame = false;

    public GameManager(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.coinflipGames = new ConcurrentHashMap<>();
        this.storageManager = plugin.getStorageManager();
        this.pairings = new ConcurrentHashMap<>();
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
    public void removeCoinflipGame(UUID uuid) {
        coinflipGames.remove(uuid);
        plugin.getScheduler().runTaskAsynchronously(() -> storageManager.getStorageHandler().deleteCoinfip(uuid));
    }

    /**
     * Register a creator/opponent pair when a game actually starts.
     *
     * @param creator  The UUID of the game owner (creator)
     * @param opponent The UUID of the opponent
     */
    public void registerPair(UUID creator, UUID opponent) {
        pairings.put(creator, opponent);
    }

    /**
     * Resolve the owner (creator) UUID for any participant in a running game.
     * If the participant is the owner, returns that UUID.
     * If the participant is the opponent, returns the creator's UUID.
     */
    public Optional<UUID> getOwnerFor(UUID participant) {
        if (pairings.containsKey(participant)) {
            return Optional.of(participant);
        }

        for (Map.Entry<UUID, UUID> e : pairings.entrySet()) {
            if (participant.equals(e.getValue())) {
                return Optional.of(e.getKey());
            }
        }

        return Optional.empty();
    }

    /**
     * Get the opponent of a participant (works whether participant is creator or opponent).
     *
     * @param participant The participant's UUID
     * @return Optional opponent UUID
     */
    public Optional<UUID> getOpponent(UUID participant) {
        if (pairings.containsKey(participant)) {
            return Optional.ofNullable(pairings.get(participant));
        }

        for (Map.Entry<UUID, UUID> entry : pairings.entrySet()) {
            if (participant.equals(entry.getValue())) {
                return Optional.of(entry.getKey());
            }
        }

        return Optional.empty();
    }

    /**
     * Remove a pairing by either participant.
     *
     * @param participant The participant's UUID (creator or opponent)
     */
    public void removePairByAny(UUID participant) {
        if (pairings.remove(participant) != null) {
            return;
        }

        UUID toRemove = null;
        for (Map.Entry<UUID, UUID> entry : pairings.entrySet()) {
            if (participant.equals(entry.getValue())) {
                toRemove = entry.getKey();
                break;
            }
        }

        if (toRemove != null) {
            pairings.remove(toRemove);
        }
    }

    /**
     * Get all coinflip games
     *
     * @return Map of UUID and CoinflipGame object
     */
    public Map<UUID, CoinflipGame> getCoinflipGames() {
        return coinflipGames;
    }

    public boolean canStartGame() {
        return canStartGame;
    }

    public void canStartGame(boolean canStartGame) {
        this.canStartGame = canStartGame;
    }
}
