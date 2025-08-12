/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage.handler.impl;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.cache.ActiveGamesCache;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.storage.handler.GameShutdownProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record DefaultGameShutdownProvider(DeluxeCoinflipPlugin plugin) implements GameShutdownProvider {

    @Override
    public void shutdownAll() {
        shutdownActiveGames();
        shutdownNonActiveListings();
    }

    @Override
    public void shutdownActiveGames() {
        final ActiveGamesCache activeCache = plugin.getActiveGamesCache();

        final Collection<CoinflipGame> activeGames = new LinkedHashSet<>(activeCache.getAllUniqueGames());
        for (CoinflipGame game : activeGames) {
            if (game == null) {
                continue;
            }

            if (game.isActiveGame()) {
                game.stopAnimation();
            }

            final Set<UUID> participants = collectParticipants(activeCache, game);
            activeCache.unregister(game);

            final EconomyProvider provider = plugin.getEconomyManager().getEconomyProvider(game.getProvider());
            if (provider == null) {
                removeListingAndStorage(game.getPlayerUUID());
                continue;
            }

            final long amount = game.getAmount();
            final String amountFormatted = NumberFormat.getNumberInstance(Locale.US).format(amount);

            for (UUID participantId : participants) {
                refundPlayer(provider, participantId, amount, amountFormatted, game.getProvider());
            }

            removeListingAndStorage(game.getPlayerUUID());
        }

        activeCache.clear();
    }

    @Override
    public void shutdownNonActiveListings() {

        final Map<UUID, CoinflipGame> listings = plugin.getGameManager().getCoinflipGames();
        final List<CoinflipGame> snapshot = new ArrayList<>(listings.values());

        for (CoinflipGame game : snapshot) {
            if (game == null || game.isActiveGame()) {
                continue;
            }

            final UUID creatorId = game.getPlayerUUID();
            if (creatorId == null) {
                continue;
            }

            final EconomyProvider provider = plugin.getEconomyManager().getEconomyProvider(game.getProvider());
            if (provider == null) {
                removeListingAndStorage(creatorId);
                continue;
            }

            final long amount = game.getAmount();
            final String amountFormatted = NumberFormat.getNumberInstance(Locale.US).format(amount);

            refundPlayer(provider, creatorId, amount, amountFormatted, game.getProvider());
            removeListingAndStorage(creatorId);
        }
    }

    private Set<UUID> collectParticipants(ActiveGamesCache cache, CoinflipGame game) {
        final Set<UUID> participants = new LinkedHashSet<>(cache.getParticipants(game));

        final UUID creatorId = game.getPlayerUUID();
        final UUID opponentId = game.getOpponentUUID();

        if (creatorId != null) {
            participants.add(creatorId);
        }
        if (opponentId != null) {
            participants.add(opponentId);
        }

        return participants;
    }

    private void refundPlayer(EconomyProvider provider, UUID playerId,
                              long amount, String amountFormatted, String providerIdentifier) {

        if (playerId == null) {
            return;
        }

        final Player online = plugin.getServer().getPlayer(playerId);
        if (online != null) {
            Messages.GAME_REFUNDED.send(online, "{AMOUNT}", amountFormatted, "{CURRENCY}", providerIdentifier);
        }

        final OfflinePlayer offline = plugin.getServer().getOfflinePlayer(playerId);
        provider.deposit(offline, amount);
    }

    private void removeListingAndStorage(UUID creatorId) {
        if (creatorId == null) {
            return;
        }

        plugin.getGameManager().removeCoinflipGame(creatorId);
        plugin.getStorageManager().getStorageHandler().deleteCoinflip(creatorId);
    }
}
