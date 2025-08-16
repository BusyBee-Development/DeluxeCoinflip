/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.utility;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface Base64Util {

    Map<String, ItemStack> CACHE = new ConcurrentHashMap<>();
    UUID PROFILE_UUID = UUID.fromString("92864445-51c5-4c3b-9039-517c9927d1b4");

    String JSON_PREFIX = "{\"textures\":{\"SKIN\":{\"url\":\"";
    String JSON_SUFFIX = "\"}}}";

    /**
     * Build (or retrieve from cache) a player head with the given Base64 texture JSON.
     * Returns a clone so callers cannot mutate cached instances.
     */
    static ItemStack getBaseHead(String base64TextureJson) {
        if (base64TextureJson == null || base64TextureJson.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        ItemStack cached = CACHE.get(base64TextureJson);
        if (cached != null) {
            return cached.clone();
        }

        URL skinUrl = decodeSkinUrl(base64TextureJson);
        if (skinUrl == null) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            return head;
        }

        applySkin(meta, skinUrl);

        head.setItemMeta(meta);
        CACHE.put(base64TextureJson, head.clone());
        return head;
    }

    @SuppressWarnings("deprecation")
    private static void applySkin(SkullMeta meta, URL skinUrl) {
        PlayerProfile profile = createProfile();
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(skinUrl);
        profile.setTextures(textures);

        meta.setOwnerProfile(profile);
    }

    @SuppressWarnings("deprecation")
    private static PlayerProfile createProfile() {
        try {
            return Bukkit.createProfile(PROFILE_UUID);
        } catch (NoSuchMethodError ignored) {
            return Bukkit.createPlayerProfile(PROFILE_UUID);
        }
    }

    private static URL decodeSkinUrl(String base64TextureJson) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64TextureJson), StandardCharsets.UTF_8);

            int start = decoded.indexOf(JSON_PREFIX);
            if (start >= 0) {
                start += JSON_PREFIX.length();
                int end = decoded.indexOf(JSON_SUFFIX, start);
                if (end > start) {
                    return new URL(decoded.substring(start, end));
                }
            }

            int httpIndex = decoded.indexOf("http");
            if (httpIndex >= 0) {
                int quoteEnd = decoded.indexOf('"', httpIndex);
                String urlString = (quoteEnd > httpIndex)
                        ? decoded.substring(httpIndex, quoteEnd)
                        : decoded.substring(httpIndex);
                return new URL(urlString);
            }
        } catch (IllegalArgumentException | MalformedURLException ignored) {
            // Malformed base64 or URL
        }

        return null;
    }
}
