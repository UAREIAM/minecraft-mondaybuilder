package com.mondaybuilder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mondaybuilder.core.session.WordCategory;
import dev.architectury.platform.Platform;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Platform.getConfigFolder().resolve("mondaybuilder");
    private static final Path SERVER_ROOT = Platform.getGameFolder();

    public static ModConfig.General general = new ModConfig.General();
    public static ModConfig.MapConfig map = new ModConfig.MapConfig();
    public static ModConfig.Items items = new ModConfig.Items();
    public static ModConfig.Words words = new ModConfig.Words();

    public static void loadAll() {
        general = load("config.json", ModConfig.General.class, new ModConfig.General());
        map = load("map.json", ModConfig.MapConfig.class, new ModConfig.MapConfig());
        items = load("items.json", ModConfig.Items.class, new ModConfig.Items());
        
        loadWords();
    }

    private static void loadWords() {
        words = new ModConfig.Words();
        
        // Default words
        Map<String, List<String>> defaults = new HashMap<>();
        defaults.put("EASY", new ArrayList<>(Arrays.asList("Flower", "Tree", "Arrow", "Window", "Fence", "Glasses", "Heart", "Moon", "Table", "Ladder", "Stair", "LOL", "Flash", "Square")));
        defaults.put("INTERMEDIATE", new ArrayList<>(Arrays.asList("Bird", "Door", "House", "Sword", "Creeper", "Helmet", "Hat", "Bucket", "Telephone", "Cookie", "Star", "Pizza", "Fire", "Hammer", "Axe", "Pickaxe", "Fish", "Bed", "Chair", "Key", "Candle", "Cloud", "Shoe", "Leaf", "67")));
        defaults.put("STRONG", new ArrayList<>(Arrays.asList("Snowman", "Butterfly", "Bottle", "Car", "Book", "Bee", "Rocket", "Smiley", "Chicken", "Umbrella", "Dice", "Saturn", "Apple", "Frog", "Cat", "Pig")));

        File yamlFile = SERVER_ROOT.resolve("words.yaml").toFile();
        if (!yamlFile.exists()) {
            yamlFile = CONFIG_DIR.resolve("words.yaml").toFile();
        }

        if (yamlFile.exists()) {
            try (InputStream input = new FileInputStream(yamlFile)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(input);
                if (data != null) {
                    for (WordCategory cat : WordCategory.values()) {
                        Object list = data.get(cat.name());
                        if (list instanceof List) {
                            List<String> wordList = new ArrayList<>();
                            for (Object o : (List<?>) list) wordList.add(o.toString());
                            words.categories.put(cat.name(), wordList);
                        } else {
                            words.categories.put(cat.name(), defaults.get(cat.name()));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Fill missing categories with defaults
        for (WordCategory cat : WordCategory.values()) {
            if (!words.categories.containsKey(cat.name()) || words.categories.get(cat.name()).isEmpty()) {
                words.categories.put(cat.name(), defaults.get(cat.name()));
            }
        }

        // If YAML didn't exist, save it to config dir for reference
        File configYaml = CONFIG_DIR.resolve("words.yaml").toFile();
        if (!configYaml.exists() && !SERVER_ROOT.resolve("words.yaml").toFile().exists()) {
            saveYaml(configYaml, words.categories);
        }
    }

    private static void saveYaml(File file, Object data) {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            Yaml yaml = new Yaml();
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static <T> T load(String filename, Class<T> clazz, T defaultValue) {
        File file = CONFIG_DIR.resolve(filename).toFile();
        if (!file.exists()) {
            save(filename, defaultValue);
            return defaultValue;
        }

        try (FileReader reader = new FileReader(file)) {
            T loaded = GSON.fromJson(reader, clazz);
            return loaded != null ? loaded : defaultValue;
        } catch (IOException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    private static void save(String filename, Object data) {
        File file = CONFIG_DIR.resolve(filename).toFile();
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
