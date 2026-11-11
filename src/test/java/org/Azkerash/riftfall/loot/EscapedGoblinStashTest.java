package org.Azkerash.riftfall.loot;
import java.util.UUID;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EscapedGoblinStashTest {
    @Test void fullEntityRecordsSurviveSerializationWithoutExposingMutableInternals(){
        var id=UUID.randomUUID();var entity=new CompoundTag();entity.putString("id","riftfall:goblin");entity.putFloat("Health",7);
        var inventory=new CompoundTag();inventory.putInt("Diamonds",64);entity.put("RiftfallInventory",inventory);
        var record=new CompoundTag();record.putUUID("UUID",id);record.put("Entity",entity);record.putString("Dimension","minecraft:overworld");
        var list=new ListTag();list.add(record);var root=new CompoundTag();root.put("Entries",list);
        var stash=EscapedGoblinStash.load(root,null);assertEquals(1,stash.size());
        var copy=stash.record(id).orElseThrow();copy.getCompound("Entity").putFloat("Health",0);
        var restored=EscapedGoblinStash.load(stash.save(new CompoundTag(),null),null);
        assertEquals(7,restored.record(id).orElseThrow().getCompound("Entity").getFloat("Health"));
        assertEquals(64,restored.record(id).orElseThrow().getCompound("Entity").getCompound("RiftfallInventory").getInt("Diamonds"));
    }
}
