package net.zithium.deluxecoinflip.api;

import net.zithium.deluxecoinflip.placeholders.PlaceholderRegistry;
import org.bukkit.entity.Player;

/**
 * Public API exposed via Bukkit Services to let external plugins register
 * and apply DeluxeCoinflip placeholders.
 */
public interface DeluxePlaceholdersApi {

    /**
     * Register or replace a placeholder provider by KEY (case-insensitive).
     */
    void register(String key, PlaceholderRegistry.Provider provider);

    /**
     * Unregister a placeholder provider by KEY (case-insensitive).
     */
    void unregister(String key);

    /**
     * Apply known placeholders to a message for a given player context.
     */
    String apply(String message, Player player);
}
