package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeyserLedgerTest {
    @Test void capacityAndTickGenerationIncludeTheEntireFootprint(){
        var small=new GeyserRegion(0,0,9,405);var large=new GeyserRegion(0,0,17,2890);
        assertEquals(162000,GeyserLedger.capacity(small));assertEquals(40.5,GeyserLedger.regeneration(small));
        assertEquals(578000,GeyserLedger.capacity(large));assertEquals(144.5,GeyserLedger.regeneration(large));
        assertEquals(600,1200*GeyserLedger.REGENERATION_PER_CHUNK_PER_TICK);
    }
    @Test void unloadedLedgerPersistsOneBalanceTimestampAndCollectiveRate(){
        var tag=new CompoundTag();tag.putInt("Version",1);var list=new ListTag();var row=new CompoundTag();row.putLong("Region",ChunkPos.asLong(10,-10));row.putDouble("Mana",123456);row.putLong("Updated",8000);row.putDouble("Capacity",162000);row.putDouble("RegenerationPerTick",40.5);list.add(row);tag.put("Pools",list);
        var restored=GeyserLedger.load(tag,null).save(new CompoundTag(),null).getList("Pools",10).getCompound(0);
        assertEquals(123456,restored.getDouble("Mana"));assertEquals(8000,restored.getLong("Updated"));assertEquals(40.5,restored.getDouble("RegenerationPerTick"));
        row.putDouble("RegenerationPerTick",.5);assertThrows(IllegalArgumentException.class,()->GeyserLedger.load(tag,null));
    }
}
