package org.Azkerash.riftfall.behavior;

import java.util.*;
import java.util.function.*;
import io.redspace.ironsspellbooks.entity.spells.HealingAoe;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Reusable Iron's Spells healing-zone retreat executor. Species eligibility and morale are supplied by its owner. */
public final class SeekHealingCircleGoal extends Goal {
    private final Mob mob;
    private final Supplier<HealingRetreatSettings> tuning;
    private final DoubleSupplier confidencePoints;
    private final Predicate<LivingEntity> eligibleCaster;
    private HealingAoe circle;
    private Path path;
    private boolean running,riskUsed;
    private long nextPlan;
    private String reason="not evaluated";
    public SeekHealingCircleGoal(Mob mob,Supplier<HealingRetreatSettings> tuning,DoubleSupplier confidencePoints,Predicate<LivingEntity> eligibleCaster) {
        this.mob=mob;this.tuning=tuning;this.confidencePoints=confidencePoints;this.eligibleCaster=eligibleCaster;
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    private boolean active(HealingAoe zone) {
        return zone!=null && zone.isAlive() && zone.level()==mob.level() && zone.isCircular()
            && zone.tickCount>zone.getDelay() && remaining(zone)>0 && zone.getDamage()>0
            && zone.getOwner() instanceof LivingEntity caster && eligibleCaster.test(caster) && Utils.shouldHealEntity(caster,mob);
    }
    private int remaining(HealingAoe zone) { return zone.getDuration()+zone.getDelay()-zone.tickCount; }
    private boolean inCircle() {
        if (circle==null) return false;
        double radius=Math.max(.1,circle.getRadius()-mob.getBbWidth()*.5-.3);
        return mob.position().subtract(circle.position()).horizontalDistanceSqr()<=radius*radius && Math.abs(mob.getY()-circle.getY())<=1;
    }
    private List<Vec3> threats() {
        var result=new ArrayList<Vec3>();var target=mob.getTarget();
        if (target!=null && target.isAlive() && !target.isSpectator() && !(target instanceof Player p && p.isCreative()) && !mob.isAlliedTo(target) && mob.canAttack(target) && mob.hasLineOfSight(target)) result.add(target.position());
        for (var player:mob.level().getEntitiesOfClass(Player.class,mob.getBoundingBox().inflate(tuning.get().searchRadius()+tuning.get().dangerDistance()),
                p->!p.isCreative() && !p.isSpectator() && p.isAlive() && !mob.isAlliedTo(p) && mob.canAttack(p) && mob.hasLineOfSight(p)))
            if (player!=target) result.add(player.position());
        return result;
    }
    @Override public boolean canUse() {
        if (mob.getHealth()>=mob.getMaxHealth()*tuning.get().entryHealth()) { reason="health above entry threshold";return false; }
        return plan();
    }
    @Override public boolean canContinueToUse() {
        if (mob.getHealth()>=mob.getMaxHealth()*Math.max(tuning.get().entryHealth(),tuning.get().exitHealth())) { reason="recovered";return false; }
        if (!active(circle)) { reason="circle expired or no longer eligible";nextPlan=0;return false; }
        if (mob.level().getGameTime()>=nextPlan || !inCircle() && mob.getNavigation().isDone()) return plan();
        return true;
    }
    private boolean plan() {
        long now=mob.level().getGameTime();
        if (now<nextPlan) return running && active(circle);
        nextPlan=now+10;var settings=tuning.get();boolean confident=confidencePoints.getAsDouble()>=settings.riskPoints();
        var threats=threats();reason="no active eligible circle";
        var zones=mob.level().getEntitiesOfClass(HealingAoe.class,mob.getBoundingBox().inflate(settings.searchRadius()),zone->active(zone) && zone.distanceToSqr(mob)<=settings.searchRadius()*settings.searchRadius());
        zones.sort(Comparator.comparingDouble(z->z.distanceToSqr(mob)));
        for (var zone:zones) {
            if (!confident && threats.stream().anyMatch(enemy->enemy.distanceTo(zone.position())<settings.dangerDistance())) { reason="enemy threatens circle or route";continue; }
            if (remaining(zone)<=zone.getReapplicationDelay()) { reason="circle expires before useful arrival";continue; }
            if (!mob.hasLineOfSight(zone)) { reason="circle not visible";continue; }
            double radius=Math.max(.1,zone.getRadius()-mob.getBbWidth()*.5-.5);
            var points=new ArrayList<Vec3>();
            if (mob.position().subtract(zone.position()).horizontalDistanceSqr()<radius*radius && Math.abs(mob.getY()-zone.getY())<=1) points.add(mob.position());
            var toward=mob.position().subtract(zone.position()).multiply(1,0,1).normalize();
            points.add(zone.position().add(toward.scale(radius)));
            points.add(zone.position());
            for (int i=0;i<8;i++) points.add(zone.position().add(Math.cos(i*Math.PI/4)*radius,0,Math.sin(i*Math.PI/4)*radius));
            points.sort(Comparator.comparingDouble(p->p.distanceToSqr(mob.position())));
            for (var point:points) {
                BlockPos feet=BlockPos.containing(point);Vec3 end=Vec3.atBottomCenterOf(feet);
                if (end.subtract(zone.position()).horizontalDistanceSqr()>radius*radius || Math.abs(end.y-zone.getY())>1 || !safeFloor(feet)) continue;
                var candidate=mob.getNavigation().createPath(feet,0);
                if (candidate==null || !candidate.canReach()) { reason="no reachable path";continue; }
                var nodes=new ArrayList<Vec3>();double length=0;Vec3 previous=mob.position();boolean physical=true;
                for (int i=0;i<candidate.getNodeCount();i++) {
                    var node=candidate.getNode(i);var pos=node.asBlockPos();
                    if (!safeFloor(pos)) { physical=false;break; }
                    var vec=Vec3.atBottomCenterOf(pos);nodes.add(vec);length+=previous.distanceTo(vec);previous=vec;
                }
                if (!physical) { reason="hazardous terrain";continue; }
                boolean safe=HealingRouteSafety.safe(mob.position(),nodes,zone.position(),threats,settings.dangerDistance(),false);
                if (!safe && !confident) { reason="enemy threatens circle or route";continue; }
                int travel=(int)Math.ceil(length/Math.max(.05,mob.getAttributeValue(Attributes.MOVEMENT_SPEED)*.6));
                if (remaining(zone)<=travel+zone.getReapplicationDelay()) { reason="circle expires before useful arrival";continue; }
                circle=zone;path=candidate;riskUsed=!safe;reason=riskUsed?"confidence permits risky healing route":"safe reachable healing circle";
                if (running) move();
                return true;
            }
        }
        circle=null;path=null;return false;
    }
    private boolean safeFloor(BlockPos pos) {
        var level=mob.level();
        if (!level.hasChunkAt(pos) || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()) return false;
        for (var check:List.of(pos,pos.below())) {
            var state=level.getBlockState(check);
            if (state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)) return false;
        }
        return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)
            && level.noCollision(mob,mob.getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(mob.position())));
    }
    private void move() {
        if (inCircle()) {
            mob.setSprinting(false);
            mob.getNavigation().stop();
        } else mob.setSprinting(path!=null && mob.getNavigation().moveTo(path,1.1));
    }
    @Override public void start() {
        running=true;mob.getPersistentData().putInt("RiftfallHealingRetreats",mob.getPersistentData().getInt("RiftfallHealingRetreats")+1);mob.getPersistentData().putBoolean("RiftfallRetreating",true);
        mob.getPersistentData().putBoolean("RiftfallSeekingHealing",true);move();
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() {
        if (inCircle()) {
            mob.setSprinting(false);
            mob.getNavigation().stop();
        } else if (mob.getNavigation().isDone()) mob.setSprinting(false);
        var target=mob.getTarget();if (target!=null) mob.getLookControl().setLookAt(target,45,45);
    }
    @Override public void stop() {
        running=false;nextPlan=0;mob.setSprinting(false);mob.getNavigation().stop();circle=null;path=null;
        mob.getPersistentData().putBoolean("RiftfallRetreating",false);mob.getPersistentData().putBoolean("RiftfallSeekingHealing",false);
    }
    public void signals(Map<String,Double> signals) {
        var config=tuning.get();
        signals.put("healing.entry_health_fraction",config.entryHealth());signals.put("healing.exit_health_fraction",Math.max(config.entryHealth(),config.exitHealth()));
        signals.put("healing.danger_distance",config.dangerDistance());signals.put("healing.risk_points",config.riskPoints());
        signals.put("healing.risk_override",circle!=null && riskUsed?1D:0D);
        signals.put("healing.circle_available",circle!=null?1D:0D);
        signals.put("healing.remaining_ticks",circle==null?0D:(double)Math.max(0,remaining(circle)));
        signals.put("healing.in_circle",inCircle()?1D:0D);
    }
    public boolean wanted() { return running || mob.getHealth()<mob.getMaxHealth()*tuning.get().entryHealth(); }
    public String reason() { return reason; }
}
