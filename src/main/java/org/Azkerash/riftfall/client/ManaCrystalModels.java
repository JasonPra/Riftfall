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
    private record Mesh(List<Surface> surfaces,BlockPos pos,CrystalTier tier,int faces,Neighbor[] neighbors,Map<Integer,CrystalTier> materials){}
    private static final ModelProperty<Mesh> SURFACES=new ModelProperty<>();
    private static ModelResourceLocation junction(CrystalTier tier,int corner){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/mana_crystals/"+tier.id()+"_junction_"+corner));}
    @SubscribeEvent public static void additional(ModelEvent.RegisterAdditional event){for(var tier:CrystalTier.values()){for(int corner=0;corner<4;corner++)event.register(junction(tier,corner));}}
    @SubscribeEvent public static void baked(ModelEvent.ModifyBakingResult event){
        var registry=event.getModels();
        TextureAtlasSprite[][] textures=new TextureAtlasSprite[8][2];
        for(var tier:CrystalTier.values())for(int layer=0;layer<2;layer++)textures[tier.ordinal()][layer]=event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/"+tier.id()+"_mana_crystal"+(layer==1?"_core":""))));
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
            var wrapper=new SurfaceModel(parts,junctions,textures);
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
        private final TextureAtlasSprite[][] textures;
        private final Surface[][] junctions;
        SurfaceModel(Surface[][][] parts,Surface[][] junctions,TextureAtlasSprite[][] textures){super(parts[Direction.UP.ordinal()][0][0].model());this.parts=parts;this.junctions=junctions;this.textures=textures;}
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
            var tier=((ManaCrystalBlock)state.getBlock()).tier();var neighbors=new Neighbor[6];var palette=new CrystalTier[6];boolean mixed=false;
            for(var direction:Direction.values()){
                var adjacent=pos.relative(direction);var other=level.getBlockState(adjacent);
                if(other.getBlock() instanceof ManaCrystalBlock crystal){neighbors[direction.ordinal()]=new Neighbor(crystal.tier(),ManaCrystalBlock.surfaceMask(level,adjacent,other),other.getValue(ManaCrystalBlock.LAYERS),other.getValue(ManaCrystalBlock.FACING));palette[direction.ordinal()]=(mask&neighbors[direction.ordinal()].surfaces())!=0?crystal.tier():null;mixed|=palette[direction.ordinal()]!=null&&crystal.tier()!=tier;}

            }
            var materials=mixed?fragmentMaterials(surfaces,state.getSeed(pos),pos,tier,palette):Map.<Integer,CrystalTier>of();
            return incoming.derive().with(SURFACES,new Mesh(List.copyOf(surfaces),pos.immutable(),tier,mask,neighbors,materials)).build();
        }
        private static int fragment(Surface surface,BakedQuad quad){return quad.getTintIndex()<0?-1:quad.getTintIndex()*6+surface.state().getValue(ManaCrystalBlock.FACING).ordinal();}
        private static final class Bounds {
            double x0=Double.POSITIVE_INFINITY,y0=x0,z0=x0,x1=Double.NEGATIVE_INFINITY,y1=x1,z1=x1;
            void add(BakedQuad quad){var data=quad.getVertices();int stride=data.length/4;for(int i=0;i<4;i++){double x=Float.intBitsToFloat(data[i*stride]),y=Float.intBitsToFloat(data[i*stride+1]),z=Float.intBitsToFloat(data[i*stride+2]);x0=Math.min(x0,x);y0=Math.min(y0,y);z0=Math.min(z0,z);x1=Math.max(x1,x);y1=Math.max(y1,y);z1=Math.max(z1,z);}}
        }
        private static Map<Integer,CrystalTier> fragmentMaterials(List<Surface> surfaces,long seed,BlockPos pos,CrystalTier own,CrystalTier[] palette){
            var bounds=new HashMap<Integer,Bounds>();
            for(int bucket=0;bucket<7;bucket++){
                var random=RandomSource.create(seed);Direction side=bucket==6?null:Direction.values()[bucket];
                for(var surface:surfaces)for(var quad:surface.model().getQuads(surface.state(),side,random,ModelData.EMPTY,RenderType.translucent())){int id=fragment(surface,quad);if(id>=0&&((id/6)&1048576)!=0)bounds.computeIfAbsent(id,key->new Bounds()).add(quad);}
            }
            var selected=new HashMap<Integer,CrystalTier>();
            // Each differently colored shared edge gets two visible daughter prisms,
            // with a stronger chance of a third. Never recolor the central columns or root.
            for(int side=0;side<6;side++){
                if(palette[side]==null||palette[side]==own)continue;
                var edgePalette=new CrystalTier[6];edgePalette[side]=palette[side];var candidates=new ArrayList<Integer>();
                for(var entry:bounds.entrySet()){
                    var b=entry.getValue();var box=new CrystalFragmentColors.Bounds(b.x0,b.y0,b.z0,b.x1,b.y1,b.z1);
                    if(CrystalFragmentColors.choose(own,edgePalette,box,0,1)!=own)candidates.add(entry.getKey());
                }
                candidates.sort(Integer::compare);if(candidates.isEmpty())continue;
                int first=Math.floorMod(pos.asLong()^((long)side*0x9e3779b97f4a7c15L),candidates.size());
                selected.put(candidates.get(first),palette[side]);
                int borrowed=1;
                for(int n=1;n<candidates.size()&&borrowed<3;n++){
                    int id=candidates.get((first+n)%candidates.size());if(selected.containsKey(id))continue;
                    var b=bounds.get(id);var box=new CrystalFragmentColors.Bounds(b.x0,b.y0,b.z0,b.x1,b.y1,b.z1);
                    if(borrowed<2||CrystalFragmentColors.choose(own,edgePalette,box,pos.asLong()^((long)id*0x9e3779b97f4a7c15L),.65)!=own){selected.put(id,palette[side]);borrowed++;}
                }
            }
            return Map.copyOf(selected);
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
            for(var part:mesh.surfaces())for(var raw:part.model().getQuads(part.state(),side,random,ModelData.EMPTY,renderType)){
                var quad=CrystalCoreLighting.core(raw)?CrystalCoreLighting.fullbright(raw):raw;
                quads.add(new BakedQuad(quad.getVertices(),fragment(part,quad),quad.getDirection(),quad.getSprite(),quad.isShade(),quad.hasAmbientOcclusion()));
            }
            var visible=CrystalQuadUnion.clean(quads,quad->edgePriority(quad,mesh));
            var result=new ArrayList<BakedQuad>(visible.size());for(var quad:visible)result.add(material(quad,mesh));return result;
        }

        /** Higher-tier material wins competing coplanar edge pieces; never biases unrelated depth. */
        private int edgePriority(BakedQuad quad,Mesh mesh){return mesh.materials().getOrDefault(quad.getTintIndex(),mesh.tier()).ordinal()+(CrystalCoreLighting.core(quad)?16:0);}
        private BakedQuad material(BakedQuad quad,Mesh mesh){
            var selected=mesh.materials().getOrDefault(quad.getTintIndex(),mesh.tier());var target=textures[selected.ordinal()][CrystalCoreLighting.core(quad)?1:0];var sprite=quad.getSprite();var data=quad.getVertices();
            if(sprite!=target){data=data.clone();int stride=data.length/4;for(int vertex=0;vertex<4;vertex++){int offset=vertex*stride;data[offset+4]=Float.floatToRawIntBits(target.getU(sprite.getUOffset(Float.intBitsToFloat(data[offset+4]))));data[offset+5]=Float.floatToRawIntBits(target.getV(sprite.getVOffset(Float.intBitsToFloat(data[offset+5]))));}}
            return new BakedQuad(data,-1,quad.getDirection(),target,quad.isShade(),quad.hasAmbientOcclusion());
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){return getQuads(state,side,random,ModelData.EMPTY,null);}
    }
    private ManaCrystalModels(){}
}
