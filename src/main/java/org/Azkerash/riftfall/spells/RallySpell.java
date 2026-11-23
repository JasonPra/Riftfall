package org.Azkerash.riftfall.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import org.Azkerash.riftfall.GoblinEntity;
import org.Azkerash.riftfall.GoblinRally;
import org.Azkerash.riftfall.Riftfall;

import java.util.*;

/** Iron's support spell: level one targets an ally, level two rallies the local group. */
public final class RallySpell extends AbstractSpell {
    public static final int DURATION = 400;
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Riftfall.MODID, "rally");
    private final DefaultConfig config =
            new DefaultConfig()
                    .setMinRarity(SpellRarity.RARE)
                    .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
                    .setMaxLevel(2)
                    .setCooldownSeconds(30)
                    .setAllowCrafting(false)
                    .build();

    public RallySpell() {
        baseManaCost = 80;
        manaCostPerLevel = 60;
        castTime = 30;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        level >= 2 ? "ui.riftfall.rally.group" : "ui.riftfall.rally.single",
                        DURATION / 20,
                        GoblinRally.RANGE));
    }

    private List<GoblinEntity> recipients(
            ServerLevel world, int level, LivingEntity caster, MagicData data) {
        if (level >= 2) {
            return GoblinRally.needingHelp(caster).isEmpty()
                    ? List.of()
                    : GoblinRally.allies(caster);
        }
        if (data.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            var target = targetData.getTarget(world);
            if (target instanceof GoblinEntity goblin
                    && GoblinRally.reachable(caster, goblin)
                    && GoblinRally.needsHelp(goblin)) {
                return List.of(goblin);
            }
            return List.of();
        }
        var candidates = GoblinRally.needingHelp(caster);
        if (candidates.isEmpty()) {
            return List.of();
        }
        data.setAdditionalCastData(new TargetEntityCastData(candidates.getFirst()));
        return List.of(candidates.getFirst());
    }

    @Override
    public boolean checkPreCastConditions(
            Level world, int level, LivingEntity caster, MagicData data) {
        return world instanceof ServerLevel server
                && !recipients(server, level, caster, data).isEmpty();
    }

    @Override
    public void onCast(
            Level world, int level, LivingEntity caster, CastSource source, MagicData data) {
        if (world instanceof ServerLevel server) {
            for (var ally : recipients(server, level, caster, data)) {
                ally.addEffect(new MobEffectInstance(RiftfallSpells.RALLIED, DURATION, 0), caster);
                GoblinRally.refresh(ally);
                server.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        ally.getX(),
                        ally.getEyeY(),
                        ally.getZ(),
                        12,
                        .4,
                        .3,
                        .4,
                        .02);
            }
            playSound(getCastFinishSound(), caster);
        }
        super.onCast(world, level, caster, source, data);
    }
}
