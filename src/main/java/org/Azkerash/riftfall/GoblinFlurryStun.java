package org.Azkerash.riftfall;

import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.world.damagesource.StunType;

/** Extend accepted native stuns just enough to connect the next landed flurry cut. */
final class GoblinFlurryStun {
    private static final String CONTACT="RiftfallFlurryStunContact";
    static float chainTime(float nativeTime) { return Math.max(nativeTime,.35F); }
    static void register() {
        EpicFightEventHooks.Entity.ON_STUNNED.registerEvent(event -> {
            var source=event.getDamageSource();
            var target=event.getEntityPatch().getOriginal();
            var patch=event.getEntityPatch();
            if (source.getEntity() instanceof GoblinEntity && source.getAnimation()!=null
                && source.getAnimation().registryName()!=null
                && "riftfall:biped/goblin_twohand_flurry".equals(source.getAnimation().registryName().toString())
                && event.getStunType()==StunType.SHORT && patch.getStunArmor()<=source.calculateImpact())
                target.getPersistentData().putLong(CONTACT,target.level().getGameTime());
            else target.getPersistentData().remove(CONTACT);
        });
        EpicFightEventHooks.Entity.APPLY_STUN.registerEvent(event -> {
            var target=event.getEntityPatch().getOriginal();
            var data=target.getPersistentData();
            if (event.getStunType()==StunType.SHORT && data.contains(CONTACT)
                && data.getLong(CONTACT)==target.level().getGameTime()) {
                event.setStunTime(chainTime(event.getStunTime()));
                data.remove(CONTACT);
                data.putInt("RiftfallFlurryStuns",data.getInt("RiftfallFlurryStuns")+1);
            }
        });
    }
    private GoblinFlurryStun() {}
}
