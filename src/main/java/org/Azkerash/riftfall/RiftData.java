package org.Azkerash.riftfall;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class RiftData extends SavedData {
    final List<Rift> rifts = new ArrayList<>();
    final RiftPlacement.Index placement = new RiftPlacement.Index();
    int nextId;

    static RiftData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new Factory<>(RiftData::new, RiftData::load, null), "riftfall_rifts");
    }

    void addRift(Rift r) {
        rifts.add(r);
        placement.add(new net.minecraft.world.level.ChunkPos(r.pos));
    }

    int activeCount() { return (int) rifts.stream().filter(r -> !r.dormant).count(); }

    static RiftData load(CompoundTag tag, HolderLookup.Provider registries) {
        RiftData data = new RiftData();
        data.nextId = tag.getInt("nextId");
        ListTag list = tag.getList("rifts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Rift r = new Rift(t.getInt("id"), BlockPos.of(t.getLong("pos")), t.getInt("remaining"));
            r.exteriorYaw = t.getFloat("exteriorYaw");
            r.sourceBiome = t.getString("sourceBiome");
            r.theme = t.contains("theme") ? t.getString("theme") : "plains";
            if (t.contains("nativeMobs")) {
                r.nativeMobs.clear();
                for (Tag value : t.getList("nativeMobs", Tag.TAG_STRING)) r.nativeMobs.add(value.getAsString());
            }
            r.rosterVersion=t.getInt("rosterVersion"); r.layoutVersion=t.getInt("layoutVersion");
            CompoundTag roster=t.getCompound("roster");
            for (String id : roster.getAllKeys()) r.roster.put(id,roster.getInt(id));
            if (t.hasUUID("boss")) r.boss = t.getUUID("boss");
            r.built = t.getBoolean("built");
            r.waveTicks = t.getInt("waveTicks");
            r.cleared = t.getBoolean("cleared");
            r.dormant = t.getBoolean("dormant");
            r.dormantTicks = t.getInt("dormantTicks");
            r.essence = t.getDouble("essence");
            r.energy = t.contains("energy") ? t.getDouble("energy") : 600;
            r.refilling = t.getBoolean("refilling");
            r.cycle = t.getInt("cycle");
            r.replenishTicks = Math.max(0, Math.min(RiftReplenishment.INTERVAL, t.getInt("replenishTicks")));
            r.enemyCapacity = t.contains("enemyCapacity") ? Math.max(0, t.getInt("enemyCapacity")) : -1;
            r.encounterLevel = Math.max(1, t.getInt("encounterLevel"));
            r.radius = Math.max(10, Math.min(26, t.getInt("radius")));
            readIds(t.getList("enemies", Tag.TAG_COMPOUND), r.enemies);
            readIds(t.getList("outside", Tag.TAG_COMPOUND), r.outside);
            readIds(t.getList("occupants", Tag.TAG_COMPOUND), r.occupants);
            readIds(t.getList("retired", Tag.TAG_COMPOUND), r.retired);
            data.addRift(r);
        }
        return data;
    }

    private static void readIds(ListTag list, Set<UUID> target) {
        for (int i = 0; i < list.size(); i++) target.add(list.getCompound(i).getUUID("uuid"));
    }

    private static ListTag writeIds(Set<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) { CompoundTag t = new CompoundTag(); t.putUUID("uuid", id); list.add(t); }
        return list;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("nextId", nextId);
        ListTag list = new ListTag();
        for (Rift r : rifts) {
            CompoundTag t = new CompoundTag();
            t.putInt("id", r.id); t.putLong("pos", r.pos.asLong());
            t.putInt("remaining", r.remaining); t.putBoolean("built", r.built);
            t.putInt("waveTicks", r.waveTicks);
            t.putBoolean("cleared", r.cleared); t.putBoolean("dormant", r.dormant);
            t.putInt("dormantTicks", r.dormantTicks); t.putDouble("essence", r.essence); t.putDouble("energy", r.energy); t.putBoolean("refilling", r.refilling);
            t.putInt("replenishTicks", r.replenishTicks); t.putInt("enemyCapacity", r.enemyCapacity);
            t.putInt("cycle", r.cycle); t.putInt("encounterLevel", r.encounterLevel); t.putInt("radius", r.radius);
            t.putFloat("exteriorYaw", r.exteriorYaw);
            t.putString("sourceBiome", r.sourceBiome); t.putString("theme", r.theme);
            ListTag natives = new ListTag();
            for (String id : r.nativeMobs) natives.add(net.minecraft.nbt.StringTag.valueOf(id));
            t.put("nativeMobs", natives);
            t.putInt("rosterVersion",r.rosterVersion); t.putInt("layoutVersion",r.layoutVersion);
            CompoundTag roster=new CompoundTag();
            r.roster.forEach(roster::putInt); t.put("roster",roster);
            if (r.boss != null) t.putUUID("boss", r.boss);
            t.put("enemies", writeIds(r.enemies)); t.put("outside", writeIds(r.outside)); t.put("occupants", writeIds(r.occupants)); t.put("retired", writeIds(r.retired));
            list.add(t);
        }
        tag.put("rifts", list);
        return tag;
    }

    static final class Rift {
        final int id;
        final BlockPos pos;
        int remaining;
        int waveTicks;
        boolean built;
        float exteriorYaw;
        boolean cleared;
        boolean opening;
        boolean refitting;
        int emptyEnemyTicks;
        boolean dormant;
        int dormantTicks;
        double essence;
        double energy;
        boolean refilling;
        int cycle;
        int replenishTicks;
        int enemyCapacity = -1;
        int encounterLevel = 1;
        int entityGraceTicks = 200;
        int radius = 10;
        String sourceBiome = "";
        String theme = "plains";
        int rosterVersion,layoutVersion;
        final java.util.Map<String,Integer> roster = new java.util.LinkedHashMap<>();
        final Set<String> nativeMobs = RiftEcology.defaults();
        UUID boss;
        net.minecraft.server.level.ServerBossEvent bossBar;
        final Set<UUID> enemies = new HashSet<>();
        final Set<UUID> outside = new HashSet<>();
        final Set<UUID> occupants = new HashSet<>();
        final Set<UUID> retired = new HashSet<>();
        final java.util.Map<UUID, net.minecraft.world.entity.Mob> spawned = new java.util.HashMap<>();
        Rift(int id, BlockPos pos, int remaining) { this.id = id; this.pos = pos; this.remaining = remaining; }
        BlockPos arena() { return new BlockPos(id * 64, 80, 0); }
        // Positive Z is south: place the exit near the south wall, looking into the room.
        BlockPos exit() { return arena().offset(0, 1, radius - 2); }
        int level() { return RiftProgression.level(essence, Config.RIFT_UPGRADE_ESSENCE.get(), Config.RIFT_MAX_LEVEL.get()); }
        boolean contains(double x, double z) { return Math.abs(x - (arena().getX() + .5)) <= radius + 1 && Math.abs(z - .5) <= radius + 1; }
        BlockPos entry() { return exit().relative(Direction.NORTH, 3); }
        float portalYaw(boolean exit) { return exit ? Direction.NORTH.toYRot() : exteriorYaw; }
    }
}
