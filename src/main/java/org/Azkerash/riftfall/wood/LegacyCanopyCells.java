package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.api.cell.*;
import com.dtteam.dynamictrees.api.voxmap.SimpleVoxmap;
import com.dtteam.dynamictrees.systems.cell.NormalCell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Broad, supported leaf lobes let a few genuine forks carry a full rounded crown. */
public final class LegacyCanopyCells extends CellKit {
    private final Cell[] leaves=new Cell[8];
    private final Cell twig=new NormalCell(6);
    private final SimpleVoxmap cluster;
    public LegacyCanopyCells(){
        super(LegacyTrees.ID);
        for(int value=0;value<leaves.length;value++){
            final int hydration=value;
            leaves[value]=new NormalCell(value){
                @Override public int getValueFromSide(Direction side){
                    return Math.max(0,hydration-(side.getAxis()==Direction.Axis.Y?1:0));
                }
            };
        }
        byte[] voxels=new byte[11*7*11];
        for(int y=-3;y<=3;y++)for(int z=-5;z<=5;z++)for(int x=-5;x<=5;x++){
            int hydration=6-Math.abs(x)-Math.abs(z)-2*Math.abs(y)+(y==0?0:1);
            if(x==0&&y==0&&z==0)hydration=0;
            voxels[((y+3)*11+z+5)*11+x+5]=(byte)Math.max(0,hydration);
        }
        cluster=new SimpleVoxmap(11,7,11,voxels).setCenter(new BlockPos(5,3,5));
    }
    @Override public Cell getCellForLeaves(int hydration){return leaves[hydration];}
    @Override public Cell getCellForBranch(int radius,int metadata){return radius==1?twig:CellNull.NULL_CELL;}
    @Override public CellSolver getCellSolver(){return neighbors->{
        int strongest=0;
        for(Direction side:Direction.values())strongest=Math.max(strongest,neighbors[side.ordinal()].getValueFromSide(side.getOpposite()));
        return Math.max(0,strongest-1);
    };}
    @Override public SimpleVoxmap getLeafCluster(){return cluster;}
    @Override public int getDefaultHydration(){return 5;}
}
