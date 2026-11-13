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
    private static final ResourceLocation BARK=ResourceLocation.fromNamespaceAndPath("riftfall","block/vermillion_legacy_log");
    private final TextureAtlasSprite exposed;
    private final Map<BakedQuad,BakedQuad> exposedQuads=Collections.synchronizedMap(new IdentityHashMap<>());
    public LegacyBranchModel(BakedModel original,TextureAtlasSprite exposed){super(original);this.exposed=exposed;}

    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult event){
        var family=com.dtteam.dynamictrees.tree.family.Family.REGISTRY.get(LegacyTrees.ID);
        var block=((org.Azkerash.riftfall.wood.LegacyTreeFamily)family).getWoundedBranch();
        var sprite=event.getTextureGetter().apply(new Material(InventoryMenu.BLOCK_ATLAS,
            ResourceLocation.fromNamespaceAndPath("riftfall","block/stripped_vermillion_legacy_log")));
        var wrappers=new IdentityHashMap<BakedModel,LegacyBranchModel>();
        for(var state:block.getStateDefinition().getPossibleStates())
            event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state),(key,model)->
                wrappers.computeIfAbsent(model,original->new LegacyBranchModel(original,sprite)));
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type){
        return faces(originalModel.getQuads(state,side,random,data,type),data.has(FACES)?data.get(FACES):0);
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){
        return originalModel.getQuads(state,side,random);
    }
    @Override public ModelData getModelData(net.minecraft.world.level.BlockAndTintGetter world,net.minecraft.core.BlockPos pos,BlockState state,ModelData data){
        return originalModel.getModelData(world,pos,state,data).derive()
            .with(FACES,org.Azkerash.riftfall.wood.LegacyBarkWound.faces(world,pos)).build();
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
