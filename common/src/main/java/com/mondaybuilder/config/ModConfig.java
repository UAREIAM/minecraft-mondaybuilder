package com.mondaybuilder.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    public static class Location {
        public String world = "minecraft:overworld";
        public double x, y, z;
        public float yaw, pitch;

        public Location() {}
        public Location(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public Location(String world, double x, double y, double z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class Area {
        public String world;
        public double x1, y1, z1, x2, y2, z2;

        public Area() {}
        public Area(String world, double x1, double y1, double z1, double x2, double y2, double z2) {
            this.world = world;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
        }

        public boolean contains(String world, double x, double y, double z) {
            if (!this.world.equals(world)) return false;
            return x >= Math.min(x1, x2) && x <= Math.max(x1, x2) &&
                   y >= Math.min(y1, y2) && y <= Math.max(y1, y2) &&
                   z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
        }
    }

    public static class MapConfig {
        public Location lobby = new Location("draft:end", 0, 43, 0);
        public Location arenaCenter = new Location("draft:end", 0, 50, 0);
        public Location arenaLobby = new Location("draft:end", 12, 50, 12);
        public int arenaSize = 50;
        public int buildAreaSize = 20;

        public Area joiningArea = new Area("minecraft:overworld", 39, 0, 39, 49, 0, 49);
        public Area lobbyArea = new Area("draft:end", -9, 43, -9, 9, 47, 9);
        public Area arenaArea = new Area("draft:end", -19, 50, -19, 19, 61, 19);
        public Area stageArea = new Area("draft:end", -7, 50, -7, 7, 61, 7);
        public Location stage = new Location("draft:end", 0, 50, 0);
    }

    public static class General {
        public int roundsToPlay = 10;
        public int maxPlayers = 15;
        public boolean miniGamesEnabled = true;
        public String resourcePackUrl = "";
        public String resourcePackHash = "";
        public String resourcePackPrompt = "Please download the Monday Builder resource pack to hear the game sounds!";
        public boolean resourcePackRequired = false;
    }

    public static class Items {
        public List<String> pool = new ArrayList<>();
    }

    public static class Words {
        public Map<String, List<String>> categories = new HashMap<>();
    }

    public static class Localization {
        public Map<String, String> strings = new HashMap<>();

        public Localization() {
            // Default strings
            strings.put("game.starting", "The game is starting!");
            strings.put("player.joined", "%s joined the game!");
            strings.put("welcome.master", "Welcome %s! You are the §6Game Master§a. Have fun!");
            strings.put("welcome.player", "Welcome %s! Have fun and enjoy the game.");
            strings.put("game.over", "Game Over!");
            strings.put("game.winner", "The winner is: %s!");
            strings.put("game.over.title", "GAME OVER");
            strings.put("game.over.subtitle", "The winner is: %s");
            strings.put("player.guessed", "%s guessed correctly!");
            strings.put("nobody.guessed", "Nobody guessed it! Word was: %s");
            strings.put("round.starting", "Starting Round %s");
            strings.put("get.ready.title", "GET READY TO BUILD!");
            strings.put("get.ready.subtitle", "Your word is: %s (%s)");
            strings.put("is.building.title", "%s is the builder!");
            strings.put("is.building.subtitle", "Category: %s");
            strings.put("actionbar.time", "[%s/%s] Time: %s");
            strings.put("actionbar.builder", "[%s/%s] Word: %s (%s) | Time: %s");
            strings.put("actionbar.guesser", "[%s/%s] Guess the word! (Category: %s) | Time: %s");
            
            strings.put("command.skip", "Skipped to next round.");
            strings.put("command.setword", "Word set to: %s");
            strings.put("command.setbuilder", "Builder set to: %s");
            strings.put("command.only.gm", "Only the Game Master can start the game!");
            strings.put("command.already.running", "Game is already in progress!");
            strings.put("command.starting", "Game starting with %s rounds!");
            
            strings.put("game.builder.left.restart", "The builder left the game. Restarting round %s...");
            strings.put("game.builder.left.next", "The builder left the game. Starting next round...");
            strings.put("ui.scoreboard.title", "§6Score Board");
            
            strings.put("command.info.title", "=== Game Info ===");
            strings.put("command.info.state", "State: %s");
            strings.put("command.info.word", "Word: %s");
            strings.put("command.info.round", "Round: %s/%s");
            strings.put("command.info.gm", "Game Master: %s");
            strings.put("command.info.players", "--- Players ---");
            strings.put("command.info.player.format", "- %s: %s%s");

            strings.put("ui.choose.skill", "Choose your skill");
            strings.put("ui.category.easy", "Easy");
            strings.put("ui.category.intermediate", "Intermediate");
            strings.put("ui.category.strong", "Strong");
        }

        public String get(String key) {
            return strings.getOrDefault(key, key);
        }

        public String get(String key, Object... args) {
            return String.format(get(key), args);
        }
    }

    public static class MainConfig {
        public General general = new General();
        public Items items = new Items();
        public MapConfig map = new MapConfig();
    }
}
