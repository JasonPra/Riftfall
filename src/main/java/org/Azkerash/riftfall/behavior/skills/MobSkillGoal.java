package org.Azkerash.riftfall.behavior.skills;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import yesman.epicfight.skill.SkillSlots;

/** A shared AI executor. Species policy decides its priority in a behavior engine. */
public final class MobSkillGoal extends Goal {
    private final Mob owner;private final SkillSlots slot;
    private boolean staminaScope;
    private MobActiveSkill skill;private MobActiveSkill.Execution execution;
    public MobSkillGoal(Mob owner,SkillSlots slot){this.owner=owner;this.slot=slot;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
    public SkillSlots slot(){return slot;}
    public static String behaviorId(SkillSlots slot){return "skill_"+slot.name().toLowerCase(java.util.Locale.ROOT);}
    public boolean committed(){return execution!=null && execution.committed();}
    @Override public boolean canUse(){
        var equipped=MobSkillLoadout.equipped(owner,slot);skill=equipped==null?null:MobSkillLoadout.adapter(equipped);
        return skill!=null && !owner.isNoAi() && MobSkillLoadout.ready(owner,slot) && skill.canUse(owner,owner.getTarget());
    }
    @Override public void start(){
        if(!org.Azkerash.riftfall.stamina.MobStamina.spend(owner,skill.staminaCost(owner),"skill "+slot.name()))return;
        org.Azkerash.riftfall.stamina.MobStamina.beginSkill(owner);staminaScope=true;
        try{execution=skill.start(owner,owner.getTarget());}
        catch(RuntimeException ex){endStaminaScope();throw ex;}
        if(execution!=null)MobSkillLoadout.used(owner,slot,skill.cooldownTicks());else endStaminaScope();}
    private void endStaminaScope(){if(staminaScope)org.Azkerash.riftfall.stamina.MobStamina.endSkill(owner);staminaScope=false;}
    @Override public boolean canContinueToUse(){return execution!=null && execution.running();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){if(execution!=null)execution.tick();}
    @Override public void stop(){try{if(execution!=null)execution.stop();}finally{execution=null;endStaminaScope();}}
}
