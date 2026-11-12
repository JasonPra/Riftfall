package org.Azkerash.riftfall.client;

import java.util.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
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
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.*;
import org.Azkerash.riftfall.Riftfall;
import org.Azkerash.riftfall.crystals.*;

/** Native NeoForge model data composes growth on all solid contacts without more block-state properties. */
@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class ManaCrystalModels {
    private record Surface(BlockState state,BakedModel model){}
    private record Mesh(List<Surface> surfaces,BlockPos pos,CrystalTier tier,boolean mixed,boolean adjacent,Map<Surface,Map<Integer,double[]>> bounds,Map<Surface,CrystalTier[]> colors){}
    private static final ModelProperty<Mesh> SURFACES=new ModelProperty<>();
    @SubscribeEvent public static void baked(ModelEvent.ModifyBakingResult event){
        var registry=event.getModels();
        var neutral=event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/white_mana_crystal")));
        var bedNeutral=event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/white_mana_crystal_bed")));
        var tipNeutral=event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/white_mana_crystal_tip")));
        var tipCapNeutral=event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"block/white_mana_crystal_tip_cap")));
        for(var tier:CrystalTier.values()){
            var block=ManaCrystals.block(tier);Surface[][][] parts=new Surface[6][8][16];
            for(var face:Direction.values())for(int layers=1;layers<=8;layers++)for(int joins=0;joins<16;joins++){
                var state=block.defaultBlockState().setValue(ManaCrystalBlock.FACING,face).setValue(ManaCrystalBlock.LAYERS,layers);
                for(int i=0;i<4;i++)state=state.setValue(ManaCrystalBlock.JOINS.get(i),(joins&(1<<i))!=0);
                var raw=registry.get(BlockModelShaper.stateToModelLocation(state));
                if(raw==null)throw new IllegalStateException("Missing mana crystal surface model: "+state);
                parts[face.ordinal()][layers-1][joins]=new Surface(state,raw);
            }
            var wrapper=new SurfaceModel(parts,neutral,bedNeutral,tipNeutral,tipCapNeutral);
            for(var state:block.getStateDefinition().getPossibleStates())registry.put(BlockModelShaper.stateToModelLocation(state),wrapper);
        }
    }
    private static final class SurfaceModel extends BakedModelWrapper<BakedModel> {
        private final Surface[][][] parts;
        private final TextureAtlasSprite neutral,bedNeutral,tipNeutral,tipCapNeutral;
        SurfaceModel(Surface[][][] parts,TextureAtlasSprite neutral,TextureAtlasSprite bedNeutral,TextureAtlasSprite tipNeutral,TextureAtlasSprite tipCapNeutral){super(parts[Direction.UP.ordinal()][0][0].model());this.parts=parts;this.neutral=neutral;this.bedNeutral=bedNeutral;this.tipNeutral=tipNeutral;this.tipCapNeutral=tipCapNeutral;}
        @Override public ChunkRenderTypeSet getRenderTypes(BlockState state,RandomSource random,ModelData data){return ChunkRenderTypeSet.of(RenderType.solid(),RenderType.translucent());}
        @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData incoming){
            int mask=ManaCrystalBlock.surfaceMask(level,pos,state),layers=state.getValue(ManaCrystalBlock.LAYERS);var primary=state.getValue(ManaCrystalBlock.FACING);
            var surfaces=new ArrayList<Surface>(Integer.bitCount(mask));
            for(var face:Direction.values())if(CrystalSurfaceConnections.includes(mask,face)){
                int growth=face==primary?layers:(layers+1)/2;
                var part=parts[face.ordinal()][growth-1][ManaCrystalBlock.surfaceJoins(level,pos,face)];
                if(layers==8){
                    var profile=CrystalAdultDensity.profile(face,offset->{var p=pos.offset(offset.x(),offset.y(),offset.z());var other=level.getBlockState(p);return other.is(state.getBlock())&&ManaCrystalBlock.adult(other)&&CrystalSurfaceConnections.includes(ManaCrystalBlock.surfaceMask(level,p,other),face);});
                    if(profile.neighbors()>0)part=new Surface(part.state(),new CrystalDenseModel(part.model(),part.state(),face,profile,state.getSeed(pos)));
                }
                surfaces.add(part);
            }
            var beds=new ArrayList<CrystalBedModel>();
            for(int i=0;i<surfaces.size();i++){
                var part=surfaces.get(i);var bed=new CrystalBedModel(part.model(),part.state(),state.getSeed(pos));
                beds.add(bed);surfaces.set(i,new Surface(part.state(),bed));
            }
            var contacts=new ArrayList<CrystalBedModel.OccupiedBox>();
            for(var side:Direction.values()){
                var adjacent=pos.relative(side);var other=level.getBlockState(adjacent);if(!(other.getBlock() instanceof ManaCrystalBlock))continue;
                int common=mask&ManaCrystalBlock.surfaceMask(level,adjacent,other);
                for(var face:Direction.values())if(face.getAxis()!=side.getAxis()&&CrystalSurfaceConnections.includes(common,face)){
                    int growth=other.getValue(ManaCrystalBlock.FACING)==face?other.getValue(ManaCrystalBlock.LAYERS):(other.getValue(ManaCrystalBlock.LAYERS)+1)/2;
                    double height=Math.min(1.2,growth*.6)/16;double[] min={side.getStepX(),side.getStepY(),side.getStepZ()},max={min[0]+1,min[1]+1,min[2]+1};
                    int axis=face.getAxis().ordinal();if(face.getStepX()+face.getStepY()+face.getStepZ()>0)max[axis]=min[axis]+height;else min[axis]=max[axis]-height;
                    contacts.add(new CrystalBedModel.OccupiedBox(min[0],min[1],min[2],max[0],max[1],max[2]));
                }
            }
            CrystalBedModel.clean(beds,contacts);
            var tier=((ManaCrystalBlock)state.getBlock()).tier();boolean mixed=false,adjacentCrystal=false;
            var surfaceColors=new HashMap<Surface,CrystalTier[]>();
            for(var part:surfaces){
                var colors=CrystalSurfaceColors.forFace(part.state().getValue(ManaCrystalBlock.FACING),offset->{
                    var adjacent=pos.offset(offset.x(),offset.y(),offset.z());var other=level.getBlockState(adjacent);
                    return other.getBlock() instanceof ManaCrystalBlock crystal?new CrystalSurfaceColors.Contact(crystal.tier(),ManaCrystalBlock.surfaceMask(level,adjacent,other)):null;
                });
                surfaceColors.put(part,colors);
                for(var color:colors)if(color!=null){adjacentCrystal=true;mixed|=color!=tier;}
            }
            var bounds=new HashMap<Surface,Map<Integer,double[]>>();
            if(mixed)for(var part:surfaces)for(int bucket=0;bucket<7;bucket++)for(var quad:part.model().getQuads(part.state(),bucket==6?null:Direction.values()[bucket],RandomSource.create(state.getSeed(pos)),ModelData.EMPTY,null)){
                var box=bounds.computeIfAbsent(part,key->new HashMap<>()).computeIfAbsent(quad.getTintIndex(),id->new double[]{1,1,1,0,0,0});var vertices=quad.getVertices();int stride=vertices.length/4;
                for(int v=0;v<4;v++)for(int axis=0;axis<3;axis++){double value=Float.intBitsToFloat(vertices[v*stride+axis]);box[axis]=Math.min(box[axis],value);box[axis+3]=Math.max(box[axis+3],value);}
            }
            return incoming.derive().with(SURFACES,new Mesh(List.copyOf(surfaces),pos.immutable(),tier,mixed,adjacentCrystal,bounds,surfaceColors)).build();
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType renderType){
            var mesh=data.get(SURFACES);
            if(mesh==null){
                if(state==null)return layer(originalModel.getQuads(null,side,random,data,null),renderType);
                int joins=0;for(int i=0;i<4;i++)if(state.getValue(ManaCrystalBlock.JOINS.get(i)))joins|=1<<i;
                var part=parts[state.getValue(ManaCrystalBlock.FACING).ordinal()][state.getValue(ManaCrystalBlock.LAYERS)-1][joins];
                return layer(part.model().getQuads(part.state(),side,random,ModelData.EMPTY,null),renderType);
            }
            if(!mesh.adjacent()&&mesh.surfaces().size()==1){var part=mesh.surfaces().getFirst();return layer(part.model().getQuads(part.state(),side,random,ModelData.EMPTY,null),renderType);}
            var quads=new ArrayList<BakedQuad>();
            for(var part:mesh.surfaces())for(var quad:part.model().getQuads(part.state(),side,RandomSource.create(part.state().getSeed(mesh.pos())),ModelData.EMPTY,renderType)){
                if(renderType!=null&&CrystalBedModel.isBed(quad)!=(renderType==RenderType.solid()))continue;
                quads.add(mesh.mixed()?blend(quad,part,mesh,mesh.colors().get(part)):quad);
            }
            return quads;
        }
        private static List<BakedQuad> layer(List<BakedQuad> quads,RenderType type){return type==null?quads:CrystalBedModel.layer(quads,type==RenderType.solid());}
        private BakedQuad blend(BakedQuad quad,Surface part,Mesh mesh,CrystalTier[] colors){
            int[] vertices=quad.getVertices().clone();int stride=vertices.length/4;var sprite=quad.getSprite();
            boolean bed=CrystalBedModel.isBed(quad);var texture=sprite.contents().name().getPath();
            var targetSprite=bed?bedNeutral:texture.endsWith("_tip_cap")?tipCapNeutral:texture.endsWith("_tip")?tipNeutral:neutral;
            int prismColor=bed?0:CrystalPrismColor.choose(mesh.tier(),colors,mesh.bounds().get(part).get(quad.getTintIndex()),mesh.pos().asLong(),quad.getTintIndex()).color();
            for(int vertex=0;vertex<4;vertex++){
                int offset=vertex*stride;double x=Float.intBitsToFloat(vertices[offset]),y=Float.intBitsToFloat(vertices[offset+1]),z=Float.intBitsToFloat(vertices[offset+2]);
                int color=bed
                    ?CrystalEdgeBlend.color(mesh.tier(),colors,x,y,z,mesh.pos().getX()+x,mesh.pos().getY()+y,mesh.pos().getZ()+z)
                    :prismColor;
                int r=(int)Math.round(((color>>16)&255)*255.0/216),g=(int)Math.round(((color>>8)&255)*255.0/223),b=(int)Math.round((color&255)*255.0/232);
                vertices[offset+3]=0xff000000|Math.clamp(r,0,255)|Math.clamp(g,0,255)<<8|Math.clamp(b,0,255)<<16;
                vertices[offset+4]=Float.floatToRawIntBits(targetSprite.getU(sprite.getUOffset(Float.intBitsToFloat(vertices[offset+4]))));
                vertices[offset+5]=Float.floatToRawIntBits(targetSprite.getV(sprite.getVOffset(Float.intBitsToFloat(vertices[offset+5]))));
            }
            return new BakedQuad(vertices,quad.getTintIndex(),quad.getDirection(),targetSprite,quad.isShade(),quad.hasAmbientOcclusion());
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){return getQuads(state,side,random,ModelData.EMPTY,null);}
    }
    private ManaCrystalModels(){}
}
