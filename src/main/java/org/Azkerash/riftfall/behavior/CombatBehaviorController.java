package org.Azkerash.riftfall.behavior;

import java.util.*;
import java.util.function.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.Azkerash.riftfall.confidence.*;

/** Reusable confidence + local combat awareness. Does not choose factions or attach species-specific goals. */
public final class CombatBehaviorController {
    private final Mob mob;
    private final CombatConfidenceController confidence;
    private final int memoryTicks;
    private final BiConsumer<CombatSituation,Map<String,Double>> extraSignals;
    private LivingEntity remembered;
    private Vec3 lastSeen;
    private long seenAt;
    public CombatBehaviorController(Mob mob,Supplier<ConfidenceConfig.Settings> settings) {
        this(mob,new CombatConfidenceController(mob,settings),100,(s,signals)->{});
    }
    public CombatBehaviorController(Mob mob,CombatConfidenceController confidence,int memoryTicks,
            BiConsumer<CombatSituation,Map<String,Double>> extraSignals) {
        this.mob=Objects.requireNonNull(mob);this.confidence=Objects.requireNonNull(confidence);
        if (memoryTicks<0) throw new IllegalArgumentException("Negative target memory");
        this.memoryTicks=memoryTicks;this.extraSignals=Objects.requireNonNull(extraSignals);
    }
    private boolean valid(LivingEntity target) {
        return target!=null && target.isAlive() && target.level()==mob.level() && !target.isSpectator()
            && !(target instanceof Player p && p.isCreative()) && !mob.isAlliedTo(target) && mob.canAttack(target);
    }
    public CombatSituation observe() {
        if (mob.level().isClientSide) throw new IllegalStateException("Behavior sensing must run on the server");
        long now=mob.level().getGameTime();LivingEntity target=mob.getTarget();
        boolean visible=valid(target) && mob.distanceToSqr(target)<=Math.pow(mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE),2)
            && mob.getSensing().hasLineOfSight(target);
        if (visible) { remembered=target;lastSeen=target.position();seenAt=now; }
        else if (!valid(remembered) || now-seenAt>memoryTicks) { remembered=null;lastSeen=null; }
        // Hidden targets do not leak their current position, equipment or health into confidence inputs.
        var result=confidence.update(visible?target:null);var settings=confidence.settings();
        Map<String,Double> signals=new HashMap<>();
        signals.put("confidence",result.value());signals.put("fear",1-result.value());signals.put("confidence.points",result.points());
        signals.put("bold",result.atLeast(settings.boldPoints())?1D:0D);
        signals.put("self.health",(double)mob.getHealth()/mob.getMaxHealth());
        signals.put("self.wounded",mob.getHealth()<mob.getMaxHealth()*settings.woundedFraction()?1D:0D);
        signals.put("allies",(double)confidence.allyCount());
        signals.put("target.present",visible || remembered!=null?1D:0D);
        signals.put("target.health",visible?(double)target.getHealth()/target.getMaxHealth():0D);
        signals.put("target.poison",visible && target.hasEffect(net.minecraft.world.effect.MobEffects.POISON)?1D:0D);
        signals.put("target.wither",visible && target.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)?1D:0D);
        signals.put("target.visible",visible?1D:0D);signals.put("target.remembered",!visible && remembered!=null?1D:0D);
        signals.put("target.distance",lastSeen==null?0D:mob.position().distanceTo(lastSeen));
        var preliminary=new CombatSituation(new BehaviorContext(signals),result,visible?target:null,visible,lastSeen);
        extraSignals.accept(preliminary,signals);
        return new CombatSituation(new BehaviorContext(signals),result,visible?target:null,visible,lastSeen);
    }
    /** Called by the goal bridge; inspect with /data get entity ... NeoForgeData.RiftfallBehavior. */
    public void diagnostics(CombatSituation situation,BehaviorEngine.Decision decision) {
        var data=new net.minecraft.nbt.CompoundTag();
        data.putString("Action",decision.selected().orElse("none"));data.putDouble("Confidence",situation.confidence().value());
        data.putDouble("Points",situation.confidence().points());
        var scores=new net.minecraft.nbt.CompoundTag();decision.scores().forEach(scores::putDouble);data.put("Scores",scores);
        var factors=new net.minecraft.nbt.CompoundTag();situation.confidence().contributions().forEach(factors::putDouble);data.put("Contributions",factors);
        var sensed=new net.minecraft.nbt.CompoundTag();situation.context().signals().forEach(sensed::putDouble);data.put("Signals",sensed);
        mob.getPersistentData().put("RiftfallBehavior",data);
    }
}
