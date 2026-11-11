package org.Azkerash.riftfall.client;

import java.util.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.*;
import org.Azkerash.riftfall.Riftfall;
import org.Azkerash.riftfall.crystals.*;

/** Native NeoForge model data composes growth on all solid contacts without more block-state properties. */
@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class ManaCrystalModels {
    private record Surface(BlockState state,BakedModel model){}
    private record Neighbor(CrystalTier tier,int surfaces,int layers,Direction primary){}
    private record Mesh(List<Surface> surfaces,BlockPos pos,CrystalTier tier,int faces,Neighbor[] neighbors,boolean mixed,boolean adjacent){}
    private static final ModelProperty<Mesh> SURFACES=new ModelProperty<>();
    private static ModelResourceLocation junction(CrystalTier tier,int corner){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/mana_crystals/"+tier.id()+"_junction_"+corner));}
    @SubscribeEvent public static void additional(ModelEvent.RegisterAdditional event){for(var tier:CrystalTier.values())for(int corner=0;corner<4;corner++)event.register(junction(tier,corner));}
    @SubscribeEvent public static void baked(ModelEvent.ModifyBakingResult event){
        var registry=event.getModels();
        var neutral=event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/white_mana_crystal")));
        for(var tier:CrystalTier.values()){
            var block=ManaCrystals.block(tier);Surface[][][] parts=new Surface[6][8][16];
            for(var face:Direction.values())for(int layers=1;layers<=8;layers++)for(int joins=0;joins<16;joins++){
                var state=block.defaultBlockState().setValue(ManaCrystalBlock.FACING,face).setValue(ManaCrystalBlock.LAYERS,layers);
                for(int i=0;i<4;i++)state=state.setValue(ManaCrystalBlock.JOINS.get(i),(joins&(1<<i))!=0);
                var raw=registry.get(BlockModelShaper.stateToModelLocation(state));
                if(raw==null)throw new IllegalStateException("Missing mana crystal surface model: "+state);
                parts[face.ordinal()][layers-1][joins]=new Surface(state,raw);
            }
            Surface[][] junctions=new Surface[6][4];
            for(int corner=0;corner<4;corner++){
                var raw=registry.get(junction(tier,corner));if(raw==null)throw new IllegalStateException("Missing crystal junction model");
                for(var face:Direction.values())junctions[face.ordinal()][corner]=new Surface(block.defaultBlockState().setValue(ManaCrystalBlock.FACING,face).setValue(ManaCrystalBlock.LAYERS,8),new JunctionModel(raw,face));
            }
            var wrapper=new SurfaceModel(parts,junctions,neutral);
            for(var state:block.getStateDefinition().getPossibleStates())registry.put(BlockModelShaper.stateToModelLocation(state),wrapper);
        }
    }
    private static final class JunctionModel extends BakedModelWrapper<BakedModel> {
        private final List<List<BakedQuad>> buckets=new ArrayList<>(Collections.nCopies(7,null));
        JunctionModel(BakedModel original,Direction face){super(original);
            for(int bucket=0;bucket<7;bucket++){
                Direction local=bucket==6?null:Direction.values()[bucket],world=local==null?null:CrystalSurfaceRotation.direction(local,face);
                var quads=new ArrayList<BakedQuad>();for(var quad:original.getQuads(null,local,RandomSource.create(0),ModelData.EMPTY,RenderType.translucent()))quads.add(CrystalSurfaceRotation.rotate(quad,face));buckets.set(world==null?6:world.ordinal(),List.copyOf(quads));
            }
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType renderType){return buckets.get(side==null?6:side.ordinal());}
    }
    private static final class SurfaceModel extends BakedModelWrapper<BakedModel> {
        private final Surface[][][] parts;
        private final TextureAtlasSprite neutral;
        private final Surface[][] junctions;
        SurfaceModel(Surface[][][] parts,Surface[][] junctions,TextureAtlasSprite neutral){super(parts[Direction.UP.ordinal()][0][0].model());this.parts=parts;this.junctions=junctions;this.neutral=neutral;}
        @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData incoming){
            int mask=ManaCrystalBlock.surfaceMask(level,pos,state),layers=state.getValue(ManaCrystalBlock.LAYERS);var primary=state.getValue(ManaCrystalBlock.FACING);
            var surfaces=new ArrayList<Surface>(Integer.bitCount(mask));
            for(var face:Direction.values())if(CrystalSurfaceConnections.includes(mask,face)){
                int growth=face==primary?layers:(layers+1)/2;
                surfaces.add(parts[face.ordinal()][growth-1][ManaCrystalBlock.surfaceJoins(level,pos,face)]);
            }
            if(layers==8){
                var cache=new HashMap<CrystalSurfaceConnections.Offset,Integer>();cache.put(new CrystalSurfaceConnections.Offset(0,0,0),mask);
                java.util.function.Function<CrystalSurfaceConnections.Offset,Integer> mature=offset->cache.computeIfAbsent(offset,key->{var adjacent=pos.offset(key.x(),key.y(),key.z());var other=level.getBlockState(adjacent);return ManaCrystalBlock.adult(other)?ManaCrystalBlock.surfaceMask(level,adjacent,other):0;});
                for(var face:Direction.values())if(CrystalSurfaceConnections.includes(mask,face)){
                    int corners=CrystalSurfaceConnections.junctionCorners(face,mature);for(int corner=0;corner<4;corner++)if((corners&(1<<corner))!=0)surfaces.add(junctions[face.ordinal()][corner]);
                }
            }
            var tier=((ManaCrystalBlock)state.getBlock()).tier();var neighbors=new Neighbor[6];boolean mixed=false,adjacentCrystal=false;
            for(var direction:Direction.values()){
                var adjacent=pos.relative(direction);var other=level.getBlockState(adjacent);
                if(other.getBlock() instanceof ManaCrystalBlock crystal){adjacentCrystal=true;neighbors[direction.ordinal()]=new Neighbor(crystal.tier(),ManaCrystalBlock.surfaceMask(level,adjacent,other),other.getValue(ManaCrystalBlock.LAYERS),other.getValue(ManaCrystalBlock.FACING));mixed|=crystal.tier()!=tier;}
            }
            return incoming.derive().with(SURFACES,new Mesh(List.copyOf(surfaces),pos.immutable(),tier,mask,neighbors,mixed,adjacentCrystal)).build();
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType renderType){
            var mesh=data.get(SURFACES);
            if(mesh==null){
                if(state==null)return CrystalQuadUnion.clean(originalModel.getQuads(null,side,random,data,renderType),q->0);
                int joins=0;for(int i=0;i<4;i++)if(state.getValue(ManaCrystalBlock.JOINS.get(i)))joins|=1<<i;
                var part=parts[state.getValue(ManaCrystalBlock.FACING).ordinal()][state.getValue(ManaCrystalBlock.LAYERS)-1][joins];
                return CrystalQuadUnion.clean(part.model().getQuads(part.state(),side,random,ModelData.EMPTY,renderType),q->((ManaCrystalBlock)state.getBlock()).tier().ordinal());
            }
            var quads=new ArrayList<BakedQuad>();
            CrystalTier[] colors=mesh.mixed()?new CrystalTier[6]:null;if(colors!=null)for(int i=0;i<6;i++)if(mesh.neighbors()[i]!=null)colors[i]=mesh.neighbors()[i].tier();
            for(var part:mesh.surfaces())for(var quad:part.model().getQuads(part.state(),side,random,ModelData.EMPTY,renderType)){
                if(side!=null&&coveredRoot(quad,side,mesh))continue;
                quads.add(quad);
            }
            var visible=CrystalQuadUnion.clean(quads,quad->edgePriority(quad,mesh));
            if(!mesh.mixed())return visible;
            var blended=new ArrayList<BakedQuad>(visible.size());for(var quad:visible)blended.add(blend(quad,mesh,colors));return blended;
        }
        /** Higher-tier material wins competing coplanar edge pieces; never biases unrelated depth. */
        private int edgePriority(BakedQuad quad,Mesh mesh){
            int priority=mesh.tier().ordinal();double x=0,y=0,z=0;var vertices=quad.getVertices();int stride=vertices.length/4;
            for(int vertex=0;vertex<4;vertex++){x+=Float.intBitsToFloat(vertices[vertex*stride])*.25;y+=Float.intBitsToFloat(vertices[vertex*stride+1])*.25;z+=Float.intBitsToFloat(vertices[vertex*stride+2])*.25;}
            for(var direction:Direction.values()){var neighbor=mesh.neighbors()[direction.ordinal()];if(neighbor!=null&&distance(direction,x,y,z)<CrystalEdgeBlend.WIDTH)priority=Math.max(priority,neighbor.tier().ordinal());}
            return priority;
        }
        /** Hide only boundary faces demonstrably covered by the neighbor's continuous mineral root. */
        private boolean coveredRoot(BakedQuad quad,Direction side,Mesh mesh){
            var neighbor=mesh.neighbors()[side.ordinal()];if(neighbor==null)return false;
            int common=mesh.faces()&neighbor.surfaces();var vertices=quad.getVertices();int stride=vertices.length/4;
            for(var surface:Direction.values())if(surface.getAxis()!=side.getAxis()&&CrystalSurfaceConnections.includes(common,surface)){
                int growth=neighbor.primary()==surface?neighbor.layers():(neighbor.layers()+1)/2;double thickness=Math.min(1.2,growth*.6)/16.0;boolean covered=true;
                for(int vertex=0;vertex<4;vertex++){
                    double x=Float.intBitsToFloat(vertices[vertex*stride]),y=Float.intBitsToFloat(vertices[vertex*stride+1]),z=Float.intBitsToFloat(vertices[vertex*stride+2]);
                    double boundary=distance(side,x,y,z),root=distance(surface.getOpposite(),x,y,z);
                    if(Math.abs(boundary)>1e-5||root< -1e-5||root>thickness+1e-5){covered=false;break;}
                }if(covered)return true;
            }return false;
        }
        private static double distance(Direction side,double x,double y,double z){return switch(side){case DOWN->y;case UP->1-y;case NORTH->z;case SOUTH->1-z;case WEST->x;case EAST->1-x;};}
        private BakedQuad blend(BakedQuad quad,Mesh mesh,CrystalTier[] colors){
            int[] vertices=quad.getVertices().clone();int stride=vertices.length/4;var sprite=quad.getSprite();
            for(int vertex=0;vertex<4;vertex++){
                int offset=vertex*stride;double x=Float.intBitsToFloat(vertices[offset]),y=Float.intBitsToFloat(vertices[offset+1]),z=Float.intBitsToFloat(vertices[offset+2]);
                int color=CrystalEdgeBlend.color(mesh.tier(),colors,x,y,z,mesh.pos().getX()+x,mesh.pos().getY()+y,mesh.pos().getZ()+z);
                int r=(int)Math.round(((color>>16)&255)*255.0/216),g=(int)Math.round(((color>>8)&255)*255.0/223),b=(int)Math.round((color&255)*255.0/232);
                vertices[offset+3]=0xff000000|Math.clamp(r,0,255)|Math.clamp(g,0,255)<<8|Math.clamp(b,0,255)<<16;
                vertices[offset+4]=Float.floatToRawIntBits(neutral.getU(sprite.getUOffset(Float.intBitsToFloat(vertices[offset+4]))));
                vertices[offset+5]=Float.floatToRawIntBits(neutral.getV(sprite.getVOffset(Float.intBitsToFloat(vertices[offset+5]))));
            }
            return new BakedQuad(vertices,quad.getTintIndex(),quad.getDirection(),neutral,quad.isShade(),quad.hasAmbientOcclusion());
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){return getQuads(state,side,random,ModelData.EMPTY,null);}
    }
    private ManaCrystalModels(){}
}
