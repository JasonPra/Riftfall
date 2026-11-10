package org.Azkerash.riftfall.behavior;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Reusable, bounded squad regrouping. The owner supplies membership and distances. */
public final class RegroupGoal extends Goal {
    private final Mob mob;
    private final Predicate<Mob> member;
    private final double searchRadius,startDistance,arrivalDistance,speed;
    private Mob ally;
    private Path path;
    private long nextPlan;
    public RegroupGoal(Mob mob,Predicate<Mob> member,double searchRadius,double startDistance,double arrivalDistance,double speed) {
        this.mob=mob;this.member=member;this.searchRadius=searchRadius;this.startDistance=startDistance;this.arrivalDistance=arrivalDistance;this.speed=speed;
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    private boolean eligible(Mob other) {
        return other!=mob && other.isAlive() && !other.isNoAi() && mob.isAlliedTo(other) && member.test(other);
    }
    private boolean closeThreat() {
        var target=mob.getTarget();
        return target!=null && target.isAlive() && mob.getSensing().hasLineOfSight(target) && mob.distanceToSqr(target)<=startDistance*startDistance;
    }
    private boolean plan() {
        path=mob.getNavigation().createPath(ally.blockPosition(),0);
        if(path==null || !path.canReach())return false;
        var target=mob.getTarget();
        if(target==null || !target.isAlive() || !mob.getSensing().hasLineOfSight(target))return true;
        var nodes=new ArrayList<Vec3>();
        for(int i=0;i<path.getNodeCount();i++)nodes.add(Vec3.atBottomCenterOf(path.getNode(i).asBlockPos()));
        return HealingRouteSafety.safe(mob.position(),nodes,ally.position(),List.of(target.position()),4,false);
    }
    @Override public boolean canUse() {
        if(mob.level().getGameTime()<nextPlan || closeThreat())return false;
        nextPlan=mob.level().getGameTime()+20;
        ally=mob.level().getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(searchRadius),this::eligible).stream()
            .filter(other->mob.distanceToSqr(other)<=searchRadius*searchRadius && mob.getSensing().hasLineOfSight(other))
            .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        return ally!=null && mob.distanceToSqr(ally)>startDistance*startDistance && plan();
    }
    @Override public boolean canContinueToUse() {
        return ally!=null && eligible(ally) && !closeThreat() && mob.distanceToSqr(ally)>arrivalDistance*arrivalDistance
            && mob.distanceToSqr(ally)<=searchRadius*searchRadius && !mob.getNavigation().isDone();
    }
    private void move(){mob.setSprinting(mob.getNavigation().moveTo(path,speed));}
    @Override public void start(){nextPlan=mob.level().getGameTime()+10;move();mob.getPersistentData().putBoolean("RiftfallRegrouping",true);}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        mob.getLookControl().setLookAt(ally,45,45);
        if(mob.level().getGameTime()>=nextPlan){
            nextPlan=mob.level().getGameTime()+10;
            if(plan())move();else{mob.getNavigation().stop();mob.setSprinting(false);}
        }
    }
    @Override public void stop(){mob.setSprinting(false);mob.getNavigation().stop();ally=null;path=null;mob.getPersistentData().putBoolean("RiftfallRegrouping",false);}
}
