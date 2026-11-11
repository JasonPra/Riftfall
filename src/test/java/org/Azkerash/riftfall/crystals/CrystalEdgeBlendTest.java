package org.Azkerash.riftfall.crystals;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalEdgeBlendTest {
    @Test void centersAndUnconnectedEdgesKeepTheirTierColor(){
        var neighbors=new CrystalTier[6];assertEquals(CrystalTier.GREEN.color(),CrystalEdgeBlend.color(CrystalTier.GREEN,neighbors,0,0,0,0,0,0));
        neighbors[5]=CrystalTier.BLUE;assertEquals(CrystalTier.GREEN.color(),CrystalEdgeBlend.color(CrystalTier.GREEN,neighbors,.5,.5,.5,.5,.5,.5));
        assertEquals(CrystalTier.GREEN.color(),CrystalEdgeBlend.color(CrystalTier.GREEN,neighbors,1-CrystalEdgeBlend.WIDTH,.5,.5,.75,.5,.5));
        Arrays.fill(neighbors,CrystalTier.GREEN);assertEquals(CrystalTier.GREEN.color(),CrystalEdgeBlend.color(CrystalTier.GREEN,neighbors,0,0,0,0,0,0));
    }
    @Test void bothBlocksAgreeOnTheSameSharedSeamColor(){
        for(var own:CrystalTier.values())for(var other:CrystalTier.values())for(int side=0;side<6;side++){
            var a=new CrystalTier[6];a[side]=other;var b=new CrystalTier[6];b[side^1]=own;
            double[] p={.375,.375,.375},q=p.clone();int axis=switch(side/2){case 0->1;case 1->2;default->0;};p[axis]=(side&1)==0?0:1;q[axis]=1-p[axis];
            int first=CrystalEdgeBlend.color(own,a,p[0],p[1],p[2],-17.5,22,81.25),second=CrystalEdgeBlend.color(other,b,q[0],q[1],q[2],-17.5,22,81.25);
            assertEquals(first,second,"No color discontinuity at the shared face");
        }
    }
    @Test void grainIsStableButVariesAlongTheEdge(){
        var neighbors=new CrystalTier[6];neighbors[5]=CrystalTier.BLUE;Set<Integer> colors=new HashSet<>();
        for(int i=0;i<32;i++){int a=CrystalEdgeBlend.color(CrystalTier.GREEN,neighbors,1,.5,.5,1,i/8.0,.5);assertEquals(a,CrystalEdgeBlend.color(CrystalTier.GREEN,neighbors,1,.5,.5,1,i/8.0,.5));colors.add(a);}
        assertTrue(colors.size()>4,"Edge should intermix in mineral patches, not one uniform stripe");
    }
    @Test void mixingSeveralNeighborsKeepsValidColorChannels(){
        var neighbors=new CrystalTier[]{CrystalTier.RED,CrystalTier.WHITE,CrystalTier.YELLOW,CrystalTier.VIOLET,CrystalTier.CYAN,CrystalTier.BLUE};
        for(var own:CrystalTier.values())for(int mask=0;mask<8;mask++){
            double x=mask&1,y=(mask>>1)&1,z=(mask>>2)&1;int color=CrystalEdgeBlend.color(own,neighbors,x,y,z,x,y,z);
            assertEquals(0,color>>>24);for(int shift:new int[]{0,8,16}){int min=(own.color()>>shift)&255,max=min;for(var neighbor:neighbors){int channel=(neighbor.color()>>shift)&255;min=Math.min(min,channel);max=Math.max(max,channel);}int actual=(color>>shift)&255;assertTrue(actual>=min&&actual<=max);}
        }
    }
}
