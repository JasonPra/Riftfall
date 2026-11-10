package org.Azkerash.riftfall.stamina;

/** Server-owned resource accounting, independent of entity AI and rendering. Units are points and ticks. */
public final class StaminaState {
    private final StaminaBudget budget=new StaminaBudget();
    public StaminaBudget budget(){return budget;}
    private double current=Double.NaN;
    private boolean panting;
    private int recoveryDelay;
    private String lastExpense="none";
    public double current(double maximum){if(Double.isNaN(current))current=maximum;return current=Math.clamp(current,0,maximum);}
    public boolean panting(){return panting;}
    public String lastExpense(){return lastExpense;}
    public boolean canSpend(double amount,double maximum){return !panting && current(maximum)+1E-8>=amount;}
    public boolean spend(double amount,double maximum,int delay,String reason){
        if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("Invalid stamina expense");
        if(!canSpend(amount,maximum)){panting=true;lastExpense="exhausted: "+reason;return false;}
        current=Math.max(0,current(maximum)-amount);recoveryDelay=delay;lastExpense=reason;return true;
    }
    public static double regenerationMultiplier(boolean panting,boolean fleeing,boolean confident){
        return panting?2:fleeing?1.25:confident?1.15:1;
    }
    public void tick(double maximum,double perSecond,boolean fleeing,boolean confident,boolean working){
        current(maximum);
        if(panting){
            current=Math.min(maximum,current+perSecond/20*regenerationMultiplier(true,fleeing,confident));
            if(current>=maximum-1E-8){current=maximum;panting=false;recoveryDelay=0;}
        }else if(recoveryDelay>0)recoveryDelay--;
        else if(!working)current=Math.min(maximum,current+perSecond/20*regenerationMultiplier(false,fleeing,confident));
    }
    public void restore(double value,boolean exhausted,int delay,double maximum){
        budget.reset();
        current=Double.isFinite(value)?Math.clamp(value,0,maximum):maximum;panting=exhausted;recoveryDelay=Math.max(0,delay);
    }
    public int recoveryDelay(){return recoveryDelay;}
}
