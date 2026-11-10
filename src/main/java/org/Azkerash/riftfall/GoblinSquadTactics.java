package org.Azkerash.riftfall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Goblin physical formation cache and movement executors. Morale and action selection belong to the shared engine. */
final class GoblinSquadTactics {
    final GoblinEntity mob;
    private LivingEntity enemy,observedTarget;
    private long nextScan,nextWarriorPlan;
    private List<GoblinEntity> warriors=List.of(),archers=List.of();
    private Plan warriorPlan;
    private UUID ringEnemy;
    private double ringBearing;
    GoblinSquadTactics(GoblinEntity mob) { this.mob=mob; }
    static boolean validEnemy(GoblinEntity mob,LivingEntity target) {
        return target!=null && target.isAlive() && target.level()==mob.level() && !target.isSpectator()
            && !(target instanceof Player player && player.isCreative()) && !mob.isAlliedTo(target) && mob.canAttack(target);
    }
    static double fleeDistance(double confidence) { return Math.max(2.5,6.5-4*confidence); }
    static double retreatHealth(double confidence) { return .45-.25*confidence; }
    static Vec3 ringSlot(Vec3 enemy,int index,int count,double bearing,double radius) {
        double angle=bearing+Math.PI*2*index/count;
        return enemy.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
    }
    static Vec3 screen(Vec3 enemy,Vec3 archer,double distance) {
        Vec3 away=archer.subtract(enemy).multiply(1,0,1).normalize();
        if (away.lengthSqr()<.001) away=new Vec3(1,0,0);
        return enemy.add(away.scale(Math.min(distance,Math.sqrt(archer.distanceToSqr(enemy))*.5)));
    }
    private void scan() {
        var target=mob.getTarget();long now=mob.level().getGameTime();
        if (target==observedTarget && now<nextScan) return;
        if (target!=observedTarget) { warriorPlan=null;nextWarriorPlan=0; }
        observedTarget=target;enemy=validEnemy(mob,target)?target:null;nextScan=now+10;
        // This cache allocates physical formation slots only. Confidence and sensing belong to the shared engine.
        var goblins=mob.level().getEntitiesOfClass(GoblinEntity.class,mob.getBoundingBox().inflate(16,4,16),
            g->g.isAlive() && !g.isNoAi() && mob.isAlliedTo(g) && Math.abs(g.getY()-mob.getY())<=4
                && (enemy==null || g.getTarget()==null || g.getTarget()==enemy));
        if (!goblins.contains(mob)) goblins.add(mob);
        goblins.sort(Comparator.comparing(GoblinEntity::getUUID));
        warriors=goblins.stream().filter(g->!g.isRanged() && !g.getPersistentData().getBoolean("RiftfallRetreating")).toList();
        archers=goblins.stream().filter(GoblinEntity::isArcher).toList();
        mob.getPersistentData().putInt("RiftfallWarriorSupport",warriors.size());
    }
    double confidence() { return mob.behavior.confidence(); }
    boolean bold() { return mob.behavior.bold(); }
    // A ring still needs actual melee bodies; weighted morale determines whether they attempt it.
    private boolean canFlank() { return warriors.size()>=2 && bold(); }
    double panicRange() { scan();return fleeDistance(confidence()); }
    boolean hasWarriorPlan() { scan();return !warriors.isEmpty() && (canFlank() || !archers.isEmpty()); }
    static boolean inFrontOfArcher(Vec3 enemy,Vec3 guard,Vec3 archer) {
        Vec3 axis=archer.subtract(enemy).multiply(1,0,1),offset=guard.subtract(enemy).multiply(1,0,1);
        double projection=offset.dot(axis);
        return projection>0 && projection<axis.lengthSqr() && projection>.65*Math.sqrt(offset.lengthSqr()*axis.lengthSqr());
    }
    boolean mayStrike() {
        scan(); if (warriorPlan==null || !validEnemy(mob,enemy)) return true;
        int index=warriors.indexOf(mob); if (index<0) return true;
        boolean pack=canFlank();
        int guards=Math.min(archers.size(),pack?Math.max(1,warriors.size()/3):warriors.size());
        if (index<guards) {
            var archer=archers.get(index%archers.size());
            if (archer.distanceToSqr(enemy)<4) return true;
            return inFrontOfArcher(enemy.position(),mob.position(),archer.position());
        }
        if (!pack) return true;
        Vec3 slot=ringSlot(enemy.position(),index,warriors.size(),bearing(),1);
        double wanted=Math.atan2(slot.z-enemy.getZ(),slot.x-enemy.getX());
        double current=Math.atan2(mob.getZ()-enemy.getZ(),mob.getX()-enemy.getX());
        return Math.abs(angleDifference(wanted,current))<.6;
    }
    private double bearing() {
        var leader=warriors.getFirst().squad;
        if (!enemy.getUUID().equals(leader.ringEnemy)) {
            leader.ringEnemy=enemy.getUUID();
            leader.ringBearing=archers.isEmpty() ? (enemy.getUUID().hashCode()&65535)/65536.0*Math.PI*2
                : Math.atan2(archers.getFirst().getZ()-enemy.getZ(),archers.getFirst().getX()-enemy.getX());
        }
        return leader.ringBearing;
    }
    private static double angleDifference(double to,double from) { return Math.atan2(Math.sin(to-from),Math.cos(to-from)); }
    static Vec3 orbitWaypoint(Vec3 position,Vec3 enemy,Vec3 slot,double radius) {
        double current=Math.atan2(position.z-enemy.z,position.x-enemy.x);
        double wanted=Math.atan2(slot.z-enemy.z,slot.x-enemy.x),difference=angleDifference(wanted,current);
        if (Math.abs(difference)>.55) {
            double next=current+Math.clamp(difference,-Math.PI/3,Math.PI/3);
            return enemy.add(Math.cos(next)*radius,0,Math.sin(next)*radius);
        }
        return slot;
    }
    void moveWarrior() {
        scan(); if (!validEnemy(mob,enemy) || !hasWarriorPlan()) {
            warriorPlan=null;mob.setSprinting(false);mob.getPersistentData().putBoolean("RiftfallFlanking",false);mob.getPersistentData().putBoolean("RiftfallScreening",false);return;
        }
        long now=mob.level().getGameTime();
        if (now>=nextWarriorPlan) {
            nextWarriorPlan=now+12;int index=warriors.indexOf(mob);if (index<0) { mob.setSprinting(false);return; }
            boolean pack=canFlank();
            int guards=Math.min(archers.size(),pack?Math.max(1,warriors.size()/3):warriors.size());
            Vec3 destination;
            if (!archers.isEmpty() && index<guards) {
                var archer=archers.get(index%archers.size());destination=screen(enemy.position(),archer.position(),2.5);
                if (!pack && warriors.size()>1) {
                    Vec3 line=archer.position().subtract(enemy.position()).multiply(1,0,1).normalize();
                    destination=destination.add(-line.z*(index-(warriors.size()-1)*.5),0,line.x*(index-(warriors.size()-1)*.5));
                }
            } else if (pack) destination=ringSlot(enemy.position(),index,warriors.size(),bearing(),enemy.getBbWidth()*.5+.75);
            else { warriorPlan=null;mob.setSprinting(false);mob.getPersistentData().putBoolean("RiftfallFlanking",false);mob.getPersistentData().putBoolean("RiftfallScreening",false);return; }
            Vec3 waypoint=orbitWaypoint(mob.position(),enemy.position(),destination,Math.max(2.7,enemy.getBbWidth()*.5+1.8));
            warriorPlan=near(waypoint,false);
            mob.getPersistentData().putBoolean("RiftfallFlanking",pack && index>=guards);
            mob.getPersistentData().putBoolean("RiftfallScreening",!archers.isEmpty() && index<guards);
        }
        boolean sprint=false;
        if (warriorPlan!=null) {
            if (mob.distanceToSqr(warriorPlan.position)<.45*.45) mob.getNavigation().stop();
            else sprint=mob.getNavigation().moveTo(warriorPlan.path,1.05)
                && mob.getPersistentData().getBoolean("RiftfallScreening");
        }
        mob.setSprinting(sprint);
    }
    Plan retreatToAlly() {
        var target=mob.getTarget();if(!validEnemy(mob,target))return null;
        var allies=mob.level().getEntitiesOfClass(GoblinEntity.class,mob.getBoundingBox().inflate(16,4,16),
            other->other!=mob && other.isAlive() && !other.isNoAi() && mob.isAlliedTo(other)
                && !other.isBurrowing() && !other.isPanicFleeing() && mob.getSensing().hasLineOfSight(other));
        allies.sort(Comparator.comparingDouble(other->other.distanceToSqr(mob)));
        for(var ally:allies.stream().limit(4).toList()) {
            var plan=near(rearSlot(target.position(),ally.position(),2,0),false);
            if(plan==null || plan.position.distanceToSqr(target.position())<=mob.distanceToSqr(target))continue;
            var nodes=new ArrayList<Vec3>();
            for(int i=0;i<plan.path.getNodeCount();i++)nodes.add(Vec3.atBottomCenterOf(plan.path.getNode(i).asBlockPos()));
            if(org.Azkerash.riftfall.behavior.HealingRouteSafety.safe(mob.position(),nodes,plan.position,List.of(target.position()),4,false))return plan;
        }
        return null;
    }
    record Plan(Vec3 position,Path path) {}
    private boolean safe(BlockPos feet) {
        var level=mob.level();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(feet.below())) return false;
        var floor=level.getBlockState(feet.below());
        return floor.isFaceSturdy(level,feet.below(),Direction.UP) && !floor.is(Blocks.MAGMA_BLOCK)
            && !floor.is(Blocks.CAMPFIRE) && !floor.is(Blocks.SOUL_CAMPFIRE)
            && level.getFluidState(feet).isEmpty() && level.getFluidState(feet.below()).isEmpty()
            && level.noCollision(mob,mob.getBoundingBox().move(Vec3.atBottomCenterOf(feet).subtract(mob.position())));
    }
    private boolean sight(Vec3 position) {
        return mob.level().clip(new ClipContext(position.add(0,mob.getEyeHeight(),0),enemy.getEyePosition(),
            ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mob)).getType()==HitResult.Type.MISS;
    }
    private Plan reachable(Vec3 point) {
        var path=mob.getNavigation().createPath(BlockPos.containing(point),0);
        return path!=null && path.canReach() ? new Plan(point,path) : null;
    }
    private Plan near(Vec3 desired,boolean ranged) {
        // Resolve only loaded, supported surfaces; the navigator must actually reach them.
        var candidates=new ArrayList<Vec3>();
        for (int dx=-1;dx<=1;dx++) for (int dz=-1;dz<=1;dz++) for (int dy=-1;dy<=2;dy++) {
            BlockPos feet=BlockPos.containing(desired.x+dx,mob.getY()+dy,desired.z+dz);
            if (safe(feet)) candidates.add(Vec3.atBottomCenterOf(feet));
        }
        candidates.sort(Comparator.comparingDouble(p->p.distanceToSqr(desired)));
        for (Vec3 candidate : candidates.stream().limit(6).toList()) {
            if (ranged && !sight(candidate)) continue;
            var plan=reachable(candidate); if (plan!=null) return plan;
        }
        return null;
    }
    static Vec3 rearSlot(Vec3 enemy,Vec3 defender,double depth,double lateral) {
        Vec3 away=defender.subtract(enemy).multiply(1,0,1).normalize();
        if (away.lengthSqr()<.001) away=new Vec3(1,0,0);
        return defender.add(away.scale(depth)).add(-away.z*lateral,0,away.x*lateral);
    }
    static Vec3 panicPoint(Vec3 position,Vec3 enemy,double turn,double length) {
        double angle=Math.atan2(position.z-enemy.z,position.x-enemy.x)+turn;
        return position.add(Math.cos(angle)*length,0,Math.sin(angle)*length);
    }
    static double firingScore(Vec3 position,Vec3 ideal,Vec3 origin,double enemyY) {
        return position.distanceToSqr(ideal)*.35+position.distanceToSqr(origin)*.1-Math.clamp(position.y-enemyY,0,2)*4;
    }
    private Vec3 rear() {
        scan();
        if (warriors.isEmpty()) {
            // Unsupported archers may seek nearby height, but must not turn repositioning into perfect kiting.
            return mob.position();
        }
        var defender=warriors.stream().min(Comparator.comparingDouble(g->g.distanceToSqr(mob))).orElseThrow();
        int lane=archers.indexOf(mob);double side=lane<0?0:(lane%2==0?1:-1)*(1.8+lane*.3);
        return rearSlot(enemy.position(),defender.position(),4-confidence(),side);
    }
    Plan firingPosition() {
        scan(); if (!validEnemy(mob,enemy)) return null;
        Vec3 ideal=rear();var candidates=new ArrayList<Vec3>();
        for (int dx=-3;dx<=3;dx+=2) for (int dz=-3;dz<=3;dz+=2) for (int dy=-1;dy<=2;dy++) {
            BlockPos feet=BlockPos.containing(ideal.x+dx,mob.getY()+dy,ideal.z+dz);
            Vec3 pos=Vec3.atBottomCenterOf(feet);
            double distance=pos.distanceToSqr(enemy.position());
            if (distance>=4*4 && distance<=19*19 && safe(feet) && sight(pos) && clearShotFrom(pos.add(0,mob.getEyeHeight(),0))) candidates.add(pos);
        }
        candidates.sort(Comparator.comparingDouble(p->firingScore(p,ideal,mob.position(),enemy.getY())));
        for (Vec3 candidate : candidates.stream().limit(8).toList()) {
            var plan=reachable(candidate); if (plan!=null) return plan;
        }
        return null;
    }
    boolean clearShot() {
        var target=mob.getTarget();if (!validEnemy(mob,target)) return false;
        return clearShotFrom(mob.getEyePosition());
    }
    private boolean clearShotFrom(Vec3 start) {
        Vec3 end=mob.getTarget().getEyePosition();
        return mob.level().getEntitiesOfClass(GoblinEntity.class,new net.minecraft.world.phys.AABB(start,end).inflate(1),
            g->g!=mob && g.isAlive() && mob.isAlliedTo(g)).stream()
            .noneMatch(g->g.getBoundingBox().inflate(.12).clip(start,end).isPresent());
    }
    static final class Position extends Goal {
        final GoblinEntity mob;Plan plan;int ticks;long ready;
        Position(GoblinEntity mob) { this.mob=mob;setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (mob.level().getGameTime()<ready || !validEnemy(mob,mob.getTarget()) || mob.isUsingItem()
                || mob.getPersistentData().getBoolean("RiftfallCasting") || GoblinTactics.coverSafe(mob)) return false;
            plan=mob.squad.firingPosition();
            return plan!=null && (mob.distanceToSqr(plan.position)>2.25 || !mob.squad.clearShot());
        }
        @Override public void start() {
            ticks=0;ready=mob.level().getGameTime()+30;
            mob.setSprinting(mob.isArcher() || mob.distanceToSqr(mob.getTarget())>10*10);
            mob.getNavigation().moveTo(plan.path,1.0);
            mob.getPersistentData().putBoolean("RiftfallFormation",true);
            mob.getPersistentData().putBoolean("RiftfallHighGround",plan.position.y>mob.getTarget().getY()+.5);
        }
        @Override public boolean canContinueToUse() {
            return ticks<28 && validEnemy(mob,mob.getTarget()) && !mob.getNavigation().isDone() && mob.distanceToSqr(plan.position)>1
                && !GoblinTactics.coverSafe(mob);
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            ticks++;
            var target=mob.getTarget();
            mob.setSprinting(target!=null && !mob.getNavigation().isDone() && (mob.isArcher() || mob.distanceToSqr(target)>10*10));
        }
        @Override public void stop() {
            mob.setSprinting(false);
            ready=mob.level().getGameTime()+50;mob.getNavigation().stop();mob.getPersistentData().putBoolean("RiftfallFormation",false);
        }
    }
    static class PanicRetreat extends Goal {
        final GoblinEntity mob;Plan plan;int ticks,duration;long ready;boolean organized;
        PanicRetreat(GoblinEntity mob) { this.mob=mob;setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            var enemy=mob.getTarget();long now=mob.level().getGameTime();
            if (!validEnemy(mob,enemy) || GoblinTactics.coverSafe(mob) || now<ready
                || mob.distanceToSqr(enemy)>Math.pow(mob.squad.panicRange(),2)) return false;
            organized=mob.squad.bold();
            plan=organized?mob.squad.firingPosition():null;
            if (plan==null || plan.position.distanceToSqr(enemy.position())<mob.distanceToSqr(enemy)) {
                plan=null;
                // Frightened stragglers make short, imperfect turns instead of selecting the optimal escape vector.
                for (int i=0;i<5 && plan==null;i++) {
                    double turn=(mob.getRandom().nextDouble()-.5)*Math.PI*1.4;
                    double length=3+mob.getRandom().nextDouble()*3;
                    Vec3 desired=panicPoint(mob.position(),enemy.position(),turn,length);
                    if (desired.distanceToSqr(enemy.position())<Math.max(2*2,mob.distanceToSqr(enemy)*.7)) continue;
                    plan=mob.squad.near(desired,false);
                }
            }
            if (plan==null) { ready=now+10;return false; }
            duration=organized?35:18+mob.getRandom().nextInt(13);
            return true;
        }
        @Override public void start() {
            ticks=0;
            if (mob.isArcher()) mob.setSprinting(true);
            mob.getPersistentData().putBoolean("RiftfallPanicked",!organized);mob.getPersistentData().putBoolean("RiftfallRetreating",true);mob.getNavigation().moveTo(plan.path,1.0);
        }
        @Override public boolean canContinueToUse() {
            return ticks<duration && validEnemy(mob,mob.getTarget()) && mob.squad.bold()==organized && !mob.getNavigation().isDone()
                && !GoblinTactics.coverSafe(mob) && mob.distanceToSqr(plan.position)>1
                && mob.distanceToSqr(mob.getTarget())<Math.pow(mob.squad.panicRange()+2,2);
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() { ticks++; }
        @Override public void stop() {
            if (mob.isArcher()) mob.setSprinting(false);
            ready=mob.level().getGameTime()+20+mob.getRandom().nextInt(21);mob.getNavigation().stop();
            mob.getPersistentData().putBoolean("RiftfallRetreating",false);mob.getPersistentData().putBoolean("RiftfallPanicked",false);
        }
    }
    static final class Assist extends Goal {
        final GoblinEntity mob;LivingEntity enemy;
        Assist(GoblinEntity mob) { this.mob=mob;setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() {
            if (validEnemy(mob,mob.getTarget())) return false;
            enemy=mob.level().getEntitiesOfClass(GoblinEntity.class,mob.getBoundingBox().inflate(16),
                g->g!=mob && g.isAlive() && mob.isAlliedTo(g) && validEnemy(mob,g.getTarget()))
                .stream().min(Comparator.comparingDouble(g->g.distanceToSqr(mob))).map(GoblinEntity::getTarget).orElse(null);
            return enemy!=null && mob.getSensing().hasLineOfSight(enemy);
        }
        @Override public void start() { mob.setTarget(enemy); }
        @Override public boolean canContinueToUse() { return validEnemy(mob,enemy) && mob.distanceToSqr(enemy)<28*28; }
    }
}
