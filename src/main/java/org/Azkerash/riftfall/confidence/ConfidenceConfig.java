package org.Azkerash.riftfall.confidence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;

/** Server tuning for the initial shared combat profile; callers can also supply independent profiles. */
public final class ConfidenceConfig {
    public record Settings(ConfidenceProfile profile,double boldPoints,double woundedFraction,double vulnerableFraction,double retreatingMultiplier) {}
    public static Settings defaults() {
        return new Settings(new ConfidenceProfile(Map.of("riftfall:goblin",5D,"riftfall:goblin_archer",1D,"riftfall:goblin_shaman",10D),
            Map.ofEntries(Map.entry("combat.damage_advantage",4D),Map.entry("combat.armor_advantage",5D),Map.entry("combat.toughness_advantage",3D),Map.entry("combat.health_advantage",2D),Map.entry("base",5D),Map.entry("self.low_health",-9D),Map.entry("target.low_health",10D),
                Map.entry("self.witnessed_ally_death",-8D),Map.entry("self.poison",-8D),Map.entry("self.wither",-10D),Map.entry("self.harmful_effect",-4D),
                Map.entry("target.poison",8D),Map.entry("target.wither",8D),Map.entry("equipment.unarmed",-5D),
                Map.entry("equipment.shield_only",-3D),Map.entry("equipment.sword",3D),Map.entry("equipment.sword_shield",6D),
                Map.entry("equipment.bow",2D),Map.entry("equipment.bow_shield",1D),Map.entry("equipment.staff",4D),
                Map.entry("equipment.staff_shield",6D),Map.entry("equipment.other_weapon",2D),Map.entry("equipment.other_weapon_shield",4D)),40),
            25,.35,.30,.25);
    }
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> UNITS,FACTORS,OVERRIDES;
    private static final ModConfigSpec.IntValue WITNESS_DURATION,WITNESS_CAP;
    private static final ModConfigSpec.DoubleValue WITNESS_RADIUS;
    private static volatile WitnessSettings witness=WitnessSettings.defaults();
    private static volatile Map<String,Map<String,Double>> overrides=Map.of();
    private static final ModConfigSpec.DoubleValue SCALE,BOLD,WOUNDED,VULNERABLE,RETREATING,ENGAGE;
    private static volatile double engagePoints=12;
    private static volatile Settings current=defaults();
    static {
        var defaults=defaults();var b=new ModConfigSpec.Builder();b.push("combatConfidence");
        UNITS=b.comment("Entity ID=confidence points. Only allied, alive, available nearby units contribute; the observer never counts itself.")
            .defineListAllowEmpty("unitWeights",entries(defaults.profile.unitWeights()),()->"example:unit=5",v->valid(v,true));
        FACTORS=b.comment("Factor key=signed points. Equipment factors are mutually exclusive; situational bonuses stack. Add keys for future combat behaviors.")
            .defineListAllowEmpty("factorWeights",entries(defaults.profile.factorWeights()),()->"example.factor=1",v->valid(v,false));
        SCALE=b.comment("Points corresponding to full confidence; final confidence is clamped to 0..1.").defineInRange("normalizationPoints",40D,1D,10000D);
        BOLD=b.comment("Points needed for organized retreat and bold tactics.").defineInRange("boldPoints",25D,0D,10000D);
        ENGAGE=b.comment("Points needed to voluntarily advance into melee; cornered actors can still defend themselves.").defineInRange("engagePoints",12D,-1000D,1000D);
        WOUNDED=b.defineInRange("woundedHealthFraction",.35D,0D,1D);
        VULNERABLE=b.defineInRange("vulnerableTargetHealthFraction",.30D,0D,1D);
        RETREATING=b.comment("Multiplier applied to the contribution of an ally that is retreating.").defineInRange("retreatingAllyMultiplier",.25D,0D,1D);
        WITNESS_DURATION=b.defineInRange("witnessDurationTicks",200,0,72000);
        WITNESS_CAP=b.defineInRange("witnessMaxStacks",3,0,32);
        WITNESS_RADIUS=b.defineInRange("witnessRadius",24D,0D,128D);
        OVERRIDES=b.comment("Observer entity ID|key=value. Keys: factor.<name>, unit.<ally ID>, normalizationPoints, boldPoints, health fractions, retreatingAllyMultiplier, witnessRadius, witnessDurationTicks, witnessMaxStacks.")
            .defineListAllowEmpty("entityOverrides",List.<String>of(),()->"riftfall:behavior_tester|factor.self.witnessed_ally_death=-8",ConfidenceConfig::validOverride);
        b.pop();SPEC=b.build();
    }
    private static List<String> entries(Map<String,Double> values) {
        return values.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getKey()+"="+e.getValue()).toList();
    }
    private static boolean valid(Object value,boolean unit) {
        if (!(value instanceof String s)) return false;
        int split=s.lastIndexOf('=');if (split<=0) return false;
        String key=s.substring(0,split).trim();
        if (!key.matches(unit?"[a-z0-9_.-]+:[a-z0-9/._-]+":"[a-z0-9_.:-]+")) return false;
        try { double weight=Double.parseDouble(s.substring(split+1).trim());return Double.isFinite(weight) && weight>=(unit?0:-1000) && weight<=1000; }
        catch (NumberFormatException e) { return false; }
    }
    private static Map<String,Double> parse(List<? extends String> entries) {
        Map<String,Double> values=new LinkedHashMap<>();
        for (String s:entries) { int split=s.lastIndexOf('=');values.put(s.substring(0,split).trim(),Double.parseDouble(s.substring(split+1).trim())); }
        return values;
    }
    /** Existing TOML lists predate these factors. Explicit values, including zero, always win. */
    public static Map<String,Double> withStatusDefaults(Map<String,Double> configured) {
        Map<String,Double> factors=new LinkedHashMap<>(configured);
        for (String key:List.of("self.poison","self.wither","self.harmful_effect","self.witnessed_ally_death","combat.damage_advantage","combat.armor_advantage","combat.toughness_advantage","combat.health_advantage"))
            factors.putIfAbsent(key,defaults().profile().factorWeights().get(key));
        return factors;
    }
    private static boolean validOverride(Object value) {
        if (!(value instanceof String s)) return false;
        int bar=s.indexOf('|'),equal=s.lastIndexOf('=');
        if (bar<1 || equal<=bar+1 || !s.substring(0,bar).matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) return false;
        try { return EntityConfidenceTuning.valid(s.substring(bar+1,equal),Double.parseDouble(s.substring(equal+1))); }
        catch (NumberFormatException e) { return false; }
    }
    public static Settings forEntity(String id,Settings base) {
        var values=overrides.get(id);return values==null?base:EntityConfidenceTuning.apply(base,values);
    }
    public static double engagePointsFor(String id) { return overrides.getOrDefault(id,Map.of()).getOrDefault("engagePoints",engagePoints); }
    public static Settings settingsFor(String id) { return forEntity(id,current); }
    public static double maxWitnessRadius() {
        return Math.max(witness.radius(),overrides.values().stream().mapToDouble(v->v.getOrDefault("witnessRadius",0D)).max().orElse(0));
    }
    public static WitnessSettings witnessSettingsFor(String id) {
        return EntityConfidenceTuning.witness(witness,overrides.getOrDefault(id,Map.of()));
    }
    public static void loaded(ModConfigEvent event) {
        if (event.getConfig().getSpec()==SPEC) {
            current=new Settings(new ConfidenceProfile(parse(UNITS.get()),withStatusDefaults(parse(FACTORS.get())),SCALE.get()),BOLD.get(),WOUNDED.get(),VULNERABLE.get(),RETREATING.get());
            engagePoints=ENGAGE.get();
            witness=new WitnessSettings(WITNESS_RADIUS.get(),WITNESS_DURATION.get(),WITNESS_CAP.get());
            Map<String,Map<String,Double>> parsed=new LinkedHashMap<>();
            for (String entry:OVERRIDES.get()) {
                int bar=entry.indexOf('|'),equal=entry.lastIndexOf('=');
                parsed.computeIfAbsent(entry.substring(0,bar),key->new LinkedHashMap<>())
                    .put(entry.substring(bar+1,equal),Double.parseDouble(entry.substring(equal+1)));
            }
            Map<String,Map<String,Double>> frozen=new LinkedHashMap<>();parsed.forEach((key,value)->frozen.put(key,Map.copyOf(value)));
            overrides=Map.copyOf(frozen);
        }
    }
    public static Settings settings() { return current; }
    private ConfidenceConfig() {}
}
