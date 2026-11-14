package org.Azkerash.riftfall.fauna;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.Azkerash.riftfall.behavior.*;
import org.Azkerash.riftfall.confidence.*;
import org.Azkerash.riftfall.mana.MobMana;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;

/** Shared goblin utility/awareness engine, with braver pack-animal policies and quadruped actions. */
final class RyBehavior {
    final RyEntity mob;
    final CombatConfidenceController confidence;
    private final CombatBehaviorController awareness;
    private CombatSituation sensed;
    private int windCooldown=60,biteCooldown,actionTicks,windup,warningTicks;
    private long nextPath,retreatUntil,lastThreat;
    private boolean retreating;
    private LivingEntity victim,ward;
    private RyEntity adult;
    private Player intruder;
    private String active="idle";
    private static final BehaviorEngine ENGINE=RyBehaviorProfile.engine();
    RyBehavior(RyEntity mob) {
        this.mob=mob;
        confidence=new CombatConfidenceController(mob,RyBehavior::confidenceSettings,mob::isAlliedTo,
            a->a instanceof RyEntity ry&&(ry.isBaby()||ry.getHealth()<ry.getMaxHealth()*.25)?.25:1,
            (target,factors)->{},20,10);
        awareness=new CombatBehaviorController(mob,confidence,100,this::sense);
    }
    static ConfidenceConfig.Settings confidenceSettings() {
        // No "unarmed" penalty for a beast with natural weapons; less sensitive to fear than goblins.
        var factors=new HashMap<>(ConfidenceConfig.defaults().profile().factorWeights());
        factors.put("base",20D);factors.put("equipment.unarmed",0D);
        factors.put("self.low_health",-10D);factors.put("self.witnessed_ally_death",-3D);
        return new ConfidenceConfig.Settings(new ConfidenceProfile(Map.of("riftfall:ry",8D),factors,40),25,.25,.3,.5);
    }
    void upkeep() {
        if(windCooldown>0)windCooldown--;if(biteCooldown>0)biteCooldown--;
        if(mob.tickCount%20!=0)return;
        mob.getPersistentData().putString("NpcFaction","riftfall:magic_beasts");
        // Protect a wounded packmate or pup from its actual recent attacker.
        ward=null;adult=nearestAdult();
        for(var ry:mob.level().getEntitiesOfClass(RyEntity.class,mob.getBoundingBox().inflate(20),r->r!=mob&&r.isAlive())) {
            if(ry.isBaby()||ry.getHealth()<ry.getMaxHealth()*.4)ward=ry;
            var threat=ry.getLastHurtByMob();
            if(!mob.isBaby()&&mob.getTarget()==null&&threat!=null&&threat.isAlive()&&!mob.isAlliedTo(threat)
                &&ry.tickCount-ry.getLastHurtByMobTimestamp()<100&&mob.hasLineOfSight(threat))mob.setTarget(threat);
        }
        // Wild packs warn before attacking close intruders; no universal player hatred.
        if(!mob.isTame()&&!mob.isBaby()&&mob.getTarget()==null) {
            var nearby=mob.level().getNearestPlayer(mob,6);
            if(nearby!=null&&!nearby.isCreative()&&!nearby.isSpectator()&&mob.hasLineOfSight(nearby)
                &&mob.withinTerritory(nearby.blockPosition(),24)) {
                if(intruder!=nearby){intruder=nearby;warningTicks=0;}
                warningTicks+=20;
                if(warningTicks==20)mob.playSound(net.minecraft.sounds.SoundEvents.WOLF_GROWL,1,.75F);
                if(warningTicks>=80)mob.setTarget(nearby);
            } else {intruder=null;warningTicks=0;}
        }
        if(mob.getTarget()!=null&&(mob.isAlliedTo(mob.getTarget())||!mob.getTarget().isAlive()
            ||mob.getTarget() instanceof Player p&&(p.isCreative()||p.isSpectator())
            ||!mob.isTame()&&!mob.withinTerritory(mob.blockPosition(),56)))mob.setTarget(null);
    }
    private void sense(CombatSituation s,Map<String,Double> v) {
        sensed=s;long now=mob.level().getGameTime();double health=mob.getHealth()/mob.getMaxHealth();
        boolean threat=s.context().active("target.present"),visible=s.targetVisible(),interrupted=RyPatch.interrupted(mob);
        if(threat)lastThreat=now;
        if(RyBehaviorProfile.shouldRetreat(health,s.confidence().points(),mob.isBaby(),threat)){retreating=true;retreatUntil=now+80;}
        if(retreating&&now>=retreatUntil&&(!threat||health>=.42))retreating=false;
        mob.getPersistentData().putBoolean("RiftfallRetreating",retreating);
        v.put("interrupted",interrupted?1D:0D);
        boolean committed=(active.equals("cast")&&actionTicks>0&&actionTicks<windup+9)
            ||active.equals("bite")&&actionTicks>0&&actionTicks<14;
        for(String id:ENGINE.behaviorIds()){v.put("ready."+id,0D);v.put("committed."+id,0D);}
        if(interrupted)return;
        if(committed){v.put("ready."+active,1D);v.put("committed."+active,1D);return;}
        if(retreating&&threat){v.put("ready.retreat",1D);return;}
        if(visible&&!mob.isBaby()) {
            double distance=s.context().get("target.distance");
            boolean canCast=windCooldown==0&&distance>=2&&distance<=12&&MobMana.canCast(mob,SpellRegistry.GUST_SPELL.get(),1);
            v.put("ready.cast",canCast?1D:0D);
            v.put("ready.bite",biteCooldown==0&&distance<=2.4?1D:0D);
            v.put("ready.flank",s.context().get("allies")>0&&distance>2.4&&distance<10?1D:0D);
            v.put("ready.pursue",1D);
        } else if(threat) v.put("ready.search",1D);
        else {
            v.put("ready.regroup",mob.isBaby()&&adult!=null&&mob.distanceTo(adult)>5?1D:0D);
            v.put("ready.guard",!mob.isBaby()&&ward!=null&&mob.distanceTo(ward)>4?1D:0D);
            v.put("ready.warn",intruder!=null?1D:0D);
            v.put("ready.rest",health<.8&&now-lastThreat>200?1D:0D);
            v.put("ready.patrol",mob.isInSittingPose()?0D:1D);
        }
    }
    private RyEntity nearestAdult() {
        return mob.level().getEntitiesOfClass(RyEntity.class,mob.getBoundingBox().inflate(24),r->r!=mob&&!r.isBaby()&&r.isAlive())
            .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
    }
    WeightedBehaviorGoal goal() {
        Map<String,BehaviorAction> actions=new HashMap<>();
        for(String id:ENGINE.behaviorIds())actions.put(id,new BehaviorAction(){
            @Override public void start(CombatSituation s){begin(id,s);}
            @Override public void tick(CombatSituation s){execute(id,s);}
            @Override public void stop(){finish(id);}
        });
        return new WeightedBehaviorGoal(awareness,ENGINE,actions);
    }
    private void begin(String id,CombatSituation s) {
        active=id;actionTicks=0;nextPath=0;victim=s.target();
        if(id.equals("cast")) {
            windup=Math.max(20,MobMana.windup(mob,SpellRegistry.GUST_SPELL.get(),1));
            mob.getNavigation().stop();mob.getPersistentData().putBoolean("RiftfallCasting",true);
            RyPatch.play(mob,RyAnimations.CHARGE);
            SpellRegistry.GUST_SPELL.get().playSound(SpellRegistry.GUST_SPELL.get().getCastStartSound(),mob);
        }
        if(id.equals("bite")){mob.getNavigation().stop();biteCooldown=28;RyPatch.play(mob,RyAnimations.BITE);}
    }
    private void execute(String id,CombatSituation s) {
        actionTicks++;mob.setAggressive(s.targetVisible()||id.equals("warn"));mob.setSprinting(id.equals("retreat")||id.equals("pursue"));
        if(s.target()!=null)mob.getLookControl().setLookAt(s.target(),45,45);
        switch(id) {
            case "cast" -> cast(s);
            case "bite" -> {if(actionTicks==7&&victim!=null&&victim.isAlive()&&!mob.isAlliedTo(victim)&&mob.distanceTo(victim)<2.8&&mob.hasLineOfSight(victim))mob.doHurtTarget(victim);}
            case "retreat" -> {if(s.lastSeenPosition()!=null)pathAway(s.lastSeenPosition());}
            case "pursue" -> path(s.lastSeenPosition(),1.15);
            case "flank" -> {
                if(s.target()!=null){double angle=Math.atan2(mob.getZ()-s.target().getZ(),mob.getX()-s.target().getX())+(mob.getUUID().getLeastSignificantBits()%2==0?.55:-.55);
                    double radius=Math.max(1.8,mob.distanceTo(s.target())-.7);path(s.target().position().add(Math.cos(angle)*radius,0,Math.sin(angle)*radius),1.1);}
            }
            case "search" -> path(s.lastSeenPosition(),1);
            case "regroup" -> {if(adult!=null)path(adult.position(),1.1);}
            case "guard" -> {if(ward!=null)path(ward.position(),.95);}
            case "warn" -> {mob.getNavigation().stop();if(intruder!=null)mob.getLookControl().setLookAt(intruder,45,45);}
            case "rest" -> {mob.getNavigation().stop();if(mob.tickCount%40==0&&mob.hurtTime==0)mob.heal(.5F);}
            case "patrol" -> patrol();
            default -> mob.getNavigation().stop();
        }
    }
    private void cast(CombatSituation s) {
        if(victim==null||!victim.isAlive()||mob.isAlliedTo(victim)||!mob.hasLineOfSight(victim)||mob.distanceTo(victim)>14){actionTicks=windup+9;return;}
        mob.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,victim.getEyePosition());
        if(actionTicks<windup&&actionTicks%4==0)((net.minecraft.server.level.ServerLevel)mob.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,mob.getX(),mob.getEyeY(),mob.getZ(),2,.2,.1,.2,.02);
        if(actionTicks==windup) {
            var spell=SpellRegistry.GUST_SPELL.get();var data=MobMana.data(mob);
            if(spell.checkPreCastConditions(mob.level(),1,mob,data)&&MobMana.spend(mob,spell,1)) {
                RyPatch.play(mob,RyAnimations.RELEASE);
                spell.onServerPreCast(mob.level(),1,mob,data);spell.onCast(mob.level(),1,mob,CastSource.MOB,data);spell.onServerCastComplete(mob.level(),1,mob,data,false);
                mob.getPersistentData().putInt("RyWindCasts",mob.getPersistentData().getInt("RyWindCasts")+1);
            }
            windCooldown=MobMana.cooldown(mob,200);
        }
    }
    private void finish(String id) {
        mob.setSprinting(false);mob.getNavigation().stop();
        if(id.equals("cast")){mob.getPersistentData().putBoolean("RiftfallCasting",false);MobMana.data(mob).resetCastingState();windCooldown=Math.max(windCooldown,40);}
    }
    private void path(Vec3 pos,double speed) {
        if(pos==null||mob.level().getGameTime()<nextPath)return;nextPath=mob.level().getGameTime()+10;
        mob.getNavigation().moveTo(pos.x,pos.y,pos.z,speed);
    }
    private void pathAway(Vec3 threat) {
        if(mob.level().getGameTime()<nextPath)return;
        var ally=adult;
        if(ally!=null&&ally.position().distanceToSqr(threat)>mob.position().distanceToSqr(threat)&&mob.distanceTo(ally)>4)path(ally.position(),1.25);
        else {var pos=DefaultRandomPos.getPosAway(mob,16,7,threat);if(pos!=null)path(pos,1.3);else nextPath=mob.level().getGameTime()+10;}
    }
    private void patrol() {
        if(!mob.getNavigation().isDone())return;
        if(!mob.withinTerritory(mob.blockPosition(),20)){path(Vec3.atBottomCenterOf(mob.territory()),.85);return;}
        if(mob.level().getGameTime()<nextPath)return;
        var pos=DefaultRandomPos.getPos(mob,10,5);nextPath=mob.level().getGameTime()+40;
        if(pos!=null&&mob.withinTerritory(net.minecraft.core.BlockPos.containing(pos),24))mob.getNavigation().moveTo(pos.x,pos.y,pos.z,.75);
    }
}
