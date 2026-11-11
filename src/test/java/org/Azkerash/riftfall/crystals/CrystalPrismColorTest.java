package org.Azkerash.riftfall.crystals;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalPrismColorTest {
    @Test void interiorNeverChangesAndEdgesUseOnlyExactNeighborColors(){
        var neighbors=new CrystalTier[]{CrystalTier.WHITE,CrystalTier.ORANGE,CrystalTier.WHITE,CrystalTier.ORANGE,CrystalTier.WHITE,CrystalTier.ORANGE};
        boolean own=false,other=false;
        for(int piece=0;piece<240;piece++){
            assertEquals(CrystalTier.RED,CrystalPrismColor.choose(CrystalTier.RED,neighbors,new double[]{.3,.3,.3,.7,.7,.7},123,piece));
            var selected=CrystalPrismColor.choose(CrystalTier.RED,neighbors,new double[]{0,0,0,1,1,1},123,piece);
            assertTrue(selected==CrystalTier.RED||selected==CrystalTier.WHITE||selected==CrystalTier.ORANGE);
            assertEquals(selected,CrystalPrismColor.choose(CrystalTier.RED,neighbors,new double[]{0,0,0,1,1,1},123,piece));
            own|=selected==CrystalTier.RED;other|=selected!=CrystalTier.RED;
        }
        assertTrue(own&&other);
    }
}
