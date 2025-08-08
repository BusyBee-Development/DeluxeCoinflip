/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.listener.game;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.Locale;

public final class GameQuitListener implements Listener {

    private final DeluxeCoinflipPlugin plugin;

    public GameQuitListener(@NotNull DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;

        // Register the listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        final Player quitter = event.getPlayer();

        final CoinflipGame game = this.plugin.getGameManager().getCoinflipGame(quitter.getUniqueId());
        if (game == null || game.isActiveGame()) {
            return;
        }

        // Refund the creator
        this.plugin.getEconomyManager()
                .getEconomyProvider(game.getProvider())
                .deposit(game.getOfflinePlayer(), game.getAmount());

        // Notify the creator if they're online (should be quitting here, but just in case)
        if (quitter.isOnline()) {
            Messages.GAME_REFUNDED.send(
                    quitter,
                    "{AMOUNT}", NumberFormat.getNumberInstance(Locale.US).format(game.getAmount()),
                    "{PROVIDER}", game.getProvider()
            );
        }

        // Remove the game from storage and listing
        this.plugin.getStorageManager().getStorageHandler().deleteCoinfip(game.getPlayerUUID());
        this.plugin.getGameManager().removeCoinflipGame(game.getPlayerUUID());
    }
}
