/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.utility;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record ItemStackBuilder(ItemStack ITEM_STACK) {

    private static DeluxeCoinflipPlugin plugin;

    public static void setPlugin(DeluxeCoinflipPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    public ItemStackBuilder(Material material) {
        this(new ItemStack(material));
    }

    public static ItemStackBuilder getItemStack(ConfigurationSection section) {
        if (section == null) {
            return new ItemStackBuilder(Material.BARRIER).withName("&cInvalid material.");
        }

        final String materialName = Objects.toString(section.getString("material"), "BARRIER").toUpperCase();
        final Material resolved = Material.matchMaterial(materialName);
        ItemStack item = (resolved != null) ? new ItemStack(resolved) : new ItemStack(Material.BARRIER);

        if (item.getType() == Material.PLAYER_HEAD && section.contains("base64")) {
            final String base64 = section.getString("base64");
            if (base64 != null && !base64.isEmpty()) {
                item = Base64Util.getBaseHead(base64).clone();
            }
        }

        ItemStackBuilder builder = new ItemStackBuilder(item);

        if (section.contains("amount")) {
            builder.withAmount(section.getInt("amount"));
        }

        if (section.contains("display_name")) {
            builder.withName(section.getString("display_name"));
        }

        if (section.contains("lore")) {
            builder.withLore(section.getStringList("lore"));
        }

        if (section.contains("custom_model_data")) {
            builder.withCustomModelData(section.getInt("custom_model_data"));
        }

        if (section.getBoolean("glow", false)) {
            builder.withGlow();
        }

        if (section.contains("item_flags")) {
            final List<ItemFlag> flags = new ArrayList<>();
            for (String flagName : section.getStringList("item_flags")) {
                if (flagName == null || flagName.isEmpty()) {
                    continue;
                }
                try {
                    flags.add(ItemFlag.valueOf(flagName));
                } catch (IllegalArgumentException ignored) {
                    // Unknown flags can be ignored
                }
            }

            if (!flags.isEmpty()) {
                builder.withFlags(flags.toArray(new ItemFlag[0]));
            }
        }

        return builder;
    }

    public void withAmount(int amount) {
        if (ITEM_STACK != null) {
            ITEM_STACK.setAmount(Math.max(1, amount));
        }
    }

    public void withFlags(ItemFlag... flags) {
        if (ITEM_STACK == null || ITEM_STACK.getType() == Material.AIR) {
            return;
        }

        final ItemMeta meta = ITEM_STACK.getItemMeta();
        if (meta == null) {
            return;
        }

        if (flags != null && flags.length > 0) {
            meta.addItemFlags(flags);
        }

        ITEM_STACK.setItemMeta(meta);
    }

    @SuppressWarnings("deprecation") // legacy Spigot text API
    public ItemStackBuilder withName(String name) {
        if (ITEM_STACK == null || ITEM_STACK.getType() == Material.AIR || name == null || name.isEmpty()) {
            return this;
        }

        final ItemMeta meta = ITEM_STACK.getItemMeta();
        if (meta == null) {
            return this;
        }

        meta.setDisplayName(ColorUtil.color(name));
        ITEM_STACK.setItemMeta(meta);
        return this;
    }

    public void withCustomModelData(int data) {
        if (ITEM_STACK == null || ITEM_STACK.getType() == Material.AIR) {
            return;
        }

        final ItemMeta meta = ITEM_STACK.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setCustomModelData(data);
        ITEM_STACK.setItemMeta(meta);
    }

    public ItemStackBuilder setSkullOwner(OfflinePlayer owner) {
        if (ITEM_STACK == null) {
            return this;
        }

        final ItemMeta meta = ITEM_STACK.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(owner);
            ITEM_STACK.setItemMeta(skullMeta);
        }

        return this;
    }

    // We are fully aware that this is deprecated.
    @SuppressWarnings("deprecation")
    public ItemStackBuilder withLore(List<String> lore) {
        if (ITEM_STACK == null || ITEM_STACK.getType() == Material.AIR || lore == null || lore.isEmpty()) {
            return this;
        }

        final ItemMeta meta = ITEM_STACK.getItemMeta();
        if (meta == null) {
            return this;
        }

        final List<String> colored = new ArrayList<>(lore.size());
        for (String line : lore) {
            colored.add(ColorUtil.color(line));
        }

        meta.setLore(colored);
        ITEM_STACK.setItemMeta(meta);
        return this;
    }

    public void withGlow() {
        if (ITEM_STACK == null || ITEM_STACK.getType() == Material.AIR) {
            return;
        }

        final ItemMeta meta = ITEM_STACK.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        ITEM_STACK.setItemMeta(meta);
        ITEM_STACK.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
    }

    public ItemStack build() {
        if (plugin != null && ITEM_STACK != null) {
            final ItemMeta meta = ITEM_STACK.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(
                        plugin.getKey("dcf.dupeprotection"),
                        PersistentDataType.BYTE,
                        (byte) 1
                );

                ITEM_STACK.setItemMeta(meta);
            }
        }

        return ITEM_STACK;
    }
}
