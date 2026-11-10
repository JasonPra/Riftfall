package org.Azkerash.riftfall.stamina;

import java.util.*;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Balance values are server configurable, separate from confidence weights. */
public final class StaminaConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue MAX,REGEN,SPRINT;
    private static final ModConfigSpec.IntValue DELAY;
    private static final Map<String,ModConfigSpec.DoubleValue> COSTS=new LinkedHashMap<>();
    static {
        var b=new ModConfigSpec.Builder();
        MAX=b.defineInRange("maximum",40D,1D,10000D);
        REGEN=b.comment("Points per second. Panting doubles this; fleeing adds 25%, high confidence adds 15%. Bonuses do not stack.").defineInRange("regenerationPerSecond",4D,.01D,1000D);
        SPRINT=b.defineInRange("sprintPerSecond",4D,.01D,1000D);
        DELAY=b.defineInRange("regenerationDelayTicks",20,0,1200);
        b.push("costs");
        for(var e:Map.ofEntries(Map.entry("skill",6D),Map.entry("swipe",6D),Map.entry("flurry",12D),Map.entry("plunge",10D),Map.entry("grapple",14D),Map.entry("staff_sweep",8D),Map.entry("staff_smash",10D),Map.entry("cast",8D),Map.entry("heal",10D),Map.entry("backstab",12D),Map.entry("bow",5D),Map.entry("block_base",2D),Map.entry("block_per_damage",.5D)).entrySet())
            COSTS.put(e.getKey(),b.defineInRange(e.getKey(),e.getValue(),0D,10000D));
        b.pop();SPEC=b.build();
    }
    public static double maximum(){return MAX.get();}
    public static double regeneration(){return REGEN.get();}
    public static double sprint(){return SPRINT.get();}
    public static int delay(){return DELAY.get();}
    public static double cost(String action){var value=COSTS.get(action);if(value==null)throw new IllegalArgumentException("Unknown stamina cost: "+action);return value.get();}
    private StaminaConfig(){}
}
