package org.Azkerash.riftfall.wood;

import java.util.ArrayDeque;
import java.util.HashSet;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.tree.TreeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Loaded, root-connected time only; disconnected wood cannot seal its wounds. */
public final class HeartwoodWoundHealing {
    public static final int COMPLETE_TICKS=72_000;
    private static final int[] STARTS={0,6_000,12_000,24_000,48_000,56_400,64_800,COMPLETE_TICKS};
    private static final String[] NAMES={"Exposed wood","Sap gathering","Sap coating","Red-amber resin","Bark closing (early)","Bark closing (middle)","Bark closing (late)","Healed"};
    public static int stage(int ticks){int stage=0;while(stage+1<STARTS.length&&ticks>=STARTS[stage+1])stage++;return stage;}
    public static String name(int ticks){return NAMES[stage(ticks)];}
    public static boolean hasLivingRoot(ServerLevel level,BlockPos start){return findLivingRoot(level,start)!=null;}
    public static BlockPos findLivingRoot(ServerLevel level,BlockPos start){
        if(!level.getChunkSource().hasChunk(start.getX()>>4,start.getZ()>>4)||!(level.getBlockState(start).getBlock() instanceof LegacyBranchBlock source))return null;
        var family=source.getFamily();var queue=new ArrayDeque<BlockPos>();var seen=new HashSet<BlockPos>();queue.add(start.immutable());
        while(!queue.isEmpty()&&seen.size()<8192){
            var pos=queue.removeFirst();if(!seen.add(pos)||!level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4))continue;
            var state=level.getBlockState(pos);
            if(TreeHelper.isRooty(state)){
                var species=TreeHelper.getExactSpecies(level,pos);
                if(species.isValid()&&species.getFamily()==family)return pos.immutable();
                continue;
            }
            if(!(state.getBlock() instanceof BranchBlock branch)||branch.getFamily()!=family)continue;
            for(Direction side:Direction.values()){
                var next=pos.relative(side);
                if(!seen.contains(next)&&level.getChunkSource().hasChunk(next.getX()>>4,next.getZ()>>4))queue.addLast(next);
            }
        }
        return null;
    }
    private HeartwoodWoundHealing(){}
}
