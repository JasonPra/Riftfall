package org.Azkerash.riftfall.crystals;

import com.google.gson.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalTest {
    private JsonObject json(String path)throws Exception{
        var stream=getClass().getResourceAsStream("/"+path);assertNotNull(stream,path);
        try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){return JsonParser.parseReader(reader).getAsJsonObject();}
    }
    @Test void eightTiersAreOrderedAndOnlyAdultsCanAdvance(){
        assertEquals(List.of("red","orange","yellow","green","cyan","blue","violet","white"),Arrays.stream(CrystalTier.values()).map(CrystalTier::id).toList());
        for(var tier:CrystalTier.values()){
            for(int i=1;i<8;i++)assertFalse(tier.canRefine(i));assertEquals(tier!=CrystalTier.WHITE,tier.canRefine(8));
            tier.next().ifPresent(next->assertEquals(tier.ordinal()+1,next.ordinal()));
        }
    }
    @Test void offspringAreLowerQualityButNotNecessarilyRed(){
        assertEquals(CrystalTier.VIOLET,CrystalTier.WHITE.seedTier(.2));assertEquals(CrystalTier.BLUE,CrystalTier.WHITE.seedTier(.8));assertEquals(CrystalTier.CYAN,CrystalTier.WHITE.seedTier(.99));
        for(var tier:CrystalTier.values())for(double draw:new double[]{0,.7,.95,.999}){var child=tier.seedTier(draw);assertTrue(child.ordinal()<tier.ordinal()||tier==CrystalTier.RED&&child==CrystalTier.RED);}
    }
    @Test void initialGeologicalTiersFollowOuterMiddleAndInnerZones(){
        for(int i=0;i<1000;i++){
            double draw=i/1000.0;assertTrue(CrystalTier.natural(0,1,draw,true).ordinal()<=2);
            var middle=CrystalTier.natural(.5,1,draw,true);assertTrue(middle.ordinal()>=2&&middle.ordinal()<=4);
        }
        assertEquals(CrystalTier.WHITE,CrystalTier.natural(1,2,0,true));assertEquals(CrystalTier.VIOLET,CrystalTier.natural(1,2,0,false));assertEquals(CrystalTier.BLUE,CrystalTier.natural(1,1,.05,true));
    }
    @Test void invalidTierDrawsAndThresholdsCannotSilentlyPass(){
        assertThrows(IllegalArgumentException.class,()->CrystalTier.WHITE.seedTier(Double.NaN));assertThrows(IllegalArgumentException.class,()->CrystalTier.natural(1,1,1,true));
        assertFalse(CrystalRules.nucleation(1999.99));assertTrue(CrystalRules.nucleation(2000));assertFalse(CrystalRules.growth(599.99));assertTrue(CrystalRules.growth(600));assertEquals(600,CrystalRules.cost());assertEquals(.10,CrystalRules.chance());
    }
    @Test void texturePixelsAreSmokyTranslucentAcrossAllEightColors()throws Exception{
        for(var tier:CrystalTier.values())try(var stream=getClass().getResourceAsStream("/assets/riftfall/textures/block/"+tier.id()+"_mana_crystal.png")){
            assertNotNull(stream);var image=ImageIO.read(stream);assertEquals(32,image.getWidth());Set<Integer> colors=new HashSet<>();
            for(int y=0;y<32;y++)for(int x=0;x<32;x++){int pixel=image.getRGB(x,y);assertTrue((pixel>>>24)>100&&(pixel>>>24)<255);colors.add(pixel&0xffffff);}assertTrue(colors.size()>20);
        }
    }
    @Test void modelsCoverAllLayersFacesAndHaveBluntFullWidthConnectedBases()throws Exception{
        for(var tier:CrystalTier.values()){
            var states=json("assets/riftfall/blockstates/"+tier.id()+"_mana_crystal_cluster.json").getAsJsonObject("variants");assertEquals(48,states.size());
            for(int layer=1;layer<=8;layer++){
                var model=json("assets/riftfall/models/block/mana_crystals/"+tier.id()+"_"+layer+".json");assertEquals("minecraft:translucent",model.get("render_type").getAsString());double peak=0;boolean joined=false;
                for(var e:model.getAsJsonArray("elements")){
                    var element=e.getAsJsonObject();assertFalse(element.has("rotation"));var a=element.getAsJsonArray("from");var b=element.getAsJsonArray("to");
                    for(int axis=0;axis<3;axis++){assertTrue(a.get(axis).getAsDouble()>=0);assertTrue(b.get(axis).getAsDouble()<=16);assertTrue(a.get(axis).getAsDouble()<b.get(axis).getAsDouble());}
                    assertTrue(b.get(1).getAsDouble()<=layer*2);peak=Math.max(peak,b.get(1).getAsDouble());
                    if(a.get(0).getAsDouble()==0&&a.get(2).getAsDouble()==0&&b.get(0).getAsDouble()==16&&b.get(2).getAsDouble()==16)joined=true;
                }assertEquals(layer*2,peak);assertTrue(joined);
                for(String direction:List.of("up","down","north","south","east","west"))assertTrue(states.has("facing="+direction+",layers="+layer));
            }
        }
    }
    @Test void everyLayerDropsRawItemsAndCuttingIsOneWay()throws Exception{
        var tag=json("data/riftfall/tags/item/cut_mana_crystals.json").getAsJsonArray("values");assertEquals(8,tag.size());
        for(var tier:CrystalTier.values()){
            var recipe=json("data/riftfall/recipe/cut_"+tier.id()+"_mana_crystal.json");assertEquals("minecraft:stonecutting",recipe.get("type").getAsString());assertEquals("riftfall:raw_"+tier.id()+"_mana_crystal",recipe.getAsJsonObject("ingredient").get("item").getAsString());assertEquals("riftfall:cut_"+tier.id()+"_mana_crystal",recipe.getAsJsonObject("result").get("id").getAsString());assertEquals(1,recipe.getAsJsonObject("result").get("count").getAsInt());assertTrue(tag.contains(recipe.getAsJsonObject("result").get("id")));
            var children=json("data/riftfall/loot_table/blocks/"+tier.id()+"_mana_crystal_cluster.json").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("children");assertEquals(8,children.size());
            for(int i=0;i<8;i++){var entry=children.get(i).getAsJsonObject();assertEquals(i+1,entry.getAsJsonArray("functions").get(0).getAsJsonObject().get("count").getAsInt());assertTrue(entry.get("name").getAsString().contains("raw_"));}
        }
    }
    @Test void supportsAreNaturalStoneRatherThanSoilWoodOrCrystals()throws Exception{
        String values=json("data/riftfall/tags/block/mana_crystal_supports.json").getAsJsonArray("values").toString();assertTrue(values.contains("base_stone_overworld"));assertFalse(values.contains("dirt"));assertFalse(values.contains("planks"));assertFalse(values.contains("crystal"));
    }
}
