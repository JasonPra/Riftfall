package org.Azkerash.riftfall;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** A short, interruptible rear ambush. All movement and escape progress are server-owned. */
public class GrappleGoal extends Goal {
    private static final Map<UUID,GrappleGoal> ACTIVE=new HashMap<>();
    private static final net.minecraft.resources.ResourceLocation RESTRAINT=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"goblin_grapple");
    private void restrain(boolean enable) {
        for (var type : java.util.List.of(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH)) {
            var attribute=victim.getAttribute(type); if (attribute==null) continue;
            attribute.removeModifier(RESTRAINT);
            if (enable) attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(RESTRAINT,-1,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
    public static boolean restrained(LivingEntity player) { return ACTIVE.containsKey(player.getUUID()); }
    /** Geometry is relative to the victim's facing; timings and damage come from the attack animation. */
    public record Settings(double reach,double backOffset,double climbHeight,int climbTicks,int cooldownTicks,double cooldownRadius) {
        public Settings {
            if(!Double.isFinite(reach)||reach<=0||!Double.isFinite(backOffset)||backOffset<0||!Double.isFinite(climbHeight)||climbHeight<0
                ||climbTicks<1||cooldownTicks<0||!Double.isFinite(cooldownRadius)||cooldownRadius<=0)throw new IllegalArgumentException("Invalid grapple settings");
        }
        public static Settings defaults(){return new Settings(1.5,.45,.55,8,AmbushCooldown.DEFAULT_RECOVERY_TICKS,AmbushCooldown.DEFAULT_RADIUS);}
    }
    /** Optional species-specific synchronized state; mechanics do not require a goblin entity. */
    public interface Visuals {
        default void began(Mob actor,LivingEntity target){}
        default void state(Mob actor,LivingEntity target,int escapeProgress){}
        default void released(Mob actor,LivingEntity target,boolean escaped){}
    }
    final Mob mob;
    private final AnimationAccessor<? extends AttackAnimation> animation;
    private final AnimationAccessor<? extends ActionAnimation> releaseAnimation;
    private final Settings settings;
    private final java.util.function.Predicate<Mob> allowed;
    private final Visuals visuals;
    private int duration;
    final GrappleStruggle struggle=new GrappleStruggle();
    LivingEntity victim;
    Vec3 anchor,latchFrom;
    long began,ready;
    boolean oldGravity,oldPhysics;

    public GrappleGoal(Mob mob,AnimationAccessor<? extends AttackAnimation> animation,
            AnimationAccessor<? extends ActionAnimation> releaseAnimation,Settings settings,
            java.util.function.Predicate<Mob> allowed,Visuals visuals) {
        this.mob=java.util.Objects.requireNonNull(mob);this.animation=java.util.Objects.requireNonNull(animation);
        this.releaseAnimation=releaseAnimation;this.settings=java.util.Objects.requireNonNull(settings);
        this.allowed=java.util.Objects.requireNonNull(allowed);this.visuals=java.util.Objects.requireNonNull(visuals);
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));
    }
    private LivingEntityPatch<?> patch(){return EpicFightCapabilities.getEntityPatch(mob,LivingEntityPatch.class);}
    private boolean interrupted(){var patch=patch();return patch==null||patch.isStunned();}
    public static boolean isBehind(Vec3 look,Vec3 offset) {
        return look.multiply(1,0,1).normalize().dot(offset.multiply(1,0,1).normalize())<-.65;
    }
    @Override public boolean canUse() {
        LivingEntity player=mob.getTarget();
        if (!allowed.test(mob) || player==null || player==mob || !mob.isAlive() || mob.isNoAi() || mob.level()!=player.level() || restrained(mob)
            || player instanceof Player p && (p.isCreative() || p.isSpectator())
            || !player.isAlive() || player.isPassenger() || player.isInWaterOrBubble() || !player.onGround()
            || BackstabSequence.captured(player) || ACTIVE.containsKey(player.getUUID()) || !mob.onGround() || interrupted()
            || mob.level().getGameTime()<ready || !AmbushCooldown.ready(mob) || BackstabSequence.captured(mob) || mob.isAlliedTo(player)) return false;
        Vec3 offset=mob.position().subtract(player.position());
        return offset.horizontalDistanceSqr()<settings.reach()*settings.reach() && Math.abs(offset.y)<.6 && isBehind(player.getLookAngle(),offset)
            && mob.getSensing().hasLineOfSight(player) && clearBack(player);
    }
    private Vec3 backPosition(LivingEntity player) {
        return player.position().subtract(Vec3.directionFromRotation(0,player.getYRot()).scale(settings.backOffset())).add(0,settings.climbHeight(),0);
    }
    private boolean clearBack(LivingEntity player) {
        return mob.level().noCollision(mob,mob.getBoundingBox().move(backPosition(player).subtract(mob.position())));
    }
    @Override public void start() {
        if(!canUse()){victim=null;return;}
        if(!org.Azkerash.riftfall.stamina.MobStamina.spend(mob,"grapple")){victim=null;return;}
        victim=mob.getTarget(); anchor=victim.position(); latchFrom=mob.position(); began=mob.level().getGameTime(); duration=Math.max(1,(int)Math.ceil(animation.get().getTotalTime()*20));
        ready=AmbushCooldown.begin(mob,duration,settings.cooldownTicks(),settings.cooldownRadius());
        visuals.began(mob,victim);
        oldGravity=mob.isNoGravity(); oldPhysics=mob.noPhysics; struggle.reset();
        ACTIVE.put(victim.getUUID(),this); restrain(true); visuals.state(mob,victim,0);
        if(victim instanceof ServerPlayer player)GrappleStatePayload.send(player,true,0);
        mob.getNavigation().stop(); mob.setNoGravity(true); mob.noPhysics=true;
        patch().playAnimationSynchronized(animation,0);
        mob.getPersistentData().putInt("RiftfallGrapples",mob.getPersistentData().getInt("RiftfallGrapples")+1);
    }
    private boolean valid() {
        return victim!=null && victim.isAlive() && !(victim instanceof ServerPlayer p && p.hasDisconnected())
            && !(victim instanceof Player p && (p.isCreative() || p.isSpectator())) && !victim.isRemoved()
            && !victim.isPassenger() && allowed.test(mob) && mob.isAlive() && !mob.isRemoved() && !mob.isNoAi() && !interrupted()
            && mob.level()==victim.level() && mob.level().getGameTime()-began<duration
            && victim.position().distanceToSqr(anchor)<16 && !mob.isAlliedTo(victim) && clearBack(victim);
    }
    @Override public boolean canContinueToUse() { return valid() && ACTIVE.get(victim.getUUID())==this; }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() {
        if (!valid()) { release(false); return; }
        mob.getNavigation().stop(); mob.setDeltaMovement(Vec3.ZERO);
        double climb=Math.min(1,(mob.level().getGameTime()-began)/(double)settings.climbTicks());
        mob.setPos(latchFrom.lerp(backPosition(victim),climb));
        mob.setYRot(victim.getYRot()); mob.setYBodyRot(victim.getYRot()); mob.setYHeadRot(victim.getYRot()); mob.fallDistance=0;
        // AttackAnimation phases own damage timing, joint colliders and hit locations.
        // No range damage or duplicate server-tick stab schedule is applied here.
    }
    private void release(boolean escaped) {
        if (victim==null) return;
        ACTIVE.remove(victim.getUUID(),this); restrain(false); visuals.released(mob,victim,escaped);
        if(victim instanceof ServerPlayer player && !player.hasDisconnected())GrappleStatePayload.send(player,false,0);
        mob.setNoGravity(oldGravity); mob.noPhysics=oldPhysics;
        if (escaped && mob.isAlive()) {
            Vec3 away=Vec3.directionFromRotation(0,victim.getYRot()).scale(-.35);
            mob.setDeltaMovement(away.x,.2,away.z); mob.hasImpulse=true;
            mob.getPersistentData().putInt("RiftfallGrappleEscapes",mob.getPersistentData().getInt("RiftfallGrappleEscapes")+1);
        }
        if (mob.isAlive() && !mob.isRemoved() && !interrupted() && releaseAnimation!=null) patch().playAnimationSynchronized(releaseAnimation,0);
        victim=null;
    }
    @Override public void stop() { release(false); }
    public static void struggle(ServerPlayer player) {
        var grapple=ACTIVE.get(player.getUUID());
        if (grapple==null || !grapple.valid()) return;
        if (grapple.struggle.press(player.level().getGameTime())) {
            grapple.visuals.state(grapple.mob,player,grapple.struggle.progress());
            GrappleStatePayload.send(player,true,grapple.struggle.progress());
            if (grapple.struggle.complete()) grapple.release(true);
        }
    }
    @SubscribeEvent public static void hold(EntityTickEvent.Post event) {
        if(event.getEntity() instanceof LivingEntity entity && !(entity instanceof Player) && !entity.level().isClientSide)hold(entity);
    }
    @SubscribeEvent public static void holdPlayer(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer player)hold(player);
    }
    private static void hold(LivingEntity player){
        var grapple=ACTIVE.get(player.getUUID()); if (grapple==null) return;
        if (!grapple.valid()) { grapple.release(false); return; }
        player.setDeltaMovement(Vec3.ZERO); player.fallDistance=0;
        // Keep camera aim free. Teleports/dimension changes break the hold instead of snapping the player back.
        if (player.position().distanceToSqr(grapple.anchor)>.0001) {
            if(player instanceof ServerPlayer serverPlayer)serverPlayer.connection.teleport(grapple.anchor.x,grapple.anchor.y,grapple.anchor.z,player.getYRot(),player.getXRot());
            else player.setPos(grapple.anchor);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        var grapple=ACTIVE.get(event.getEntity().getUUID()); if (grapple!=null) grapple.release(false);
    }
    @SubscribeEvent public static void leaving(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if(event.getLevel().isClientSide)return;
        for(var grapple:java.util.List.copyOf(ACTIVE.values()))
            if(grapple.mob==event.getEntity() || grapple.victim==event.getEntity())grapple.release(false);
    }
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for (var grapple : java.util.List.copyOf(ACTIVE.values())) grapple.release(false);
        ACTIVE.clear();
    }
}
