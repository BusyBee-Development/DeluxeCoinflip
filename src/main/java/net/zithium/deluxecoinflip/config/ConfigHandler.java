/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.logging.Level;

public class ConfigHandler {

    private final JavaPlugin plugin;
    private final String name;
    private final File file;
    private FileConfiguration configuration;

    public ConfigHandler(JavaPlugin plugin, File path, String name) {
        this.plugin = plugin;
        this.name = name.endsWith(".yml") ? name : name + ".yml";
        this.file = new File(path, this.name);
        this.configuration = new YamlConfiguration();
    }

    public ConfigHandler(JavaPlugin plugin, String name) {
        this(plugin, plugin.getDataFolder(), name);
    }

    public void saveDefaultConfig() {
        try {
            File parent = this.file.getParentFile();
            if (!parent.exists()) {
                boolean made = parent.mkdirs();
                if (!made && !parent.exists()) {
                    throw new IOException("Failed to create directories for: " + parent.getAbsolutePath());
                }
            }

            if (!this.file.exists()) {
                String relative = toDataFolderRelativePath(this.file, this.plugin.getDataFolder());
                InputStream res = this.plugin.getResource(relative);
                if (res != null) {
                    res.close();
                    this.plugin.saveResource(relative, false);
                } else {
                    boolean created = this.file.createNewFile();
                    if (!created && !this.file.exists()) {
                        throw new IOException("Failed to create file: " + this.file.getAbsolutePath());
                    }
                }
            }

            this.configuration = new YamlConfiguration();

            String relative = toDataFolderRelativePath(this.file, this.plugin.getDataFolder());
            InputStream resForDefaults = this.plugin.getResource(relative);
            if (resForDefaults != null) {
                try (InputStreamReader reader = new InputStreamReader(resForDefaults, StandardCharsets.UTF_8)) {
                    YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
                    this.configuration.setDefaults(defaults);
                    this.configuration.options().copyDefaults(true);
                }
            }

            this.configuration.load(this.file);
        } catch (IOException | InvalidConfigurationException e) {
            this.plugin.getLogger().log(Level.SEVERE, String.format("""
                    ============= CONFIGURATION ERROR =============
                    There was an error loading %s
                    Please check for any obvious configuration mistakes
                    such as using tabs for spaces or forgetting to end quotes
                    before reporting to the developer. The plugin will now disable..
                    ============= CONFIGURATION ERROR =============
                    """, this.name), e);
            this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
        }
    }

    public void save() {
        if (this.configuration == null) {
            return;
        }

        try {
            this.configuration.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error saving configuration '" + this.name + "'.", e);
        }
    }

    public void reload() {
        this.configuration = YamlConfiguration.loadConfiguration(this.file);

        String relative = toDataFolderRelativePath(this.file, this.plugin.getDataFolder());
        InputStream resForDefaults = this.plugin.getResource(relative);
        if (resForDefaults != null) {
            try (InputStreamReader reader = new InputStreamReader(resForDefaults, StandardCharsets.UTF_8)) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
                this.configuration.setDefaults(defaults);
                this.configuration.options().copyDefaults(true);
            } catch (IOException ignored) {
                // This marks that the file was successfully read.
            }
        }
    }

    public FileConfiguration getConfig() {
        return this.configuration;
    }

    public File getFile() {
        return this.file;
    }

    private static String toDataFolderRelativePath(File target, File dataFolder) {
        Path base = dataFolder.toPath().toAbsolutePath().normalize();
        Path path = target.toPath().toAbsolutePath().normalize();
        return base.relativize(path).toString().replace(File.separatorChar, '/');
    }
}
