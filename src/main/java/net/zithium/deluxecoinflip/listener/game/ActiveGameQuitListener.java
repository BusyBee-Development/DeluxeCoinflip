/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.listener.game;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.cache.ActiveGamesCache;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.*;

public final class ActiveGameQuitListener implements Listener {
    private final DeluxeCoinflipPlugin plugin;
    private final ActiveGamesCache activeGamesCache;

    public ActiveGameQuitListener(@NotNull DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.activeGamesCache = plugin.getActiveGamesCache();

        // Register the listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        final Player quitter = event.getPlayer();

        // Get the player's game
        final CoinflipGame game = this.activeGamesCache.getGame(quitter.getUniqueId());
        if (game == null || !game.isActiveGame()) {
            return;
        }

        // Stop the animation
        game.stopAnimation();

        // Get participants
        final List<UUID> participants = new ArrayList<>(new LinkedHashSet<>(
                this.activeGamesCache.getParticipants(game)
        ));

        // Remove from active cache
        this.activeGamesCache.unregister(game);

        // Prepare economy and amount string
        final EconomyProvider economyProvider = this.plugin.getEconomyManager()
                .getEconomyProvider(game.getProvider());
        final String amountStr = NumberFormat.getNumberInstance(Locale.US)
                .format(game.getAmount());

        // Notify and refund all participants
        for (UUID participantId : participants) {
            final Player participant = this.plugin.getServer().getPlayer(participantId);
            if (participant != null) {
                Messages.GAME_REFUNDED.send(
                        participant,
                        "{AMOUNT}", amountStr,
                        "{PROVIDER}", game.getProvider()
                );
            }

            economyProvider.deposit(this.plugin.getServer().getOfflinePlayer(participantId), game.getAmount());
        }

        // Remove game listing
        this.plugin.getGameManager().removeCoinflipGame(game.getPlayerUUID());
    }
}
