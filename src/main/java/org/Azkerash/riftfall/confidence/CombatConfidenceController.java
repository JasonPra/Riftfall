package org.Azkerash.riftfall.confidence;

import java.util.*;
import java.util.function.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Per-entity support cache and confidence inputs, independent of any species' goals. Server-side only. */
public final class CombatConfidenceController {
    private final Mob mob;
    private final Supplier<ConfidenceConfig.Settings> settings;
    private final Predicate<LivingEntity> allied;
    private final ToDoubleFunction<LivingEntity> availability;
    private final BiConsumer<LivingEntity,Map<String,Double>> extraFactors;
    private final double radius;
    private final int scanTicks;
    private long nextScan=Long.MIN_VALUE;
    private LivingEntity previousTarget;
    private Map<String,Double> units=Map.of();
    private int allyCount;
    public CombatConfidenceController(Mob mob,Supplier<ConfidenceConfig.Settings> settings) {
        this(mob,settings,mob::isAlliedTo,ally->1,(target,factors)->{},16,10);
    }
    /** Availability permits wounded/retreating discounts; extraFactors can add species-specific conditions. */
    public CombatConfidenceController(Mob mob,Supplier<ConfidenceConfig.Settings> settings,Predicate<LivingEntity> allied,
            ToDoubleFunction<LivingEntity> availability,BiConsumer<LivingEntity,Map<String,Double>> extraFactors,double radius,int scanTicks) {
        this.mob=Objects.requireNonNull(mob);this.settings=Objects.requireNonNull(settings);
        this.allied=Objects.requireNonNull(allied);this.availability=Objects.requireNonNull(availability);this.extraFactors=Objects.requireNonNull(extraFactors);
        if (!Double.isFinite(radius) || radius<=0 || scanTicks<1) throw new IllegalArgumentException("Invalid sensing interval/radius");
        this.radius=radius;this.scanTicks=scanTicks;
    }
    public ConfidenceEngine.Result update(LivingEntity target) {
        if (mob.level().isClientSide) throw new IllegalStateException("Confidence sensing must run on the server");
        long now=mob.level().getGameTime();
        if (target!=previousTarget || now>=nextScan) {
            previousTarget=target;nextScan=now+scanTicks;Map<String,Double> nearby=new HashMap<>();allyCount=0;
            for (var ally:mob.level().getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(radius,4,radius),
                    a->a!=mob && a.isAlive() && !a.isSpectator() && !(a instanceof Mob m && m.isNoAi())
                        && allied.test(a) && Math.abs(a.getY()-mob.getY())<=4
                        && (target==null || !(a instanceof Mob m) || m.getTarget()==null || m.getTarget()==target))) {
                double count=availability.applyAsDouble(ally);
                if (!Double.isFinite(count) || count<0) throw new IllegalArgumentException("Invalid ally availability");
                if (count>0) allyCount++;
                nearby.merge(BuiltInRegistries.ENTITY_TYPE.getKey(ally.getType()).toString(),count,Double::sum);
            }
            units=Map.copyOf(nearby);
        }
        var config=settings.get();
        var factors=CombatConfidence.factors(mob,target,config.woundedFraction(),config.vulnerableFraction());
        extraFactors.accept(target,factors);
        return ConfidenceEngine.evaluate(config.profile(),units,factors);
    }
    public int allyCount() { return allyCount; }
    public ConfidenceConfig.Settings settings() { return settings.get(); }
}
