package org.Azkerash.riftfall.mixin;
import com.dtteam.dynamictrees.worldgen.JoCode;
import com.dtteam.dynamictrees.tree.species.Species;
import net.minecraft.core.*;
import net.minecraft.world.level.LevelAccessor;
import org.Azkerash.riftfall.wood.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Neighboring placeholder trees must not graft onto Heartwood across the biome boundary. */
@Mixin(JoCode.class)
public abstract class HeartwoodNetworkIsolationMixin {
 @Inject(method="setBlockForGeneration",at=@At("HEAD"),cancellable=true)
 private void riftfall$separateHeartwood(LevelAccessor level,Species species,BlockPos pos,Direction from,boolean careful,boolean roots,CallbackInfoReturnable<Boolean> cir){
  if(!(level instanceof net.minecraft.server.level.WorldGenRegion)||species.getRegistryName().equals(LegacyTrees.ID))return;
  if(level.getBlockState(pos).getBlock() instanceof LegacyBranchBlock){cir.setReturnValue(true);return;}
  for(Direction side:Direction.values())if(level.getBlockState(pos.relative(side)).getBlock() instanceof LegacyBranchBlock){cir.setReturnValue(true);return;}
 }
}
