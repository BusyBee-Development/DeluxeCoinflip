/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.utility;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.cache.ActiveGamesCache;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class GameShutdownUtil {

    public static void shutdownAll(@NotNull DeluxeCoinflipPlugin plugin) {
        shutdownActiveGames(plugin);
        shutdownNonActiveListings(plugin);
    }

    /**
     * Handle ACTIVE games: stop animations and refund BOTH participants.
     */
    public static void shutdownActiveGames(@NotNull DeluxeCoinflipPlugin plugin) {
        ActiveGamesCache cache = plugin.getActiveGamesCache();
        Collection<CoinflipGame> activeGames = new LinkedHashSet<>(cache.getAllUniqueGames());

        for (CoinflipGame game : activeGames) {
            if (game.isActiveGame()) {
                game.stopAnimation();
            }

            List<UUID> participants = new ArrayList<>(new LinkedHashSet<>(cache.getParticipants(game)));
            if (participants.isEmpty()) {
                // Fallback from game fields if cache returns nothing
                UUID creatorId = game.getPlayerUUID();
                if (creatorId != null) participants.add(creatorId);
                UUID oppId = game.getOpponentUUID();
                if (oppId != null && !oppId.equals(creatorId)) participants.add(oppId);
            }

            // Unregister from active cache
            cache.unregister(game);

            // Refund both sides
            EconomyProvider provider = plugin.getEconomyManager().getEconomyProvider(game.getProvider());
            long amount = game.getAmount();
            String amountStr = NumberFormat.getNumberInstance(Locale.US).format(amount);

            for (UUID pid : participants) {
                Player online = plugin.getServer().getPlayer(pid);
                if (online != null) {
                    Messages.GAME_REFUNDED.send(online, "{AMOUNT}", amountStr, "{PROVIDER}", game.getProvider());
                }
                OfflinePlayer off = plugin.getServer().getOfflinePlayer(pid);
                provider.deposit(off, amount);
            }

            // Clean listing/storage by creator
            plugin.getGameManager().removeCoinflipGame(game.getPlayerUUID());
            plugin.getStorageManager().getStorageHandler().deleteCoinflip(game.getPlayerUUID());
        }

        cache.clear();
    }

    /**
     * Handle NON-ACTIVE listings: refund CREATOR only using GameManager (not the active cache).
     */
    public static void shutdownNonActiveListings(@NotNull DeluxeCoinflipPlugin plugin) {
        Map<UUID, CoinflipGame> listings = plugin.getGameManager().getCoinflipGames();
        List<CoinflipGame> toProcess = new ArrayList<>(listings.values());

        for (CoinflipGame game : toProcess) {
            if (game == null) continue;
            if (game.isActiveGame()) continue; // Active games already handled above

            UUID creatorId = game.getPlayerUUID();
            if (creatorId == null) continue;

            EconomyProvider provider = plugin.getEconomyManager().getEconomyProvider(game.getProvider());
            long amount = game.getAmount();
            String amountStr = NumberFormat.getNumberInstance(Locale.US).format(amount);

            // Refund creator only
            OfflinePlayer creatorOff = plugin.getServer().getOfflinePlayer(creatorId);
            provider.deposit(creatorOff, amount);

            Player creatorOnline = plugin.getServer().getPlayer(creatorId);
            if (creatorOnline != null) {
                Messages.GAME_REFUNDED.send(creatorOnline, "{AMOUNT}", amountStr, "{PROVIDER}", game.getProvider());
            }

            // Remove listing + storage
            plugin.getGameManager().removeCoinflipGame(creatorId);
            plugin.getStorageManager().getStorageHandler().deleteCoinflip(creatorId);
        }
    }
}
