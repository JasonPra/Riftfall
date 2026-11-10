package org.Azkerash.riftfall.behavior;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BurrowTerrainTest {
    @Test void lowHardnessRequiresExplicitNaturalSoilEligibility(){
        for(float hardness:new float[]{0,.1F,.5F,.6F,.8F})assertTrue(BurrowTerrain.canDig(true,hardness));
        for(float hardness:new float[]{-.1F,-1,.81F,1.5F,2,50,Float.NaN,Float.POSITIVE_INFINITY})assertFalse(BurrowTerrain.canDig(true,hardness));
        assertFalse(BurrowTerrain.canDig(false,0));assertFalse(BurrowTerrain.canDig(false,.5F),"Weak crafted blocks must not qualify");
    }
    @Test void shippedAllowlistIncludesSoftSoilAndExcludesRockWoodAndCraftedMaterials() throws Exception {
        try(var stream=getClass().getResourceAsStream("/data/riftfall/tags/block/burrowable.json")){
            var values=JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject().getAsJsonArray("values");
            var allowed=new HashSet<String>();values.forEach(v->allowed.add(v.getAsString()));
            for(String name:new String[]{"grass_block","dirt","sand","red_sand","gravel","mud","clay"})assertTrue(allowed.contains("minecraft:"+name));
            for(String name:new String[]{"stone","cobblestone","deepslate","sandstone","netherrack","oak_log","oak_planks","glass","white_wool","hay_block","bricks"})assertFalse(allowed.contains("minecraft:"+name));
            assertTrue(allowed.stream().noneMatch(id->id.startsWith("#")),"Broad tags can accidentally admit crafted or woody blocks");
        }
    }
}
