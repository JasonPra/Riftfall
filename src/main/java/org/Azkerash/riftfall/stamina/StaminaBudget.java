package org.Azkerash.riftfall.stamina;

/** Per-entity transient accounting; a paid animation/skill must not pay again for every hit or projectile. */
public final class StaminaBudget {
    private long animationUntil=Long.MIN_VALUE,contactTick=Long.MIN_VALUE;
    private int skillDepth;
    public boolean skillCovered(){return skillDepth>0;}
    public void reset(){animationUntil=Long.MIN_VALUE;contactTick=Long.MIN_VALUE;skillDepth=0;}
    public boolean covered(long tick){return skillDepth>0 || tick<animationUntil;}
    public void animation(long tick,int duration){animationUntil=Math.max(animationUntil,tick+Math.max(1,duration));}
    public void beginSkill(){skillDepth++;}
    public void endSkill(){skillDepth=Math.max(0,skillDepth-1);}
    public boolean sameContactTick(long tick){return contactTick==tick;}
    public void contact(long tick){contactTick=tick;}
}
