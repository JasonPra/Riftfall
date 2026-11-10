package org.Azkerash.riftfall.mana;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-species base values; equipment modifies Iron's attributes on top of these. */
public final class ManaConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue MAX,REGEN;
    private static final ModConfigSpec.IntValue DELAY;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> PROFILES;
    public record Profile(double maximum,double regeneration){}
    static {
        var b=new ModConfigSpec.Builder();
        MAX=b.defineInRange("maximum",100D,0D,1000000D);
        REGEN=b.comment("Mana points per second before Iron's mana_regen multiplier.").defineInRange("regenerationPerSecond",3D,0D,10000D);
        DELAY=b.defineInRange("regenerationDelayTicks",40,0,12000);
        PROFILES=b.comment("entity_id=base capacity,points per second. Iron's native casters keep their own base attribute.")
            .defineListAllowEmpty("entityProfiles",List.of("riftfall:goblin_shaman=160,4"),()->"minecraft:pillager=100,3",v->v instanceof String s && valid(s));
        SPEC=b.build();
    }
    static boolean valid(String s){try{parse(s);return true;}catch(RuntimeException e){return false;}}
    private static Profile parse(String s){
        var pair=s.split("=",-1);if(pair.length!=2 || net.minecraft.resources.ResourceLocation.tryParse(pair[0])==null)throw new IllegalArgumentException();
        var numbers=pair[1].split(",",-1);if(numbers.length!=2)throw new IllegalArgumentException();
        double max=Double.parseDouble(numbers[0]),regen=Double.parseDouble(numbers[1]);
        if(!Double.isFinite(max)||!Double.isFinite(regen)||max<0||max>1000000||regen<0||regen>10000)throw new IllegalArgumentException();
        return new Profile(max,regen);
    }
    public static Profile profile(Mob mob){
        String id=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        for(String row:PROFILES.get())if(row.startsWith(id+"="))return parse(row);
        return new Profile(MAX.get(),REGEN.get());
    }
    public static int delay(){return DELAY.get();}
    private ManaConfig(){}
}
