package com.minigames.pool.crazychicken.core;

import com.minigames.MiniGame;
import com.minigames.MiniGameManager;
import com.minigames.MiniGameState;
import com.mondaybuilder.core.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;

import java.util.*;

public class CrazyChickenGame extends MiniGame {
    private final List<UUID> participants = new ArrayList<>();
    private final List<UUID> totalParticipants = new ArrayList<>();
    private final List<Mob> activeMobs = new ArrayList<>();
    private final Map<UUID, ServerBossEvent> bossBars = new HashMap<>();
    private final Map<UUID, Map<EntityType<?>, Integer>> playerKills = new HashMap<>();
    private final Map<UUID, Integer> playerPoints = new HashMap<>();
    private final Map<UUID, Map<EntityType<?>, Integer>> roundKills = new HashMap<>();
    private final Map<UUID, Integer> roundPoints = new HashMap<>();
    private ServerLevel level;

    // Points mapping
    private final Map<EntityType<?>, Integer> mobPointsMap = Map.of(
        EntityType.CHICKEN, 200,
        EntityType.PARROT, 500,
        EntityType.COW, 50,
        EntityType.HORSE, 50,
        EntityType.SHEEP, 100,
        EntityType.PIG, 150,
        EntityType.RABBIT, 250,
        EntityType.BAT, 400
    );

    // Glass pane mapping
    private final Map<EntityType<?>, net.minecraft.world.item.Item> mobGlassMap = new HashMap<>();

    private void initGlassMap() {
        mobGlassMap.put(EntityType.CHICKEN, net.minecraft.world.item.Items.WHITE_STAINED_GLASS_PANE);
        mobGlassMap.put(EntityType.PARROT, net.minecraft.world.item.Items.RED_STAINED_GLASS_PANE);
        mobGlassMap.put(EntityType.COW, net.minecraft.world.item.Items.CYAN_STAINED_GLASS_PANE);
        mobGlassMap.put(EntityType.HORSE, net.minecraft.world.item.Items.LIGHT_BLUE_STAINED_GLASS_PANE);
        mobGlassMap.put(EntityType.SHEEP, net.minecraft.world.item.Items.YELLOW_STAINED_GLASS_PANE);
        mobGlassMap.put(EntityType.PIG, net.minecraft.world.item.Items.PINK_STAINED_GLASS_PANE);
        mobGlassMap.put(EntityType.RABBIT, net.minecraft.world.item.Items.GRAY_STAINED_GLASS_PANE);
        mobGlassMap.put(EntityType.BAT, net.minecraft.world.item.Items.ORANGE_STAINED_GLASS_PANE);
    }
    
    private CrazyChickenState internalState = CrazyChickenState.JOIN;
    private int currentRound = 1;
    private final int maxRounds = 10;
    private int stateTimer = 0;
    private int totalStateTicks = 0;
    
    // Mob spawning config
    private double baseSpeed = 0.6;
    private double speedIncreasePerRound = 0.15;
    private int mobsToSpawn = 5;
    
    private final Random random = new Random();
    private net.minecraft.sounds.SoundEvent currentAmbience;

    // Mob types
    private final EntityType<?>[] mobTypes = {
        EntityType.CHICKEN, EntityType.PARROT, EntityType.COW, EntityType.HORSE,
        EntityType.SHEEP, EntityType.PIG, EntityType.RABBIT, EntityType.BAT
    };

    // Areas from concept
    private final BlockPos marginLeftMin = new BlockPos(99, 1, -104);
    private final BlockPos marginLeftMax = new BlockPos(102, 7, -90);
    private final BlockPos marginRightMin = new BlockPos(99, 1, -54);
    private final BlockPos marginRightMax = new BlockPos(102, 7, -41);
    private final BlockPos movingAreaMin = new BlockPos(99, 0, -89);
    private final BlockPos movingAreaMax = new BlockPos(102, 15, -55);

    public CrazyChickenGame() {
        super("CrazyChicken");
        initGlassMap();
    }

    public enum CrazyChickenState {
        JOIN,
        BEFORE_ROUND,
        ROUND,
        AFTER_ROUND,
        BEFORE_GAME_END,
        GAME_END
    }

    private void setupMobTeam() {
        if (level == null) return;
        var scoreboard = level.getScoreboard();
        var team = scoreboard.getPlayerTeam("cc_mobs");
        if (team == null) {
            team = scoreboard.addPlayerTeam("cc_mobs");
            team.setDisplayName(net.minecraft.network.chat.Component.literal("CC Mobs"));
        }
        team.setCollisionRule(net.minecraft.world.scores.Team.CollisionRule.NEVER);
    }

