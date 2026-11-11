package org.Azkerash.riftfall.crystals;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalGlowTest {
    private JsonObject model(String name)throws Exception{try(var stream=getClass().getResourceAsStream("/assets/riftfall/models/block/mana_crystals/"+name+".json")){assertNotNull(stream);return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();}}
    @Test void prismCoresAreInsetAndRootPlatesStayUnlit()throws Exception{
        var names=new ArrayList<String>();for(int layer=1;layer<=8;layer++){for(int variant=0;variant<2;variant++)names.add("template_"+layer+"_"+variant);for(int side=0;side<4;side++)names.add("template_"+layer+"_join_"+side);}for(int corner=0;corner<4;corner++)names.add("template_junction_"+corner);
        for(var name:names){var elements=model(name).getAsJsonArray("elements");var shells=new HashMap<Integer,JsonObject>();
            for(var value:elements){var e=value.getAsJsonObject();if(e.get("shade").getAsBoolean()){assertFalse(e.has("neoforge_data"));int id=e.getAsJsonObject("faces").getAsJsonObject("up").get("tintindex").getAsInt();if(id>=0)shells.put(id,e);}else{
                int id=e.getAsJsonObject("faces").getAsJsonObject("up").get("tintindex").getAsInt();var shell=shells.get(id);assertNotNull(shell,"No root plate may have a core");var light=e.getAsJsonObject("neoforge_data");assertEquals(15,light.get("block_light").getAsInt());assertEquals(15,light.get("sky_light").getAsInt());assertFalse(light.get("ambient_occlusion").getAsBoolean());
                for(int axis=0;axis<3;axis++){assertTrue(e.getAsJsonArray("from").get(axis).getAsDouble()>shell.getAsJsonArray("from").get(axis).getAsDouble());assertTrue(e.getAsJsonArray("to").get(axis).getAsDouble()<shell.getAsJsonArray("to").get(axis).getAsDouble());}
                double base=shell.getAsJsonArray("from").get(1).getAsDouble(),height=shell.getAsJsonArray("to").get(1).getAsDouble()-base;assertTrue(e.getAsJsonArray("from").get(1).getAsDouble()>=base+height*.34);
            }}
        }
    }

    @Test void coreTexturesHaveSoftEmissionAndShellTexturesRemainCloudy()throws Exception{
        for(var tier:CrystalTier.values()){
            try(var a=getClass().getResourceAsStream("/assets/riftfall/textures/block/"+tier.id()+"_mana_crystal.png");var b=getClass().getResourceAsStream("/assets/riftfall/textures/block/"+tier.id()+"_mana_crystal_core.png")){
                assertNotNull(a);assertNotNull(b);var shell=ImageIO.read(a);var core=ImageIO.read(b);double outer=0,inner=0;
                for(int y=0;y<32;y++)for(int x=0;x<32;x++){int s=shell.getRGB(x,y),c=core.getRGB(x,y);assertTrue((s>>>24)>=205&&(s>>>24)<=250);assertTrue((c>>>24)>0&&(c>>>24)<200);outer+=luminance(s);inner+=luminance(c);}assertTrue(inner>outer*.65,tier.toString());assertTrue((core.getRGB(16,16)>>>24)>(core.getRGB(0,0)>>>24)+80,"Emission must fade towards facet edges");assertTrue((shell.getRGB(16,31)>>>24)>(shell.getRGB(16,0)>>>24),"Bases should be denser");
            }
            for(String suffix:List.of("_1","_8_wild","_8_join_0","_junction_0"))assertEquals("riftfall:block/"+tier.id()+"_mana_crystal_core",model(tier.id()+suffix).getAsJsonObject("textures").get("core").getAsString());
        }
    }
    @Test void onlyTwoSmallEdgePrismsCanBorrowMaterials()throws Exception{
        for(int layer=1;layer<=8;layer++)for(int variant=0;variant<2;variant++){
            int eligible=0;for(var value:model("template_"+layer+"_"+variant).getAsJsonArray("elements")){
                var e=value.getAsJsonObject();if(!e.get("shade").getAsBoolean())continue;int id=e.getAsJsonObject("faces").getAsJsonObject("up").get("tintindex").getAsInt();if(id<0||(id&1048576)==0)continue;eligible++;
                for(int axis=0;axis<3;axis++)assertTrue(e.getAsJsonArray("to").get(axis).getAsDouble()-e.getAsJsonArray("from").get(axis).getAsDouble()<=4);
                boolean edge=false;for(int axis:new int[]{0,2})edge|=e.getAsJsonArray("to").get(axis).getAsDouble()<=4||e.getAsJsonArray("from").get(axis).getAsDouble()>=12;assertTrue(edge);
            }assertEquals(2,eligible);
        }
    }
    @Test void everySharedEdgeHasSmallMixableDaughterCrystals()throws Exception{
        for(int layer=1;layer<=8;layer++)for(int side=0;side<4;side++){
            int count=0;
            for(var value:model("template_"+layer+"_join_"+side).getAsJsonArray("elements")){
                var e=value.getAsJsonObject();if(!e.get("shade").getAsBoolean())continue;
                int id=e.getAsJsonObject("faces").getAsJsonObject("up").get("tintindex").getAsInt();if(id<0||(id&1048576)==0)continue;
                count++;for(int axis=0;axis<3;axis++)assertTrue(e.getAsJsonArray("to").get(axis).getAsDouble()-e.getAsJsonArray("from").get(axis).getAsDouble()<=4);
                int axis=side%2==0?2:0;boolean positive=side==1||side==2;
                assertTrue(positive?e.getAsJsonArray("from").get(axis).getAsDouble()>=12:e.getAsJsonArray("to").get(axis).getAsDouble()<=4);
            }assertEquals(3,count);
        }
    }
    @Test void youngEdgeBudsTouchNeighboringYoungAndAdultCrystals()throws Exception{
        for(int layer=1;layer<=8;layer++)for(int neighborLayer:new int[]{1,8})for(int side=0;side<4;side++){
            int boundaryAxis=side%2==0?2:0,tangentAxis=boundaryAxis==0?2:0;boolean positive=side==1||side==2;
            for(var value:model("template_"+layer+"_join_"+side).getAsJsonArray("elements")){
                var e=value.getAsJsonObject();if(!e.get("shade").getAsBoolean())continue;
                int id=e.getAsJsonObject("faces").getAsJsonObject("up").get("tintindex").getAsInt();if((id&1048576)==0)continue;
                assertEquals(positive?16:0,e.getAsJsonArray(positive?"to":"from").get(boundaryAxis).getAsDouble());
                boolean touching=false;
                for(var other:model("template_"+neighborLayer+"_join_"+((side+2)%4)).getAsJsonArray("elements")){
                    var b=other.getAsJsonObject();if(!b.get("shade").getAsBoolean())continue;
                    int otherId=b.getAsJsonObject("faces").getAsJsonObject("up").get("tintindex").getAsInt();if((otherId&1048576)==0)continue;
                    boolean overlap=true;for(int axis:new int[]{tangentAxis,1})overlap&=Math.max(e.getAsJsonArray("from").get(axis).getAsDouble(),b.getAsJsonArray("from").get(axis).getAsDouble())<Math.min(e.getAsJsonArray("to").get(axis).getAsDouble(),b.getAsJsonArray("to").get(axis).getAsDouble());
                    touching|=overlap;
                }assertTrue(touching,"Shared-edge buds must meet across different growth stages");
            }
        }
    }
    private double luminance(int rgb){return .2126*((rgb>>16)&255)+.7152*((rgb>>8)&255)+.0722*(rgb&255);}
}
