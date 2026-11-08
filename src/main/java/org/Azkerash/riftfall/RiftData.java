package org.Azkerash.riftfall;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class RiftData extends SavedData {
    final List<Rift> rifts = new ArrayList<>();
    int nextId;
    int spawnTicks;

    static RiftData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new Factory<>(RiftData::new, RiftData::load, null), "riftfall_rifts");
    }

    static RiftData load(CompoundTag tag, HolderLookup.Provider registries) {
        RiftData data = new RiftData();
        data.nextId = tag.getInt("nextId");
        data.spawnTicks = tag.getInt("spawnTicks");
        ListTag list = tag.getList("rifts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Rift r = new Rift(t.getInt("id"), BlockPos.of(t.getLong("pos")), t.getInt("remaining"));
            r.built = t.getBoolean("built");
            r.waveTicks = t.getInt("waveTicks");
            readIds(t.getList("enemies", Tag.TAG_COMPOUND), r.enemies);
            readIds(t.getList("outside", Tag.TAG_COMPOUND), r.outside);
            data.rifts.add(r);
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
        tag.putInt("spawnTicks", spawnTicks);
        ListTag list = new ListTag();
        for (Rift r : rifts) {
            CompoundTag t = new CompoundTag();
            t.putInt("id", r.id); t.putLong("pos", r.pos.asLong());
            t.putInt("remaining", r.remaining); t.putBoolean("built", r.built);
            t.putInt("waveTicks", r.waveTicks);
            t.put("enemies", writeIds(r.enemies)); t.put("outside", writeIds(r.outside));
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
        final Set<UUID> enemies = new HashSet<>();
        final Set<UUID> outside = new HashSet<>();
        Rift(int id, BlockPos pos, int remaining) { this.id = id; this.pos = pos; this.remaining = remaining; }
        BlockPos arena() { return new BlockPos(id * 64, 80, 0); }
        BlockPos exit() { return arena().offset(-8, 1, 0); }
    }
}
