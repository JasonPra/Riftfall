package org.Azkerash.riftfall.crystals;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalFragmentColorsTest {
    @Test void centralFragmentsKeepTheirOwnTier(){var colors=new CrystalTier[6];Arrays.fill(colors,CrystalTier.BLUE);var bounds=new CrystalFragmentColors.Bounds(.3,.3,.3,.7,.7,.7);for(int seed=0;seed<100;seed++)assertEquals(CrystalTier.GREEN,CrystalFragmentColors.choose(CrystalTier.GREEN,colors,bounds,seed));}
    @Test void edgeFragmentsChooseActualNeighborMaterialsWithNoInterpolatedColors(){
        var colors=new CrystalTier[6];colors[5]=CrystalTier.BLUE;var bounds=new CrystalFragmentColors.Bounds(.8,.3,.3,1,.7,.7);int inherited=0;
        for(int seed=0;seed<1000;seed++){var chosen=CrystalFragmentColors.choose(CrystalTier.GREEN,colors,bounds,seed);assertTrue(chosen==CrystalTier.GREEN||chosen==CrystalTier.BLUE);assertEquals(chosen,CrystalFragmentColors.choose(CrystalTier.GREEN,colors,bounds,seed));if(chosen==CrystalTier.BLUE)inherited++;}
        assertTrue(inherited>150&&inherited<350);
    }
    @Test void requiredEdgePieceAlwaysUsesTheAdjacentTier(){
        var colors=new CrystalTier[6];colors[5]=CrystalTier.WHITE;
        var edge=new CrystalFragmentColors.Bounds(.8,.3,.3,1,.5,.5);
        for(int seed=0;seed<100;seed++)assertEquals(CrystalTier.WHITE,CrystalFragmentColors.choose(CrystalTier.BLUE,colors,edge,seed,1));
        assertEquals(CrystalTier.BLUE,CrystalFragmentColors.choose(CrystalTier.BLUE,colors,new CrystalFragmentColors.Bounds(.3,.3,.3,.7,.7,.7),0,1));
    }
    @Test void missingAndMatchingNeighborsDoNotRecolorFragments(){var colors=new CrystalTier[6];var bounds=new CrystalFragmentColors.Bounds(0,0,0,1,1,1);for(int seed=0;seed<100;seed++)assertEquals(CrystalTier.RED,CrystalFragmentColors.choose(CrystalTier.RED,colors,bounds,seed));Arrays.fill(colors,CrystalTier.RED);for(int seed=0;seed<100;seed++)assertEquals(CrystalTier.RED,CrystalFragmentColors.choose(CrystalTier.RED,colors,bounds,seed));}
    @Test void inheritanceOnlyUsesFacesNearTheFragment(){var colors=new CrystalTier[6];colors[4]=CrystalTier.WHITE;colors[5]=CrystalTier.BLUE;var bounds=new CrystalFragmentColors.Bounds(.8,.3,.3,1,.7,.7);for(int seed=0;seed<100;seed++)assertNotEquals(CrystalTier.WHITE,CrystalFragmentColors.choose(CrystalTier.GREEN,colors,bounds,seed));}
}
