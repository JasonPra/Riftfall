package org.Azkerash.riftfall.worldmana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Independent chunk reservoir, separate from creature mana. */
public final class ChunkMana {
    public static final double CAPACITY=2000,BASE_REGENERATION=.50;
    public static final Codec<ChunkMana> CODEC=RecordCodecBuilder.create(instance->instance.group(
        Codec.doubleRange(0,CAPACITY).fieldOf("amount").forGetter(ChunkMana::amount)
    ).apply(instance,ChunkMana::new));
    private double amount;
    public ChunkMana(){this(0);}
    private ChunkMana(double amount){this.amount=amount;}
    public double amount(){return amount;}
    public double regenerationPerSecond(){return BASE_REGENERATION*20;}
    private static void quantity(double value){if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Mana quantity must be finite and nonnegative");}
    public double add(double requested){quantity(requested);double before=amount;amount=Math.min(CAPACITY,amount+requested);return amount-before;}
    public double extract(double requested){quantity(requested);double taken=Math.min(amount,requested);amount-=taken;return taken;}
    public boolean tryExtract(double requested){quantity(requested);if(amount<requested)return false;amount-=requested;return true;}
    /** Normal chunks receive 0.50 per loaded game tick; geyser chunks never call this. */
    public double regenerate(long loadedTicks){
        if(loadedTicks<0)throw new IllegalArgumentException("Negative elapsed ticks");
        return add(Math.min(CAPACITY,loadedTicks*BASE_REGENERATION));
    }
    public record Snapshot(double amount,double capacity,double regenerationPerSecond){}
    public Snapshot snapshot(){return new Snapshot(amount,CAPACITY,regenerationPerSecond());}
}
