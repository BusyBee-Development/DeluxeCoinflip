/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.cache;

import net.zithium.deluxecoinflip.game.CoinflipGame;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public final class ActiveGamesCache {

    private final ConcurrentMap<UUID, CoinflipGame> activeGames = new ConcurrentHashMap<>();

    public CoinflipGame getGame(@NotNull UUID playerUUID) {
        return this.activeGames.get(playerUUID);
    }

    public List<UUID> getParticipants(@NotNull CoinflipGame game) {
        return this.activeGames.entrySet().stream()
                .filter(e -> e.getValue() == game)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public boolean isInGame(@NotNull UUID playerUUID) {
        return this.activeGames.containsKey(playerUUID);
    }

    public void register(@NotNull CoinflipGame game) {
        final UUID playerId = game.getPlayerUUID();
        final UUID opponentId = game.getOpponentUUID();

        this.activeGames.put(playerId, game);

        if (opponentId != null) {
            this.activeGames.put(opponentId, game);
        }
    }

    public void unregister(@NotNull CoinflipGame game) {
        this.activeGames.entrySet().removeIf(e -> e.getValue() == game);
    }

    public java.util.Collection<CoinflipGame> getAllUniqueGames() {
        return new java.util.LinkedHashSet<>(this.activeGames.values());
    }

    public void clear() {
        this.activeGames.clear();
    }
}
