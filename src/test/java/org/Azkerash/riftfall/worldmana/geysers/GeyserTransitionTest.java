package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.world.level.biome.Biomes;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeyserTransitionTest {
    @Test void oceanBufferIncludesLandBeachShallowsAndDeepOcean(){
        var c=GeyserTransition.Climate.WARM_SEA;
        assertEquals(Biomes.PLAINS,GeyserTransition.biome(c,true,.1));
        assertEquals(Biomes.BEACH,GeyserTransition.biome(c,true,.3));
        assertEquals(Biomes.WARM_OCEAN,GeyserTransition.biome(c,true,.5));
        assertEquals(Biomes.LUKEWARM_OCEAN,GeyserTransition.biome(c,true,.7));
        assertEquals(Biomes.DEEP_LUKEWARM_OCEAN,GeyserTransition.biome(c,true,.9));
        assertEquals(Biomes.SNOWY_BEACH,GeyserTransition.biome(GeyserTransition.Climate.FROZEN_SEA,true,.3));
        assertEquals(Biomes.DEEP_COLD_OCEAN,GeyserTransition.biome(GeyserTransition.Climate.COLD_SEA,true,.9));
    }
    @Test void inlandBuffersChooseAppropriateVanillaWoodland(){
        assertEquals(Biomes.FOREST,GeyserTransition.biome(GeyserTransition.Climate.TEMPERATE,false,.5));
        assertEquals(Biomes.TAIGA,GeyserTransition.biome(GeyserTransition.Climate.COLD,false,.5));
        assertEquals(Biomes.SAVANNA,GeyserTransition.biome(GeyserTransition.Climate.DRY,false,.5));
        assertEquals(Biomes.SPARSE_JUNGLE,GeyserTransition.biome(GeyserTransition.Climate.WARM,false,.5));
    }
    @Test void terrainJoinsExactlyAtBothEndsWithoutAnOceanWall(){
        for(var c:GeyserTransition.Climate.values())for(int original:new int[]{25,55,70,180}){
            assertEquals(78,GeyserTransition.floor(c,0,78,original,63));
            assertEquals(original,GeyserTransition.floor(c,1,78,original,63));
        }
        int previous=78;boolean beach=false,shallow=false,deep=false;
        for(int d=1;d<=384;d++){
            int height=GeyserTransition.floor(GeyserTransition.Climate.SEA,d/384.,78,25,63);
            assertTrue(Math.abs(height-previous)<=2,"Abrupt shore step at "+d);previous=height;
            beach|=height>=62&&height<=65;shallow|=height>=53&&height<62;deep|=height<42;
        }
        assertTrue(beach&&shallow&&deep);
    }
    @Test void beltStaysWithinThePlacementCellAndDoesNotEnlargeManaFootprint(){
        var g=new GeyserRegion(256,256,257,1000);double edge=g.centerX()+g.halfWidth()*16+8;
        assertEquals(0,GeyserTransition.distance(g,edge,g.centerZ()));
        assertEquals(384,GeyserTransition.distance(g,edge+384,g.centerZ()));
        assertFalse(g.contains((int)(edge+128)>>4,g.centerChunkZ()));
        assertEquals(0,GeyserTransition.progress(0,123,-234));
        assertEquals(1,GeyserTransition.progress(384,123,-234));
    }
}
