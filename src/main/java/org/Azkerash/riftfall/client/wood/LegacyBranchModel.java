package org.Azkerash.riftfall.client.wood;

import java.util.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.Azkerash.riftfall.wood.LegacyBranchBlock;
import org.Azkerash.riftfall.wood.LegacyTrees;

/** Keep DT's live branch geometry/connections, replacing bark UVs only on stripped faces. */
@EventBusSubscriber(modid="riftfall",value=Dist.CLIENT)
public final class LegacyBranchModel extends BakedModelWrapper<BakedModel> {
    public static final ModelProperty<Integer> FACES=new ModelProperty<>();
    public static final ModelProperty<Long> PATCHES=new ModelProperty<>();
    public static final ModelProperty<Integer> SECTIONS=new ModelProperty<>();
    private static final ResourceLocation BARK=ResourceLocation.fromNamespaceAndPath("riftfall","block/vermillion_legacy_log");
    public static final ModelProperty<org.Azkerash.riftfall.wood.LegacyBarkWound.Visual> HEALING=new ModelProperty<>();
    private final TextureAtlasSprite exposed;
    private final TextureAtlasSprite[] stages,glows;
    private final Map<BakedQuad,BakedQuad> exposedQuads=Collections.synchronizedMap(new IdentityHashMap<>());
    public LegacyBranchModel(BakedModel original,TextureAtlasSprite[] stages,TextureAtlasSprite[] glows){super(original);this.exposed=stages[0];this.stages=stages;this.glows=glows;}
    @Override public net.neoforged.neoforge.client.ChunkRenderTypeSet getRenderTypes(BlockState state,RandomSource random,ModelData data){
        return net.neoforged.neoforge.client.ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    }

    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult event){
        var family=com.dtteam.dynamictrees.tree.family.Family.REGISTRY.get(LegacyTrees.ID);
        var block=((org.Azkerash.riftfall.wood.LegacyTreeFamily)family).getWoundedBranch();
        var sprite=event.getTextureGetter().apply(new Material(InventoryMenu.BLOCK_ATLAS,
            ResourceLocation.fromNamespaceAndPath("riftfall","block/stripped_vermillion_legacy_log")));
        var stages=new TextureAtlasSprite[7];var glows=new TextureAtlasSprite[7];stages[0]=sprite;
        String[] names={"sap_1","sap_2","resin","closing_1","closing_2","closing_3"};
        for(int i=0;i<names.length;i++)stages[i+1]=event.getTextureGetter().apply(new Material(InventoryMenu.BLOCK_ATLAS,ResourceLocation.fromNamespaceAndPath("riftfall","block/heartwood_wound_"+names[i])));
        for(int i=1;i<=2;i++)glows[i]=event.getTextureGetter().apply(new Material(InventoryMenu.BLOCK_ATLAS,ResourceLocation.fromNamespaceAndPath("riftfall","block/heartwood_wound_sap_"+i+"_glow")));
        var wrappers=new IdentityHashMap<BakedModel,LegacyBranchModel>();
        for(var state:block.getStateDefinition().getPossibleStates())
            event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state),(key,model)->
                wrappers.computeIfAbsent(model,original->new LegacyBranchModel(original,stages,glows)));
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type){
        var quads=originalModel.getQuads(state,side,random,data,null);
        if(!data.has(PATCHES))return faces(quads,data.has(FACES)?data.get(FACES):0);
        long patches=data.get(PATCHES);if(patches==0)return quads;
        var result=new ArrayList<BakedQuad>();
        for(var quad:quads){
            if(quad.getSprite().contents().name().equals(BARK)){
                int radius=((LegacyBranchBlock)state.getBlock()).getRadius(state),sections=data.has(SECTIONS)?data.get(SECTIONS):0;
                var visual=data.has(HEALING)?data.get(HEALING):org.Azkerash.riftfall.wood.LegacyBarkWound.Visual.EMPTY;
                result.addAll(LegacyBarkPatches.split(quad,patches,radius,sections,visual,stages,glows,false));
                result.addAll(LegacyBarkPatches.split(quad,patches,radius,sections,visual,stages,glows,true));
            }
            else result.add(quad);
        }
        return result;
    }
    @Deprecated
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){
        return originalModel.getQuads(state,side,random,ModelData.EMPTY,null);
    }
    @Override public ModelData getModelData(net.minecraft.world.level.BlockAndTintGetter world,net.minecraft.core.BlockPos pos,BlockState state,ModelData data){
        return originalModel.getModelData(world,pos,state,data).derive()
            .with(FACES,org.Azkerash.riftfall.wood.LegacyBarkWound.faces(world,pos))
            .with(PATCHES,org.Azkerash.riftfall.wood.LegacyBarkWound.patches(world,pos))
            .with(SECTIONS,org.Azkerash.riftfall.wood.LegacyBarkWound.sections(world,pos))
            .with(HEALING,org.Azkerash.riftfall.wood.LegacyBarkWound.visual(world,pos)).build();
    }
    private List<BakedQuad> faces(List<BakedQuad> quads,int mask){
        if(mask==0)return quads;
        var result=new ArrayList<BakedQuad>(quads.size());
        for(var quad:quads)result.add((mask&LegacyBranchBlock.faceBit(quad.getDirection()))!=0&&quad.getSprite().contents().name().equals(BARK)
            ?exposedQuads.computeIfAbsent(quad,this::expose):quad);
        return result;
    }
    private BakedQuad expose(BakedQuad quad){
        int[] vertices=quad.getVertices().clone();int stride=vertices.length/4;var bark=quad.getSprite();
        for(int vertex=0;vertex<4;vertex++){
            int uv=vertex*stride+4;
            float u=(Float.intBitsToFloat(vertices[uv])-bark.getU0())/(bark.getU1()-bark.getU0());
            float v=(Float.intBitsToFloat(vertices[uv+1])-bark.getV0())/(bark.getV1()-bark.getV0());
            vertices[uv]=Float.floatToRawIntBits(exposed.getU0()+u*(exposed.getU1()-exposed.getU0()));
            vertices[uv+1]=Float.floatToRawIntBits(exposed.getV0()+v*(exposed.getV1()-exposed.getV0()));
        }
        return new BakedQuad(vertices,quad.getTintIndex(),quad.getDirection(),exposed,quad.isShade(),quad.hasAmbientOcclusion());
    }
}
