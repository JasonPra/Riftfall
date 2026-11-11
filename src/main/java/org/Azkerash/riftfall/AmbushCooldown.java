package org.Azkerash.riftfall;

import net.minecraft.world.entity.Mob;

/** Shared grapple/backstab recovery. One local scan on commitment; readiness is an O(1) NBT read. */
public final class AmbushCooldown {
    public static final int DEFAULT_RECOVERY_TICKS=200;
    public static final double DEFAULT_RADIUS=16;
    private static final String KEY="RiftfallAmbushReady";
    public static boolean ready(Mob actor){return actor.level().getGameTime()>=actor.getPersistentData().getLong(KEY);}
    public static long deadline(long existing,long now,int animationTicks,int recoveryTicks){
        if(animationTicks<0||recoveryTicks<0)throw new IllegalArgumentException("Negative ambush timing");
        return Math.max(existing,now+(long)animationTicks+recoveryTicks);
    }
    /** Prevent both ambush types until the animation plus recovery have elapsed. Does not affect player skills. */
    public static long begin(Mob actor,int animationTicks,int recoveryTicks,double radius){
        if(actor.level().isClientSide)throw new IllegalStateException("Ambush cooldowns are server-owned");
        if(!Double.isFinite(radius)||radius<=0)throw new IllegalArgumentException("Invalid ambush radius");
        long until=deadline(actor.getPersistentData().getLong(KEY),actor.level().getGameTime(),animationTicks,recoveryTicks);
        actor.getPersistentData().putLong(KEY,until);
        for(var nearby:actor.level().getEntitiesOfClass(Mob.class,actor.getBoundingBox().inflate(radius),
                mob->mob!=actor&&mob.isAlive()&&mob.getType()==actor.getType()&&mob.distanceToSqr(actor)<=radius*radius))
            nearby.getPersistentData().putLong(KEY,Math.max(until,nearby.getPersistentData().getLong(KEY)));
        return until;
    }
    private AmbushCooldown(){}
}
