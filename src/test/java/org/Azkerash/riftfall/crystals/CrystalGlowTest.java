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
    @Test void everyStemRootJunctionAndBridgeHasAnInsetEmissiveCore()throws Exception{
        var names=new ArrayList<String>();for(int layer=1;layer<=8;layer++){for(int variant=0;variant<2;variant++)names.add("template_"+layer+"_"+variant);for(int side=0;side<4;side++)names.add("template_"+layer+"_join_"+side);}for(int corner=0;corner<4;corner++)names.add("template_junction_"+corner);names.add("template_bridge");
        for(var name:names){var elements=model(name).getAsJsonArray("elements");assertEquals(0,elements.size()%2);int count=elements.size()/2;
            for(int i=0;i<count;i++){var shell=elements.get(i).getAsJsonObject();var core=elements.get(i+count).getAsJsonObject();assertTrue(shell.get("shade").getAsBoolean());assertFalse(shell.has("neoforge_data"));assertFalse(core.get("shade").getAsBoolean());var light=core.getAsJsonObject("neoforge_data");assertEquals(15,light.get("block_light").getAsInt());assertEquals(15,light.get("sky_light").getAsInt());assertFalse(light.get("ambient_occlusion").getAsBoolean());
                for(int axis=0;axis<3;axis++){assertTrue(core.getAsJsonArray("from").get(axis).getAsDouble()>shell.getAsJsonArray("from").get(axis).getAsDouble());assertTrue(core.getAsJsonArray("to").get(axis).getAsDouble()<shell.getAsJsonArray("to").get(axis).getAsDouble());}
                for(var face:shell.getAsJsonObject("faces").entrySet()){var outer=face.getValue().getAsJsonObject();var inner=core.getAsJsonObject("faces").getAsJsonObject(face.getKey());assertEquals("#core",inner.get("texture").getAsString());assertFalse(inner.has("cullface"));assertEquals(outer.get("tintindex"),inner.get("tintindex"),"Core and shell must inherit the same material");}
            }
        }
    }
    @Test void coreTexturesAreOpaqueBrightAndShellTexturesRemainTranslucent()throws Exception{
        for(var tier:CrystalTier.values()){
            try(var a=getClass().getResourceAsStream("/assets/riftfall/textures/block/"+tier.id()+"_mana_crystal.png");var b=getClass().getResourceAsStream("/assets/riftfall/textures/block/"+tier.id()+"_mana_crystal_core.png")){
                assertNotNull(a);assertNotNull(b);var shell=ImageIO.read(a);var core=ImageIO.read(b);double outer=0,inner=0;
                for(int y=0;y<32;y++)for(int x=0;x<32;x++){int s=shell.getRGB(x,y),c=core.getRGB(x,y);assertTrue((s>>>24)>=90&&(s>>>24)<=155);assertEquals(255,c>>>24);outer+=luminance(s);inner+=luminance(c);}assertTrue(inner>outer,tier.toString());
            }
            for(String suffix:List.of("_1","_8_wild","_8_join_0","_junction_0","_bridge"))assertEquals("riftfall:block/"+tier.id()+"_mana_crystal_core",model(tier.id()+suffix).getAsJsonObject("textures").get("core").getAsString());
        }
    }
    @Test void overhangingBridgeUvsStayInsideTheirTexture()throws Exception{
        for(var value:model("template_bridge").getAsJsonArray("elements"))for(var face:value.getAsJsonObject().getAsJsonObject("faces").entrySet()){
            var uv=face.getValue().getAsJsonObject().getAsJsonArray("uv");assertNotNull(uv);for(var coordinate:uv)assertTrue(coordinate.getAsDouble()>=0&&coordinate.getAsDouble()<=16,"No atlas bleeding from overhanging geometry");
        }
    }
    private double luminance(int rgb){return .2126*((rgb>>16)&255)+.7152*((rgb>>8)&255)+.0722*(rgb&255);}
}
