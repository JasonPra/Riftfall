package org.Azkerash.riftfall.wood;

import java.util.*;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.dtteam.dynamictrees.tree.species.Species;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;

/** Read-only measurements of the connected living branch network; never advances growth. */
public final class LegacyTreeSurvey {
    public record Node(int x,int y,int z,int radius){}
    public record Snapshot(BlockPos root,Species species,List<Node> nodes,int tips,int height,int width,int depth,
                           int trunkRadius,int maxRadius,int fertility,float growthRate,boolean complete,long fingerprint){}
    public static BlockPos root(ServerLevel level,BlockPos target){
        target=TreeHelper.dereferenceTrunkShell(level,target);
        return TreeHelper.isRooty(level.getBlockState(target))?target:TreeHelper.findRootNode(level,target);
    }
    public static Snapshot measure(ServerLevel level,BlockPos root){
        if(!level.hasChunkAt(root)||!TreeHelper.isRooty(level.getBlockState(root)))throw new IllegalArgumentException("Look at a living trunk, or specify its soil coordinates.");
        Species species=TreeHelper.getExactSpecies(level,root);
        if(!species.isValid())throw new IllegalArgumentException("Could not identify this tree's species.");
        var family=species.getFamily();var todo=new ArrayDeque<BlockPos>();var visited=new HashSet<BlockPos>();
        var nodes=new ArrayList<Node>();todo.add(root.above());boolean complete=true;int tips=0,maxRadius=0,height=0,minX=0,maxX=0,minZ=0,maxZ=0;
        long fingerprint=0;
        while(!todo.isEmpty()){
            var pos=todo.removeFirst();if(!visited.add(pos)||pos.getY()<=root.getY())continue;
            if(!level.hasChunkAt(pos)){complete=false;continue;}
            var state=level.getBlockState(pos);
            if(!(state.getBlock() instanceof BranchBlock branch)||branch.getFamily()!=family)continue;
            if(nodes.size()>=32768){complete=false;break;}
            int radius=branch.getRadius(state),x=pos.getX()-root.getX(),y=pos.getY()-root.getY(),z=pos.getZ()-root.getZ();
            nodes.add(new Node(x,y,z,radius));maxRadius=Math.max(maxRadius,radius);height=Math.max(height,y);
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
            fingerprint^=org.Azkerash.riftfall.worldmana.geysers.GeyserLayout.mix(new BlockPos(x,y,z).asLong()^radius*7919L);
            int connections=0;
            for(Direction side:Direction.values()){
                var next=pos.relative(side);if(next.getY()<=root.getY())continue;
                if(!level.hasChunkAt(next)){complete=false;continue;}
                var other=level.getBlockState(next);
                if(other.getBlock() instanceof BranchBlock b&&b.getFamily()==family){connections++;if(!visited.contains(next))todo.add(next);}
            }
            if(connections<=1&&!pos.equals(root.above()))tips++;
        }
        if(nodes.isEmpty())throw new IllegalArgumentException("This soil has no connected living trunk.");
        int fertility=TreeHelper.getRooty(level.getBlockState(root)).getFertility(level.getBlockState(root),level,root);
        return new Snapshot(root.immutable(),species,List.copyOf(nodes),tips,height,maxX-minX+1,maxZ-minZ+1,
            TreeHelper.getRadius(level,root.above()),maxRadius,fertility,species.getGrowthRate(level,root),complete,fingerprint);
    }
    private LegacyTreeSurvey(){}
}
