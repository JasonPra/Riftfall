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
    private record Mesh(List<Surface> surfaces,BlockPos pos,CrystalTier tier,int faces,Neighbor[] neighbors,Map<Integer,CrystalTier> materials,List<BakedQuad> occluders){}
    private static final ModelProperty<Mesh> SURFACES=new ModelProperty<>();
    private static ModelResourceLocation junction(CrystalTier tier,int corner){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/mana_crystals/"+tier.id()+"_junction_"+corner));}
    private static ModelResourceLocation edge(CrystalTier tier,int layer,int side){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/mana_crystals/"+tier.id()+"_"+layer+"_join_"+side));}
    private static ModelResourceLocation mega(CrystalTier tier,int preset,int quarter){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/mana_crystals/"+tier.id()+"_mega_"+preset+"_"+quarter));}
    @SubscribeEvent public static void additional(ModelEvent.RegisterAdditional event){for(var tier:CrystalTier.values()){for(int corner=0;corner<4;corner++)event.register(junction(tier,corner));for(int layer=1;layer<=8;layer++)for(int side=0;side<4;side++)event.register(edge(tier,layer,side));for(int preset=0;preset<6;preset++)for(int quarter=0;quarter<4;quarter++)event.register(mega(tier,preset,quarter));}}
    @SubscribeEvent public static void baked(ModelEvent.ModifyBakingResult event){
        var registry=event.getModels();
        TextureAtlasSprite[][] textures=new TextureAtlasSprite[8][3];
        for(var tier:CrystalTier.values())for(int layer=0;layer<3;layer++)textures[tier.ordinal()][layer]=event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/"+tier.id()+"_mana_crystal"+(layer==1?"_core":layer==2?"_large":""))));
        for(var tier:CrystalTier.values()){
            var block=ManaCrystals.block(tier);
            BakedModel[][][] joinsModels=new BakedModel[6][8][4];
            for(var face:Direction.values())for(int layer=1;layer<=8;layer++)for(int side=0;side<4;side++)joinsModels[face.ordinal()][layer-1][side]=new JunctionModel(registry.get(edge(tier,layer,side)),face);
            Surface[][][][] parts=new Surface[6][6][8][16];
            for(int preset=0;preset<6;preset++)for(var face:Direction.values())for(int layers=1;layers<=8;layers++)for(int joins=0;joins<16;joins++){
                var state=block.defaultBlockState().setValue(ManaCrystalBlock.VARIANT,preset).setValue(ManaCrystalBlock.FACING,face).setValue(ManaCrystalBlock.LAYERS,layers);
                for(int i=0;i<4;i++)state=state.setValue(ManaCrystalBlock.JOINS.get(i),(joins&(1<<i))!=0);
                var raw=registry.get(BlockModelShaper.stateToModelLocation(state));
                if(raw==null)throw new IllegalStateException("Missing mana crystal surface model: "+state);
                var joined=new ArrayList<BakedModel>();for(int i=0;i<4;i++)if((joins&(1<<i))!=0)joined.add(joinsModels[face.ordinal()][layers-1][i]);
                parts[preset][face.ordinal()][layers-1][joins]=new Surface(state,joined.isEmpty()?raw:new JoinedModel(raw,List.copyOf(joined)));
            }
            Surface[][] junctions=new Surface[6][4];
            for(int corner=0;corner<4;corner++){
                var raw=registry.get(junction(tier,corner));if(raw==null)throw new IllegalStateException("Missing crystal junction model");
                for(var face:Direction.values())junctions[face.ordinal()][corner]=new Surface(block.defaultBlockState().setValue(ManaCrystalBlock.FACING,face).setValue(ManaCrystalBlock.LAYERS,8),new JunctionModel(raw,face));
            }
            Surface[][][][] corners=new Surface[6][8][4][2];
            for(var face:Direction.values())for(int layer=1;layer<=8;layer++)for(int side=0;side<4;side++)for(int wrap=0;wrap<2;wrap++){
                var edgeState=block.defaultBlockState().setValue(ManaCrystalBlock.FACING,face).setValue(ManaCrystalBlock.LAYERS,layer);
                corners[face.ordinal()][layer-1][side][wrap]=new Surface(edgeState,new SharedEdgeModel(registry.get(edge(tier,layer,side)),face,CrystalSurfaceConnections.tangent(face,side),wrap==1));
            }
            BakedModel[][][] megaModels=new BakedModel[6][6][4];
            for(var face:Direction.values())for(int preset=0;preset<6;preset++)for(int quarter=0;quarter<4;quarter++)megaModels[face.ordinal()][preset][quarter]=new JunctionModel(registry.get(mega(tier,preset,quarter)),face);
            var wrapper=new SurfaceModel(parts,junctions,corners,megaModels,joinsModels,textures);
            for(var state:block.getStateDefinition().getPossibleStates())registry.put(BlockModelShaper.stateToModelLocation(state),wrapper);
        }
    }
    /** Compose cached edge meshes directly instead of loading every connection as a multipart selector. */
    private static final class JoinedModel extends BakedModelWrapper<BakedModel>{
        private final List<BakedModel> edges;
        JoinedModel(BakedModel body,List<BakedModel> edges){super(body);this.edges=edges;}
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type){
            var result=new ArrayList<BakedQuad>(originalModel.getQuads(state,side,random,data,type));for(var edge:edges)result.addAll(edge.getQuads(state,side,random,data,type));return result;
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
    /** One endpoint owns these small overlapping prisms, avoiding duplicate translucent faces. */
    private static final class SharedEdgeModel extends BakedModelWrapper<BakedModel>{
        private final List<BakedQuad> quads;
        SharedEdgeModel(BakedModel raw,Direction face,Direction side,boolean wrap){super(raw);var result=new ArrayList<BakedQuad>();
            for(int bucket=0;bucket<7;bucket++)for(var q:raw.getQuads(null,bucket==6?null:Direction.values()[bucket],RandomSource.create(0),ModelData.EMPTY,RenderType.translucent())){
                if(q.getTintIndex()<0||(q.getTintIndex()&1048576)==0)continue;
                var rotated=CrystalSurfaceRotation.rotate(q,face);var data=rotated.getVertices().clone();int stride=data.length/4;
                double[] shift={(side.getStepX()-(wrap?face.getStepX():0))/16.0,(side.getStepY()-(wrap?face.getStepY():0))/16.0,(side.getStepZ()-(wrap?face.getStepZ():0))/16.0};
                for(int v=0;v<4;v++)for(int axis=0;axis<3;axis++)data[v*stride+axis]=Float.floatToRawIntBits((float)(Float.intBitsToFloat(data[v*stride+axis])+shift[axis]));
                result.add(new BakedQuad(data,q.getTintIndex()|2097152,rotated.getDirection(),q.getSprite(),q.isShade(),q.hasAmbientOcclusion()));
            }quads=List.copyOf(result);
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type){return side==null?quads:List.of();}
    }
    private static final class SurfaceModel extends BakedModelWrapper<BakedModel> {
        private final Surface[][][][] parts;
        private final TextureAtlasSprite[][] textures;
        private final Surface[][] junctions;
        private final Surface[][][][] corners;
        private final BakedModel[][][] megaModels,edgeModels;
        SurfaceModel(Surface[][][][] parts,Surface[][] junctions,Surface[][][][] corners,BakedModel[][][] megaModels,BakedModel[][][] edgeModels,TextureAtlasSprite[][] textures){super(parts[0][Direction.UP.ordinal()][0][0].model());this.parts=parts;this.junctions=junctions;this.corners=corners;this.megaModels=megaModels;this.edgeModels=edgeModels;this.textures=textures;}
        @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData incoming){
            int mask=ManaCrystalBlock.surfaceMask(level,pos,state);var surfaces=surfaces(level,pos,state);
            var occluders=new ArrayList<BakedQuad>();
            // Lower positions own overlaps. Reconstruct their geometry without recursively asking for model data.
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){
                if(dx==0&&dy==0&&dz==0)continue;
                var neighborPos=pos.offset(dx,dy,dz);var neighbor=level.getBlockState(neighborPos);
                if(neighbor.isSolidRender(level,neighborPos)){
                    for(var face:Direction.values()){var solid=CrystalNeighborFaces.solidFace(face,dx,dy,dz);if(solid!=null)occluders.add(solid);}continue;
                }
                if(pos.compareTo(neighborPos)<=0||!(neighbor.getBlock() instanceof ManaCrystalBlock))continue;
                for(var surface:sharedSurfaces(level,neighborPos,neighbor))for(int bucket=0;bucket<7;bucket++){
                    for(var quad:surface.model().getQuads(surface.state(),bucket==6?null:Direction.values()[bucket],RandomSource.create(neighbor.getSeed(neighborPos)),ModelData.EMPTY,RenderType.translucent())){
                        var clipped=CrystalNeighborFaces.mask(quad,dx,dy,dz);if(clipped!=null)occluders.add(clipped);
                    }
                }
            }
            var tier=((ManaCrystalBlock)state.getBlock()).tier();var neighbors=new Neighbor[6];var palette=new CrystalTier[6];boolean mixed=false;
            for(var direction:Direction.values()){
                var adjacent=pos.relative(direction);var other=level.getBlockState(adjacent);
                if(other.getBlock() instanceof ManaCrystalBlock crystal){neighbors[direction.ordinal()]=new Neighbor(crystal.tier(),ManaCrystalBlock.surfaceMask(level,adjacent,other),other.getValue(ManaCrystalBlock.LAYERS),other.getValue(ManaCrystalBlock.FACING));palette[direction.ordinal()]=crystal.tier();mixed|=palette[direction.ordinal()]!=null&&crystal.tier()!=tier;}

            }
            // A convex stone corner puts the other crystal diagonally across the edge.
            for(var face:Direction.values())if(CrystalSurfaceConnections.includes(mask,face))for(int i=0;i<4;i++){
                var side=CrystalSurfaceConnections.tangent(face,i);if(palette[side.ordinal()]!=null)continue;
                var around=pos.relative(side).relative(face.getOpposite());var other=level.getBlockState(around);
                if(other.getBlock() instanceof ManaCrystalBlock crystal&&CrystalSurfaceConnections.includes(ManaCrystalBlock.surfaceMask(level,around,other),side)){
                    palette[side.ordinal()]=crystal.tier();mixed|=crystal.tier()!=tier;
                }
            }
            var materials=mixed?fragmentMaterials(surfaces,state.getSeed(pos),pos,tier,palette):Map.<Integer,CrystalTier>of();
            return incoming.derive().with(SURFACES,new Mesh(List.copyOf(surfaces),pos.immutable(),tier,mask,neighbors,materials,List.copyOf(occluders))).build();
        }
        private List<Surface> surfaces(BlockAndTintGetter level,BlockPos pos,BlockState state){
            int mask=ManaCrystalBlock.surfaceMask(level,pos,state),layers=state.getValue(ManaCrystalBlock.LAYERS);var primary=state.getValue(ManaCrystalBlock.FACING);
            var group=CrystalLargeGroups.find(pos,primary,p->{var other=level.getBlockState(p);return other.getBlock() instanceof ManaCrystalBlock crystal?new CrystalLargeGroups.Member(crystal.tier(),other.getValue(ManaCrystalBlock.LAYERS),other.getValue(ManaCrystalBlock.FACING),other.getValue(ManaCrystalBlock.VARIANT)):null;});
            var surfaces=new ArrayList<Surface>(Integer.bitCount(mask));
            for(var face:Direction.values())if(CrystalSurfaceConnections.includes(mask,face)){
                int growth=face==primary?layers:(layers+1)/2;
                int joins=edgeJoins(level,pos,face);
                if(group!=null&&face==primary){
                    var outside=new ArrayList<BakedModel>();
                    for(int i=0;i<4;i++){
                        boolean internal=switch(i){case 0->group.v()==1;case 1->group.u()==0;case 2->group.v()==0;default->group.u()==1;};
                        if(!internal&&(joins&(1<<i))!=0)outside.add(edgeModels[face.ordinal()][7][i]);
                    }
                    var model=megaModels[face.ordinal()][group.preset()][group.quarter()];
                    surfaces.add(new Surface(state,outside.isEmpty()?model:new JoinedModel(model,List.copyOf(outside))));
                }else surfaces.add(parts[state.getValue(ManaCrystalBlock.VARIANT)][face.ordinal()][growth-1][joins]);
            }
            surfaces.addAll(sharedSurfaces(level,pos,state));
            if(layers==8){
                var cache=new HashMap<CrystalSurfaceConnections.Offset,Integer>();cache.put(new CrystalSurfaceConnections.Offset(0,0,0),mask);
                java.util.function.Function<CrystalSurfaceConnections.Offset,Integer> mature=offset->cache.computeIfAbsent(offset,key->{var adjacent=pos.offset(key.x(),key.y(),key.z());var other=level.getBlockState(adjacent);return ManaCrystalBlock.adult(other)?ManaCrystalBlock.surfaceMask(level,adjacent,other):0;});
                for(var face:Direction.values())if(CrystalSurfaceConnections.includes(mask,face)&&(group==null||face!=primary)){
                    int corners=CrystalSurfaceConnections.junctionCorners(face,mature);for(int corner=0;corner<4;corner++)if((corners&(1<<corner))!=0)surfaces.add(junctions[face.ordinal()][corner]);
                }
            }
            return surfaces;
        }
        private List<Surface> sharedSurfaces(BlockAndTintGetter level,BlockPos pos,BlockState state){
            var surfaces=new ArrayList<Surface>();var primary=state.getValue(ManaCrystalBlock.FACING);int layers=state.getValue(ManaCrystalBlock.LAYERS);
            for(int i=0;i<4;i++){
                var side=CrystalSurfaceConnections.tangent(primary,i);var adjacent=pos.relative(side);var other=level.getBlockState(adjacent);
                if(other.getBlock() instanceof ManaCrystalBlock&&other.getValue(ManaCrystalBlock.FACING)!=primary&&pos.compareTo(adjacent)<0)surfaces.add(corners[primary.ordinal()][layers-1][i][0]);
                var around=adjacent.relative(primary.getOpposite());var wrapped=level.getBlockState(around);
                if(wrapped.getBlock() instanceof ManaCrystalBlock&&CrystalSurfaceConnections.includes(ManaCrystalBlock.surfaceMask(level,around,wrapped),side)&&pos.compareTo(around)<0)surfaces.add(corners[primary.ordinal()][layers-1][i][1]);
            }
            return surfaces;
        }
        /** Only actual neighboring crystals need seam buds; solid contacts alone need no rim. */
        private static int edgeJoins(BlockAndTintGetter level,BlockPos pos,Direction face){
            return CrystalSurfaceConnections.joins(face,0,offset->{var adjacent=pos.offset(offset.x(),offset.y(),offset.z());var other=level.getBlockState(adjacent);return other.getBlock() instanceof ManaCrystalBlock?ManaCrystalBlock.surfaceMask(level,adjacent,other):0;});
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
            // Each differently colored shared edge gets three visible daughter prisms,
            // with a stronger chance of further pieces. Never recolor the central columns or root.
            for(int side=0;side<6;side++){
                if(palette[side]==null||palette[side]==own)continue;
                var edgePalette=new CrystalTier[6];edgePalette[side]=palette[side];var candidates=new ArrayList<Integer>();
                for(var entry:bounds.entrySet()){
                    if(((entry.getKey()/6)&2097152)!=0)continue;
                    var b=entry.getValue();var box=new CrystalFragmentColors.Bounds(b.x0,b.y0,b.z0,b.x1,b.y1,b.z1);
                    if(CrystalFragmentColors.choose(own,edgePalette,box,0,1)!=own)candidates.add(entry.getKey());
                }
                candidates.sort(Integer::compare);if(candidates.isEmpty())continue;
                int first=Math.floorMod(pos.asLong()^((long)side*0x9e3779b97f4a7c15L),candidates.size());
                selected.put(candidates.get(first),palette[side]);
                int borrowed=1;
                for(int n=1;n<candidates.size()&&borrowed<5;n++){
                    int id=candidates.get((first+n)%candidates.size());if(selected.containsKey(id))continue;
                    var b=bounds.get(id);var box=new CrystalFragmentColors.Bounds(b.x0,b.y0,b.z0,b.x1,b.y1,b.z1);
                    if(borrowed<3||CrystalFragmentColors.choose(own,edgePalette,box,pos.asLong()^((long)id*0x9e3779b97f4a7c15L),.85)!=own){selected.put(id,palette[side]);borrowed++;}
                }
            }
            bounds.forEach((id,b)->{if(((id/6)&2097152)!=0){var chosen=CrystalFragmentColors.choose(own,palette,new CrystalFragmentColors.Bounds(b.x0,b.y0,b.z0,b.x1,b.y1,b.z1),pos.asLong()^((long)id*0x9e3779b97f4a7c15L),.5);if(chosen!=own)selected.put(id,chosen);}});
            return Map.copyOf(selected);
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType renderType){
            var mesh=data.get(SURFACES);
            if(mesh==null){
                if(state==null)return CrystalQuadUnion.clean(originalModel.getQuads(null,side,random,data,renderType),q->0);
                int joins=0;for(int i=0;i<4;i++)if(state.getValue(ManaCrystalBlock.JOINS.get(i)))joins|=1<<i;
                var part=parts[state.getValue(ManaCrystalBlock.VARIANT)][state.getValue(ManaCrystalBlock.FACING).ordinal()][state.getValue(ManaCrystalBlock.LAYERS)-1][joins];
                return CrystalQuadUnion.clean(part.model().getQuads(part.state(),side,random,ModelData.EMPTY,renderType),q->((ManaCrystalBlock)state.getBlock()).tier().ordinal());
            }
            var quads=new ArrayList<BakedQuad>();
            for(var part:mesh.surfaces())for(var raw:part.model().getQuads(part.state(),side,random,ModelData.EMPTY,renderType)){
                var quad=CrystalCoreLighting.core(raw)?CrystalCoreLighting.fullbright(raw):raw;
                quads.add(new BakedQuad(quad.getVertices(),fragment(part,quad),quad.getDirection(),quad.getSprite(),quad.isShade(),quad.hasAmbientOcclusion()));
            }
            if(quads.isEmpty())return List.of();
            quads.addAll(mesh.occluders());
            var visible=CrystalQuadUnion.clean(quads,quad->quad.getTintIndex()==CrystalNeighborFaces.MASK_ID?Integer.MAX_VALUE:edgePriority(quad,mesh));
            var result=new ArrayList<BakedQuad>(visible.size());for(var quad:visible)if(quad.getTintIndex()!=CrystalNeighborFaces.MASK_ID)result.add(material(quad,mesh));return result;
        }

        /** Higher-tier material wins competing coplanar edge pieces; never biases unrelated depth. */
        private int edgePriority(BakedQuad quad,Mesh mesh){return mesh.materials().getOrDefault(quad.getTintIndex(),mesh.tier()).ordinal()+(CrystalCoreLighting.core(quad)?16:0);}
        private BakedQuad material(BakedQuad quad,Mesh mesh){
            var selected=mesh.materials().getOrDefault(quad.getTintIndex(),mesh.tier());var target=textures[selected.ordinal()][CrystalCoreLighting.core(quad)?1:quad.getSprite().contents().name().getPath().endsWith("_mana_crystal_large")?2:0];var sprite=quad.getSprite();var data=quad.getVertices();
            if(sprite!=target){data=data.clone();int stride=data.length/4;for(int vertex=0;vertex<4;vertex++){int offset=vertex*stride;data[offset+4]=Float.floatToRawIntBits(target.getU(sprite.getUOffset(Float.intBitsToFloat(data[offset+4]))));data[offset+5]=Float.floatToRawIntBits(target.getV(sprite.getVOffset(Float.intBitsToFloat(data[offset+5]))));}}
            return new BakedQuad(data,-1,quad.getDirection(),target,quad.isShade(),quad.hasAmbientOcclusion());
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){return getQuads(state,side,random,ModelData.EMPTY,null);}
    }
    private ManaCrystalModels(){}
}
