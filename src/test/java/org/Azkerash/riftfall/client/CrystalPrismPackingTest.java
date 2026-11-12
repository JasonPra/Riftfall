package org.Azkerash.riftfall.client;

import java.util.*;
import java.io.*;
import com.google.gson.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.Azkerash.riftfall.crystals.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.Azkerash.riftfall.client.CrystalPrismPacking.V;

class CrystalPrismPackingTest {
    private static V direction(Direction d){return new V(d.getStepX(),d.getStepY(),d.getStepZ());}
    private List<BakedQuad> template(int layer,String suffix,Direction growth)throws Exception{
        var path="/assets/riftfall/models/block/mana_crystals/template_"+layer+"_"+suffix+".json";
        JsonObject model;try(var in=getClass().getResourceAsStream(path);var reader=new InputStreamReader(in)){model=JsonParser.parseReader(reader).getAsJsonObject();}
        var result=new ArrayList<BakedQuad>();
        for(var value:model.getAsJsonArray("elements")){
            var element=value.getAsJsonObject();var from=element.getAsJsonArray("from");var to=element.getAsJsonArray("to");
            for(var entry:element.getAsJsonObject("faces").entrySet()){
                var face=entry.getValue().getAsJsonObject();int tint=face.get("tintindex").getAsInt();if((tint&65536)!=0)continue;
                var normal=Direction.valueOf(entry.getKey().toUpperCase(Locale.ROOT));int axis=normal.getAxis().ordinal(),u=(axis+1)%3,v=(axis+2)%3;
                boolean positive=normal.getStepX()+normal.getStepY()+normal.getStepZ()>0;int[][] corners={{0,0},{1,0},{1,1},{0,1}};int[] data=new int[32];
                for(int i=0;i<4;i++){
                    int j=positive?i:3-i;double[] p=new double[3];p[axis]=(positive?to:from).get(axis).getAsDouble();p[u]=(corners[j][0]==0?from:to).get(u).getAsDouble();p[v]=(corners[j][1]==0?from:to).get(v).getAsDouble();
                    if(element.has("rotation")){
                        var r=element.getAsJsonObject("rotation");var origin=r.getAsJsonArray("origin");int a=r.get("axis").getAsString().equals("x")?1:0,b=r.get("axis").getAsString().equals("x")?2:1;
                        double angle=Math.toRadians(r.get("angle").getAsDouble()),x=p[a]-origin.get(a).getAsDouble(),y=p[b]-origin.get(b).getAsDouble();
                        p[a]=origin.get(a).getAsDouble()+x*Math.cos(angle)-y*Math.sin(angle);p[b]=origin.get(b).getAsDouble()+x*Math.sin(angle)+y*Math.cos(angle);
                    }
                    var out=new V(.5,.5,.5).add(direction(CrystalSurfaceConnections.tangent(growth,1)).mul(p[0]/16-.5)).add(direction(growth).mul(p[1]/16-.5)).add(direction(CrystalSurfaceConnections.tangent(growth,2)).mul(p[2]/16-.5));
                    data[i*8]=Float.floatToRawIntBits((float)out.x());data[i*8+1]=Float.floatToRawIntBits((float)out.y());data[i*8+2]=Float.floatToRawIntBits((float)out.z());data[i*8+3]=-1;data[i*8+4]=Float.floatToRawIntBits(i/4f);data[i*8+5]=Float.floatToRawIntBits(i/5f);data[i*8+6]=1234;
                }
                result.add(new BakedQuad(data,tint,normal,null,true,true));
            }
        }return result;
    }
    private V vertex(BakedQuad quad,int i){var data=quad.getVertices();return new V(Float.intBitsToFloat(data[i*8]),Float.intBitsToFloat(data[i*8+1]),Float.intBitsToFloat(data[i*8+2]));}
    private List<List<BakedQuad>> buckets(List<BakedQuad> quads){var list=new ArrayList<List<BakedQuad>>();for(int i=0;i<6;i++)list.add(List.of());list.add(quads);return list;}
    private void assertPacked(List<List<List<BakedQuad>>> models,List<Direction> growth){
        var original=models.stream().map(m->m.get(6)).toList();
        CrystalPrismPacking.pack(models,growth);var boxes=new ArrayList<CrystalPrismPacking.Box>();
        for(int model=0;model<models.size();model++){
            var after=models.get(model).get(6);var before=original.get(model);assertEquals(before.size(),after.size(),"Every textured prism face is retained");
            var groups=new HashMap<Integer,List<BakedQuad>>();
            for(int q=0;q<after.size();q++){
                var a=before.get(q);var b=after.get(q);assertEquals(a.getTintIndex(),b.getTintIndex());
                var normalA=vertex(a,1).sub(vertex(a,0)).cross(vertex(a,3).sub(vertex(a,0)));
                var normalB=vertex(b,1).sub(vertex(b,0)).cross(vertex(b,3).sub(vertex(b,0)));
                assertTrue(normalA.dot(normalB)>0,"Fitting must not reverse face winding or collapse faces");
                for(int vertex=0;vertex<4;vertex++)for(int attribute=3;attribute<8;attribute++)assertEquals(a.getVertices()[vertex*8+attribute],b.getVertices()[vertex*8+attribute]);
                groups.computeIfAbsent(b.getTintIndex(),id->new ArrayList<>()).add(b);
            }
            for(var quads:groups.values()){assertEquals(6,quads.size());var box=CrystalPrismPacking.bounds(quads);assertNotNull(box);assertTrue(box.contained());for(var old:boxes)assertFalse(CrystalPrismPacking.penetrates(old,box),"Prisms must not grow through each other");boxes.add(box);}
        }
        var once=models.stream().map(m->m.get(6).stream().map(q->q.getVertices().clone()).toList()).toList();
        var repeat=new ArrayList<List<List<BakedQuad>>>();for(var old:original)repeat.add(buckets(old));CrystalPrismPacking.pack(repeat,growth);
        for(int m=0;m<repeat.size();m++)for(int q=0;q<repeat.get(m).get(6).size();q++)assertArrayEquals(once.get(m).get(q),repeat.get(m).get(6).get(q).getVertices(),"Layout is stable and camera independent");
    }
    @Test void actualStageAndConnectionTemplatesFitWithoutDeletingFaces()throws Exception{
        for(int layer=1;layer<=8;layer++)for(var growth:Direction.values())for(int mask:new int[]{0,1,3,5,15}){
            var quads=new ArrayList<>(template(layer,Integer.toString(layer%2),growth));
            for(int side=0;side<4;side++)if((mask&(1<<side))!=0)quads.addAll(template(layer,"join_"+side,growth));
            assertPacked(List.of(buckets(quads)),List.of(growth));
        }
    }
    @Test void denseAdultPrismsStillMeetWithoutPenetrating()throws Exception{
        for(var growth:Direction.values()){
            var quads=new ArrayList<>(template(8,"0",growth));for(int side=0;side<4;side++)quads.addAll(template(8,"join_"+side,growth));
            BakedModel raw=new BakedModelWrapper<BakedModel>(null){@Override public List<BakedQuad> getQuads(BlockState s,Direction side,RandomSource r,ModelData d,RenderType type){return side==null?quads:List.of();}};
            var dense=new CrystalDenseModel(raw,null,growth,CrystalAdultDensity.profile(growth,p->true),1);
            assertPacked(List.of(buckets(dense.getQuads(null,null,RandomSource.create(1),ModelData.EMPTY,null))),List.of(growth));
        }
    }
    @Test void fittedPrismFacesAreNotLeftInNeighborOcclusionBuckets()throws Exception{
        var quads=template(8,"0",Direction.UP);var model=new ArrayList<List<BakedQuad>>();
        for(int bucket=0;bucket<6;bucket++){int side=bucket;model.add(quads.stream().filter(q->q.getDirection().ordinal()==side).toList());}model.add(List.of());
        CrystalPrismPacking.pack(List.of(model),List.of(Direction.UP));
        for(int bucket=0;bucket<6;bucket++)assertTrue(model.get(bucket).isEmpty());
        assertEquals(quads.size(),model.get(6).size());
    }
    @Test void encasedCrystalsCanFitAllSixSupportedSurfaces()throws Exception{
        var models=new ArrayList<List<List<BakedQuad>>>();var growth=List.of(Direction.values());
        for(var face:growth){int stage=face==Direction.UP?8:4;var quads=new ArrayList<>(template(stage,"1",face));for(int side=0;side<4;side++)quads.addAll(template(stage,"join_"+side,face));models.add(buckets(quads));}
        assertPacked(models,growth);
    }
    @Test void multipleSupportedSurfacesShareSpaceAtCorners()throws Exception{
        for(int layer:new int[]{1,4,8}){
            var models=new ArrayList<List<List<BakedQuad>>>();var growth=List.of(Direction.UP,Direction.NORTH,Direction.EAST);
            for(var face:growth){var quads=new ArrayList<>(template(face==Direction.UP?layer:(layer+1)/2,"0",face));for(int side=0;side<4;side++)quads.addAll(template(face==Direction.UP?layer:(layer+1)/2,"join_"+side,face));models.add(buckets(quads));}
            assertPacked(models,growth);
        }
    }
}
