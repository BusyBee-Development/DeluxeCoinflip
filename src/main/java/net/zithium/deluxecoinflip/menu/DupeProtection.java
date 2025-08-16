/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.menu;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.concurrent.CompletableFuture;

public record DupeProtection(NamespacedKey dupeKey) implements Listener {

    private static final String KEY_PATH = "dcf.dupeprotection";

    public DupeProtection(DeluxeCoinflipPlugin plugin) {
        this(plugin.getKey(KEY_PATH));
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void cleanPlayerInventory(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            ItemStack[] contents = player.getInventory().getContents();
            boolean changed = false;

            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack item = contents[slot];
                if (isProtected(item)) {
                    contents[slot] = null;
                    changed = true;
                }
            }

            if (changed && player.isOnline()) {
                player.getInventory().setContents(contents);
            }

            return changed;
        });
    }

    private boolean isProtected(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(dupeKey, PersistentDataType.BYTE);
    }

    private void cancelIfProtected(ItemStack item, Cancellable event) {
        if (isProtected(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        cancelIfProtected(event.getItemDrop().getItemStack(), event);
        cleanPlayerInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        cancelIfProtected(event.getItem(), event);
        cleanPlayerInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        cleanPlayerInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            cleanPlayerInventory(player);
        }
    }
}
