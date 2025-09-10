package net.zithium.deluxecoinflip.placeholders;

import net.zithium.deluxecoinflip.api.DeluxePlaceholdersApi;
import org.bukkit.entity.Player;

public class InternalDeluxePlaceholdersApi implements DeluxePlaceholdersApi {

    private final PlaceholderRegistry registry;

    public InternalDeluxePlaceholdersApi(PlaceholderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void register(String key, PlaceholderRegistry.Provider provider) {
        registry.register(key, provider);
    }

    @Override
    public void unregister(String key) {
        registry.unregister(key);
    }

    @Override
    public String apply(String message, Player player) {
        return registry.apply(message, player);
    }

    public PlaceholderRegistry getRegistry() {
        return registry;
    }
}
