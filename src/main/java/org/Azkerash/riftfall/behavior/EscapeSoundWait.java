package org.Azkerash.riftfall.behavior;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded observer audio leases: completion acknowledgements normally release the wait immediately. */
public final class EscapeSoundWait {
    private record Clip(long sequence,long expires) {}
    private final Map<UUID,Clip> clips=new HashMap<>();
    private long holdSince=-1;
    public boolean started(UUID observer,long sequence,long now,int duration) {
        if(sequence<=0 || duration<1 || duration>140 || holdSince>=0 && now-holdSince>=6)return false;
        var previous=clips.get(observer);
        if(previous!=null && previous.sequence()>=sequence)return false;
        clips.put(observer,new Clip(sequence,now+duration));return true;
    }
    public void finished(UUID observer,long sequence) {
        var clip=clips.get(observer);
        if(clip!=null && clip.sequence()==sequence)clips.remove(observer);
    }
    public boolean ready(long now) {
        if(holdSince<0)holdSince=now;
        clips.values().removeIf(clip->clip.expires()<=now);
        // Allow the completed burrow state to reach observers before deciding that nobody is speaking.
        return now-holdSince>=6 && clips.isEmpty();
    }
    public void resetHold(){holdSince=-1;}
}
