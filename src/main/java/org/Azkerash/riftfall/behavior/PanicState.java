package org.Azkerash.riftfall.behavior;

/** Pure panic lifecycle; environmental escape restrictions do not suppress fear itself. */
public final class PanicState {
    public enum Stage { CALM, PANICKED, BURROWING, UNDERGROUND, ESCAPED }
    private Stage stage=Stage.CALM;
    private long lowSince=-1,recoveredSince=-1,panicSince,burrowSince;
    public Stage stage(){return stage;}
    public boolean active(){return stage==Stage.PANICKED||stage==Stage.BURROWING||stage==Stage.UNDERGROUND;}
    public void observe(long now,double points,boolean threat,boolean enabled,PanicSettings s){
        if(!enabled){reset();return;}
        if(stage==Stage.CALM){
            if(threat&&points<=s.entryPoints()){
                if(lowSince<0)lowSince=now;
                if(now-lowSince+1>=s.entryTicks()){stage=Stage.PANICKED;panicSince=now;recoveredSince=-1;}
            }else lowSince=-1;
        }else if(stage==Stage.PANICKED){
            if(points>=s.recoveryPoints()){
                if(recoveredSince<0)recoveredSince=now;
                if(now-panicSince>=s.minimumTicks()&&now-recoveredSince+1>=s.recoveryTicks())reset();
            }else recoveredSince=-1;
        }
    }
    public boolean beginBurrow(long now,boolean safe,boolean allowed){
        if(stage!=Stage.PANICKED||!safe||!allowed)return false;
        stage=Stage.BURROWING;burrowSince=now;return true;
    }
    public boolean advanceBurrow(long now,boolean safe,boolean allowed,PanicSettings s){
        return advanceBurrow(now,safe,allowed,s,true);
    }
    public boolean advanceBurrow(long now,boolean safe,boolean allowed,PanicSettings s,boolean completionReady){
        if(stage!=Stage.BURROWING && stage!=Stage.UNDERGROUND)return false;
        if(!allowed || stage==Stage.BURROWING && !safe){abortBurrow();return false;}
        if(now-burrowSince>=s.burrowTicks()){
            stage=Stage.UNDERGROUND;
            if(completionReady){stage=Stage.ESCAPED;return true;}
        }
        return false;
    }
    public double progress(long now,PanicSettings s){return stage==Stage.UNDERGROUND?1:stage==Stage.BURROWING?Math.clamp((double)(now-burrowSince)/s.burrowTicks(),0,1):0;}
    public void abortBurrow(){if(stage==Stage.BURROWING||stage==Stage.UNDERGROUND)stage=Stage.PANICKED;}
    public void reset(){stage=Stage.CALM;lowSince=-1;recoveredSince=-1;}
}
