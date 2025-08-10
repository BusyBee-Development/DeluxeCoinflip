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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

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
            this.activeGames.entrySet().removeIf(e -> e.getValue() == game);
        }
    }

    public List<UUID> getParticipants(@NotNull CoinflipGame game) {
        final UUID a = game.getPlayerUUID();
        final UUID b = game.getOpponentUUID();

        if (a != null && b != null) {
            return Arrays.asList(a, b);
        }

        if (a != null) {
            return Collections.singletonList(a);
        }

        if (b != null) {
            return Collections.singletonList(b);
        }

        return this.activeGames.entrySet().stream()
                .filter(e -> e.getValue() == game)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Collection<CoinflipGame> getAllUniqueGames() {
        return new LinkedHashSet<>(this.activeGames.values());
    }

    public void clear() {
        this.activeGames.clear();
    }
}
