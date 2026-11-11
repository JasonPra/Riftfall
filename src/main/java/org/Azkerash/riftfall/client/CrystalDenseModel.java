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
import org.Azkerash.riftfall.crystals.*;

/** Widen existing whole prisms toward mature neighbors. Constructed only while rebuilding a chunk mesh. */
public final class CrystalDenseModel extends BakedModelWrapper<BakedModel> {
    private final List<List<BakedQuad>> buckets=new ArrayList<>();
    public CrystalDenseModel(BakedModel original,BlockState state,Direction face,CrystalAdultDensity.Profile profile,long seed){
        super(original);var x=CrystalSurfaceConnections.tangent(face,1);var z=CrystalSurfaceConnections.tangent(face,2);var bounds=new HashMap<Integer,double[]>();
        for(int bucket=0;bucket<7;bucket++){
            var quads=original.getQuads(state,bucket==6?null:Direction.values()[bucket],RandomSource.create(seed),ModelData.EMPTY,null);buckets.add(quads);
            for(var q:quads){var box=bounds.computeIfAbsent(q.getTintIndex(),key->new double[]{99,99,-99,-99});var data=q.getVertices();int stride=data.length/4;
                for(int v=0;v<4;v++){double a=Float.intBitsToFloat(data[v*stride])-.5,b=Float.intBitsToFloat(data[v*stride+1])-.5,c=Float.intBitsToFloat(data[v*stride+2])-.5;
                    double u=.5+a*x.getStepX()+b*x.getStepY()+c*x.getStepZ(),w=.5+a*z.getStepX()+b*z.getStepY()+c*z.getStepZ();box[0]=Math.min(box[0],u);box[1]=Math.min(box[1],w);box[2]=Math.max(box[2],u);box[3]=Math.max(box[3],w);
                }
            }
        }
        for(int bucket=0;bucket<7;bucket++){
            var output=new ArrayList<BakedQuad>();
            for(var q:buckets.get(bucket)){
                if((q.getTintIndex()&65536)!=0){output.add(q);continue;}
                var box=bounds.get(q.getTintIndex());var uFit=CrystalAdultDensity.fit(box[0],box[2],profile.xScale(),profile.xShift());var vFit=CrystalAdultDensity.fit(box[1],box[3],profile.zScale(),profile.zShift());var data=q.getVertices().clone();int stride=data.length/4;
                for(int v=0;v<4;v++){
                    double a=Float.intBitsToFloat(data[v*stride])-.5,b=Float.intBitsToFloat(data[v*stride+1])-.5,c=Float.intBitsToFloat(data[v*stride+2])-.5;
                    double u=.5+a*x.getStepX()+b*x.getStepY()+c*x.getStepZ(),w=.5+a*z.getStepX()+b*z.getStepY()+c*z.getStepZ(),h=.5+a*face.getStepX()+b*face.getStepY()+c*face.getStepZ();
                    u=uFit[0]+(u-box[0])/(box[2]-box[0])*(uFit[1]-uFit[0]);w=vFit[0]+(w-box[1])/(box[3]-box[1])*(vFit[1]-vFit[0]);
                    data[v*stride]=Float.floatToRawIntBits((float)(.5+(u-.5)*x.getStepX()+(h-.5)*face.getStepX()+(w-.5)*z.getStepX()));
                    data[v*stride+1]=Float.floatToRawIntBits((float)(.5+(u-.5)*x.getStepY()+(h-.5)*face.getStepY()+(w-.5)*z.getStepY()));
                    data[v*stride+2]=Float.floatToRawIntBits((float)(.5+(u-.5)*x.getStepZ()+(h-.5)*face.getStepZ()+(w-.5)*z.getStepZ()));
                }
                output.add(new BakedQuad(data,q.getTintIndex(),q.getDirection(),q.getSprite(),q.isShade(),q.hasAmbientOcclusion()));
            }buckets.set(bucket,List.copyOf(output));
        }
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type){return buckets.get(side==null?6:side.ordinal());}
}
