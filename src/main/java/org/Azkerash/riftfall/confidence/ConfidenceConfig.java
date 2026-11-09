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
            Map.ofEntries(Map.entry("base",5D),Map.entry("self.low_health",-9D),Map.entry("target.low_health",10D),
                Map.entry("self.poison",-8D),Map.entry("self.wither",-10D),Map.entry("self.harmful_effect",-4D),
                Map.entry("target.poison",8D),Map.entry("target.wither",8D),Map.entry("equipment.unarmed",-5D),
                Map.entry("equipment.shield_only",-3D),Map.entry("equipment.sword",3D),Map.entry("equipment.sword_shield",6D),
                Map.entry("equipment.bow",2D),Map.entry("equipment.bow_shield",1D),Map.entry("equipment.staff",4D),
                Map.entry("equipment.staff_shield",6D),Map.entry("equipment.other_weapon",2D),Map.entry("equipment.other_weapon_shield",4D)),40),
            25,.35,.30,.25);
    }
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> UNITS,FACTORS;
    private static final ModConfigSpec.DoubleValue SCALE,BOLD,WOUNDED,VULNERABLE,RETREATING;
    private static volatile Settings current=defaults();
    static {
        var defaults=defaults();var b=new ModConfigSpec.Builder();b.push("combatConfidence");
        UNITS=b.comment("Entity ID=confidence points. Only allied, alive, available nearby units contribute; the observer never counts itself.")
            .defineListAllowEmpty("unitWeights",entries(defaults.profile.unitWeights()),()->"example:unit=5",v->valid(v,true));
        FACTORS=b.comment("Factor key=signed points. Equipment factors are mutually exclusive; situational bonuses stack. Add keys for future combat behaviors.")
            .defineListAllowEmpty("factorWeights",entries(defaults.profile.factorWeights()),()->"example.factor=1",v->valid(v,false));
        SCALE=b.comment("Points corresponding to full confidence; final confidence is clamped to 0..1.").defineInRange("normalizationPoints",40D,1D,10000D);
        BOLD=b.comment("Points needed for organized retreat and bold tactics.").defineInRange("boldPoints",25D,0D,10000D);
        WOUNDED=b.defineInRange("woundedHealthFraction",.35D,0D,1D);
        VULNERABLE=b.defineInRange("vulnerableTargetHealthFraction",.30D,0D,1D);
        RETREATING=b.comment("Multiplier applied to the contribution of an ally that is retreating.").defineInRange("retreatingAllyMultiplier",.25D,0D,1D);
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
        for (String key:List.of("self.poison","self.wither","self.harmful_effect"))
            factors.putIfAbsent(key,defaults().profile().factorWeights().get(key));
        return factors;
    }
    public static void loaded(ModConfigEvent event) {
        if (event.getConfig().getSpec()==SPEC)
            current=new Settings(new ConfidenceProfile(parse(UNITS.get()),withStatusDefaults(parse(FACTORS.get())),SCALE.get()),BOLD.get(),WOUNDED.get(),VULNERABLE.get(),RETREATING.get());
    }
    public static Settings settings() { return current; }
    private ConfidenceConfig() {}
}
