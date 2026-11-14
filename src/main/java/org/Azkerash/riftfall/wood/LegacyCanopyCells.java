package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.api.cell.*;
import com.dtteam.dynamictrees.api.voxmap.SimpleVoxmap;
import com.dtteam.dynamictrees.systems.cell.NormalCell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Compact leaf clusters expose the branching structure instead of burying it in foliage. */
public final class LegacyCanopyCells extends CellKit {
    private final Cell[] leaves=new Cell[8];
    private final Cell twig=new NormalCell(4);
    private final SimpleVoxmap cluster;
    public LegacyCanopyCells(){
        super(LegacyTrees.ID);
        for(int value=0;value<leaves.length;value++){
            final int hydration=value;
            leaves[value]=new NormalCell(value){
                @Override public int getValueFromSide(Direction side){
                    return Math.max(0,hydration-(side.getAxis()==Direction.Axis.Y?2:0));
                }
            };
        }
        byte[] voxels=new byte[7*3*7];
        for(int y=-1;y<=1;y++)for(int z=-3;z<=3;z++)for(int x=-3;x<=3;x++){
            int hydration=4-Math.abs(x)-Math.abs(z)-Math.abs(y);
            if(x==0&&y==0&&z==0)hydration=0;
            voxels[((y+1)*7+z+3)*7+x+3]=(byte)Math.max(0,hydration);
        }
        cluster=new SimpleVoxmap(7,3,7,voxels).setCenter(new BlockPos(3,1,3));
    }
    @Override public Cell getCellForLeaves(int hydration){return leaves[hydration];}
    @Override public Cell getCellForBranch(int radius,int metadata){return radius==1?twig:CellNull.NULL_CELL;}
    @Override public CellSolver getCellSolver(){return neighbors->{
        int strongest=0;
        for(Direction side:Direction.values())strongest=Math.max(strongest,neighbors[side.ordinal()].getValueFromSide(side.getOpposite()));
        return Math.max(0,strongest-1);
    };}
    @Override public SimpleVoxmap getLeafCluster(){return cluster;}
    @Override public int getDefaultHydration(){return 3;}
}
