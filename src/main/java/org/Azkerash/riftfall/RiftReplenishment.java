package org.Azkerash.riftfall;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.Azkerash.riftfall.RiftData.Rift;

/** Replaces losses in an abandoned encounter without rebuilding it or its rewards. */
final class RiftReplenishment {
    static final int INTERVAL = 5 * 60 * 20;

    static int advance(int ticks, boolean eligible, boolean occupied) {
        return !eligible || occupied ? 0 : Math.min(INTERVAL, ticks + 1);
    }

    static void tick(ServerLevel world, Rift r) {
        if (r.enemyCapacity < 0 && r.built)
            r.enemyCapacity = Math.max(normalCount(r), RiftProgression.enemies(Config.ENEMIES.get(), r.encounterLevel));
        boolean occupied = !r.occupants.isEmpty() || world.players().stream()
            .anyMatch(p -> p.isAlive() && !p.isSpectator() && r.contains(p.getX(), p.getZ()));
        boolean eligible = r.built && !r.cleared && !r.dormant && !r.enemies.isEmpty()
            && normalCount(r) < r.enemyCapacity;
        r.replenishTicks = advance(r.replenishTicks, eligible, occupied);
        if (r.replenishTicks < INTERVAL || world.getGameTime() % 20 != 0 || r.energy < 10) return;
        for (int attempt = 0; attempt < 96; attempt++) {
            BlockPos pos = r.arena().offset(world.random.nextInt(r.radius * 2 - 5) - r.radius + 3,
                1, world.random.nextInt(r.radius * 2 - 5) - r.radius + 3);
            if (!RiftEvents.safe(world, pos) || (r.boss != null && RiftBosses.inRoom(r, pos.getX(), pos.getZ()))) continue;
            var spawned = RiftMobs.spawn(world, pos, r.encounterLevel, r.energy, r);
            if (spawned == null) continue;
            r.energy -= spawned.cost();
            RiftMobs.team(world, spawned.mob(), r.id);
            spawned.mob().addTag("riftfall_rift_" + r.id);
            r.enemies.add(spawned.mob().getUUID());
            r.spawned.put(spawned.mob().getUUID(), spawned.mob());
            r.replenishTicks = 0;
            return;
        }
    }

    private static int normalCount(Rift r) {
        return r.enemies.size() - (r.boss != null && r.enemies.contains(r.boss) ? 1 : 0);
    }
    private RiftReplenishment() {}
}
