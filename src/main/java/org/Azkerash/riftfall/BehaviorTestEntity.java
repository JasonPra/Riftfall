package org.Azkerash.riftfall;

import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.Azkerash.riftfall.behavior.*;
import org.Azkerash.riftfall.confidence.*;

/** Spawn-egg-only diagnostic actor. No goblin AI, natural spawning, Epic Fight patch or loot. */
public final class BehaviorTestEntity extends Monster {
    private CombatBehaviorController behavior;
    private long nextPath,nextAttack;
    public BehaviorTestEntity(EntityType<? extends BehaviorTestEntity> type,Level level) {
        super(type,level);setPersistenceRequired();xpReward=0;
    }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.MOVEMENT_SPEED,.23)
            .add(Attributes.FOLLOW_RANGE,24).add(Attributes.ATTACK_DAMAGE,2);
    }
    @Override protected void registerGoals() {
        // This is the full integration: sensing controller + policy + action implementations.
        var confidence=new CombatConfidenceController(this,BehaviorTestProfile::confidenceSettings,this::isAlliedTo,
            ally->ally.getPersistentData().getBoolean("RiftfallBehaviorRetreating")
                ? BehaviorTestProfile.confidenceSettings().retreatingMultiplier():1,(target,factors)->{},16,10);
        behavior=new CombatBehaviorController(this,confidence,100,(s,signals)->{});
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new WeightedBehaviorGoal(behavior,BehaviorTestProfile.engine(),Map.of(
            "idle",s->getNavigation().stop(),
            "watch",s->{ getNavigation().stop();if (s.target()!=null) getLookControl().setLookAt(s.target(),45,45); },
            "approach",movement(false),
            "search",movement(true),
            "attack",s->{
                getNavigation().stop();
                if (s.target()!=null) {
                    getLookControl().setLookAt(s.target(),45,45);
                    if (tickCount>=nextAttack && getSensing().hasLineOfSight(s.target()) && distanceToSqr(s.target())<=1.8*1.8) {
                        swing(net.minecraft.world.InteractionHand.MAIN_HAND);doHurtTarget(s.target());nextAttack=tickCount+25;
                    }
                }
            },
            "retreat",new BehaviorAction() {
                @Override public void start(CombatSituation s) { nextPath=0;getPersistentData().putBoolean("RiftfallBehaviorRetreating",true); }
                @Override public void stop() { getNavigation().stop();getPersistentData().putBoolean("RiftfallBehaviorRetreating",false);nextPath=0; }
                @Override public void tick(CombatSituation s) {
                    if (s.target()!=null) getLookControl().setLookAt(s.target(),45,45);
                    if (tickCount<nextPath || s.lastSeenPosition()==null) return;
                    nextPath=tickCount+10;
                    var point=LandRandomPos.getPosAway(BehaviorTestEntity.this,10,3,s.lastSeenPosition());
                    if (point!=null) getNavigation().moveTo(point.x,point.y,point.z,1.15);
                }
            }
        )));
        targetSelector.addGoal(0,new HurtByTargetGoal(this));
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    private BehaviorAction movement(boolean search) {
        return new BehaviorAction() {
            @Override public void start(CombatSituation s) { nextPath=0; }
            @Override public void tick(CombatSituation s) { approach(s,search); }
            @Override public void stop() { getNavigation().stop(); }
        };
    }
    private void approach(CombatSituation s,boolean search) {
        if (s.lastSeenPosition()==null) return;
        if (s.target()!=null) getLookControl().setLookAt(s.target(),45,45);
        if (tickCount<nextPath) return;
        nextPath=tickCount+10;var pos=s.lastSeenPosition();
        if (search && position().distanceToSqr(pos)<1) { getNavigation().stop();return; }
        getNavigation().moveTo(pos.x,pos.y,pos.z,search?.8:1);
    }
    @Override public boolean isAlliedTo(Entity other) {
        return other instanceof BehaviorTestEntity && getTeam()==null && other.getTeam()==null || super.isAlliedTo(other);
    }
    @Override public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && tickCount%5==0) {
            var debug=getPersistentData().getCompound("RiftfallBehavior");
            setCustomName(Component.literal(String.format(java.util.Locale.ROOT,"%s | confidence %.0f%% | %.1f pts",
                debug.getString("Action"),debug.getDouble("Confidence")*100,debug.getDouble("Points"))));
            setCustomNameVisible(true);
        }
    }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        getPersistentData().remove("RiftfallBehaviorRetreating");
        getPersistentData().remove("RiftfallBehavior");
    }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
}
