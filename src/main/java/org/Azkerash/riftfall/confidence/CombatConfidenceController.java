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
    private final WitnessedDeathMemory witnessed=new WitnessedDeathMemory();
    private Map<String,Double> lastFactors=Map.of();
    private java.util.List<net.minecraft.nbt.CompoundTag> allyDetails=java.util.List.of(),excludedDetails=java.util.List.of();
    public CombatConfidenceController(Mob mob) { this(mob,ConfidenceConfig::settings); }
    private String entityId() { return BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(); }
    public WitnessSettings witnessSettings() { return ConfidenceConfig.witnessSettingsFor(entityId()); }
    public void witnessDeath(LivingEntity victim,LivingEntity killer) {
        var config=witnessSettings();
        if (config.radius()==0 || !allied.test(victim) || killer==mob || allied.test(killer)
                || mob.distanceToSqr(victim)>config.radius()*config.radius() || !mob.hasLineOfSight(victim)) return;
        witnessed.remember(mob.level().getGameTime(),config,victim.getUUID(),BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString(),
            killer.getUUID(),BuiltInRegistries.ENTITY_TYPE.getKey(killer.getType()).toString());
        nextScan=Long.MIN_VALUE;
    }
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
            previousTarget=target;nextScan=now+scanTicks;Map<String,Double> nearby=new HashMap<>();allyCount=0;var details=new ArrayList<net.minecraft.nbt.CompoundTag>();var excluded=new ArrayList<net.minecraft.nbt.CompoundTag>();
            for (var ally:mob.level().getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(radius,4,radius),a->a!=mob)) {
                String reason=!ally.isAlive()?"dead":ally.isSpectator()?"spectator":ally instanceof Mob m && m.isNoAi()?"AI disabled":
                    !allied.test(ally)?"not allied":Math.abs(ally.getY()-mob.getY())>4?"vertical distance":
                    target!=null && ally instanceof Mob m && m.getTarget()!=null && m.getTarget()!=target?"different enemy":null;
                if (reason!=null) {
                    var entry=new net.minecraft.nbt.CompoundTag();entry.putUUID("UUID",ally.getUUID());
                    entry.putString("Type",BuiltInRegistries.ENTITY_TYPE.getKey(ally.getType()).toString());entry.putString("Reason",reason);excluded.add(entry);continue;
                }
                double count=availability.applyAsDouble(ally);
                if (!Double.isFinite(count) || count<0) throw new IllegalArgumentException("Invalid ally availability");
                if (count>0) allyCount++;
                String id=BuiltInRegistries.ENTITY_TYPE.getKey(ally.getType()).toString();
                nearby.merge(id,count,Double::sum);
                var detail=new net.minecraft.nbt.CompoundTag();detail.putUUID("UUID",ally.getUUID());detail.putString("Type",id);
                detail.putDouble("Distance",mob.distanceTo(ally));detail.putDouble("Availability",count);details.add(detail);
            }
            units=Map.copyOf(nearby);allyDetails=List.copyOf(details);excludedDetails=List.copyOf(excluded);
        }
        var config=settings();
        var factors=CombatConfidence.factors(mob,target,config.woundedFraction(),config.vulnerableFraction());
        int deaths=witnessed.active(now,witnessSettings().maxStacks()).size();
        if (deaths>0) factors.put("self.witnessed_ally_death",(double)deaths);
        extraFactors.accept(target,factors);
        lastFactors=Map.copyOf(factors);
        return ConfidenceEngine.evaluate(config.profile(),units,factors);
    }
    public int allyCount() { return allyCount; }
    public ConfidenceConfig.Settings settings() { return ConfidenceConfig.forEntity(entityId(),settings.get()); }
    public net.minecraft.nbt.CompoundTag diagnostics() {
        var data=new net.minecraft.nbt.CompoundTag();var config=settings();var witnessConfig=witnessSettings();
        data.putString("ObserverType",entityId());data.putFloat("Health",mob.getHealth());data.putFloat("MaxHealth",mob.getMaxHealth());
        data.putString("MainHand",BuiltInRegistries.ITEM.getKey(mob.getMainHandItem().getItem()).toString());
        data.putString("OffHand",BuiltInRegistries.ITEM.getKey(mob.getOffhandItem().getItem()).toString());
        var effects=new net.minecraft.nbt.ListTag();
        for (var effect:mob.getActiveEffects()) {
            var entry=new net.minecraft.nbt.CompoundTag();entry.putString("Type",BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString());
            entry.putString("Category",effect.getEffect().value().getCategory().name());entry.putInt("Amplifier",effect.getAmplifier());entry.putInt("RemainingTicks",effect.getDuration());effects.add(entry);
        }
        data.put("ActiveEffects",effects);
        data.putDouble("NormalizationPoints",config.profile().normalizationPoints());data.putDouble("BoldPoints",config.boldPoints());
        data.putDouble("WoundedHealthFraction",config.woundedFraction());data.putDouble("VulnerableTargetHealthFraction",config.vulnerableFraction());
        data.putDouble("RetreatingAllyMultiplier",config.retreatingMultiplier());data.putDouble("SupportRadius",radius);data.putInt("ScanIntervalTicks",scanTicks);
        var inputs=new net.minecraft.nbt.CompoundTag();
        lastFactors.forEach((key,value)-> {
            var entry=new net.minecraft.nbt.CompoundTag();double weight=config.profile().factorWeights().getOrDefault(key,0D);
            entry.putDouble("Input",value);entry.putDouble("Weight",weight);entry.putDouble("Points",value*weight);inputs.put(key,entry);
        });
        data.put("ActiveFactors",inputs);
        var allWeights=new net.minecraft.nbt.CompoundTag();config.profile().factorWeights().forEach(allWeights::putDouble);data.put("FactorWeights",allWeights);
        var allUnits=new net.minecraft.nbt.CompoundTag();config.profile().unitWeights().forEach(allUnits::putDouble);data.put("UnitWeights",allUnits);
        var allies=new net.minecraft.nbt.ListTag();
        for (var detail:allyDetails) {
            var copy=detail.copy();double weight=config.profile().unitWeights().getOrDefault(copy.getString("Type"),0D);
            copy.putDouble("Weight",weight);copy.putDouble("Points",weight*copy.getDouble("Availability"));allies.add(copy);
        }
        data.put("Allies",allies);
        var excluded=new net.minecraft.nbt.ListTag();excludedDetails.forEach(entry->excluded.add(entry.copy()));data.put("ExcludedCandidates",excluded);
        data.putLong("LastSupportScanTick",nextScan==Long.MIN_VALUE?Long.MIN_VALUE:nextScan-scanTicks);data.putLong("NextSupportScanTick",nextScan);
        data.putDouble("WitnessRadius",witnessConfig.radius());data.putInt("WitnessDurationTicks",witnessConfig.durationTicks());data.putInt("WitnessMaxStacks",witnessConfig.maxStacks());
        var memories=new net.minecraft.nbt.ListTag();long now=mob.level().getGameTime();
        for (var incident:witnessed.active(now,witnessConfig.maxStacks())) {
            var entry=new net.minecraft.nbt.CompoundTag();entry.putUUID("VictimUUID",incident.victim());entry.putString("VictimType",incident.victimType());
            entry.putUUID("KillerUUID",incident.killer());entry.putString("KillerType",incident.killerType());
            entry.putLong("RemainingTicks",incident.expiresAt()-now);memories.add(entry);
        }
        data.put("WitnessedDeaths",memories);return data;
    }
}
