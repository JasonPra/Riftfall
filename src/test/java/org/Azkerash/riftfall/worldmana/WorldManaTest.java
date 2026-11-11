package org.Azkerash.riftfall.worldmana;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldManaTest {
    @Test void eachReservoirStartsEmptyAndRegeneratesHalfAManaPerLoadedSecond(){
        var mana=new ChunkMana();assertEquals(0,mana.amount());assertEquals(.5,mana.regenerationPerSecond());
        mana.regenerate(20);assertEquals(.5,mana.amount());mana.regenerate(10);assertEquals(.75,mana.amount());
    }
    @Test void capacityAndWithdrawalsAreBoundedEvenForVeryLargeFiniteInputs(){
        var mana=new ChunkMana();assertEquals(2000,mana.add(Double.MAX_VALUE));assertEquals(0,mana.add(1));
        assertEquals(2000,mana.extract(Double.MAX_VALUE));assertEquals(0,mana.extract(1));
        mana.regenerate(Long.MAX_VALUE);assertEquals(2000,mana.amount());assertTrue(Double.isFinite(mana.amount()));
    }
    @Test void invalidRequestsNeverCorruptTheReservoir(){
        var mana=new ChunkMana();mana.add(100);
        for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}){assertThrows(IllegalArgumentException.class,()->mana.add(invalid));assertThrows(IllegalArgumentException.class,()->mana.extract(invalid));}
        assertThrows(IllegalArgumentException.class,()->mana.regenerate(-1));assertEquals(100,mana.amount());
    }
    @Test void codecPreservesFractionalManaAndIgnoresRemovedLegacyBonuses(){
        var mana=new ChunkMana();mana.add(123.456);var json=ChunkMana.CODEC.encodeStart(JsonOps.INSTANCE,mana).getOrThrow();
        assertEquals(mana.snapshot(),ChunkMana.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow().snapshot());
        var legacy=JsonParser.parseString("{\"amount\":123.456,\"mature_crystals\":50,\"crystal_deposit_generated\":true}");
        var restored=ChunkMana.CODEC.parse(JsonOps.INSTANCE,legacy).getOrThrow();assertEquals(123.456,restored.amount());assertEquals(.5,restored.regenerationPerSecond());
    }
    @Test void allOrNothingWithdrawalsNeverPartiallyConsumeAnUnaffordableCost(){
        var mana=new ChunkMana();mana.add(100);assertFalse(mana.tryExtract(150));assertEquals(100,mana.amount());assertTrue(mana.tryExtract(100));assertEquals(0,mana.amount());
        assertThrows(IllegalArgumentException.class,()->mana.tryExtract(Double.NaN));
    }
}
