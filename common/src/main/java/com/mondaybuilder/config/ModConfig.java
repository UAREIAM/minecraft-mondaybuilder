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
        public Area arenaArea = new Area("draft:end", -19, 50, -19, 19, 60, 19);
        public Area stageArea = new Area("draft:end", -7, 50, -7, 7, 60, 7);
        public Location stage = new Location("draft:end", 0, 50, 0);

        public Location actionBlock = new Location("minecraft:overworld", 44, 1, 44);
    }

    public static class General {
        public int roundsToPlay = 10;
        public int maxPlayers = 15;
        public boolean miniGamesEnabled = true;
        public boolean autoOpInTest = true;
    }

    public static class BlockEntry {
        public String id;
        public int amount;

        public BlockEntry(String id, int amount) {
            this.id = id;
            this.amount = amount;
        }
    }

    public static class Blocks {
        public List<BlockEntry> pool = new ArrayList<>();
    }

    public static class Items {
        public List<String> pool = new ArrayList<>();
    }

    public static class Words {
        public Map<String, List<String>> categories = new HashMap<>();
    }
}
