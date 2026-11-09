package org.Azkerash.riftfall;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/** Connected loaded chunks form disjoint catchments rooted at persistent rift chunks. */
final class RiftEssencePools {
    private static final Map<ServerLevel, Map<Long, Integer>> OWNERS = new HashMap<>();

    static Map<Long, Integer> assign(Set<Long> loaded, Map<Long, Integer> roots) {
        Map<Long, Integer> owned = new HashMap<>();
        Map<Integer, ChunkPos> centers = new HashMap<>();
        roots.forEach((key, id) -> centers.put(id, new ChunkPos(key)));
        var queue = new ArrayDeque<Long>();
        roots.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(root -> {
            if (loaded.contains(root.getKey()) && owned.putIfAbsent(root.getKey(), root.getValue()) == null) queue.add(root.getKey());
        });
        while (!queue.isEmpty()) {
            long key = queue.removeFirst();
            ChunkPos pos = new ChunkPos(key);
            long[] neighbors = {ChunkPos.asLong(pos.x-1,pos.z),ChunkPos.asLong(pos.x+1,pos.z),ChunkPos.asLong(pos.x,pos.z-1),ChunkPos.asLong(pos.x,pos.z+1)};
            for (long next : neighbors) if (loaded.contains(next) && withinRadius(centers.get(owned.get(key)), new ChunkPos(next))
                && owned.putIfAbsent(next, owned.get(key)) == null) queue.addLast(next);
        }
        return owned;
    }

    static boolean withinRadius(ChunkPos root, ChunkPos donor) {
        return RiftPlacement.tooClose(root, donor);
    }

    static double drawBudget(int donors, double perDonor, double totalLimit) {
        return Math.min(Math.max(0, totalLimit), Math.max(0, donors) * Math.max(0, perDonor));
    }

    static boolean canFundCrystal(double points, double criticalMass, double cost, boolean dormantRoot) {
        return CrystalGrowth.canFund(points, criticalMass, cost) && (!dormantRoot || points - cost >= criticalMass);
    }

    static double transfer(double root, double donor, double limit, double target) {
        return Math.max(0, Math.min(Math.min(donor, limit), target - root));
    }

    static void tick(ServerLevel world, RiftData data, Set<Long> tracked) {
        Map<Long, LevelChunk> chunks = new HashMap<>();
        for (long key : tracked) {
            ChunkPos pos = new ChunkPos(key);
            LevelChunk chunk = world.getChunkSource().getChunkNow(pos.x,pos.z);
            if (chunk != null) chunks.put(key, chunk);
        }
        Map<Long,Integer> roots = new HashMap<>();
        for (var r : data.rifts) roots.putIfAbsent(new ChunkPos(r.pos).toLong(), r.id);
        Map<Long,Integer> owners = assign(chunks.keySet(), roots);
        OWNERS.put(world, owners);
        double target = Config.CRITICAL_MASS.get();
        var donors = new ArrayList<>(owners.keySet());
        Collections.shuffle(donors, new java.util.Random(world.random.nextLong()));
        for (var r : data.rifts) {
            double balance = SoulEssenceEvents.availableEssence(world,r.pos);
            if (balance < 0) continue;
            r.refilling = balance < target;
            if (!r.refilling) continue;
            long root = new ChunkPos(r.pos).toLong();
            int donorCount = (int) owners.entrySet().stream().filter(e -> e.getKey() != root && e.getValue() == r.id).count();
            double budget = drawBudget(donorCount, Config.RIFT_POOL_DRAW.get(), Config.RIFT_POOL_TOTAL_DRAW.get());
            for (long donor : donors) {
                if (donor == root || owners.get(donor) != r.id) continue;
                LevelChunk chunk = chunks.get(donor);
                double amount = transfer(balance,chunk.getData(SoulEssenceEvents.ESSENCE).points(),Math.min(budget, Config.RIFT_POOL_DRAW.get()),target);
                if (amount <= 0) continue;
                SoulEssenceEvents.spend(world, chunk.getPos().getWorldPosition(), amount);
                SoulEssenceEvents.add(world,r.pos,amount);
                balance += amount; budget -= amount;
                if (budget <= 0 || balance >= target) break;
            }
            if (balance >= target) r.refilling = false;
        }
    }

    static Integer owner(ServerLevel world, ChunkPos chunk) {
        return OWNERS.getOrDefault(world, Map.of()).get(chunk.toLong());
    }
    static void clear() { OWNERS.clear(); }
    private RiftEssencePools() {}
}
