package org.Azkerash.riftfall;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.Azkerash.riftfall.behavior.TargetAwareness;

/** Cowardly warriors seize an unwatched rear opening, then yield to grapple/plunge/melee. */
final class GoblinRearAmbush extends Goal {
    private final GoblinEntity mob;
    private Path path;
    private long nextPlan;
    GoblinRearAmbush(GoblinEntity mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    static boolean opening(GoblinEntity mob) {
        var target=mob.getTarget();
        return !mob.isRanged() && !mob.isBurrowing()
            && target instanceof Player && GoblinSquadTactics.validEnemy(mob,target)
            && mob.distanceToSqr(target)<=16*16 && mob.getSensing().hasLineOfSight(target)
            && TargetAwareness.facingAway(target.getLookAngle(),mob.position().subtract(target.position()));
    }
    private boolean closeEnough(){
        var target=mob.getTarget();
        if(target!=null && mob.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem
                && org.Azkerash.riftfall.behavior.skills.MobSkillLoadout.equipped(mob,yesman.epicfight.skill.SkillSlots.IDENTITY)==RiftfallSkills.BACKSTAB.get()
                && org.Azkerash.riftfall.behavior.skills.MobSkillLoadout.ready(mob,yesman.epicfight.skill.SkillSlots.IDENTITY))
            return BackstabSequence.canStart(mob,target);
        return target!=null && mob.distanceToSqr(target)<1.3*1.3
            && GrappleGoal.isBehind(target.getLookAngle(),mob.position().subtract(target.position()));
    }
    private boolean plan(){
        var target=mob.getTarget();if(target==null)return false;
        var rear=target.position().subtract(Vec3.directionFromRotation(0,target.getYRot()).scale(1));
        if(mob.distanceToSqr(target)<4 && GrappleGoal.isBehind(target.getLookAngle(),mob.position().subtract(target.position())))rear=target.position();
        path=mob.getNavigation().createPath(net.minecraft.core.BlockPos.containing(rear),0);
        return path!=null && path.canReach() && !path.isDone();
    }
    @Override public boolean canUse(){
        if(!opening(mob)||closeEnough()||mob.level().getGameTime()<nextPlan)return false;
        nextPlan=mob.level().getGameTime()+8;return plan();
    }
    @Override public boolean canContinueToUse(){return opening(mob) && !closeEnough() && !mob.getNavigation().isDone();}
    private void move(){mob.setSprinting(mob.getNavigation().moveTo(path,1.1));}
    @Override public void start(){nextPlan=mob.level().getGameTime()+8;move();mob.getPersistentData().putBoolean("RiftfallRearAmbush",true);}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        if(!opening(mob) || closeEnough()){mob.getNavigation().stop();mob.setSprinting(false);return;}
        mob.getLookControl().setLookAt(mob.getTarget(),90,90);
        if(mob.level().getGameTime()>=nextPlan){
            nextPlan=mob.level().getGameTime()+8;
            if(plan())move();else{mob.getNavigation().stop();mob.setSprinting(false);}
        }
    }
    @Override public void stop(){mob.setSprinting(false);mob.getNavigation().stop();path=null;mob.getPersistentData().putBoolean("RiftfallRearAmbush",false);}
}
