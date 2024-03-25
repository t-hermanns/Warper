package com.smallow.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smallow.Warper;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("warper.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ModConfig loadConfig() {
        // Attempt to read the config file
        if (Files.exists(CONFIG_PATH)) {
            try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                // Deserialize the JSON back into a ModConfig object
                return GSON.fromJson(reader, ModConfig.class);
            } catch (Exception e) {
                Warper.LOGGER.error("Failed to load the config file, using default values.");
            }
        }

        // Generate default config if none found
        ModConfig defaultConfig = new ModConfig();
        saveConfig(defaultConfig);
        return defaultConfig;
    }

    public static void saveConfig(ModConfig config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent()); // Ensure the directory exists
            try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                // Serialize the ModConfig object to JSON and save
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            Warper.LOGGER.error("Failed to save the config file.");
        }
    }
}
