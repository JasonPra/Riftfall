package org.Azkerash.riftfall.worldmana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A chunk's ambient reservoir; separate from a creature's Iron's Spells mana pool. */
public final class ChunkMana {
    public static final double CAPACITY=2000,BASE_REGENERATION=.50,MATURE_CRYSTAL_BONUS=.50;
    public static final Codec<ChunkMana> CODEC=RecordCodecBuilder.create(instance->instance.group(
        Codec.doubleRange(0,CAPACITY).fieldOf("amount").forGetter(ChunkMana::amount),
        Codec.intRange(0,Integer.MAX_VALUE).optionalFieldOf("mature_crystals",0).forGetter(ChunkMana::matureCrystals)
    ).apply(instance,ChunkMana::new));
    private double amount;
    private int matureCrystals;
    public ChunkMana(){this(0,0);}
    private ChunkMana(double amount,int matureCrystals){this.amount=amount;this.matureCrystals=matureCrystals;}
    public double amount(){return amount;}
    public int matureCrystals(){return matureCrystals;}
    public double regenerationPerSecond(){return BASE_REGENERATION+MATURE_CRYSTAL_BONUS*matureCrystals;}
    public boolean setMatureCrystals(int count){if(count<0)throw new IllegalArgumentException("Negative crystal count");boolean changed=count!=matureCrystals;matureCrystals=count;return changed;}
    private static void quantity(double value){if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Mana quantity must be finite and nonnegative");}
    public double add(double requested){quantity(requested);double before=amount;amount=Math.min(CAPACITY,amount+requested);return amount-before;}
    public double extract(double requested){quantity(requested);double taken=Math.min(amount,requested);amount-=taken;return taken;}
    public double regenerate(long loadedTicks){if(loadedTicks<0)throw new IllegalArgumentException("Negative elapsed ticks");return add(Math.min(CAPACITY,loadedTicks/20.0*regenerationPerSecond()));}
    public record Snapshot(double amount,double capacity,double regenerationPerSecond,int matureCrystals){}
    public Snapshot snapshot(){return new Snapshot(amount,CAPACITY,regenerationPerSecond(),matureCrystals);}
}
