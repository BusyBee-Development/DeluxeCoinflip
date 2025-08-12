/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.cache;

import net.zithium.deluxecoinflip.game.CoinflipGame;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ActiveGamesCache {

    private final ConcurrentMap<UUID, CoinflipGame> activeGames = new ConcurrentHashMap<>();

    public CoinflipGame getGame(@NotNull UUID playerUUID) {
        return this.activeGames.get(playerUUID);
    }

    public boolean isInGame(@NotNull UUID playerUUID) {
        return this.activeGames.containsKey(playerUUID);
    }

    public void register(@NotNull CoinflipGame game) {
        final UUID playerId = game.getPlayerUUID();
        final UUID opponentId = game.getOpponentUUID();

        if (playerId != null) {
            this.activeGames.put(playerId, game);
        }

        if (opponentId != null) {
            this.activeGames.put(opponentId, game);
        }
    }

    public void unregister(@NotNull CoinflipGame game) {
        final UUID playerId = game.getPlayerUUID();
        final UUID opponentId = game.getOpponentUUID();

        boolean removedAny = false;

        if (playerId != null) {
            removedAny |= this.activeGames.remove(playerId, game);
        }

        if (opponentId != null) {
            removedAny |= this.activeGames.remove(opponentId, game);
        }

        if (!removedAny && (playerId == null || opponentId == null)) {
            this.activeGames.entrySet().removeIf(entry -> entry.getValue() == game);
        }
    }

    public List<UUID> getParticipants(@NotNull CoinflipGame game) {
        final UUID playerId = game.getPlayerUUID();
        final UUID opponentUUID = game.getOpponentUUID();

        if (playerId != null && opponentUUID != null) {
            return Arrays.asList(playerId, opponentUUID);
        }

        if (playerId != null) {
            return Collections.singletonList(playerId);
        }

        if (opponentUUID != null) {
            return Collections.singletonList(opponentUUID);
        }

        return this.activeGames.entrySet().stream()
                .filter(entry -> entry.getValue() == game)
                .map(Map.Entry::getKey)
                .toList();
    }

    public Collection<CoinflipGame> getAllUniqueGames() {
        return new LinkedHashSet<>(this.activeGames.values());
    }

    public void clear() {
        this.activeGames.clear();
    }
}