    private void teleportPlayers() {
        // Position players in front of the moving area
        // Assuming they stand at x=95, looking towards the area
        double x = 95.5;
        double y = 1.0;
        double z = -72.5; // Middle of the moving area (-89 + -55) / 2 = -72
        
        for (UUID uuid : totalParticipants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.teleportTo(level, x, y, z, Collections.emptySet(), 90.0f, 0.0f, true);
                player.setGameMode(GameType.ADVENTURE);
            }
        }
    }

    private void giveEquipment() {
        for (UUID uuid : totalParticipants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.getInventory().clearContent();
                
                // Crossbow with enchantments
                ItemStack crossbow = new ItemStack(net.minecraft.world.item.Items.CROSSBOW);
                var enchantments = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                crossbow.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PIERCING), 10);
                crossbow.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER), 20);
                crossbow.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.QUICK_CHARGE), 8);
                crossbow.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), 30);
                
                player.getInventory().setItem(0, crossbow);
                
                // 9 stacks of arrows (in the first row of inventory, not hotbar)
                for (int i = 9; i < 18; i++) {
                    player.getInventory().setItem(i, new ItemStack(net.minecraft.world.item.Items.ARROW, 64));
                }
            }
        }
    }

    @Override
    protected void onStart() {
        setupMobTeam();
        internalState = CrazyChickenState.JOIN;
        totalStateTicks = 30 * 20;
        stateTimer = totalStateTicks;
        currentRound = 1;
        playerPoints.clear();
        playerKills.clear();
        totalParticipants.clear();
        totalParticipants.addAll(participants);
        teleportPlayers();
        giveEquipment();
    }

    @Override
    protected void onPause() {
    }

    @Override
    protected void onResume() {
    }

    @Override
    protected void onStop() {
        participants.clear();
        totalParticipants.clear();
    }

    @Override
    protected void onTick() {
        if (stateTimer > 0) {
            stateTimer--;
            updateAllBossBars();
            if (stateTimer == 0) {
                transitionState();
            }
        }
        
        if (internalState == CrazyChickenState.ROUND) {
            handleMobSpawning();
            handleMobMovement();
        }
    }

    private void updateAllBossBars() {
        Component name = Component.literal("Crazy Chicken: " + internalState.name() + " - " + (stateTimer / 20) + "s");
        float progress = totalStateTicks > 0 ? (float) stateTimer / totalStateTicks : 0.0f;
        
        for (UUID uuid : totalParticipants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                ServerBossEvent bar = bossBars.computeIfAbsent(uuid, k -> {
                    ServerBossEvent b = new ServerBossEvent(name, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
                    b.addPlayer(player);
                    return b;
                });
                bar.setName(name);
                bar.setProgress(progress);
            }
        }
    }

    private void clearBossBars() {
        bossBars.values().forEach(ServerBossEvent::removeAllPlayers);
        bossBars.clear();
    }

    private void handleMobSpawning() {
        int targetMobCount = (int) (mobsToSpawn * Math.pow(1.25, currentRound - 1));
        
        // Remove oldest if over limit
        while (activeMobs.size() > targetMobCount) {
            Mob oldest = activeMobs.remove(0);
            oldest.discard();
        }

        if (activeMobs.size() < targetMobCount && random.nextFloat() < 0.1) {
            spawnMob();
        }
    }

    private void spawnMob() {
        EntityType<?> type = mobTypes[random.nextInt(mobTypes.length)];
        Mob mob = (Mob) type.create(level, EntitySpawnReason.SPAWNER);
        if (mob == null) return;

        // Choose side (left or right)
        boolean fromLeft = random.nextBoolean();
        double x = (marginLeftMin.getX() + marginLeftMax.getX()) / 2.0;
        double y = marginLeftMin.getY() + random.nextDouble() * (marginLeftMax.getY() - marginLeftMin.getY());
        double z = fromLeft ? 
            marginLeftMin.getZ() + random.nextDouble() * (marginLeftMax.getZ() - marginLeftMin.getZ()) :
            marginRightMin.getZ() + random.nextDouble() * (marginRightMax.getZ() - marginRightMin.getZ());

        mob.setPos(x, y, z);
        mob.setYRot(fromLeft ? 0.0f : 180.0f);
        mob.setXRot(0.0f);
        mob.setNoAi(true);
        
        double roundSpeedIncrease = (0.1 + random.nextDouble() * (0.275 - 0.1)) * (currentRound - 1);
        double speed = (baseSpeed + random.nextDouble() * 0.6) + roundSpeedIncrease;
        double yVelocity = (random.nextDouble() * 4.0 - 2.0) / 20.0;
        
        // Use tags for simple persistence of movement data
        mob.addTag("cc_speed:" + speed);
        mob.addTag("cc_y_vel:" + yVelocity);
        mob.addTag("cc_dir:" + (fromLeft ? 1 : -1));

        level.addFreshEntity(mob);
        var team = level.getScoreboard().getPlayerTeam("cc_mobs");
        if (team != null) {
            level.getScoreboard().addPlayerToTeam(mob.getScoreboardName(), team);
        }
        activeMobs.add(mob);
    }

    private void handleMobMovement() {
        Iterator<Mob> iterator = activeMobs.iterator();
        while (iterator.hasNext()) {
            Mob mob = iterator.next();
            if (!mob.isAlive()) {
                iterator.remove();
                continue;
            }

            double speed = 0;
            double yVel = 0;
            int dir = 0;
            
            for (String tag : mob.getTags()) {
                if (tag.startsWith("cc_speed:")) speed = Double.parseDouble(tag.substring(9));
                else if (tag.startsWith("cc_y_vel:")) yVel = Double.parseDouble(tag.substring(9));
                else if (tag.startsWith("cc_dir:")) dir = Integer.parseInt(tag.substring(7));
            }

            Vec3 pos = mob.position();
            double newZ = pos.z + (speed / 20.0) * dir;
            double newY = pos.y + yVel;
            
            mob.setPos(pos.x, newY, newZ);
            mob.setYRot(dir > 0 ? 0.0f : 180.0f);
            mob.setXRot(0.0f);

            // Randomly look at a player
            if (random.nextFloat() < 0.05) {
                ServerPlayer target = level.getRandomPlayer();
                if (target != null && totalParticipants.contains(target.getUUID())) {
                    mob.lookAt(target, 30.0F, 30.0F);
                }
            }

            // Despawn if reached other margin
            if (dir > 0 && newZ > marginRightMax.getZ()) {
                mob.discard();
                iterator.remove();
            } else if (dir < 0 && newZ < marginLeftMin.getZ()) {
                mob.discard();
                iterator.remove();
            }
        }
    }

    @Override
    public void onMobDeath(Mob mob, net.minecraft.world.damagesource.DamageSource source) {
        if (internalState != CrazyChickenState.ROUND) return;
        if (!activeMobs.contains(mob)) return;

        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player && totalParticipants.contains(player.getUUID())) {
            UUID uuid = player.getUUID();
            EntityType<?> type = mob.getType();

            // Track kill
            playerKills.computeIfAbsent(uuid, k -> new HashMap<>())
                       .merge(type, 1, Integer::sum);
            roundKills.computeIfAbsent(uuid, k -> new HashMap<>())
                       .merge(type, 1, Integer::sum);

            // Calculate points
            int points = mobPointsMap.getOrDefault(type, 0);
            playerPoints.merge(uuid, points, Integer::sum);
            roundPoints.merge(uuid, points, Integer::sum);

            checkAdvancements(player);

            // Give glass pane
            net.minecraft.world.item.Item glassPane = mobGlassMap.get(type);
            if (glassPane != null) {
                player.getInventory().add(new ItemStack(glassPane));
            }

            // Play sound?
            broadcastSound(com.mondaybuilder.registry.ModSounds.GUESS_RIGHT, 0.5f, 1.5f);
        }

        activeMobs.remove(mob);
    }

    private void broadcastSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (level == null) return;
        for (UUID uuid : totalParticipants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.playNotifySound(sound, net.minecraft.sounds.SoundSource.MASTER, volume, pitch);
            }
        }
    }

    private void transitionState() {
        switch (internalState) {
            case JOIN -> {
                internalState = CrazyChickenState.BEFORE_ROUND;
                totalStateTicks = 5 * 20;
                stateTimer = totalStateTicks;
                announceRound();
            }
            case BEFORE_ROUND -> {
                internalState = CrazyChickenState.ROUND;
                totalStateTicks = 60 * 20;
                stateTimer = totalStateTicks;
                startRound();
            }
            case ROUND -> {
                internalState = CrazyChickenState.AFTER_ROUND;
                totalStateTicks = 5 * 20;
                stateTimer = totalStateTicks;
                endRound();
            }
            case AFTER_ROUND -> {
                if (currentRound < maxRounds) {
                    currentRound++;
                    internalState = CrazyChickenState.BEFORE_ROUND;
                    totalStateTicks = 5 * 20;
                    stateTimer = totalStateTicks;
                    announceRound();
                } else {
                    internalState = CrazyChickenState.BEFORE_GAME_END;
                    totalStateTicks = 60 * 20;
                    stateTimer = totalStateTicks;
                    announceGameEnd();
                }
            }
            case BEFORE_GAME_END -> {
                internalState = CrazyChickenState.GAME_END;
                totalStateTicks = 5 * 20;
                stateTimer = totalStateTicks;
                finalizeGame();
            }
            case GAME_END -> {
                MiniGameManager.getInstance().stopActiveGame();
            }
        }
    }

    public boolean canUseItems(ServerPlayer player) {
        if (!totalParticipants.contains(player.getUUID())) return true;
        return internalState == CrazyChickenState.ROUND;
    }

    private void announceRound() {
        broadcastTitle("Round " + currentRound, "Get ready for hunt!");
        broadcastMessage("Next round starts in 5 seconds");
    }

    private void startRound() {
        activeMobs.clear();
        roundKills.clear();
        roundPoints.clear();
        broadcastMessage("Round " + currentRound + " started!");
        
        // Play random ambience
        net.minecraft.sounds.SoundEvent[] ambientSounds = {
            com.mondaybuilder.registry.ModSounds.AMBIENCE_1,
            com.mondaybuilder.registry.ModSounds.AMBIENCE_2,
            com.mondaybuilder.registry.ModSounds.AMBIENCE_3
        };
        currentAmbience = ambientSounds[random.nextInt(ambientSounds.length)];
        broadcastSound(currentAmbience, 0.3f, 1.0f);
    }

    private void checkAdvancements(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Map<EntityType<?>, Integer> rKills = roundKills.getOrDefault(uuid, Collections.emptyMap());
        int rPoints = roundPoints.getOrDefault(uuid, 0);
        int tPoints = playerPoints.getOrDefault(uuid, 0);
        
        if (rKills.getOrDefault(EntityType.CHICKEN, 0) >= 20) grantAdvancement(player, "chicken_master");
        if (rKills.getOrDefault(EntityType.PARROT, 0) >= 10) grantAdvancement(player, "paradiser");
        if (rKills.getOrDefault(EntityType.RABBIT, 0) >= 15) grantAdvancement(player, "carrot_king");
        if (rKills.getOrDefault(EntityType.BAT, 0) >= 10) grantAdvancement(player, "bat_me");
        if (rPoints >= 4000) grantAdvancement(player, "eggcellent");
        if (tPoints >= 10000) grantAdvancement(player, "eggcellent_master");
        if (tPoints >= 20000) grantAdvancement(player, "major_eggcellent");
    }

    private void grantAdvancement(ServerPlayer player, String id) {
        GameManager.getInstance().getScoring().grantAdvancement(
            level.getServer(),
            player,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mondaybuilder", "crazychicken/" + id)
        );
    }

    private void endRound() {
        // Clear remaining mobs
        activeMobs.forEach(Mob::discard);
        activeMobs.clear();

        broadcastTitle("Round " + currentRound + " finished!", "Kills this round: " + getTotalKillsInRound());
    }

    private int getTotalKillsInRound() {
        int total = 0;
        for (Map<EntityType<?>, Integer> kills : playerKills.values()) {
            for (Integer count : kills.values()) {
                total += count;
            }
        }
        return total;
    }

    private void announceGameEnd() {
        broadcastTitle("Game end!", "Let's view the score");
        showScoreboard();
    }

    private void showScoreboard() {
        broadcastMessage("--- Final Scores ---");
        
        // Header
        StringBuilder header = new StringBuilder("Player | ");
        for (EntityType<?> type : mobTypes) {
            header.append(type.getDescription().getString()).append(" | ");
        }
        header.append("TOTAL");
        broadcastMessage(header.toString());

        // Player rows
        playerPoints.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .forEach(entry -> {
                UUID uuid = entry.getKey();
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
                String name = player != null ? player.getName().getString() : uuid.toString();
                
                StringBuilder row = new StringBuilder(name).append(" | ");
                Map<EntityType<?>, Integer> kills = playerKills.getOrDefault(uuid, Collections.emptyMap());
                for (EntityType<?> type : mobTypes) {
                    row.append(kills.getOrDefault(type, 0)).append(" | ");
                }
                row.append(entry.getValue());
                broadcastMessage(row.toString());
            });
    }

    private void finalizeGame() {
        broadcastTitle("Game ended!", "Returning to lobby...");
        clearBossBars();
        
        for (UUID uuid : totalParticipants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.getInventory().clearContent();
                GameManager.getInstance().getArenaManager().teleportToLobby(player);
            }
        }
    }

    public void setParticipants(List<UUID> playerUuids) {
        this.participants.clear();
        this.participants.addAll(playerUuids);
        this.totalParticipants.clear();
        this.totalParticipants.addAll(playerUuids);
    }

    public void setLevel(ServerLevel level) {
        this.level = level;
    }

    private void broadcastMessage(String message) {
        if (level == null) return;
        level.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal(message).withStyle(ChatFormatting.YELLOW),
            false
        );
    }

    private void broadcastTitle(String title, String subtitle) {
        if (level == null) return;
        for (UUID uuid : totalParticipants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.sendSystemMessage(Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                // TODO: Use actual title packets if available in this framework
            }
        }
    }
}
