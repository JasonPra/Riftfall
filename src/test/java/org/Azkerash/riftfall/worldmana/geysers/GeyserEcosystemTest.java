package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import org.Azkerash.riftfall.crystals.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeyserEcosystemTest {
    private CompoundTag legacy(long seed,GeyserRegion... fields){
        var tag=new CompoundTag();tag.putInt("Version",1);tag.putLong("Seed",seed);var list=new ListTag();
        for(var field:fields){var row=new CompoundTag();row.putInt("X",field.centerChunkX());row.putInt("Z",field.centerChunkZ());row.putInt("Width",field.width());row.putDouble("Output",field.totalOutput());list.add(row);}tag.put("Fields",list);return tag;
    }
    @Test void mainZonesAreBiomeScaleWhileTheWhiteCoreAndOuterFringeStayCompact(){
        for(int width:new int[]{129,193,257}){
            var field=new GeyserRegion(-100,200,width,1000);assertFalse(field.legacy());assertTrue(field.blockWidth()>=2048);
            assertEquals(272,(field.coreRadiusChunks()*2+1)*16);assertEquals(64,field.outerWidthChunks()*16);
            assertTrue((field.innerRadiusChunks()-field.coreRadiusChunks())*16>=400);
            assertTrue((field.middleRadiusChunks()-field.innerRadiusChunks())*16>=400);
            for(int distance=0;distance<=field.halfWidth()+1;distance++){
                var expected=distance<=field.coreRadiusChunks()?GeyserZone.CORE:distance<=field.innerRadiusChunks()?GeyserZone.INNER:distance<=field.middleRadiusChunks()?GeyserZone.MIDDLE:distance<=field.halfWidth()?GeyserZone.OUTER:GeyserZone.OUTSIDE;
                for(int sign:new int[]{-1,1}){assertEquals(expected,field.zone(field.centerChunkX()+sign*distance,field.centerChunkZ()));assertEquals(expected,field.zone(field.centerChunkX(),field.centerChunkZ()+sign*distance));}
                assertEquals(expected!=GeyserZone.OUTSIDE,field.crystalZone(field.centerChunkX()+distance,field.centerChunkZ()));
            }
        }
    }
    @Test void coreMothersAreMostlyWhiteAndTheirSurroundingYoungGrowthIsLowerTier(){
        int white=0;
        for(int i=0;i<1000;i++){
            double draw=i/1000.0;var mother=CrystalTier.naturalDeposit(GeyserZone.CORE,draw,true,true);if(mother==CrystalTier.WHITE)white++;
            assertTrue(mother==CrystalTier.WHITE||mother==CrystalTier.VIOLET||mother==CrystalTier.BLUE);
            var halo=CrystalTier.naturalDeposit(GeyserZone.CORE,draw,true,false);assertTrue(halo.ordinal()>=CrystalTier.CYAN.ordinal()&&halo.ordinal()<CrystalTier.WHITE.ordinal());
            for(var zone:List.of(GeyserZone.INNER,GeyserZone.MIDDLE,GeyserZone.OUTER))assertNotEquals(CrystalTier.WHITE,CrystalTier.naturalDeposit(zone,draw,true,true));
            assertNotEquals(CrystalTier.WHITE,CrystalTier.naturalDeposit(GeyserZone.CORE,draw,false,true));
        }
        assertEquals(800,white);assertEquals(48,CrystalRules.coreDepositSize());
    }
    @Test void multipleLegacyFieldsInOneNewPlacementCellKeepTheirExactCentersSizesAndRules(){
        var first=new GeyserRegion(64,64,9,405);var second=new GeyserRegion(192,64,17,2890);var third=new GeyserRegion(320,192,9,405);
        var data=GeyserRegions.load(legacy(42,first,second,third),null);
        for(var field:List.of(first,second,third)){assertEquals(field,data.discover(field.centerChunkX(),field.centerChunkZ()).orElseThrow());assertTrue(field.legacy());assertFalse(field.crystalZone(field.centerChunkX(),field.centerChunkZ()));}
        assertEquals(3,data.size());var restored=GeyserRegions.load(data.save(new CompoundTag(),null),null);assertEquals(data.discovered(),restored.discovered());
    }
    @Test void newCandidatesNeverOverlapPreservedLegacyFieldsAndMixedSavesRoundTrip(){
        var modern=java.util.stream.IntStream.range(0,100).mapToObj(x->new GeyserLayout(42).cell(x,0)).flatMap(Optional::stream).findFirst().orElseThrow();
        int oldX=Math.floorDiv(modern.centerChunkX(),128)*128+64,oldZ=Math.floorDiv(modern.centerChunkZ(),128)*128+64;
        var old=new GeyserRegion(oldX,oldZ,9,405);var data=GeyserRegions.load(legacy(42,old),null);
        assertTrue(data.candidate(Math.floorDiv(modern.centerChunkX(),512),Math.floorDiv(modern.centerChunkZ(),512)).isEmpty());
        assertEquals(old,data.discover(oldX,oldZ).orElseThrow());
        GeyserRegion added=null;
        for(int x=3;x<30&&added==null;x++)added=data.candidate(x,0).orElse(null);
        assertNotNull(added);assertEquals(added,data.discover(added.centerChunkX(),added.centerChunkZ()).orElseThrow());
        var loaded=GeyserRegions.load(data.save(new CompoundTag(),null),null);assertEquals(data.discovered(),loaded.discovered());assertEquals(old,loaded.discover(oldX,oldZ).orElseThrow());
    }
    @Test void newPoolCapacityAndRegenerationUseTheWholeLargeFootprintAndSaveCleanly(){
        for(int width:new int[]{129,193,257}){
            var field=new GeyserRegion(256,256,width,1000);double cap=(double)width*width*2000,regen=(double)width*width*.5;
            assertEquals(cap,GeyserLedger.capacity(field));assertEquals(regen,GeyserLedger.regeneration(field));
            var tag=new CompoundTag();tag.putInt("Version",1);var rows=new ListTag();var row=new CompoundTag();row.putLong("Region",ChunkPos.asLong(256,256));row.putDouble("Mana",123456);row.putLong("Updated",9000);row.putDouble("Capacity",cap);row.putDouble("RegenerationPerTick",regen);rows.add(row);tag.put("Pools",rows);
            var saved=GeyserLedger.load(tag,null).save(new CompoundTag(),null).getList("Pools",10).getCompound(0);assertEquals(cap,saved.getDouble("Capacity"));assertEquals(123456,saved.getDouble("Mana"));
        }
    }
    @Test void allNewSizesStayWellWithinTheirCellsAndWorldBoundary(){
        var layout=new GeyserLayout(42);var widths=new HashSet<Integer>();
        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)layout.cell(x,z).ifPresent(g->{widths.add(g.width());assertEquals(Math.floorDiv(g.centerChunkX(),512),Math.floorDiv(g.centerChunkX()+g.halfWidth(),512));assertEquals(Math.floorDiv(g.centerChunkX(),512),Math.floorDiv(g.centerChunkX()-g.halfWidth(),512));});
        assertEquals(Set.of(129,193,257),widths);
        assertTrue(layout.cell(Integer.MAX_VALUE,Integer.MIN_VALUE).isEmpty());
    }
}
