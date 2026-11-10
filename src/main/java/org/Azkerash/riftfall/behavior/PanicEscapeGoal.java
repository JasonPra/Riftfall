package org.Azkerash.riftfall.behavior;

import java.util.*;
import java.util.function.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Reusable escape executor. Owner supplies faction, tuning, substrate and animation/visual hooks. */
public final class PanicEscapeGoal extends Goal {
    public interface Visuals {
        void update(boolean panicked,boolean escaping,double burrowProgress);
        void beginBurrow(int durationTicks);
        void abortBurrow();
        void dig(BlockState ground);
        default boolean readyToEscape(){return true;}
    }
    private final PathfinderMob mob;
    private final Supplier<PanicSettings> tuning;
    private final BooleanSupplier enabled,escapeAllowed,interrupted;
    private final BiPredicate<BlockState,BlockPos> substrate;
    private final Visuals visuals;
    private final PanicState state=new PanicState();
    private Vec3 lastThreat;
    private long nextPath;
    private double distance=Double.POSITIVE_INFINITY;
    public PanicEscapeGoal(PathfinderMob mob,Supplier<PanicSettings> tuning,BooleanSupplier enabled,BooleanSupplier escapeAllowed,
            BooleanSupplier interrupted,BiPredicate<BlockState,BlockPos> substrate,Visuals visuals){
        this.mob=mob;this.tuning=tuning;this.enabled=enabled;this.escapeAllowed=escapeAllowed;this.interrupted=interrupted;
        this.substrate=substrate;this.visuals=visuals;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));
    }
    public boolean active(){return state.active();}
    public boolean escapeAllowed(){return enabled.getAsBoolean()&&escapeAllowed.getAsBoolean();}
    public boolean burrowing(){return state.stage()==PanicState.Stage.BURROWING||state.stage()==PanicState.Stage.UNDERGROUND;}
    public void observe(CombatSituation situation){
        if(situation.targetVisible())lastThreat=situation.target().position();
        else if(situation.lastSeenPosition()!=null)lastThreat=situation.lastSeenPosition();
        boolean wasBurrowing=burrowing();
        state.observe(mob.level().getGameTime(),situation.confidence().points(),situation.context().active("target.present"),enabled.getAsBoolean(),tuning.get());
        if(wasBurrowing&&!burrowing())visuals.abortBurrow();
        if(burrowing()&&!escapeAllowed())abort();
        publish();
    }
    private boolean enemy(LivingEntity entity){
        return entity!=mob&&entity.isAlive()&&!entity.isSpectator()&&!(entity instanceof Player p&&p.isCreative())
            &&!mob.isAlliedTo(entity)&&mob.canAttack(entity);
    }
    private List<Vec3> threats(){
        var points=new ArrayList<Vec3>();
        for(var other:mob.level().getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(32),this::enemy)){
            boolean known=other instanceof Player||other==mob.getTarget()||other==mob.getLastHurtByMob()||other instanceof Mob m&&m.getTarget()==mob;
            if(known&&(mob.hasLineOfSight(other)||mob.distanceToSqr(other)<tuning.get().escapeDistance()*tuning.get().escapeDistance()))points.add(other.position());
        }
        if(lastThreat!=null)points.add(lastThreat);
        return points;
    }
    private double separation(Vec3 point,List<Vec3> threats){return threats.stream().mapToDouble(point::distanceTo).min().orElse(Double.POSITIVE_INFINITY);}
    private boolean groundSafe(){
        double feet=mob.getBoundingBox().minY;BlockPos below=BlockPos.containing(mob.getX(),feet-.05,mob.getZ());
        var ground=mob.level().getBlockState(below);var collision=ground.getCollisionShape(mob.level(),below);
        // Epic Fight can temporarily clear onGround while advancing a stationary action animation.
        // Check actual foot support instead, so digging cannot cancel its own grounded animation.
        return !collision.isEmpty()&&Math.abs(feet-(below.getY()+collision.max(Direction.Axis.Y)))<=.08&&!mob.isInWaterOrBubble()&&!mob.isInLava()&&mob.getVehicle()==null
            &&diggableColumn(below)
            &&mob.level().getFluidState(below).isEmpty();
    }
    private boolean diggableColumn(BlockPos surface){
        int depth=Math.max(2,(int)Math.ceil(mob.getBbHeight()*1.12));
        for(int i=0;i<depth;i++){
            var state=mob.level().getBlockState(surface.below(i));
            if(!substrate.test(state,surface.below(i))||!state.getFluidState().isEmpty())return false;
        }
        return true;
    }
    private boolean safe(List<Vec3> threats,boolean starting){
        distance=separation(mob.position(),threats);
        String reason=interrupted.getAsBoolean()?"hurt or stunned":!groundSafe()?"unsupported or non-burrowable ground":
            distance<(starting?tuning.get().escapeDistance():tuning.get().interruptDistance())?"threat too close":"safe";
        mob.getPersistentData().putString("RiftfallPanicSafety",reason);return reason.equals("safe");
    }
    @Override public boolean canUse(){return active()&&escapeAllowed();}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void start(){nextPath=0;mob.stopUsingItem();}
    @Override public void tick(){
        long now=mob.level().getGameTime();var s=tuning.get();var threats=threats();boolean safe=safe(threats,!burrowing());
        if(burrowing()){
            mob.getNavigation().stop();mob.setDeltaMovement(0,mob.getDeltaMovement().y,0);
            boolean ready=state.progress(now,s)>=1 && visuals.readyToEscape();
            if(state.advanceBurrow(now,safe,escapeAllowed(),s,ready)){
                // Last-moment checks run before discard, including the owner's in-rift restriction.
                mob.getPersistentData().putBoolean("RiftfallEscaped",true);mob.discard();return;
            }
            if(!burrowing()){visuals.abortBurrow();nextPath=0;}
            else if(state.progress(now,s)<1 && now%2==0){
                int depth=Math.max(2,(int)Math.ceil(mob.getBbHeight()*1.12));
                int layer=Math.min(depth-1,(int)(state.progress(now,s)*depth));
                var ground=BlockPos.containing(mob.getX(),mob.getBoundingBox().minY-.05,mob.getZ()).below(layer);
                visuals.dig(mob.level().getBlockState(ground));
            }
        }else if(state.beginBurrow(now,safe,escapeAllowed())){
            mob.getNavigation().stop();mob.stopUsingItem();visuals.beginBurrow(s.burrowTicks());
        }else if(now>=nextPath){
            nextPath=now+8;plan(threats,s);
        }
        publish();
    }
    private void plan(List<Vec3> threats,PanicSettings s){
        Vec3 nearest=threats.stream().min(Comparator.comparingDouble(mob.position()::distanceToSqr)).orElse(lastThreat);
        if(nearest==null){var point=LandRandomPos.getPos(mob,12,3);if(point!=null)mob.getNavigation().moveTo(point.x,point.y,point.z,s.fleeSpeed());return;}
        net.minecraft.world.level.pathfinder.Path best=null;double score=separation(mob.position(),threats);
        for(int i=0;i<12;i++){
            var point=LandRandomPos.getPosAway(mob,16,4,nearest);if(point==null)continue;
            var path=mob.getNavigation().createPath(BlockPos.containing(point),0);
            if(path==null||!path.canReach())continue;
            var end=Vec3.atBottomCenterOf(path.getEndNode().asBlockPos());
            double candidate=separation(end,threats);
            if(candidate>score+.25){score=candidate;best=path;}
        }
        if(best!=null){mob.getNavigation().moveTo(best,s.fleeSpeed());var end=best.getEndNode();mob.getLookControl().setLookAt(end.x,mob.getEyeY(),end.z,90,90);}
    }
    private void abort(){state.abortBurrow();visuals.abortBurrow();nextPath=0;}
    @Override public void stop(){mob.getNavigation().stop();if(burrowing())abort();publish();}
    private void publish(){
        boolean active=active();var data=mob.getPersistentData();
        data.putBoolean("RiftfallPanicking",active);data.putBoolean("RiftfallBurrowing",burrowing());
        data.putBoolean("RiftfallPanicEscapeAllowed",escapeAllowed());
        data.putDouble("RiftfallBurrowProgress",state.progress(mob.level().getGameTime(),tuning.get()));
        data.putBoolean("RiftfallBurrowWaitingForSound",state.stage()==PanicState.Stage.UNDERGROUND);
        visuals.update(active,active&&escapeAllowed(),data.getDouble("RiftfallBurrowProgress"));
    }
    public void signals(Map<String,Double> signals){
        signals.put("panic.active",active()?1D:0D);signals.put("panic.escape_allowed",escapeAllowed()?1D:0D);
        signals.put("panic.burrowing",burrowing()?1D:0D);signals.put("panic.progress",state.progress(mob.level().getGameTime(),tuning.get()));
        signals.put("panic.waiting_for_sound",state.stage()==PanicState.Stage.UNDERGROUND?1D:0D);
        if(Double.isFinite(distance))signals.put("panic.distance",distance);
    }
}
