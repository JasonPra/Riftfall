package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.*;

/** Shared biome/terrain transition coordinates. The mana footprint remains unchanged. */
public final class GeyserTransition {
    public static final int WIDTH=384;
    public enum Climate {TEMPERATE,WARM,COLD,DRY,MOUNTAIN,WARM_SEA,SEA,COLD_SEA,FROZEN_SEA}
    public static boolean ocean(Climate c){return c==Climate.WARM_SEA||c==Climate.SEA||c==Climate.COLD_SEA||c==Climate.FROZEN_SEA;}
    /** Negative inside the field, positive outside, in blocks. */
    public static double distance(GeyserRegion g,double x,double z){
        return GeyserShape.distance(g,x,z);
    }
    public static double progress(double distance,double x,double z){
        double t=Math.clamp(distance/WIDTH,0,1);
        double irregular=Math.sin(Math.PI*t)*(12*Math.sin(x/63)+16*Math.cos(z/71));
        return Math.clamp((distance+irregular)/WIDTH,0,1);
    }
    private static double smooth(double t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
    public static ResourceKey<Biome> biome(Climate climate,boolean deep,double t){
        if(t<.20)return climate==Climate.COLD||climate==Climate.FROZEN_SEA?Biomes.SNOWY_PLAINS:climate==Climate.DRY?Biomes.SAVANNA:Biomes.PLAINS;
        if(!ocean(climate))return switch(climate){case WARM->Biomes.SPARSE_JUNGLE;case COLD->Biomes.TAIGA;case DRY->Biomes.SAVANNA;case MOUNTAIN->Biomes.MEADOW;default->Biomes.FOREST;};
        if(t<.38)return climate==Climate.FROZEN_SEA?Biomes.SNOWY_BEACH:Biomes.BEACH;
        if(climate==Climate.WARM_SEA)return deep&&t>.78?Biomes.DEEP_LUKEWARM_OCEAN:t>.62?Biomes.LUKEWARM_OCEAN:Biomes.WARM_OCEAN;
        if(climate==Climate.COLD_SEA)return deep&&t>.78?Biomes.DEEP_COLD_OCEAN:Biomes.COLD_OCEAN;
        if(climate==Climate.FROZEN_SEA)return deep&&t>.78?Biomes.DEEP_FROZEN_OCEAN:Biomes.FROZEN_OCEAN;
        return deep&&t>.78?Biomes.DEEP_OCEAN:Biomes.OCEAN;
    }
    public static int floor(Climate climate,double t,int custom,int original,int sea){
        if(t<=0)return custom;if(t>=1)return original;
        if(!ocean(climate)){double blend=smooth(t);return (int)Math.round(custom+(original-custom)*blend);}
        double value;
        if(t<.20)value=custom+(sea+4-custom)*smooth(t/.20);
        else if(t<.38)value=sea+4-5*smooth((t-.20)/.18);
        else if(t<.62)value=sea-1+(Math.max(original,sea-9)-(sea-1))*smooth((t-.38)/.24);
        else if(t<.80){double shallow=Math.max(original,sea-9);value=shallow+(Math.max(original,sea-22)-shallow)*smooth((t-.62)/.18);}
        else {double outer=Math.max(original,sea-22);value=outer+(original-outer)*smooth((t-.80)/.20);}
        return (int)Math.round(value);
    }
    public static boolean nativePatch(double t,int x,int z){
        double noise=.5+.25*Math.sin(x/49.0+Math.cos(z/91.0))+.25*Math.cos(z/67.0);
        return noise<smooth((t-.72)/.28);
    }
    private GeyserTransition(){}
}
