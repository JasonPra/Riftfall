package org.Azkerash.riftfall.crystals;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import org.Azkerash.riftfall.worldmana.geysers.GeyserRegion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RefinementAssistanceTest {
    private static RefinementBatch.Crystal crystal(int x,int z,CrystalTier tier){return new RefinementBatch.Crystal(new BlockPos(x,-40,z).asLong(),tier,tier,0);}
    private static RefinementBatch.Chunk chunk(long id,RefinementBatch.Crystal... crystals){return new RefinementBatch.Chunk(id,1,List.of(crystals));}
    @Test void tableGivesStrongLowTierHelpAndVeryLittleHelpNearWhite(){
        var table=RefinementBoostTable.defaults();
        assertEquals(3,table.speed(CrystalTier.RED,CrystalTier.WHITE));assertEquals(2.5,table.speed(CrystalTier.ORANGE,CrystalTier.WHITE));
        assertEquals(1.05,table.speed(CrystalTier.VIOLET,CrystalTier.WHITE));
        for(var tier:CrystalTier.values()){assertEquals(1,table.speed(tier,tier));assertEquals(1,table.speed(tier,CrystalTier.RED));}
        assertThrows(IllegalArgumentException.class,()->new RefinementBoostTable(List.of(5.0)));
    }
    @Test void onlyHighestFaceNeighborCountsAndSixWhitesDoNotStack(){
        var red=crystal(0,0,CrystalTier.RED);var list=new ArrayList<RefinementBatch.Crystal>();list.add(red);
        var origin=BlockPos.of(red.position());
        for(var face:Direction.values())list.add(new RefinementBatch.Crystal(origin.relative(face).asLong(),CrystalTier.WHITE,CrystalTier.WHITE,0));
        assertEquals(3,RefinementNeighbors.speeds(list,RefinementBoostTable.defaults()).get(red.position()));
        assertEquals(1,RefinementNeighbors.speeds(List.of(red,crystal(1,1,CrystalTier.WHITE)),RefinementBoostTable.defaults()).get(red.position()));
    }
    @Test void assistanceCrossesChunkBoundariesAndPreservesUpgradeCost(){
        var input=List.of(chunk(0,crystal(15,0,CrystalTier.RED)),chunk(1,crystal(16,0,CrystalTier.WHITE)));
        var settings=new RefinementBatch.Settings(100,600,1);
        assertEquals(0,RefinementBatch.advance(input,33,2000,4000,0,settings,Set.of()).upgrades());
        var result=RefinementBatch.advance(input,34,2000,4000,0,settings,Set.of());
        assertEquals(1,result.upgrades());assertEquals(1400,result.mana());assertEquals(CrystalTier.ORANGE,result.chunks().getFirst().crystals().getFirst().tier());
        var dry=RefinementBatch.advance(input,1000,599,4000,0,settings,Set.of());assertEquals(0,dry.upgrades());assertEquals(0,dry.chunks().getFirst().crystals().getFirst().progress());
    }
    @Test void virtualUpgradesRecomputeAssistanceAndLongCatchupMatchesShortSteps(){
        var random=new Random(749);
        for(int trial=0;trial<20;trial++){
            var crystals=new ArrayList<RefinementBatch.Crystal>();
            for(int x=0;x<3;x++)for(int z=0;z<3;z++)crystals.add(crystal(x,z,CrystalTier.values()[random.nextInt(8)]));
            var input=List.of(new RefinementBatch.Chunk(0,1,crystals));var settings=new RefinementBatch.Settings(100,600,1.25);
            var full=RefinementBatch.advance(input,1000,1500,162000,25,settings,Set.of());
            var current=input;double mana=1500;Set<Long> served=Set.of();int upgrades=0;
            for(int step=0;step<100;step++){var result=RefinementBatch.advance(current,10,mana,162000,25,settings,served);current=result.chunks();mana=result.mana();served=result.served();upgrades+=result.upgrades();}
            assertEquals(full.upgrades(),upgrades);assertEquals(full.mana(),mana,1e-6);assertEquals(full.served(),served);
            for(int i=0;i<crystals.size();i++){var expected=full.chunks().getFirst().crystals().get(i);var actual=current.getFirst().crystals().get(i);assertEquals(expected.tier(),actual.tier());assertEquals(expected.progress(),actual.progress(),1e-6);}
        }
    }
    @Test void savedContactsAndLazyProgressDoNotGrantRetroactiveBonuses(){
        var store=new RefinementStore();var entry=store.entry(0);entry.region=0;entry.exposure=1;var red=crystal(0,0,CrystalTier.RED);
        int contacts=RefinementNeighbors.with(0,Direction.EAST.ordinal(),CrystalTier.WHITE);
        entry.crystals=List.of(new RefinementBatch.Crystal(red.position(),red.tier(),red.tier(),0,contacts));
        var region=new GeyserRegion(0,0,9,405);
        store.advance(region,100,2000,162000,40.5);store.prepare(0);
        assertEquals(300,entry.crystals.getFirst().progress());
        var loaded=RefinementStore.load(store.save(new CompoundTag(),null),null);assertEquals(contacts,loaded.entries.get(0L).crystals.getFirst().contacts());
        entry.crystals=List.of(new RefinementBatch.Crystal(red.position(),red.tier(),red.tier(),300,0));
        store.advance(region,100,2000,162000,40.5);store.prepare(0);assertEquals(400,entry.crystals.getFirst().progress());
    }
    @Test void existingVersionTwoLedgersWithoutContactsRemainReadable(){
        var store=new RefinementStore();var entry=store.entry(0);entry.region=0;entry.exposure=1;entry.crystals=List.of(crystal(0,0,CrystalTier.RED));
        var tag=store.save(new CompoundTag(),null);var saved=tag.getList("Chunks",10).getCompound(0).getList("Crystals",10).getCompound(0);saved.remove("Contacts");
        assertEquals(0,RefinementStore.load(tag,null).entries.get(0L).crystals.getFirst().contacts());
    }
    @Test void higherTierHeightGrowthSlowsButWaterAndSpreadingRemainUnchanged(){
        for(var tier:CrystalTier.values()){
            double speed=CrystalRules.heightChance(false,tier);assertEquals(1.2,CrystalRules.heightChance(true,tier)/speed,1e-12);
            if(tier!=CrystalTier.RED)assertTrue(speed<CrystalRules.heightChance(false,CrystalTier.values()[tier.ordinal()-1]));
        }
        assertEquals(.1,CrystalRules.spreadChance(false));assertEquals(.12,CrystalRules.spreadChance(true));
        assertEquals(Math.pow(1.1,7),CrystalRules.heightChance(false,CrystalTier.RED)/CrystalRules.heightChance(false,CrystalTier.WHITE),1e-12);
    }
}
