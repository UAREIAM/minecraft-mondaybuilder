package com.minigames.pool.crazychicken;

import com.minigames.pool.crazychicken.core.CrazyChickenGame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    // Mob types
    private final EntityType<?>[] mobTypes = {
        EntityType.CHICKEN, EntityType.PARROT, EntityType.COW, EntityType.HORSE,
        EntityType.SHEEP, EntityType.PIG, EntityType.RABBIT, EntityType.BAT
    };

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

        double spawnChance = activeMobs.size() < targetMobCount / 2 ? 0.6 : 0.2;
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
        double y = 6.0 + (random.nextDouble() * 6.0 - 2.0);
        double z = fromLeft ?
            marginLeftMin.getZ() + random.nextDouble() * (marginLeftMax.getZ() - marginLeftMin.getZ()) :
            marginRightMin.getZ() + random.nextDouble() * (marginRightMax.getZ() - marginRightMin.getZ());

        mob.setPos(x, y, z);
        mob.setYRot(fromLeft ? 0.0f : 180.0f);
        mob.setXRot(0.0f);
        mob.setNoAi(true);

        double baseSpeed = 1.75;
        double roundSpeedIncrease = (0.1 + random.nextDouble() * (0.275 - 0.1)) * (currentRound - 1);
        double speed = (baseSpeed + random.nextDouble() * 0.5) + roundSpeedIncrease;
        double yVelocity = 0.0;

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

    private void handleMobMovement(List<UUID> totalParticipants) {
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

            // 40% chance to look at player as per refined todo
            if (random.nextFloat() < 0.40) {
                ServerPlayer target = level.getRandomPlayer();
                if (target != null && totalParticipants.contains(target.getUUID())) {
                    mob.lookAt(target, 30.0F, 30.0F);
                }
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

    public void clearMobs() {
        activeMobs.forEach(Mob::discard);
        activeMobs.clear();
    }
}
