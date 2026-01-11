package com.minigames.pool.crazychicken;

import com.minigames.pool.crazychicken.core.CrazyChickenGame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class RoundManager {
    private final CrazyChickenGame game;
    private final List<Mob> activeMobs = new ArrayList<>();
    private final Random random = new Random();
    private ServerLevel level;

    // Areas
    private final BlockPos marginLeftMin = new BlockPos(99, 1, -104);
    private final BlockPos marginLeftMax = new BlockPos(102, 7, -90);
    private final BlockPos marginRightMin = new BlockPos(99, 1, -54);
    private final BlockPos marginRightMax = new BlockPos(102, 7, -41);
    private final BlockPos movingAreaMin = new BlockPos(99, 0, -89);
    private final BlockPos movingAreaMax = new BlockPos(102, 15, -55);

    // Mob types and sounds
    private final EntityType<?>[] mobTypes = {
        EntityType.CHICKEN, EntityType.PARROT, EntityType.COW, EntityType.HORSE,
        EntityType.SHEEP, EntityType.PIG, EntityType.RABBIT, EntityType.BAT
    };

    private final Map<EntityType<?>, SoundEvent> mobAmbientSounds = Map.of(
        EntityType.CHICKEN, SoundEvents.CHICKEN_AMBIENT,
        EntityType.PARROT, SoundEvents.PARROT_AMBIENT,
        EntityType.COW, SoundEvents.COW_AMBIENT,
        EntityType.HORSE, SoundEvents.HORSE_AMBIENT,
        EntityType.SHEEP, SoundEvents.SHEEP_AMBIENT,
        EntityType.PIG, SoundEvents.PIG_AMBIENT,
        EntityType.RABBIT, SoundEvents.RABBIT_AMBIENT,
        EntityType.BAT, SoundEvents.BAT_AMBIENT
    );

    public RoundManager(CrazyChickenGame game) {
        this.game = game;
    }

    public void setLevel(ServerLevel level) {
        this.level = level;
    }

    public List<Mob> getActiveMobs() {
        return activeMobs;
    }

    public void tick(int currentRound, List<UUID> totalParticipants) {
        handleMobSpawning(currentRound, totalParticipants);
        handleMobMovement(totalParticipants);
    }

    private int getTargetMobCount(int participantsCount) {
        int playerCount = Math.max(1, participantsCount);
        if (playerCount <= 1) return 30;
        if (playerCount == 2) return 60;
        if (playerCount == 3) return 90;
        if (playerCount == 4) return 120;
        if (playerCount == 5) return 150;
        return 200;
    }

    private void handleMobSpawning(int currentRound, List<UUID> totalParticipants) {
        int targetMobCount = getTargetMobCount(totalParticipants.size());

        while (activeMobs.size() > targetMobCount) {
            Mob oldest = activeMobs.remove(0);
            oldest.discard();
        }

        double fillRatio = (double) activeMobs.size() / targetMobCount;
        double spawnChance = (double) targetMobCount / 500.0;

        if (fillRatio < 0.3) {
            spawnChance *= 2.0;
        } else if (fillRatio > 0.8) {
            spawnChance *= 0.5;
        }

        if (activeMobs.size() < targetMobCount && random.nextFloat() < spawnChance) {
            spawnMob(currentRound);
        }
    }

    private void spawnMob(int currentRound) {
        if (level == null) return;
        EntityType<?> type = mobTypes[random.nextInt(mobTypes.length)];
        Mob mob = (Mob) type.create(level, EntitySpawnReason.SPAWNER);
        if (mob == null) return;

        boolean fromLeft = random.nextBoolean();
        double x = (marginLeftMin.getX() + marginLeftMax.getX()) / 2.0;
        double y = 2.0 + random.nextDouble() * 4.0;
        double z = fromLeft ?
            marginLeftMin.getZ() + random.nextDouble() * (marginLeftMax.getZ() - marginLeftMin.getZ()) :
            marginRightMin.getZ() + random.nextDouble() * (marginRightMax.getZ() - marginRightMin.getZ());

        mob.setPos(x, y, z);
        float initialYRot = fromLeft ? 0.0f : 180.0f;
        mob.setYRot(initialYRot);
        mob.setYBodyRot(initialYRot);
        mob.setYHeadRot(initialYRot);
        mob.setXRot(0.0f);
        mob.setNoAi(true);

        double baseSpeed = 1.75;
        double roundSpeedIncrease = (0.1 + random.nextDouble() * (0.275 - 0.1)) * (currentRound - 1);
        double speed = (baseSpeed + random.nextDouble() * 0.5) + roundSpeedIncrease;

        mob.addTag("cc_speed:" + speed);
        mob.addTag("cc_dir:" + (fromLeft ? 1 : -1));
        
        if (random.nextFloat() < 0.40) {
            mob.addTag("cc_look_at:1");
        }

        if (random.nextFloat() < 0.30) {
            mob.addTag("cc_y_mover:1");
            mob.addTag("cc_target_y:" + (2.0 + random.nextDouble() * 4.0));
        } else {
            mob.addTag("cc_target_y:" + y);
        }

        level.addFreshEntity(mob);
        var team = level.getScoreboard().getPlayerTeam("cc_mobs");
        if (team != null) {
            level.getScoreboard().addPlayerToTeam(mob.getScoreboardName(), team);
        }
        activeMobs.add(mob);
    }

    private void handleMobMovement(List<UUID> totalParticipants) {
        Iterator<Mob> iterator = activeMobs.iterator();
        while (iterator.hasNext()) {
            Mob mob = iterator.next();
            if (!mob.isAlive()) {
                iterator.remove();
                continue;
            }

            double speed = 0;
            int dir = 0;
            boolean lookAtPlayer = false;
            boolean yMover = false;
            double targetY = mob.getY();

            for (String tag : mob.getTags()) {
                if (tag.startsWith("cc_speed:")) speed = Double.parseDouble(tag.substring(9));
                else if (tag.startsWith("cc_dir:")) dir = Integer.parseInt(tag.substring(7));
                else if (tag.equals("cc_look_at:1")) lookAtPlayer = true;
                else if (tag.equals("cc_y_mover:1")) yMover = true;
                else if (tag.startsWith("cc_target_y:")) targetY = Double.parseDouble(tag.substring(12));
            }

            Vec3 pos = mob.position();
            double newZ = pos.z + (speed / 20.0) * dir;
            double newY = pos.y;

            if (yMover) {
                if (Math.abs(newY - targetY) < 0.1) {
                    if (random.nextFloat() < 0.02) { 
                        targetY = 2.0 + random.nextDouble() * 4.0;
                        updateTag(mob, "cc_target_y:", String.valueOf(targetY));
                    }
                }
                
                double ySpeed = 0.05;
                if (newY < targetY) newY += ySpeed;
                else if (newY > targetY) newY -= ySpeed;
            }

            mob.setPos(pos.x, newY, newZ);

            // Play ambient sound randomly
            if (random.nextFloat() < 0.001f) {
                SoundEvent sound = mobAmbientSounds.get(mob.getType());
                if (sound != null) {
                    level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), sound, SoundSource.NEUTRAL, 2.0f, 0.8f + random.nextFloat() * 0.4f);
                }
            }
            
            if (lookAtPlayer && random.nextFloat() < 0.1) {
                ServerPlayer target = level.getRandomPlayer();
                if (target != null && totalParticipants.contains(target.getUUID())) {
                    mob.lookAt(target, 30.0F, 30.0F);
                }
            } else {
                mob.setYRot(dir > 0 ? 0.0f : 180.0f);
                mob.setXRot(0.0f);
            }

            if (dir > 0 && newZ > marginRightMax.getZ()) {
                mob.discard();
                iterator.remove();
            } else if (dir < 0 && newZ < marginLeftMin.getZ()) {
                mob.discard();
                iterator.remove();
            }
        }
    }

    private void updateTag(Mob mob, String prefix, String newValue) {
        mob.getTags().removeIf(tag -> tag.startsWith(prefix));
        mob.addTag(prefix + newValue);
    }

    public void clearMobs() {
        activeMobs.forEach(Mob::discard);
        activeMobs.clear();

        if (level != null) {
            clearArea(marginLeftMin, marginLeftMax);
            clearArea(marginRightMin, marginRightMax);
            clearArea(movingAreaMin, movingAreaMax);
        }
    }

    private void clearArea(BlockPos min, BlockPos max) {
        net.minecraft.world.phys.AABB aabb = new net.minecraft.world.phys.AABB(
                min.getX(), min.getY(), min.getZ(),
                max.getX() + 1, max.getY() + 1, max.getZ() + 1
        );
        level.getEntitiesOfClass(Mob.class, aabb, mob -> {
            for (EntityType<?> type : mobTypes) {
                if (mob.getType() == type) return true;
            }
            return mob.getTags().stream().anyMatch(tag -> tag.startsWith("cc_"));
        }).forEach(Mob::discard);
    }
}
