/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.game;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public record GameAnimationRunner(DeluxeCoinflipPlugin plugin) {

    public void runAnimation(OfflinePlayer winner, OfflinePlayer loser, CoinflipGame game, Gui winnerGui, Gui loserGui) {
        final WrappedScheduler scheduler = plugin.getScheduler();

        boolean isWinnerGamePlayer = winner.getUniqueId().equals(game.getPlayerUUID());

        ItemStack winnerItem = new ItemStackBuilder(
                isWinnerGamePlayer ? game.getCachedHead() : new ItemStack(Material.PLAYER_HEAD))
                .withName(ColorUtil.color("<yellow>" + winner.getName()))
                .setSkullOwner(winner)
                .build();

        ItemStack loserItem = new ItemStackBuilder(
                isWinnerGamePlayer ? new ItemStack(Material.PLAYER_HEAD) : game.getCachedHead())
                .withName(ColorUtil.color("<yellow>" + loser.getName()))
                .setSkullOwner(loser)
                .build();

        GuiItem winnerHead = new GuiItem(winnerItem);
        GuiItem loserHead = new GuiItem(loserItem);

        Player winnerPlayer = Bukkit.getPlayer(winner.getUniqueId());
        Player loserPlayer = Bukkit.getPlayer(loser.getUniqueId());

        if (winnerPlayer != null) {
            scheduler.runTaskAtEntity(winnerPlayer, () -> {
                winnerGui.open(winnerPlayer);
                plugin.getInventoryManager().getCoinflipGUI().startAnimation(
                    scheduler, winnerGui, winnerHead, loserHead,
                    winner, loser, game, winnerPlayer, true);
            });
        }

        if (loserPlayer != null) {
            scheduler.runTaskAtEntity(loserPlayer, () -> {
                loserGui.open(loserPlayer);
                plugin.getInventoryManager().getCoinflipGUI().startAnimation(
                    scheduler, loserGui, winnerHead, loserHead,
                    winner, loser, game, loserPlayer, false);
            });
        }

        UUID creator = game.getPlayerUUID();
        UUID other = isWinnerGamePlayer ? loser.getUniqueId() : winner.getUniqueId();
        plugin.getGameManager().registerPair(creator, other);
    }
}
