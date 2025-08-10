/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.game;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import net.zithium.deluxecoinflip.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public record GameAnimationRunner(DeluxeCoinflipPlugin plugin) {

    public void runAnimation(OfflinePlayer winner, OfflinePlayer loser, CoinflipGame game,
                             Gui winnerGui, Gui loserGui, SecureRandom random) {

        WrappedScheduler scheduler = plugin.getScheduler();

        boolean creatorIsWinner = winner.getUniqueId().equals(game.getPlayerUUID());

        ItemStack winnerStack = buildConfiguredPlayerItem("coinflip-gui.player-items.winner",
                winner, creatorIsWinner ? game.getCachedHead() : null);
        ItemStack loserStack = buildConfiguredPlayerItem("coinflip-gui.player-items.loser",
                loser, creatorIsWinner ? null : game.getCachedHead());

        GuiItem winnerHead = new GuiItem(winnerStack);
        GuiItem loserHead = new GuiItem(loserStack);

        Player winnerPlayer = Bukkit.getPlayer(winner.getUniqueId());
        Player loserPlayer = Bukkit.getPlayer(loser.getUniqueId());

        if (winnerPlayer != null) {
            scheduler.runTaskAtEntity(winnerPlayer, () -> {
                winnerGui.open(winnerPlayer);
                plugin.getInventoryManager().getCoinflipGUI().startAnimation(
                        scheduler, winnerGui, winnerHead, loserHead,
                        winner, loser, game, winnerPlayer, random, true);
            });
        }

        if (loserPlayer != null) {
            scheduler.runTaskAtEntity(loserPlayer, () -> {
                loserGui.open(loserPlayer);
                plugin.getInventoryManager().getCoinflipGUI().startAnimation(
                        scheduler, loserGui, winnerHead, loserHead,
                        winner, loser, game, loserPlayer, random, false);
            });
        }
    }

    private ItemStack buildConfiguredPlayerItem(String path, OfflinePlayer player, ItemStack cachedHeadIfAny) {
        ConfigurationSection section = plugin.getConfigHandler(ConfigType.CONFIG).getConfig().getConfigurationSection(path);
        if (section == null) {
            ItemStack base = cachedHeadIfAny != null ? cachedHeadIfAny.clone() : new ItemStack(Material.PLAYER_HEAD);
            return new ItemStackBuilder(base)
                    .withName(TextUtil.color("<yellow>" + safeName(player)))
                    .setSkullOwner(player)
                    .build();
        }

        String displayNameRaw = section.getString("display_name", "&e{PLAYER}");
        String displayName = TextUtil.color(displayNameRaw.replace("{PLAYER}", safeName(player)));

        List<String> loreLines = section.getStringList("lore");
        List<String> coloredLore = new ArrayList<>();
        if (!loreLines.isEmpty()) {
            for (String line : loreLines) {
                coloredLore.add(TextUtil.color(line.replace("{PLAYER}", safeName(player))));
            }
        }

        String materialString = section.getString("material", "PLAYER_HEAD");
        Material material = Material.matchMaterial(materialString);
        if (material == null) {
            material = Material.PLAYER_HEAD;
        }

        boolean usePlayerSkin = section.getBoolean("use-player-skin", true);
        String base64 = section.getString("base64", "");
        boolean hasBase64 = !base64.isEmpty();

        ItemStackBuilder builder;
        if (material == Material.PLAYER_HEAD && usePlayerSkin && !hasBase64 && cachedHeadIfAny != null) {
            builder = new ItemStackBuilder(cachedHeadIfAny.clone());
        } else {
            builder = ItemStackBuilder.getItemStack(section);
        }

        builder.withName(displayName);
        if (!coloredLore.isEmpty()) {
            builder.withLore(coloredLore);
        }

        if (material == Material.PLAYER_HEAD && usePlayerSkin && !hasBase64) {
            builder.setSkullOwner(player);
        }

        return builder.build();
    }

    private static String safeName(OfflinePlayer player) {
        return player.getName() == null ? "Unknown" : player.getName();
    }
}
