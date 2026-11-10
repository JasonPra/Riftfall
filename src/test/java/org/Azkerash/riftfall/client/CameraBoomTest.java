package org.Azkerash.riftfall.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CameraBoomTest {
    @Test void aWallRetractsImmediatelyAndExpansionNeverOvershoots(){
        var b=new CameraBoom();assertEquals(4.5,b.advance(4.5,.016,10));assertEquals(.6,b.advance(.6,.001,10));
        double next=b.advance(4.5,.016,10);assertTrue(next>.6 && next<4.5);assertEquals(0,b.advance(0,.016,10));
    }
    @Test void recoveryIsIndependentOfFrameRate(){
        var a=new CameraBoom();var b=new CameraBoom();a.advance(1,0,10);b.advance(1,0,10);
        double x=0,y=0;for(int i=0;i<30;i++)x=a.advance(4.5,1D/30,10);for(int i=0;i<120;i++)y=b.advance(4.5,1D/120,10);
        assertEquals(x,y,1E-9);
    }
    @Test void resettingPerspectiveDoesNotCarryAnObstructedBoomIntoTheNextWorld(){
        var b=new CameraBoom();b.advance(.1,.016,10);b.reset();assertEquals(4.5,b.advance(4.5,.016,10));
    }
}
