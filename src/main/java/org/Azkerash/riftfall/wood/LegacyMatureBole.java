package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.systems.genfeature.*;
import com.dtteam.dynamictrees.systems.genfeature.context.*;
import com.dtteam.dynamictrees.tree.TreeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;

/** Mature Legacy trees retain their three-block lower bole beneath the spreading scaffold. */
public final class LegacyMatureBole extends GenFeature {
    public LegacyMatureBole(){super(ResourceLocation.fromNamespaceAndPath("riftfall","legacy_mature_bole"));}
    @Override protected void registerProperties(){}
    @Override protected boolean postGenerate(GenFeatureConfiguration config,PostGenerationContext context){
        return thicken(context.level(),context.pos().above());
    }
    @Override protected boolean postGrow(GenFeatureConfiguration config,PostGrowContext context){
        return thicken(context.level(),context.treePos());
    }
    private boolean thicken(LevelAccessor level,BlockPos base){
        // Leave seedlings and slender juveniles to ordinary DT growth.
        if(TreeHelper.getRadius(level,base)<16||!(level.getBlockState(base.above(12)).getBlock() instanceof LegacyBranchBlock))return false;
        for(int height=0;height<8;height++){
            var pos=base.above(height);var state=level.getBlockState(pos);
            if(!(state.getBlock() instanceof LegacyBranchBlock branch))break;
            int radius=height<5?24:28-height;
            if(branch.getRadius(state)<radius)branch.setRadius(level,pos,radius,null,3);
        }
        return true;
    }
}
