package org.Azkerash.riftfall.client;
import java.util.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.Azkerash.riftfall.*;
import org.Azkerash.riftfall.facial.*;
/** Goblin sound-to-rig adapter. The envelope and priority mixer are reusable for other species. */
@EventBusSubscriber(modid=Riftfall.MODID,value=Dist.CLIENT)
public final class GoblinVocalAnimation {
    private static final Map<GoblinEntity,VocalTrack> TRACKS=new WeakHashMap<>();
    private static final VoiceEnvelope GRUNT=new VoiceEnvelope(.5F,0,.7F,.5F,.8F,.25F,0);
    private static VocalTrack track(GoblinEntity mob){return TRACKS.computeIfAbsent(mob,m->new VocalTrack());}
    public static void cue(GoblinEntity mob,int kind,float pitch){
        var envelope=switch(kind){case GoblinVocalCuePayload.LAUGH->GoblinVoiceEnvelopes.LAUGH;case GoblinVocalCuePayload.DEATH->GoblinVoiceEnvelopes.DEATH;case GoblinVocalCuePayload.GRUNT->GRUNT;default->GoblinVoiceEnvelopes.SCREAM;};
        if(kind==GoblinVocalCuePayload.DEATH){TRACKS.remove(mob);GoblinFearAudio.stop(mob);}
        track(mob).start("voice",envelope,mob.tickCount,pitch*(kind==GoblinVocalCuePayload.HURT?1.5:1),kind==GoblinVocalCuePayload.DEATH?40:30);
    }
    public static void fear(GoblinEntity mob,float pitch){track(mob).start("fear",GoblinVoiceEnvelopes.SCREAM,mob.tickCount,pitch,10);}
    public static void stopFear(GoblinEntity mob){var track=TRACKS.get(mob);if(track!=null)track.stop("fear");}
    public static FaceFrame frame(GoblinEntity mob,FaceFrame frame,double tick){
        if(frame.jaw()>=0)return frame;
        var track=TRACKS.get(mob);int jaw=track==null?-1:track.jaw(tick);
        return jaw<0?frame:new FaceFrame(frame.expression(),frame.blink(),frame.gazeX(),frame.gazeY(),jaw);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var level=net.minecraft.client.Minecraft.getInstance().level;
        TRACKS.entrySet().removeIf(e->{if(e.getKey().isRemoved()||e.getKey().level()!=level)return true;e.getValue().prune(e.getKey().tickCount);return e.getValue().empty();});
    }
    private GoblinVocalAnimation(){}
}
