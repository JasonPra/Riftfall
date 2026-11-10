package org.Azkerash.riftfall.client;

import com.google.gson.JsonParser;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoblinFaceBindingTest {
    @Test void mouthSamplesTheMovedFangsWithoutPaddingOrResizingThem() throws Exception {
        var panel=GoblinFaceBinding.MOUTH;
        for(String role:new String[]{"goblin","goblin_archer","goblin_shaman"}) {
            try(var stream=getClass().getResourceAsStream("/assets/riftfall/textures/entity/"+role+".png")) {
                var image=ImageIO.read(stream);
                assertEquals(0xfff0e4b7,image.getRGB(panel.u()+1,panel.v()));
                assertEquals(0xfff0e4b7,image.getRGB(panel.u()+5,panel.v()));
                assertEquals(0,image.getRGB(panel.u()+1,panel.v()+1)>>>24,"Transparent row must stay transparent");
            }
        }
        assertEquals(1.1F,Math.abs(panel.xAt(33)-panel.xAt(34)),.00001);
        assertEquals(1.0625F,panel.yAt(5)-panel.yAt(4),.00001);
        assertEquals(3.3F,panel.xAt(33),.00001,"Match the original north-face UV orientation");
        assertEquals(.625F,panel.uLeft());assertEquals(.5F,panel.uRight());
        assertEquals(.0625F,panel.v0());assertEquals(.09375F,panel.v1());
    }
    @Test void epicFightMouthPlacementMatchesTheActualSkinnedHeadSurface() throws Exception {
        try(var stream=getClass().getResourceAsStream("/assets/riftfall/animmodels/entity/goblin.json")) {
            var positions=JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonObject()
                .getAsJsonObject("vertices").getAsJsonObject("positions").getAsJsonArray("array");
            float top=positions.get(2).getAsFloat(),bottom=positions.get(8).getAsFloat(),front=positions.get(1).getAsFloat();
            float mouthWorldHeight=top+(bottom-top)*6/8;
            float expected=-(mouthWorldHeight-1.513972F)*16;
            assertEquals(expected,GoblinFaceBinding.MOUTH.top()*GoblinFaceBinding.EPIC_Y_SCALE+GoblinFaceBinding.EPIC_Y_OFFSET,.0001);
            float surface=-(front-.000946F)*16;
            assertEquals(surface-.01F,GoblinFaceBinding.MOUTH.depth()+GoblinFaceBinding.EPIC_Z_OFFSET,.0001);
        }
    }
    @Test void narrowerEyesKeepTheArticulatedPupilsInsideTheirWhites() {
        var style=GoblinFaceBinding.STYLE;
        assertTrue(style.pupilTravelX()+style.pupilWidth()/2<1.1F);
        assertTrue(style.pupilTravelY()+style.pupilHeight()/2<style.eyeHeight()/2);
        assertTrue(style.restingBrowAngle()>0);assertTrue(style.restingLidClosure()>0);
    }
}
