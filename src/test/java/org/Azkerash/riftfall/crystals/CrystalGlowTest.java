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
        var names=new ArrayList<String>();for(int layer=1;layer<=8;layer++){for(int variant=0;variant<6;variant++)names.add("template_"+layer+"_"+variant);for(int side=0;side<4;side++)names.add("template_"+layer+"_join_"+side);}for(int corner=0;corner<4;corner++)names.add("template_junction_"+corner);
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
        for(int layer=1;layer<=8;layer++)for(int variant=0;variant<6;variant++){
            int eligible=0;for(var value:model("template_"+layer+"_"+variant).getAsJsonArray("elements")){
                var e=value.getAsJsonObject();if(!e.get("shade").getAsBoolean())continue;int id=e.getAsJsonObject("faces").getAsJsonObject("up").get("tintindex").getAsInt();if(id<0||(id&1048576)==0)continue;eligible++;
                for(int axis=0;axis<3;axis++)assertTrue(e.getAsJsonArray("to").get(axis).getAsDouble()-e.getAsJsonArray("from").get(axis).getAsDouble()<=4);
                boolean edge=false;for(int axis:new int[]{0,2})edge|=e.getAsJsonArray("to").get(axis).getAsDouble()<=4||e.getAsJsonArray("from").get(axis).getAsDouble()>=12;assertTrue(edge);
            }assertEquals(2,eligible);
        }
    }
    @Test void everySharedEdgeHasSmallMixableDaughterCrystals()throws Exception{
        for(int layer=1;layer<=8;layer++)for(int side=0;side<4;side++){
            int count=0;var heights=new HashSet<Double>();
            for(var value:model("template_"+layer+"_join_"+side).getAsJsonArray("elements")){
                var e=value.getAsJsonObject();if(!e.get("shade").getAsBoolean())continue;
                int id=e.getAsJsonObject("faces").getAsJsonObject("up").get("tintindex").getAsInt();if(id<0||(id&1048576)==0)continue;
                count++;heights.add(e.getAsJsonArray("to").get(1).getAsDouble());for(int axis=0;axis<3;axis++)assertTrue(e.getAsJsonArray("to").get(axis).getAsDouble()-e.getAsJsonArray("from").get(axis).getAsDouble()<=4);
                int axis=side%2==0?2:0;boolean positive=side==1||side==2;
                assertTrue(positive?e.getAsJsonArray("from").get(axis).getAsDouble()>=12:e.getAsJsonArray("to").get(axis).getAsDouble()<=4);
            }assertEquals(5,count);assertTrue(heights.size()>=3,"Edge buds must not flatten into a uniform rim");
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
    @Test void budsFormConnectedClumpsAtEveryGrowthStage()throws Exception{
        for(int layer=1;layer<=8;layer++)for(String suffix:List.of("_0","_1","_2","_3","_4","_5","_join_0","_join_1","_join_2","_join_3")){
            var prisms=new ArrayList<JsonObject>();for(var value:model("template_"+layer+suffix).getAsJsonArray("elements")){var e=value.getAsJsonObject();if(e.get("shade").getAsBoolean())prisms.add(e);}
            var reached=new HashSet<Integer>();reached.add(0);boolean changed=true;
            while(changed){changed=false;for(int a=0;a<prisms.size();a++)if(reached.contains(a))for(int b=0;b<prisms.size();b++)if(!reached.contains(b)){
                boolean touching=true;for(int axis=0;axis<3;axis++)touching&=Math.max(prisms.get(a).getAsJsonArray("from").get(axis).getAsDouble(),prisms.get(b).getAsJsonArray("from").get(axis).getAsDouble())<=Math.min(prisms.get(a).getAsJsonArray("to").get(axis).getAsDouble(),prisms.get(b).getAsJsonArray("to").get(axis).getAsDouble())+1e-6;
                if(touching){reached.add(b);changed=true;}
            }}assertEquals(prisms.size(),reached.size(),"No isolated buds at layer "+layer+suffix);
        }
    }
    @Test void sharedEdgesHaveNoRepeatedGapsIncludingCorners()throws Exception{
        for(int layer=1;layer<=8;layer++)for(int side=0;side<4;side++){
            int boundaryAxis=side%2==0?2:0,tangentAxis=boundaryAxis==0?2:0;
            for(int pixel=0;pixel<16;pixel++){
                double point=pixel+.5;boolean covered=false;
                for(var value:model("template_"+layer+"_join_"+side).getAsJsonArray("elements")){
                    var e=value.getAsJsonObject();if(!e.get("shade").getAsBoolean())continue;
                    int id=e.getAsJsonObject("faces").getAsJsonObject("up").get("tintindex").getAsInt();if((id&1048576)==0)continue;
                    covered|=e.getAsJsonArray("from").get(tangentAxis).getAsDouble()<=point&&e.getAsJsonArray("to").get(tangentAxis).getAsDouble()>=point;
                }assertTrue(covered,"Open seam at layer "+layer+", side "+side+", pixel "+pixel);
            }
        }
    }
    @Test void largeCrystalTipsAreMoreTransparentWhileBasesKeepTheirDensity()throws Exception{
        for(var tier:CrystalTier.values())try(var a=getClass().getResourceAsStream("/assets/riftfall/textures/block/"+tier.id()+"_mana_crystal.png");var b=getClass().getResourceAsStream("/assets/riftfall/textures/block/"+tier.id()+"_mana_crystal_large.png")){
            var normal=ImageIO.read(a);var large=ImageIO.read(b);
            for(int x=0;x<32;x++){
                assertEquals((normal.getRGB(x,0)>>>24)-40,large.getRGB(x,0)>>>24);
                for(int y=10;y<32;y++)assertEquals(normal.getRGB(x,y),large.getRGB(x,y));
            }
        }
        for(int layer=1;layer<=8;layer++)for(int preset=0;preset<6;preset++)for(var value:model("template_"+layer+"_"+preset).getAsJsonArray("elements")){
            var e=value.getAsJsonObject();if(!e.get("shade").getAsBoolean())continue;
            double height=e.getAsJsonArray("to").get(1).getAsDouble()-e.getAsJsonArray("from").get(1).getAsDouble();
            if(e.getAsJsonObject("faces").getAsJsonObject("up").get("texture").getAsString().equals("#large_crystal")){
                assertTrue(height>=6);assertEquals(1,e.getAsJsonObject("faces").getAsJsonObject("up").getAsJsonArray("uv").get(3).getAsInt());
                assertEquals(16,e.getAsJsonObject("faces").getAsJsonObject("north").getAsJsonArray("uv").get(3).getAsInt());
            }
        }
    }
    @Test void largeFormationQuartersHaveNoInternalCutFacesOrOutOfBoundsUvs()throws Exception{
        var presets=new HashSet<String>();
        for(int preset=0;preset<6;preset++){
            var silhouette=new StringBuilder();
            for(int quarter=0;quarter<4;quarter++){
                var elements=model("template_mega_"+preset+"_"+quarter).getAsJsonArray("elements");assertFalse(elements.isEmpty());
                for(var value:elements){var e=value.getAsJsonObject();var a=e.getAsJsonArray("from");var b=e.getAsJsonArray("to");
                    for(int axis=0;axis<3;axis++){assertTrue(a.get(axis).getAsDouble()>=0);assertTrue(b.get(axis).getAsDouble()<=16);assertTrue(a.get(axis).getAsDouble()<b.get(axis).getAsDouble());}
                    var faces=e.getAsJsonObject("faces");int u=quarter%2,v=quarter/2;
                    if(u==0&&b.get(0).getAsDouble()==16&&continuesIn(preset,quarter^1,e))assertFalse(faces.has("east"));if(u==1&&a.get(0).getAsDouble()==0&&continuesIn(preset,quarter^1,e))assertFalse(faces.has("west"));
                    if(v==0&&b.get(2).getAsDouble()==16&&continuesIn(preset,quarter^2,e))assertFalse(faces.has("south"));if(v==1&&a.get(2).getAsDouble()==0&&continuesIn(preset,quarter^2,e))assertFalse(faces.has("north"));
                    for(var face:faces.entrySet())for(var coordinate:face.getValue().getAsJsonObject().getAsJsonArray("uv"))assertTrue(coordinate.getAsDouble()>=0&&coordinate.getAsDouble()<=16);
                    if(e.get("shade").getAsBoolean())silhouette.append(a).append(b);
                }
            }presets.add(silhouette.toString());
        }assertEquals(6,presets.size());
    }
    @Test void largeFormationsUseConnectedNarrowColumnsInsteadOfPanels()throws Exception{
        for(int preset=0;preset<6;preset++){
            var bounds=new HashMap<Integer,double[]>();
            for(int quarter=0;quarter<4;quarter++)for(var value:model("template_mega_"+preset+"_"+quarter).getAsJsonArray("elements")){
                var e=value.getAsJsonObject();if(!e.get("shade").getAsBoolean())continue;
                int id=e.getAsJsonObject("faces").entrySet().iterator().next().getValue().getAsJsonObject().get("tintindex").getAsInt();
                var box=bounds.computeIfAbsent(id,k->new double[]{99,99,99,-99,-99,-99});
                for(int axis=0;axis<3;axis++){double shift=axis==0?(quarter%2)*16:axis==2?(quarter/2)*16:0;box[axis]=Math.min(box[axis],e.getAsJsonArray("from").get(axis).getAsDouble()+shift);box[axis+3]=Math.max(box[axis+3],e.getAsJsonArray("to").get(axis).getAsDouble()+shift);}
            }
            var prisms=new ArrayList<>(bounds.values());var reached=new HashSet<Integer>();reached.add(0);
            for(var box:prisms){assertTrue(box[3]-box[0]<=9);assertTrue(box[5]-box[2]<=9);}
            for(int pass=0;pass<prisms.size();pass++)for(int a=0;a<prisms.size();a++)if(reached.contains(a))for(int b=0;b<prisms.size();b++){
                var x=prisms.get(a);var y=prisms.get(b);boolean touch=true;for(int axis=0;axis<3;axis++)touch&=Math.max(x[axis],y[axis])<=Math.min(x[axis+3],y[axis+3])+1e-6;if(touch)reached.add(b);
            }assertEquals(prisms.size(),reached.size(),"No isolated growths in large preset "+preset);
        }
    }
    private boolean continuesIn(int preset,int quarter,JsonObject prism)throws Exception{
        var id=prism.getAsJsonObject("faces").entrySet().iterator().next().getValue().getAsJsonObject().get("tintindex");
        for(var value:model("template_mega_"+preset+"_"+quarter).getAsJsonArray("elements")){
            var other=value.getAsJsonObject();if(!other.get("shade").equals(prism.get("shade")))continue;
            for(var face:other.getAsJsonObject("faces").entrySet())if(face.getValue().getAsJsonObject().get("tintindex").equals(id))return true;
        }return false;
    }
    private double luminance(int rgb){return .2126*((rgb>>16)&255)+.7152*((rgb>>8)&255)+.0722*(rgb&255);}
}
