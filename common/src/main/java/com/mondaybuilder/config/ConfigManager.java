package com.mondaybuilder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SERVER_ROOT = Platform.getGameFolder();

    public static ModConfig.General general = new ModConfig.General();
    public static ModConfig.MapConfig map = new ModConfig.MapConfig();
    public static ModConfig.Items items = new ModConfig.Items();
    public static ModConfig.Words words = new ModConfig.Words();
    public static ModConfig.Localization localization = new ModConfig.Localization();

    public static void loadAll() {
        // Load Main Config
        ModConfig.MainConfig mainConfig = load("mondaybuilder.config.json", ModConfig.MainConfig.class);
        general = mainConfig.general;
        items = mainConfig.items;
        map = mainConfig.map;

        // Load Words
        words = load("mondaybuilder.words.json", ModConfig.Words.class);

        // Load Localization
        localization = load("mondaybuilder.localization.json", ModConfig.Localization.class);
    }

    private static <T> T load(String filename, Class<T> clazz) {
        Path serverFilePath = SERVER_ROOT.resolve(filename);
        File serverFile = serverFilePath.toFile();

        if (!serverFile.exists()) {
            copyDefaultConfig(filename, serverFilePath);
        }

        try (FileReader reader = new FileReader(serverFile)) {
            T loaded = GSON.fromJson(reader, clazz);
            return loaded != null ? loaded : GSON.fromJson(getDefaultJson(filename), clazz);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                return GSON.fromJson(getDefaultJson(filename), clazz);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load default config for " + filename, ex);
            }
        }
    }

    private static void copyDefaultConfig(String filename, Path target) {
        String resourcePath = "/mondaybuilder/" + filename.replace("mondaybuilder.", "");
        try (InputStream is = ConfigManager.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                Files.copy(is, target);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getDefaultJson(String filename) throws IOException {
        String resourcePath = "/mondaybuilder/" + filename.replace("mondaybuilder.", "");
        try (InputStream is = ConfigManager.class.getResourceAsStream(resourcePath)) {
            if (is == null) return "{}";
            return new String(is.readAllBytes());
        }
    }

    public static String getLang(String key) {
        return localization.get(key);
    }

    public static String getLang(String key, Object... args) {
        return localization.get(key, args);
    }
}
