package org.Azkerash.riftfall.crystals;

import java.util.function.Function;
import net.minecraft.core.Direction;

/** Surface topology without world access. Masks describe outward growth faces, not solid-block directions. */
public final class CrystalSurfaceConnections {
    public record Offset(int x,int y,int z){}
    public static int bit(Direction face){return 1<<face.ordinal();}
    public static boolean includes(int mask,Direction face){return (mask&bit(face))!=0;}
    public static Direction tangent(Direction facing,int index){
        if(index<0||index>3)throw new IllegalArgumentException("Invalid crystal tangent");
        if(facing==Direction.UP)return switch(index){case 0->Direction.NORTH;case 1->Direction.EAST;case 2->Direction.SOUTH;default->Direction.WEST;};
        if(facing==Direction.DOWN)return switch(index){case 0->Direction.SOUTH;case 1->Direction.EAST;case 2->Direction.NORTH;default->Direction.WEST;};
        if(index==0)return Direction.DOWN;if(index==2)return Direction.UP;
        Direction right=switch(facing){case NORTH->Direction.EAST;case EAST->Direction.SOUTH;case SOUTH->Direction.WEST;case WEST->Direction.NORTH;default->throw new IllegalStateException();};
        return index==1?right:right.getOpposite();
    }
    public static int joins(Direction face,Function<Offset,Integer> neighboringCrystalSurfaces){return joins(face,0,neighboringCrystalSurfaces);}
    public static int joins(Direction face,int ownSurfaces,Function<Offset,Integer> neighboringCrystalSurfaces){
        int mask=0;
        for(int i=0;i<4;i++){
            var side=tangent(face,i);
            var direct=new Offset(side.getStepX(),side.getStepY(),side.getStepZ());
            var around=new Offset(side.getStepX()-face.getStepX(),side.getStepY()-face.getStepY(),side.getStepZ()-face.getStepZ());
            if(includes(ownSurfaces,side.getOpposite())||includes(neighboringCrystalSurfaces.apply(direct),face)||includes(neighboringCrystalSurfaces.apply(around),side))mask|=1<<i;
        }return mask;
    }
    /** Corners of a complete pair of mature 2x2 patches growing toward each other. */
    public static int junctionCorners(Direction face,Function<Offset,Integer> matureSurfaces){
        int result=0;
        for(int corner=0;corner<4;corner++){
            var a=tangent(face,corner);var b=tangent(face,(corner+1)%4);boolean complete=true;
            for(int u=0;u<=1&&complete;u++)for(int v=0;v<=1&&complete;v++)for(int front=0;front<=1;front++){
                var p=new Offset(a.getStepX()*u+b.getStepX()*v+face.getStepX()*front,a.getStepY()*u+b.getStepY()*v+face.getStepY()*front,a.getStepZ()*u+b.getStepZ()*v+face.getStepZ()*front);
                if(!includes(matureSurfaces.apply(p),front==0?face:face.getOpposite())){complete=false;break;}
            }
            if(complete)result|=1<<corner;
        }return result;
    }
    private CrystalSurfaceConnections(){}
}
