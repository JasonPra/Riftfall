package org.Azkerash.riftfall.crystals;

import net.minecraft.core.Direction;

/** Geometry reach rules, independent of blocks, world access and rendering. */
final class CrystalSurfaceConnections {
    static boolean edge(Direction facing,Direction seam,Direction other,boolean otherAdult){
        if(seam.getAxis()==facing.getAxis())throw new IllegalArgumentException("An edge must be tangent to its surface");
        if(other==facing)return true;
        if(other.getAxis()==facing.getAxis())return false;
        // A perpendicular base at the shared edge needs no extra reach. If its exposed end
        // faces the seam, however, that crystal must span its full voxel to reach it.
        return other.getAxis()!=seam.getAxis()||other==seam.getOpposite()&&otherAdult;
    }
    static boolean tip(Direction facing,boolean adult,Direction other){return adult&&other.getAxis()!=facing.getAxis();}
    private CrystalSurfaceConnections(){}
}
