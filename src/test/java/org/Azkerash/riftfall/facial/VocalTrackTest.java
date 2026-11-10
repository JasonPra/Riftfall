package org.Azkerash.riftfall.facial;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VocalTrackTest {
    private static final VoiceEnvelope TEST=new VoiceEnvelope(2,0,1,0);
    @Test void pitchChangesJawTimingAndClipLifetime(){
        var normal=new VocalTrack();var high=new VocalTrack();normal.start("voice",TEST,100,1,10);high.start("voice",TEST,100,2,10);
        assertTrue(high.jaw(110)>normal.jaw(110));assertEquals(-1,high.jaw(121));assertTrue(normal.jaw(121)>=0);
    }
    @Test void hurtOverridesFearThenFearResumesInsteadOfBeingRestarted(){
        var t=new VocalTrack();t.start("fear",new VoiceEnvelope(4,0,1,1,0),0,1,10);
        t.start("hurt",new VoiceEnvelope(.5F,0,.1F,0),10,1,30);
        assertTrue(t.jaw(15)<=2);assertTrue(t.jaw(22)>2);t.stop("fear");assertEquals(-1,t.jaw(22));
    }
    @Test void realRecordingsHaveDistinctMovingCurvesAndBoundedOpenings(){
        for(var curve:java.util.List.of(GoblinVoiceEnvelopes.SCREAM,GoblinVoiceEnvelopes.LAUGH,GoblinVoiceEnvelopes.DEATH)){
            double lo=1,hi=0;for(double s=0;s<curve.duration();s+=.025){float x=curve.opening(s);assertTrue(x>=0&&x<=1);lo=Math.min(lo,x);hi=Math.max(hi,x);}
            assertTrue(hi-lo>.4);assertEquals(0,curve.opening(curve.duration()+1));
        }
    }
    @Test void invalidFramesAndExpiredChannelsCannotLeaveTheJawOpen(){
        assertThrows(IllegalArgumentException.class,()->new VoiceEnvelope(0,0,1));assertThrows(IllegalArgumentException.class,()->new VoiceEnvelope(1,Float.NaN,1));
        var t=new VocalTrack();t.start("voice",TEST,0,1,10);t.prune(50);assertTrue(t.empty());assertEquals(-1,t.jaw(50));
    }
}
