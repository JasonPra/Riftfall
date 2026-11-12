package org.Azkerash.riftfall.client;

import java.util.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Resolve coplanar seed-bed overlap once per chunk rebuild, without moving any surfaces. */
public final class CrystalBedModel extends BakedModelWrapper<BakedModel> {
    private final Direction growth;
    private final List<List<BakedQuad>> buckets=new ArrayList<>();
    public record OccupiedBox(double x0,double y0,double z0,double x1,double y1,double z1){
        double min(int axis){return axis==0?x0:axis==1?y0:z0;}
        double max(int axis){return axis==0?x1:axis==1?y1:z1;}
    }
    private record Plane(int axis,long coordinate,Direction normal){}
    private record Rect(double x0,double y0,double x1,double y1){}
    private static final double EPS=1e-6;
    public CrystalBedModel(BakedModel model,BlockState state,long seed){
        super(model);growth=state==null?Direction.UP:state.getValue(org.Azkerash.riftfall.crystals.ManaCrystalBlock.FACING);
        for(int i=0;i<7;i++)buckets.add(model.getQuads(state,i==6?null:Direction.values()[i],RandomSource.create(seed),ModelData.EMPTY,null));
    }
    public static void clean(List<CrystalBedModel> models){clean(models,List.of());}
    public static void clean(List<CrystalBedModel> models,List<OccupiedBox> contacts){
        CrystalPrismPacking.pack(models.stream().map(m->m.buckets).toList(),models.stream().map(m->m.growth).toList());
        var volumes=new ArrayList<>(contacts);
        for(var model:models)for(var bucket:model.buckets)for(var quad:bucket){
            if(!isBed(quad)||quad.getDirection()!=model.growth)continue;
            var min=new double[]{99,99,99};var max=new double[]{-99,-99,-99};var data=quad.getVertices();int stride=data.length/4;
            for(int vertex=0;vertex<4;vertex++)for(int axis=0;axis<3;axis++){double value=position(data,vertex,axis,stride);min[axis]=Math.min(min[axis],value);max[axis]=Math.max(max[axis],value);}
            int axis=model.growth.getAxis().ordinal();boolean positive=model.growth.getStepX()+model.growth.getStepY()+model.growth.getStepZ()>0;
            if(positive)min[axis]=0;else max[axis]=1;
            volumes.add(new OccupiedBox(min[0],min[1],min[2],max[0],max[1],max[2]));
        }
        var occupied=new HashMap<Plane,List<Rect>>();
        for(var model:models)for(int bucket=0;bucket<7;bucket++){
            var output=new ArrayList<BakedQuad>();
            for(var quad:model.buckets.get(bucket)){
                if((quad.getTintIndex()&65536)==0){output.add(quad);continue;}
                int axis=quad.getDirection().getAxis().ordinal(),u=(axis+1)%3,v=(axis+2)%3;
                var vertices=quad.getVertices();int stride=vertices.length/4;double plane=position(vertices,0,axis,stride);
                boolean flat=true;double x0=Double.POSITIVE_INFINITY,y0=x0,x1=Double.NEGATIVE_INFINITY,y1=x1;
                for(int i=0;i<4;i++){
                    flat&=Math.abs(position(vertices,i,axis,stride)-plane)<EPS;
                    x0=Math.min(x0,position(vertices,i,u,stride));x1=Math.max(x1,position(vertices,i,u,stride));
                    y0=Math.min(y0,position(vertices,i,v,stride));y1=Math.max(y1,position(vertices,i,v,stride));
                }
                if(!flat||x1-x0<EPS||y1-y0<EPS){output.add(quad);continue;}
                var rect=new Rect(x0,y0,x1,y1);var key=new Plane(axis,Math.round(plane/EPS),quad.getDirection());
                var previous=occupied.computeIfAbsent(key,k->new ArrayList<>());List<Rect> remaining=new ArrayList<>(List.of(rect));
                boolean positive=quad.getDirection().getStepX()+quad.getDirection().getStepY()+quad.getDirection().getStepZ()>0;
                for(var volume:volumes){
                    if(volume.min(axis)>plane+EPS||volume.max(axis)<plane-EPS)continue;
                    if(positive?volume.max(axis)<=plane+EPS:volume.min(axis)>=plane-EPS)continue;
                    var next=new ArrayList<Rect>();var cover=new Rect(volume.min(u),volume.min(v),volume.max(u),volume.max(v));
                    for(var piece:remaining)subtract(piece,cover,next);remaining=next;if(remaining.isEmpty())break;
                }
                for(var cover:previous){var next=new ArrayList<Rect>();for(var piece:remaining)subtract(piece,cover,next);remaining=next;if(remaining.isEmpty())break;}
                for(var piece:remaining)output.add(piece.equals(rect)?quad:fragment(quad,rect,piece,u,v));
                previous.addAll(remaining);
            }
            model.buckets.set(bucket,List.copyOf(output));
        }
    }
    public static boolean isBed(BakedQuad quad){return (quad.getTintIndex()&65536)!=0;}
    public static List<BakedQuad> layer(List<BakedQuad> quads,boolean bed){return quads.stream().filter(q->isBed(q)==bed).toList();}
    private static void subtract(Rect a,Rect b,List<Rect> out){
        double x0=Math.max(a.x0,b.x0),y0=Math.max(a.y0,b.y0),x1=Math.min(a.x1,b.x1),y1=Math.min(a.y1,b.y1);
        if(x1-x0<EPS||y1-y0<EPS){out.add(a);return;}
        add(out,a.x0,a.y0,x0,a.y1);add(out,x1,a.y0,a.x1,a.y1);
        add(out,x0,a.y0,x1,y0);add(out,x0,y1,x1,a.y1);
    }
    private static void add(List<Rect> out,double x0,double y0,double x1,double y1){if(x1-x0>EPS&&y1-y0>EPS)out.add(new Rect(x0,y0,x1,y1));}
    private static double position(int[] data,int vertex,int axis,int stride){return Float.intBitsToFloat(data[vertex*stride+axis]);}
    private static BakedQuad fragment(BakedQuad quad,Rect source,Rect piece,int u,int v){
        var input=quad.getVertices();var data=input.clone();int stride=input.length/4;
        int across=-1,along=-1;
        for(int i=1;i<4;i++){
            if(Math.abs(position(input,i,v,stride)-position(input,0,v,stride))<EPS)across=i;
            if(Math.abs(position(input,i,u,stride)-position(input,0,u,stride))<EPS)along=i;
        }
        for(int i=0;i<4;i++){
            double x=position(input,i,u,stride)<(source.x0+source.x1)/2?piece.x0:piece.x1;
            double y=position(input,i,v,stride)<(source.y0+source.y1)/2?piece.y0:piece.y1;
            data[i*stride+u]=Float.floatToRawIntBits((float)x);data[i*stride+v]=Float.floatToRawIntBits((float)y);
            double fx=(x-position(input,0,u,stride))/(position(input,across,u,stride)-position(input,0,u,stride));
            double fy=(y-position(input,0,v,stride))/(position(input,along,v,stride)-position(input,0,v,stride));
            for(int uv=4;uv<=5;uv++){
                double base=Float.intBitsToFloat(input[uv]);
                double value=base+fx*(Float.intBitsToFloat(input[across*stride+uv])-base)+fy*(Float.intBitsToFloat(input[along*stride+uv])-base);
                data[i*stride+uv]=Float.floatToRawIntBits((float)value);
            }
        }
        return new BakedQuad(data,quad.getTintIndex(),quad.getDirection(),quad.getSprite(),quad.isShade(),quad.hasAmbientOcclusion());
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type){return buckets.get(side==null?6:side.ordinal());}
}
