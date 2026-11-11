package org.Azkerash.riftfall.loot;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.Azkerash.riftfall.GoblinEntity;

/** World-owned dormant entity records. This stores escaped thieves; it never schedules or spawns encounters. */
public final class EscapedGoblinStash extends SavedData {
    private static final Factory<EscapedGoblinStash> FACTORY=new Factory<>(EscapedGoblinStash::new,EscapedGoblinStash::load,null);
    private final Map<UUID,CompoundTag> entries=new LinkedHashMap<>();
    public static EscapedGoblinStash get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(FACTORY,"riftfall_escaped_goblins");}
    public boolean store(GoblinEntity mob){
        if(!(mob.level() instanceof ServerLevel level)||!mob.isAlive()||!mob.loot().hasLoot())return false;
        var entity=new CompoundTag();if(!mob.save(entity))return false;
        var record=new CompoundTag();record.putUUID("UUID",mob.getUUID());record.putString("Dimension",level.dimension().location().toString());
        record.putLong("EscapedAt",level.getGameTime());record.putInt("DataVersion",net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        record.put("Entity",entity);entries.put(mob.getUUID(),record);setDirty();return true;
    }
    public int size(){return entries.size();}
    public Set<UUID> ids(){return Set.copyOf(entries.keySet());}
    public Optional<CompoundTag> record(UUID uuid){return Optional.ofNullable(entries.get(uuid)).map(CompoundTag::copy);}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){var list=new ListTag();entries.values().forEach(e->list.add(e.copy()));tag.put("Entries",list);return tag;}
    public static EscapedGoblinStash load(CompoundTag tag,HolderLookup.Provider provider){
        var stash=new EscapedGoblinStash();var list=tag.getList("Entries",10);
        for(int i=0;i<list.size();i++){var entry=list.getCompound(i);if(entry.hasUUID("UUID")&&entry.contains("Entity",10))stash.entries.put(entry.getUUID("UUID"),entry.copy());}
        return stash;
    }
}
