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
            for(int y=0;y<32;y++)for(int x=0;x<32;x++){int pixel=image.getRGB(x,y);assertTrue((pixel>>>24)>=205&&(pixel>>>24)<=250);colors.add(pixel&0xffffff);}assertTrue(colors.size()>20);
        }
    }
    @Test void allStagesFacesAndPresetsHaveExplicitBodyModels()throws Exception{
        for(var tier:CrystalTier.values()){
            var variants=json("assets/riftfall/blockstates/"+tier.id()+"_mana_crystal_cluster.json").getAsJsonObject("variants");assertEquals(288,variants.size());
            for(int layer=1;layer<=8;layer++)for(String face:List.of("up","down","north","south","east","west"))for(int preset=0;preset<6;preset++){
                var variant=variants.getAsJsonObject("facing="+face+",layers="+layer+",variant="+preset);assertNotNull(variant);
                String suffix=preset==0?"":preset==1?"_wild":"_preset_"+preset;assertEquals("riftfall:block/mana_crystals/"+tier.id()+"_"+layer+suffix,variant.get("model").getAsString());
            }
        }
    }
    @Test void intergrownColumnsHaveTiltAndStayInsideOneBlockAtEveryLayer()throws Exception{
        for(int layer=1;layer<=8;layer++)for(int variant=0;variant<6;variant++){
            var model=json("assets/riftfall/models/block/mana_crystals/template_"+layer+"_"+variant+".json");int tilted=0;
            for(var value:model.getAsJsonArray("elements")){
                var element=value.getAsJsonObject();var a=element.getAsJsonArray("from");var b=element.getAsJsonArray("to");
                assertFalse(a.get(0).getAsDouble()==0&&a.get(2).getAsDouble()==0&&b.get(0).getAsDouble()==16&&b.get(2).getAsDouble()==16,"No rectangular full-block slab underneath the crystals");
                for(int axis=0;axis<3;axis++)assertTrue(a.get(axis).getAsDouble()<b.get(axis).getAsDouble());
                if(element.has("rotation")&&element.get("shade").getAsBoolean())tilted++;
                for(int bits=0;bits<8;bits++){
                    double[] point=new double[3];for(int axis=0;axis<3;axis++)point[axis]=((bits&(1<<axis))==0?a:b).get(axis).getAsDouble();
                    if(element.has("rotation")){
                        var rotation=element.getAsJsonObject("rotation");var origin=rotation.getAsJsonArray("origin");int first=rotation.get("axis").getAsString().equals("x")?1:0,second=rotation.get("axis").getAsString().equals("x")?2:1;
                        double angle=Math.toRadians(rotation.get("angle").getAsDouble()),x=point[first]-origin.get(first).getAsDouble(),y=point[second]-origin.get(second).getAsDouble();
                        point[first]=origin.get(first).getAsDouble()+x*Math.cos(angle)-y*Math.sin(angle);point[second]=origin.get(second).getAsDouble()+x*Math.sin(angle)+y*Math.cos(angle);
                    }
                    for(int axis=0;axis<3;axis++){assertTrue(point[axis]>=-1e-7);assertTrue(point[axis]<=(axis==1?layer*2:16)+1e-7);}
                }
            }
            assertEquals(layer>=3?(variant==0?3:variant==1?4:2):0,tilted);
        }
    }
    @Test void everyStageHasSixDistinctPresets()throws Exception{
        for(int layer=1;layer<=8;layer++){
            var shapes=new java.util.HashSet<String>();
            for(int preset=0;preset<6;preset++){
                var elements=json("assets/riftfall/models/block/mana_crystals/template_"+layer+"_"+preset+".json").getAsJsonArray("elements");
                var silhouette=new StringBuilder();for(var value:elements){var e=value.getAsJsonObject();if(e.get("shade").getAsBoolean())silhouette.append(e.get("from")).append(e.get("to")).append(e.get("rotation"));}shapes.add(silhouette.toString());
            }assertEquals(6,shapes.size());
        }
    }
    @Test void connectionsHaveIndividualPrismsAndTierTextureInheritance()throws Exception{
        for(int layer=1;layer<=8;layer++){
            var model=json("assets/riftfall/models/block/mana_crystals/template_"+layer+"_join_0.json");var root=model.getAsJsonArray("elements").get(0).getAsJsonObject();
            for(var value:model.getAsJsonArray("elements")){var prism=value.getAsJsonObject();assertTrue(prism.getAsJsonArray("to").get(0).getAsDouble()-prism.getAsJsonArray("from").get(0).getAsDouble()<=4,"No broad backing plate");assertTrue(prism.getAsJsonArray("to").get(2).getAsDouble()-prism.getAsJsonArray("from").get(2).getAsDouble()<=4,"No broad backing plate");}
            for(var tier:CrystalTier.values())for(String suffix:List.of("","_wild","_join_0","_join_1","_join_2","_join_3")){
                var child=json("assets/riftfall/models/block/mana_crystals/"+tier.id()+"_"+layer+suffix+".json");assertTrue(child.get("parent").getAsString().contains("template_"+layer));assertEquals("riftfall:block/"+tier.id()+"_mana_crystal",child.getAsJsonObject("textures").get("crystal").getAsString());
            }
        }
    }
    @Test void floatingEndCapsAreAbsentFromEveryTier()throws Exception{
        for(var tier:CrystalTier.values())assertFalse(json("assets/riftfall/blockstates/"+tier.id()+"_mana_crystal_cluster.json").toString().contains("_tip"));
    }
    @Test void junctionBranchesReachTheSharedCornerWithoutAFlatCap()throws Exception{
        for(int corner=0;corner<4;corner++){
            var elements=json("assets/riftfall/models/block/mana_crystals/template_junction_"+corner+".json").getAsJsonArray("elements");assertEquals(6,elements.size());
            for(var value:elements){var e=value.getAsJsonObject();var a=e.getAsJsonArray("from");var b=e.getAsJsonArray("to");for(int axis=0;axis<3;axis++){assertTrue(a.get(axis).getAsDouble()>=0);assertTrue(b.get(axis).getAsDouble()<=16);assertTrue(a.get(axis).getAsDouble()<b.get(axis).getAsDouble());}assertTrue((b.get(0).getAsDouble()-a.get(0).getAsDouble())<=4);assertTrue((b.get(2).getAsDouble()-a.get(2).getAsDouble())<=4);}
            var tip=elements.get(2).getAsJsonObject();assertEquals(16,tip.getAsJsonArray("to").get(1).getAsDouble());
            for(var tier:CrystalTier.values())assertTrue(json("assets/riftfall/models/block/mana_crystals/"+tier.id()+"_junction_"+corner+".json").get("parent").getAsString().endsWith("junction_"+corner));
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
