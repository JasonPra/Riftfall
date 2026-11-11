package org.Azkerash.riftfall.mana;

/** Resource accounting shared by every mob adapter; no spell or species decisions here. */
public final class ManaBudget {
    private int delay;
    private boolean recovering;
    public void observe(double current,double maximum){
        if(current<=0)recovering=true;
        if(maximum>0 && current>=maximum)recovering=false;
    }
    /** Failed, otherwise-valid cast requests also count as depletion; an impossible spell cannot lock the pool. */
    public boolean request(double current,double maximum,double cost){
        observe(current,maximum);
        if(!Double.isFinite(cost)||cost<0||cost>maximum)return false;
        if(recovering)return false;
        if(current<cost){recovering=true;return false;}
        return true;
    }
    public boolean canSpend(double current,double cost){return !recovering&&Double.isFinite(cost)&&cost>=0&&current>=cost;}
    public double spend(double current,double cost,int regenerationDelay){
        if(!canSpend(current,cost))return current;
        if(cost>0)delay=regenerationDelay;
        double remaining=current-cost;if(remaining<=0)recovering=true;return remaining;
    }
    public double tick(double current,double maximum,double perSecond,boolean casting){
        current=Math.clamp(current,0,maximum);observe(current,maximum);
        if(delay>0){delay--;return current;}
        double next=casting?current:Math.min(maximum,current+perSecond/20);observe(next,maximum);return next;
    }
    public boolean recovering(){return recovering;}
    public void restoreRecovery(boolean value){recovering=value;}
    public int delay(){return delay;}
    public void restoreDelay(int ticks){delay=Math.max(0,ticks);}
}
