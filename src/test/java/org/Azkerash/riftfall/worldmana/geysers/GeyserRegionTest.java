package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.Azkerash.riftfall.worldmana.ChunkMana;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeyserRegionTest {
    @Test void outputAcrossTheEntireFootprintEqualsOneGeysersBudget(){
        for(var g:List.of(new GeyserRegion(0,0,9,405),new GeyserRegion(-100,100,17,2890))){
            double sum=0;int chunks=0;
            for(int x=g.centerChunkX()-g.halfWidth();x<=g.centerChunkX()+g.halfWidth();x++)for(int z=g.centerChunkZ()-g.halfWidth();z<=g.centerChunkZ()+g.halfWidth();z++){assertTrue(g.contains(x,z));sum+=g.outputAt(x,z);chunks++;}
            assertEquals(g.chunkCount(),chunks);assertEquals(g.totalOutput(),sum,1e-8);assertEquals(0,g.outputAt(g.centerChunkX()+g.halfWidth()+1,g.centerChunkZ()));
            assertTrue(g.outputAt(g.centerChunkX(),g.centerChunkZ())>g.outputAt(g.centerChunkX()+g.halfWidth(),g.centerChunkZ()+g.halfWidth()));
        }
    }
    @Test void footprintsHaveOneCenterChunkAndASeparateCrystalFringe(){
        var g=new GeyserRegion(0,0,9,405);assertEquals(144,g.blockWidth());assertEquals(81,g.chunkCount());assertEquals(8,g.centerX());assertFalse(g.crystalZone(0,0));assertTrue(g.crystalZone(4,0));assertFalse(g.crystalZone(5,0));
        assertTrue(Double.isFinite(new GeyserRegion(0,0,3,Double.MAX_VALUE).outputAt(0,0)));
        assertThrows(IllegalArgumentException.class,()->new GeyserRegion(0,0,8,100));assertThrows(IllegalArgumentException.class,()->new GeyserRegion(0,0,9,Double.NaN));
    }
    @Test void largerFieldsHaveHigherTotalOutputAndConcentration(){
        var small=new GeyserRegion(0,0,GeyserLayout.SMALL_WIDTH,GeyserLayout.SMALL_OUTPUT);var large=new GeyserRegion(0,0,GeyserLayout.LARGE_WIDTH,GeyserLayout.LARGE_OUTPUT);
        assertTrue(large.totalOutput()>small.totalOutput());assertTrue(large.totalOutput()/large.chunkCount()>small.totalOutput()/small.chunkCount());assertTrue(large.outputAt(0,0)>small.outputAt(0,0));
    }
    @Test void oneCellLookupWorksAcrossNegativeCoordinatesAndExplorationOrders(){
        var layout=new GeyserLayout(42);List<GeyserRegion> fields=new ArrayList<>();
        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)layout.cell(x,z).ifPresent(fields::add);
        assertTrue(fields.size()>100&&fields.size()<350);assertTrue(fields.stream().anyMatch(g->g.width()==GeyserLayout.SMALL_WIDTH));assertTrue(fields.stream().anyMatch(g->g.width()==GeyserLayout.LARGE_WIDTH));
        Collections.reverse(fields);
        for(var g:fields)for(int dx:new int[]{-g.halfWidth(),-1,0,1,g.halfWidth()})for(int dz:new int[]{-g.halfWidth(),-1,0,1,g.halfWidth()})assertEquals(g,layout.at(g.centerChunkX()+dx,g.centerChunkZ()+dz).orElseThrow());
    }
    @Test void fieldsStayInsideTheirCellsAndCannotOverlap(){
        var layout=new GeyserLayout(42);
        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){
            var candidate=layout.cell(x,z);if(candidate.isEmpty())continue;var g=candidate.orElseThrow();
            assertEquals(x,Math.floorDiv(g.centerChunkX()-g.halfWidth(),GeyserLayout.CELL_CHUNKS));assertEquals(x,Math.floorDiv(g.centerChunkX()+g.halfWidth(),GeyserLayout.CELL_CHUNKS));
            assertEquals(z,Math.floorDiv(g.centerChunkZ()-g.halfWidth(),GeyserLayout.CELL_CHUNKS));assertEquals(z,Math.floorDiv(g.centerChunkZ()+g.halfWidth(),GeyserLayout.CELL_CHUNKS));
        }
    }
    @Test void descriptorsAndSeedSurviveSaveAndReload(){
        var data=new GeyserRegions(42);var candidate=java.util.stream.IntStream.range(0,100).mapToObj(x->data.layout().cell(x,0)).flatMap(Optional::stream).findFirst().orElseThrow();
        assertEquals(candidate,data.discover(candidate.centerChunkX(),candidate.centerChunkZ()).orElseThrow());assertEquals(1,data.size());
        assertEquals(candidate,data.discover(candidate.centerChunkX()+candidate.halfWidth(),candidate.centerChunkZ()).orElseThrow());assertEquals(1,data.size());
        var loaded=GeyserRegions.load(data.save(new CompoundTag(),null),null);assertEquals(data.discovered(),loaded.discovered());assertEquals(42,loaded.layout().seed());
        assertEquals(candidate,loaded.discover(candidate.centerChunkX(),candidate.centerChunkZ()).orElseThrow());
    }
    @Test void chunkStorageStillCapsAtTwoThousandAndOnlyLoadedTicksRegenerate(){
        var mana=new ChunkMana();mana.regenerate(20);assertEquals(10,mana.amount());assertEquals(10,mana.snapshot().regenerationPerSecond());mana.regenerate(0);assertEquals(10,mana.amount());mana.regenerate(Long.MAX_VALUE);assertEquals(2000,mana.amount());
    }
}
