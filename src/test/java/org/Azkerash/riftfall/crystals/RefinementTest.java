package org.Azkerash.riftfall.crystals;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RefinementTest {
    private final RefinementBatch.Settings quick=new RefinementBatch.Settings(20,600,1);
    private RefinementBatch.Crystal red(long position,double progress){return new RefinementBatch.Crystal(position,CrystalTier.RED,CrystalTier.RED,progress);}
    private RefinementBatch.Chunk chunk(long id,int count,double progress){var list=new ArrayList<RefinementBatch.Crystal>();for(int i=0;i<count;i++)list.add(red(id*100+i,progress));return new RefinementBatch.Chunk(id,1,list);}
    @Test void sharedBudgetSpendsOneUpgradePerChunkPerRoundCenterFirst(){
        var result=RefinementBatch.advance(List.of(chunk(1,3,20),chunk(2,3,20),chunk(3,3,20)),0,2400,6000,0,quick,Set.of());
        assertEquals(4,result.upgrades());assertEquals(0,result.mana());
        assertEquals(2,result.chunks().get(0).crystals().stream().filter(c->c.tier()==CrystalTier.ORANGE).count());
        assertEquals(1,result.chunks().get(1).crystals().stream().filter(c->c.tier()==CrystalTier.ORANGE).count());
        assertEquals(1,result.chunks().get(2).crystals().stream().filter(c->c.tier()==CrystalTier.ORANGE).count());
    }
    @Test void unfinishedRoundsContinueAcrossBatchesRatherThanFavoringTheCenter(){
        var first=RefinementBatch.advance(List.of(chunk(1,2,20),chunk(2,2,20)),0,600,4000,0,quick,Set.of());
        var second=RefinementBatch.advance(first.chunks(),0,600,4000,0,quick,first.served());
        assertEquals(CrystalTier.ORANGE,second.chunks().get(1).crystals().getFirst().tier());
    }
    @Test void insufficientManaPausesProgressWithoutLosingIt(){
        var result=RefinementBatch.advance(List.of(chunk(1,1,10)),1000,599,2000,0,quick,Set.of());assertEquals(0,result.upgrades());assertEquals(10,result.chunks().getFirst().crystals().getFirst().progress());assertEquals(599,result.mana());
    }
    @Test void virtualGenerationFundsUpgradesAndConsumesTheCostImmediately(){
        var result=RefinementBatch.advance(List.of(chunk(1,1,0)),40,0,2000,30,quick,Set.of());assertEquals(1,result.upgrades());assertEquals(600,result.mana(),1e-8);assertEquals(CrystalTier.RED,result.chunks().getFirst().crystals().getFirst().worldTier());assertEquals(CrystalTier.ORANGE,result.chunks().getFirst().crystals().getFirst().tier());
    }
    @Test void longCatchupMatchesRepeatedShortStepsIncludingRounds(){
        var input=List.of(chunk(1,3,0),chunk(2,2,0));var settings=new RefinementBatch.Settings(100,600,1.25);
        var full=RefinementBatch.advance(input,800,1000,162000,40.5,settings,Set.of());var current=input;double mana=1000;Set<Long> served=Set.of();
        for(int i=0;i<40;i++){var step=RefinementBatch.advance(current,20,mana,162000,40.5,settings,served);current=step.chunks();mana=step.mana();served=step.served();}
        assertEquals(full.mana(),mana,1e-6);for(int c=0;c<current.size();c++)for(int i=0;i<current.get(c).crystals().size();i++){var expected=full.chunks().get(c).crystals().get(i);var actual=current.get(c).crystals().get(i);assertEquals(expected.tier(),actual.tier());assertEquals(expected.progress(),actual.progress(),1e-6);}
    }
    @Test void catchupIsBoundedByTheNumberOfPossibleTierUpgrades(){
        var result=RefinementBatch.advance(List.of(chunk(1,1,0)),1_000_000_000,0,162000,40.5,quick,Set.of());assertEquals(7,result.upgrades());assertEquals(CrystalTier.WHITE,result.chunks().getFirst().crystals().getFirst().tier());assertEquals(162000,result.mana());
    }
    @Test void defaultTimeCostAndHigherTierScalingAreExplicit(){
        var settings=CrystalRules.refinement();assertEquals(215*1200,settings.baseTicks());assertEquals(600,settings.manaCost());assertEquals(1.25,settings.tierMultiplier());assertTrue(settings.ticks(CrystalTier.VIOLET)>settings.ticks(CrystalTier.RED));
    }
    @Test void pendingTalliesAndFairRoundSurviveSaving(){
        var store=new RefinementStore();var e=store.entry(123);e.region=42;e.exposure=1;e.crystals=List.of(new RefinementBatch.Crystal(100,CrystalTier.RED,CrystalTier.YELLOW,12));store.rounds.put(42L,Set.of(123L));
        var loaded=RefinementStore.load(store.save(new CompoundTag(),null),null);var restored=loaded.entries.get(123L);assertEquals(e.crystals,restored.crystals);assertEquals(42,restored.region);assertEquals(Set.of(123L),loaded.rounds.get(42L));
    }
    @Test void lazyChunkProgressSurvivesSavingAndDoesNotGrantNewCrystalsOldTime(){
        var region=new org.Azkerash.riftfall.worldmana.geysers.GeyserRegion(0,0,9,405);var store=new RefinementStore();var e=store.entry(1);e.region=0;e.exposure=1;e.crystals=List.of(red(1,0));
        double mana=1000;for(int i=0;i<100;i++)mana=store.advance(region,20,mana,162000,40.5);
        var restored=RefinementStore.load(store.save(new CompoundTag(),null),null);assertEquals(2000,restored.entries.get(1L).crystals.getFirst().progress());
        store.prepare(0);e.crystals=List.of(e.crystals.getFirst(),red(2,0));store.advance(region,20,mana,162000,40.5);store.prepare(0);
        assertEquals(2020,e.crystals.getFirst().progress());assertEquals(20,e.crystals.get(1).progress());
    }

}
