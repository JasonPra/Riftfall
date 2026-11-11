package org.Azkerash.riftfall.crystals;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;

/** Server balance values. Initial geological deposits are separate from live condensation. */
public final class CrystalRules {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue THRESHOLD,COST,CHANCE,REFINE_MINUTES,REFINE_COST,REFINE_SCALE;
    private static final ModConfigSpec.BooleanValue WHITE;
    private static final ModConfigSpec.IntValue DEPOSIT;
    private static volatile double threshold=2000,cost=600,chance=.10;
    private static volatile boolean naturalWhite=true;
    private static volatile int depositSize=24;
    private static volatile double refineMinutes=215,refineCost=600,refineScale=1.25;
    static {
        var b=new ModConfigSpec.Builder();
        THRESHOLD=b.comment("Mana needed to nucleate a replacement seed after a deposit is completely mined out.").defineInRange("nucleationThreshold",2000.0,0,2000);
        COST=b.comment("Mana consumed by a successful spread or replacement seed. Existing height growth only needs this amount available.").defineInRange("spreadCost",600.0,.01,2000);
        CHANCE=b.comment("Branching probability per random tick; default vanilla randomTickSpeed still applies.").defineInRange("spreadChance",.10,0,1);
        WHITE=b.comment("Allow exceptionally rare white crystals in the strongest stable initial deposits.").define("naturalWhiteCrystals",true);
        DEPOSIT=b.comment("Maximum initial clusters per eligible cave chunk; about three quarters are mature.").defineInRange("initialDepositSize",24,1,128);
        REFINE_MINUTES=b.comment("Base minutes for red-to-orange refinement. Higher tiers take longer.").defineInRange("refinementMinutes",215.0,.01,1000000);
        REFINE_COST=b.comment("Mana spent on each successful crystal tier upgrade.").defineInRange("refinementManaCost",600.0,.01,2000);
        REFINE_SCALE=b.comment("Exponential time multiplier per tier, e.g. 1.25 means 25% longer each step.").defineInRange("refinementTimeMultiplierPerTier",1.25,1,10);
        SPEC=b.build();
    }
    public static boolean nucleation(double amount){return Double.isFinite(amount)&&amount>=threshold;}
    public static boolean growth(double amount){return Double.isFinite(amount)&&amount>=cost;}
    public static double cost(){return cost;}
    public static double chance(){return chance;}
    public static boolean naturalWhite(){return naturalWhite;}
    public static int depositSize(){return depositSize;}
    public static RefinementBatch.Settings refinement(){return new RefinementBatch.Settings(refineMinutes*1200,refineCost,refineScale);}
    public static void loaded(ModConfigEvent e){if(e.getConfig().getSpec()==SPEC){threshold=THRESHOLD.get();cost=COST.get();chance=CHANCE.get();naturalWhite=WHITE.get();depositSize=DEPOSIT.get();refineMinutes=REFINE_MINUTES.get();refineCost=REFINE_COST.get();refineScale=REFINE_SCALE.get();}}
    private CrystalRules(){}
}
