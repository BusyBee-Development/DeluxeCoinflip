/*
 * DeluxeCoinflip Plugin - PlaceholderAPI Expansion
 */
package net.zithium.deluxecoinflip.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.storage.StorageManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class DeluxeCoinflipExpansion extends PlaceholderExpansion {

    private final DeluxeCoinflipPlugin plugin;
    private final StorageManager storageManager;

    public DeluxeCoinflipExpansion(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "deluxecoinflip";
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        // Hearts label placeholders (text labels, not numbers)
        switch (identifier.toLowerCase()) {
            case "hearts_won":
                return "Hearts won";
            case "hearts_lost":
                return "Hearts lost";
            case "hearts_bet":
                return "Hearts bet";
        }

        Optional<PlayerData> playerDataOptional = storageManager.getPlayer(player.getUniqueId());
        if (playerDataOptional.isEmpty()) {
            return "N/A";
        }

        PlayerData playerData = playerDataOptional.get();
        return switch (identifier.toLowerCase()) {
            // Existing placeholders retained for compatibility
            case "games_played", "total_games" -> String.valueOf(plugin.getGameManager().getCoinflipGames().size());
            case "wins" -> String.valueOf(playerData.getWins());
            case "losses" -> String.valueOf(playerData.getLosses());
            case "win_percentage" -> String.valueOf(playerData.getWinPercentage());
            case "profit" -> String.valueOf(playerData.getProfit());
            case "profit_formatted" -> String.valueOf(playerData.getProfitFormatted());
            case "total_losses" -> String.valueOf(playerData.getTotalLosses());
            case "total_losses_formatted" -> String.valueOf(playerData.getTotalLossesFormatted());
            case "total_gambled" -> String.valueOf(playerData.getTotalGambled());
            case "total_gambled_formatted" -> String.valueOf(playerData.getTotalGambledFormatted());
            case "display_broadcast_messages" -> String.valueOf(playerData.isDisplayBroadcastMessages());
            default -> "";
        };
    }
}
