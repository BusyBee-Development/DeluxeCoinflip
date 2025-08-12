/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.listener.game;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.cache.ActiveGamesCache;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class ActiveGameQuitListener implements Listener {
    private final DeluxeCoinflipPlugin plugin;
    private final ActiveGamesCache activeGamesCache;

    public ActiveGameQuitListener(@NotNull DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.activeGamesCache = plugin.getActiveGamesCache();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        final Player quitter = event.getPlayer();

        final CoinflipGame game = this.activeGamesCache.getGame(quitter.getUniqueId());
        if (game == null || !game.isActiveGame()) {
            return;
        }

        final Set<UUID> participants = new LinkedHashSet<>(this.activeGamesCache.getParticipants(game));

        game.stopAnimation();
        this.activeGamesCache.unregister(game);

        final Server server = this.plugin.getServer();
        final EconomyManager economyManager = this.plugin.getEconomyManager();
        final EconomyProvider economyProvider = economyManager.getEconomyProvider(game.getProvider());
        if (economyProvider == null) {
            this.plugin.getLogger().warning("Missing economy provider '" + game.getProvider() + "'; refunds skipped.");
            this.plugin.getGameManager().removeCoinflipGame(game.getPlayerUUID());
            return;
        }

        final long amount = game.getAmount();
        final String amountFormatted = String.format(Locale.US, "%,d", amount);

        for (UUID participantId : participants) {
            final Player participant = server.getPlayer(participantId);
            if (participant != null) {
                Messages.GAME_REFUNDED.send(
                        participant,
                        "{AMOUNT}", amountFormatted,
                        "{CURRENCY}", game.getProvider()
                );
            }

            economyProvider.deposit(server.getOfflinePlayer(participantId), amount);
        }

        this.plugin.getGameManager().removeCoinflipGame(game.getPlayerUUID());
    }
}
