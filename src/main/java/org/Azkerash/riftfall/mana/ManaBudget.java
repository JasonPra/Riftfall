package org.Azkerash.riftfall.mana;

/** Resource accounting shared by every mob adapter; no spell or species decisions here. */
public final class ManaBudget {
    private int delay;
    public boolean canSpend(double current,double cost){return Double.isFinite(cost)&&cost>=0&&current>=cost;}
    public double spend(double current,double cost,int regenerationDelay){
        if(!canSpend(current,cost))return current;
        if(cost>0)delay=regenerationDelay;
        return current-cost;
    }
    public double tick(double current,double maximum,double perSecond,boolean casting){
        current=Math.clamp(current,0,maximum);
        if(delay>0){delay--;return current;}
        return casting?current:Math.min(maximum,current+perSecond/20);
    }
    public int delay(){return delay;}
    public void restoreDelay(int ticks){delay=Math.max(0,ticks);}
}
