package org.Azkerash.riftfall.loot;
import java.util.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import org.Azkerash.riftfall.GoblinEntity;
/** Idle scavenging only; combat, panic, healing and remembered targets retain higher priority. */
public final class GoblinLootGoal extends Goal {
    private final GoblinEntity mob;private ItemEntity target;private long nextScan,nextPath;private int candidateCursor;private net.minecraft.world.level.pathfinder.Path initialPath;
    public GoblinLootGoal(GoblinEntity mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    private boolean safe(){return mob.getTarget()==null && !mob.isPanicking() && !mob.isPanting() && !mob.isBurrowing() && mob.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);}
    @Override public boolean canUse(){
        if(!safe()||mob.level().getGameTime()<nextScan)return false;nextScan=org.Azkerash.riftfall.behavior.SensingSchedule.next(mob.level().getGameTime(),20,mob.getId());
        var candidates=mob.level().getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(8,3,8),e->e.isAlive()&&!e.hasPickUpDelay());
        candidates.sort(Comparator.comparingDouble(mob::distanceToSqr));target=null;initialPath=null;
        int attempts=Math.min(4,candidates.size());
        for(int i=0;i<attempts;i++){
            var item=candidates.get(Math.floorMod(candidateCursor++,candidates.size()));
            if(!mob.loot().canPickUp(item.getItem()) || mob.level().clip(new net.minecraft.world.level.ClipContext(mob.getEyePosition(),item.getBoundingBox().getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS)continue;
            var path=mob.getNavigation().createPath(item.blockPosition(),0);
            if(path!=null&&path.canReach()){target=item;initialPath=path;break;}
        }
        return target!=null;
    }
    @Override public boolean canContinueToUse(){return safe()&&target!=null&&target.isAlive()&&!target.hasPickUpDelay()&&mob.distanceToSqr(target)<12*12&&mob.loot().canPickUp(target.getItem());}
    @Override public void start(){nextPath=mob.level().getGameTime()+10;if(initialPath!=null)mob.getNavigation().moveTo(initialPath,.9);}
    @Override public void tick(){
        if(target==null)return;mob.getLookControl().setLookAt(target,30,30);
        if(mob.level().getGameTime()>=nextPath){nextPath=mob.level().getGameTime()+10;var path=mob.getNavigation().createPath(target.blockPosition(),0);if(path!=null)mob.getNavigation().moveTo(path,.9);}
    }
    @Override public void stop(){target=null;initialPath=null;mob.getNavigation().stop();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
}
