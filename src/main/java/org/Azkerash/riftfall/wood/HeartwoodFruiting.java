package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.systems.genfeature.*;
import com.dtteam.dynamictrees.systems.genfeature.context.*;
import com.dtteam.dynamictrees.systems.nodemapper.FindEndsNode;
import com.dtteam.dynamictrees.api.network.MapSignal;
import com.dtteam.dynamictrees.tree.TreeHelper;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;

/** DT discovers the same branch endpoints during world generation and later live growth. */
public final class HeartwoodFruiting extends GenFeature {
    public HeartwoodFruiting(){super(ResourceLocation.fromNamespaceAndPath("riftfall","heartwood_fruiting"));}
    @Override protected void registerProperties(){}
    public static boolean attach(LevelAccessor level,BlockPos root,BlockPos tip,int stage){
        var branch=level.getBlockState(tip);
        if(tip.getY()<root.getY()+6||!(branch.getBlock() instanceof LegacyBranchBlock wood)||wood.getRadius(branch)>2)return false;
        int connections=0;
        for(Direction side:Direction.values())if(level.getBlockState(tip.relative(side)).getBlock() instanceof LegacyBranchBlock)connections++;
        if(connections!=1)return false;
        // Keep the leaf as the attachment surface; the organ hangs beneath it.
        Direction[] sides={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
        int offset=Math.floorMod(tip.hashCode(),sides.length);
        for(int i=0;i<sides.length;i++){
            var leaf=tip.relative(sides[(i+offset)%sides.length]);
            if(!HeartwoodFruitBlock.isHeartwoodLeaf(level.getBlockState(leaf)))continue;
            var pos=leaf.below();var existing=level.getBlockState(pos);
            if((!existing.isAir()&&!HeartwoodFruitBlock.isHeartwoodLeaf(existing))||!existing.getFluidState().isEmpty())continue;
            if(!level.setBlock(pos,LegacyWood.FRUIT_BLOCK.get().defaultBlockState().setValue(HeartwoodFruitBlock.STAGE,stage),3))continue;
            if(level.getBlockEntity(pos) instanceof HeartwoodFruitEntity fruit)fruit.setRoot(root);
            return true;
        }
        return false;
    }

    @Override protected boolean postGenerate(GenFeatureConfiguration config,PostGenerationContext context){
        if(TreeHelper.getRadius(context.level(),context.pos().above())<16)return false;
        int planted=0;
        for(var tip:context.endPoints())if(context.random().nextInt(4)==0&&attach(context.level(),context.pos(),tip,context.random().nextInt(4))&&++planted>=24)break;
        return planted>0;
    }
    @Override protected boolean postGrow(GenFeatureConfiguration config,PostGrowContext context){
        if(TreeHelper.getRadius(context.level(),context.treePos())<16||context.random().nextInt(4)!=0)return false;
        var ends=new FindEndsNode();TreeHelper.startAnalysisFromRoot(context.level(),context.pos(),new MapSignal(ends));
        var tips=ends.getEnds();if(tips.isEmpty())return false;boolean planted=false;
        for(int attempt=0;attempt<3;attempt++)planted|=attach(context.level(),context.pos(),tips.get(context.random().nextInt(tips.size())),0);
        return planted;
    }
}
