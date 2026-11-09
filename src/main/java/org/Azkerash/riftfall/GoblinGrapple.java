package org.Azkerash.riftfall;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** A short, interruptible rear ambush. All movement and escape progress are server-owned. */
final class GoblinGrapple extends Goal {
    private static final Map<UUID,GoblinGrapple> ACTIVE=new HashMap<>();
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
    static final int DURATION=80;
    final GoblinEntity mob;
    final GrappleStruggle struggle=new GrappleStruggle();
    ServerPlayer victim;
    Vec3 anchor,latchFrom;
    long began,ready;
    boolean oldGravity,oldPhysics;

    GoblinGrapple(GoblinEntity mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP)); }
    static boolean isBehind(Vec3 look,Vec3 offset) {
        return look.multiply(1,0,1).normalize().dot(offset.multiply(1,0,1).normalize())<-.65;
    }
    @Override public boolean canUse() {
        if (!(mob.getTarget() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()
            || !player.isAlive() || player.isPassenger() || player.isInWaterOrBubble() || !player.onGround()
            || ACTIVE.containsKey(player.getUUID()) || !mob.onGround() || GoblinTactics.interrupted(mob)
            || mob.level().getGameTime()<ready || mob.isAlliedTo(player)) return false;
        Vec3 offset=mob.position().subtract(player.position());
        return offset.horizontalDistanceSqr()<1.5*1.5 && Math.abs(offset.y)<.6 && isBehind(player.getLookAngle(),offset)
            && mob.getSensing().hasLineOfSight(player) && clearBack(player);
    }
    private Vec3 backPosition(ServerPlayer player) {
        return player.position().subtract(Vec3.directionFromRotation(0,player.getYRot()).scale(.45)).add(0,.55,0);
    }
    private boolean clearBack(ServerPlayer player) {
        return mob.level().noCollision(mob,mob.getBoundingBox().move(backPosition(player).subtract(mob.position())));
    }
    @Override public void start() {
        victim=(ServerPlayer)mob.getTarget(); anchor=victim.position(); latchFrom=mob.position(); began=mob.level().getGameTime(); ready=began+200;
        GoblinBuckler.lowerForAttack(mob,20);
        oldGravity=mob.isNoGravity(); oldPhysics=mob.noPhysics; struggle.reset();
        ACTIVE.put(victim.getUUID(),this); restrain(true); mob.grappleState(victim.getId(),0);
        mob.getNavigation().stop(); mob.setNoGravity(true); mob.noPhysics=true;
        GoblinAnimations.play(mob,GoblinAnimations.GRAPPLE);
        mob.getPersistentData().putInt("RiftfallGrapples",mob.getPersistentData().getInt("RiftfallGrapples")+1);
    }
    private boolean valid() {
        return victim!=null && victim.isAlive() && !victim.hasDisconnected() && !victim.isCreative() && !victim.isSpectator()
            && !victim.isPassenger() && mob.isAlive() && !mob.isRemoved() && !mob.isNoAi() && !GoblinTactics.interrupted(mob)
            && mob.level()==victim.level() && mob.level().getGameTime()-began<DURATION
            && victim.position().distanceToSqr(anchor)<16 && !mob.isAlliedTo(victim) && clearBack(victim);
    }
    @Override public boolean canContinueToUse() { return valid() && ACTIVE.get(victim.getUUID())==this; }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() {
        if (!valid()) { release(false); return; }
        mob.getNavigation().stop(); mob.setDeltaMovement(Vec3.ZERO);
        double climb=Math.min(1,(mob.level().getGameTime()-began)/8.0);
        mob.setPos(latchFrom.lerp(backPosition(victim),climb));
        mob.setYRot(victim.getYRot()); mob.setYBodyRot(victim.getYRot()); mob.setYHeadRot(victim.getYRot()); mob.fallDistance=0;
        int tick=(int)(mob.level().getGameTime()-began);
        if (GrappleStruggle.stabsAt(tick)) {
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (victim.hurt(mob.damageSources().mobAttack(mob),1))
                mob.getPersistentData().putInt("RiftfallBackstabs",mob.getPersistentData().getInt("RiftfallBackstabs")+1);
        }
    }
    private void release(boolean escaped) {
        if (victim==null) return;
        ACTIVE.remove(victim.getUUID(),this); restrain(false); mob.grappleState(-1,0);
        mob.setNoGravity(oldGravity); mob.noPhysics=oldPhysics;
        if (escaped && mob.isAlive()) {
            Vec3 away=Vec3.directionFromRotation(0,victim.getYRot()).scale(-.35);
            mob.setDeltaMovement(away.x,.2,away.z); mob.hasImpulse=true;
            mob.getPersistentData().putInt("RiftfallGrappleEscapes",mob.getPersistentData().getInt("RiftfallGrappleEscapes")+1);
        }
        if (mob.isAlive() && !GoblinTactics.interrupted(mob)) GoblinAnimations.play(mob,GoblinAnimations.GRAPPLE_RELEASE);
        victim=null;
    }
    @Override public void stop() { release(false); }
    static void struggle(ServerPlayer player) {
        var grapple=ACTIVE.get(player.getUUID());
        if (grapple==null || !grapple.valid()) return;
        if (grapple.struggle.press(player.level().getGameTime())) {
            grapple.mob.grappleState(player.getId(),grapple.struggle.progress());
            if (grapple.struggle.complete()) grapple.release(true);
        }
    }
    @SubscribeEvent public static void hold(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var grapple=ACTIVE.get(player.getUUID()); if (grapple==null) return;
        if (!grapple.valid()) { grapple.release(false); return; }
        player.setDeltaMovement(Vec3.ZERO); player.fallDistance=0;
        // Keep camera aim free. Teleports/dimension changes break the hold instead of snapping the player back.
        if (player.position().distanceToSqr(grapple.anchor)>.0001)
            player.connection.teleport(grapple.anchor.x,grapple.anchor.y,grapple.anchor.z,player.getYRot(),player.getXRot());
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        var grapple=ACTIVE.get(event.getEntity().getUUID()); if (grapple!=null) grapple.release(false);
    }
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for (var grapple : java.util.List.copyOf(ACTIVE.values())) grapple.release(false);
        ACTIVE.clear();
    }
}
