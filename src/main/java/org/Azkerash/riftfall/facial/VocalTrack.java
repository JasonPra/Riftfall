package org.Azkerash.riftfall.facial;
import java.util.*;
/** Independent voice channels: a hurt voice overrides fear, which resumes if its clip is still playing. */
public final class VocalTrack {
    private record Clip(VoiceEnvelope envelope,double start,double pitch,int priority){}
    private final Map<String,Clip> clips=new HashMap<>();
    public void start(String channel,VoiceEnvelope envelope,double tick,double pitch,int priority){
        if(!Double.isFinite(tick)||!Double.isFinite(pitch)||pitch<=0)throw new IllegalArgumentException("Invalid voice timing");
        clips.put(channel,new Clip(envelope,tick,pitch,priority));
    }
    public void stop(String channel){clips.remove(channel);}
    /** -1 means no voice; let the expression/manual facial controls supply the jaw pose. */
    public int jaw(double tick){
        Clip chosen=null;double seconds=0;
        for(var entry:clips.entrySet()){
            var clip=entry.getValue();double age=(tick-clip.start)*clip.pitch/20;
            if(age<0 || age>=clip.envelope.duration())continue;
            if(chosen==null || clip.priority>chosen.priority || clip.priority==chosen.priority && clip.start>chosen.start){chosen=clip;seconds=age;}
        }
        return chosen==null?-1:Math.clamp(Math.round(chosen.envelope.opening(seconds)*15),0,15);
    }
    public void prune(double tick){clips.values().removeIf(c->(tick-c.start)*c.pitch/20>=c.envelope.duration());}
    public boolean empty(){return clips.isEmpty();}
}
