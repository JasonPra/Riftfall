package org.Azkerash.riftfall.loot;
import java.util.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import org.Azkerash.riftfall.GoblinEntity;
/** Idle scavenging only; combat, panic, healing and remembered targets retain higher priority. */
public final class GoblinLootGoal extends Goal {
    private final GoblinEntity mob;private ItemEntity target;private long nextScan,nextPath;
    public GoblinLootGoal(GoblinEntity mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    private boolean safe(){return mob.getTarget()==null && !mob.isPanicking() && !mob.isPanting() && !mob.isBurrowing() && mob.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);}
    @Override public boolean canUse(){
        if(!safe()||mob.level().getGameTime()<nextScan)return false;nextScan=mob.level().getGameTime()+20;
        target=mob.level().getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(8,3,8),e->e.isAlive()&&!e.hasPickUpDelay()&&mob.loot().canPickUp(e.getItem())&&mob.level().clip(new net.minecraft.world.level.ClipContext(mob.getEyePosition(),e.getBoundingBox().getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob)).getType()==net.minecraft.world.phys.HitResult.Type.MISS)
            .stream().sorted(Comparator.comparingDouble(mob::distanceToSqr)).filter(e->{var path=mob.getNavigation().createPath(e.blockPosition(),0);return path!=null&&path.canReach();}).findFirst().orElse(null);
        return target!=null;
    }
    @Override public boolean canContinueToUse(){return safe()&&target!=null&&target.isAlive()&&!target.hasPickUpDelay()&&mob.distanceToSqr(target)<12*12&&mob.loot().canPickUp(target.getItem());}
    @Override public void start(){nextPath=0;}
    @Override public void tick(){
        if(target==null)return;mob.getLookControl().setLookAt(target,30,30);
        if(mob.level().getGameTime()>=nextPath){nextPath=mob.level().getGameTime()+10;var path=mob.getNavigation().createPath(target.blockPosition(),0);if(path!=null)mob.getNavigation().moveTo(path,.9);}
    }
    @Override public void stop(){target=null;mob.getNavigation().stop();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
}
