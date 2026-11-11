package org.Azkerash.riftfall.crystals;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.Azkerash.riftfall.Riftfall;

public final class CrystalParticles {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> TYPES=DeferredRegister.create(Registries.PARTICLE_TYPE,Riftfall.MODID);
    public static final java.util.function.Supplier<SimpleParticleType> SPARKLE=TYPES.register("mana_crystal_sparkle",()->new SimpleParticleType(false));
    private CrystalParticles(){}
}
