package org.Azkerash.riftfall.equipment;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.item.curios.*;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.Azkerash.riftfall.Riftfall;

/** Adapters only for Iron's equipment hooks explicitly restricted to ServerPlayer in this version. */
@EventBusSubscriber(modid=Riftfall.MODID)
public final class IronMobEquipmentEffects {
    private static boolean proc(Mob mob,PassiveAbilityCurio item){
        var cooldowns=mob.getPersistentData().getCompound("RiftfallEquipmentCooldowns");String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
        long now=mob.level().getGameTime();if(cooldowns.getLong(id)>now)return false;
        cooldowns.putLong(id,now+item.getCooldownTicks(mob));mob.getPersistentData().put("RiftfallEquipmentCooldowns",cooldowns);return true;
    }
    @SubscribeEvent public static void incoming(LivingIncomingDamageEvent event){
        if(event.getEntity().level().isClientSide)return;
        if(event.getEntity() instanceof Mob defender){
            // Iron's native IMagicEntity handler already implements Fireward. Do not run it twice.
            if(!(defender instanceof IMagicEntity) && ((CurioBaseItem)ItemRegistry.FIREWARD_RING.get()).isEquippedBy(defender) && event.getSource().is(DamageTypeTags.IS_FIRE)){
                defender.clearFire();event.setCanceled(true);return;
            }
            if(!(defender instanceof IMagicEntity) && evade(defender,event.getSource())){event.setCanceled(true);return;}
            var ring=(ExpulsionRing)ItemRegistry.EXPULSION_RING.get();
            if(event.getSource().getEntity()!=null && ring.isEquippedBy(defender) && proc(defender,ring))expel(defender);
        }
        if(event.getSource().getEntity() instanceof Mob attacker){
            var lurker=(LurkerRing)ItemRegistry.LURKER_RING.get();
            if(attacker.isInvisible() && lurker.isEquippedBy(attacker) && proc(attacker,lurker))event.setAmount(event.getAmount()*LurkerRing.MULTIPLIER);
            var betrayer=(BetrayerSignetRingItem)ItemRegistry.SIGNET_OF_THE_BETRAYER.get();
            var victim=event.getEntity();double extra=victim.getAttributeValue(AttributeRegistry.MAX_MANA)-victim.getAttributeBaseValue(AttributeRegistry.MAX_MANA);
            if(extra>0 && betrayer.isEquippedBy(attacker) && proc(attacker,betrayer)){
                double bonus=0;for(double rate=.1;extra>0 && rate>0;rate-=.01){bonus+=Math.clamp(extra,0,100)*.01*rate;extra-=100;}
                event.setAmount((float)(event.getAmount()*(1+bonus)));
            }
        }
    }
    private static boolean evade(Mob mob,net.minecraft.world.damagesource.DamageSource source){
        if(mob.hasEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.EVASION))return io.redspace.ironsspellbooks.effect.EvasionEffect.doEffect(mob,source);
        return mob.hasEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.ABYSSAL_SHROUD) && io.redspace.ironsspellbooks.effect.AbyssalShroudEffect.doEffect(mob,source);
    }
    @SubscribeEvent public static void projectile(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event){
        if(event.getProjectile().level().isClientSide || !(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit)
            || !(hit.getEntity() instanceof Mob mob) || mob instanceof IMagicEntity)return;
        if(evade(mob,mob.damageSources().indirectMagic(event.getProjectile(),event.getProjectile().getOwner())))event.setCanceled(true);
    }
    private static void expel(Mob mob){
        var center=mob.getBoundingBox().getCenter();
        mob.level().explode(null,null,null,center.x,center.y,center.z,0,false,Level.ExplosionInteraction.NONE,
            ParticleTypes.GUST_EMITTER_SMALL,ParticleTypes.GUST_EMITTER_LARGE,net.minecraft.sounds.SoundEvents.WIND_CHARGE_BURST);
        for(var entity:mob.level().getEntities(mob,mob.getBoundingBox().inflate(3))){
            double d=Math.max(entity.distanceToSqr(mob),.2);
            if(d>=16 || DamageSources.isFriendlyFireBetween(mob,entity))continue;
            double force=1-d/16+.6F*(entity instanceof LivingEntity living?1-living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE):1);
            entity.setDeltaMovement(entity.getDeltaMovement().add(entity.getBoundingBox().getCenter().subtract(center).normalize().add(0,.1,0).scale(force)));entity.hurtMarked=true;
        }
    }
    private IronMobEquipmentEffects(){}
}
