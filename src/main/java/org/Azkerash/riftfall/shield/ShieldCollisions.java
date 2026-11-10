package org.Azkerash.riftfall.shield;

import java.util.function.Consumer;
import java.util.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;
import org.Azkerash.riftfall.GoblinEntity;
import org.Azkerash.riftfall.mixin.ShieldColliderAccess;
import org.Azkerash.riftfall.mixin.ShieldLineAccess;
import org.Azkerash.riftfall.mixin.ShieldPlaneAccess;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.collider.*;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.model.armature.types.ToolHolderArmature;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.ShieldCapability;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;

/** All block decisions are physical contacts, not a facing-angle or distance test. */
public final class ShieldCollisions {
    public record Held(LivingEntityPatch<?> patch,InteractionHand hand,ItemStack item) {}
    private static final ThreadLocal<Consumer<Collider>> SAMPLE=new ThreadLocal<>();
    private static final ThreadLocal<Boolean> BUILDING_SHIELD=ThreadLocal.withInitial(()->false);
    private static final Map<yesman.epicfight.api.animation.types.StaticAnimation,yesman.epicfight.api.animation.AnimationClip> GUARD_CLIPS=new java.util.concurrent.ConcurrentHashMap<>();
    public static void clearPoseCache(){GUARD_CLIPS.clear();}
    public static final class Sweep {
        public final LivingEntityPatch<?> attacker;public final AttackAnimation animation;public final AttackAnimation.Phase phase;public final float from,to;
        public net.minecraft.world.phys.AABB bounds;public final Set<Integer> shieldOnly=new HashSet<>();
        Sweep(LivingEntityPatch<?> attacker,AttackAnimation animation,AttackAnimation.Phase phase,float from,float to){this.attacker=attacker;this.animation=animation;this.phase=phase;this.from=from;this.to=to;}
    }
    private static final ThreadLocal<Deque<Sweep>> SWEEPS=ThreadLocal.withInitial(ArrayDeque::new);
    public static void beginSweep(LivingEntityPatch<?> attacker,AttackAnimation animation,AttackAnimation.Phase phase,float from,float to){SWEEPS.get().push(new Sweep(attacker,animation,phase,from,to));}
    public static void endSweep(){var stack=SWEEPS.get();if(!stack.isEmpty())stack.pop();if(stack.isEmpty())SWEEPS.remove();}
    public static Sweep currentSweep(){return SWEEPS.get().peek();}
    public static void capture(Collider collider){
        if(BUILDING_SHIELD.get() || collider instanceof MultiCollider<?>)return;
        var sample=SAMPLE.get();
        if(sample!=null)sample.accept(collider);
        else {var current=currentSweep();if(current!=null){var bounds=((ShieldColliderAccess)collider).riftfall$bounds();current.bounds=current.bounds==null?bounds:current.bounds.minmax(bounds);}}
    }
    public static boolean isShield(ItemStack item){return !item.isEmpty() && item.getItem() instanceof ShieldItem;}
    public static boolean isShield(LivingEntityPatch<?> patch,InteractionHand hand) {
        return isShield(patch.getOriginal().getItemInHand(hand)) || !patch.getOriginal().getItemInHand(hand).isEmpty() && patch.getHoldingItemCapability(hand) instanceof ShieldCapability;
    }
    public static Held held(LivingEntity defender) {
        var patch=EpicFightCapabilities.getEntityPatch(defender,LivingEntityPatch.class);
        if(patch==null || !defender.isAlive() || defender instanceof GoblinEntity goblin && goblin.isBurrowing())return null;
        if(defender.isBlocking() && defender.isUsingItem() && isShield(patch,defender.getUsedItemHand()))return new Held(patch,defender.getUsedItemHand(),defender.getUseItem());
        if(patch instanceof PlayerPatch<?> player && player.getHoldingSkill() instanceof GuardSkill) {
            var primary=player.getPrimaryItemCapability();
            for(var hand:InteractionHand.values())if(isShield(patch,hand) && primary==patch.getHoldingItemCapability(hand))return new Held(patch,hand,defender.getItemInHand(hand));
        }
        return null;
    }
    public static boolean blockable(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_SHIELD)
            && !(source instanceof EpicFightDamageSource epic && epic.is(EpicFightDamageTypeTags.GUARD_PUNCTURE))
            && !(source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getPierceLevel()>0);
    }
    public static ShieldBox box(Held held,float partial) {
        var patch=held.patch();var entity=patch.getOriginal();var model=ShieldModels.get(held.item());
        var guard=Animations.BIPED_BLOCK.get();
        var direct=held.hand()==InteractionHand.OFF_HAND?guard.mirror:guard.original;
        var current=patch.getAnimator().getPlayerFor(null);
        if(current.getAnimation().get()==Animations.BIPED_HIT_SHIELD.get()) {
            var hit=Animations.BIPED_HIT_SHIELD.get();direct=held.hand()==InteractionHand.OFF_HAND?hit.mirror:hit.original;
        }
        // Ordinary living-animation clips omit joint transforms on dedicated servers.
        // Physical shields need the same complete guard skeleton that clients render.
        var clip=GUARD_CLIPS.computeIfAbsent(direct,animation->{
            var loaded=yesman.epicfight.api.animation.AnimationManager.getInstance().loadAnimationClip(animation,yesman.epicfight.api.asset.JsonAssetLoader::loadAllJointsClipForAnimation);
            loaded.bakeKeyframes();return loaded;
        });
        float time=Math.min(Math.max(0,clip.getClipTime()-.001F),entity.getTicksUsingItem()/20F);
        if(current.getAnimation().get()==Animations.BIPED_HIT_SHIELD.get())time=Math.min(Math.max(0,clip.getClipTime()-.001F),current.getElapsedTime());
        var pose=clip.getPoseInTime(time);
        direct.modifyPose(direct,pose,patch,time,partial);
        var armature=patch.getArmature();
        var joint=model.forearm() && entity instanceof GoblinEntity?armature.searchJointByName(held.hand()==InteractionHand.OFF_HAND?"Hand_L":"Hand_R"):
            model.alwaysInHand() && armature instanceof ToolHolderArmature tools?(held.hand()==InteractionHand.OFF_HAND?tools.leftToolJoint():tools.rightToolJoint()):patch.getParentJointOfHand(held.hand());
        if(joint==null)joint=armature.rootJoint;
        var matrix=OpenMatrix4f.createTranslation((float)-entity.getX(),(float)entity.getY(),(float)-entity.getZ())
            .mulBack(patch.getModelMatrix(partial)).mulBack(armature.getBoundTransformFor(pose,joint))
            .mulBack(model.correction(held.hand(),joint.getName(),model.forearm() && entity instanceof GoblinEntity)).mulBack(model.display(held.hand()));
        boolean previous=BUILDING_SHIELD.get();BUILDING_SHIELD.set(true);
        try{return new ShieldBox(model.bounds(),matrix);}finally{BUILDING_SHIELD.set(previous);}
    }
    public static boolean contact(LivingEntity defender,DamageSource source) {
        var held=held(defender);if(held==null || !blockable(source))return false;
        var box=box(held,1);
        if(source.getDirectEntity() instanceof Projectile projectile) {
            var velocity=projectile.getDeltaMovement();
            var start=projectile.position().subtract(velocity);var end=projectile.position().add(velocity);
            if(velocity.lengthSqr()<1.0E-8)start=new Vec3(projectile.xOld,projectile.yOld,projectile.zOld);
            return box.beforeBody(start,end,defender.getBoundingBox(),projectile instanceof AbstractArrow?1D/32:projectile.getBbWidth()/2);
        }
        if(source instanceof EpicFightDamageSource epic && epic.getAnimation()!=null && epic.getAnimation().get() instanceof AttackAnimation attack
                && source.getEntity() instanceof LivingEntity attacker) {
            var context=currentSweep();
            if(context!=null && context.attacker.getOriginal()==attacker && context.animation==attack)
                return sweep(defender,box,context.attacker,attack,context.phase,Math.max(context.phase.preDelay,context.from),Math.min(context.phase.contact,context.to),Math.max(.1F,attack.getPlaySpeed(context.attacker,attack)));
            var attackerPatch=EpicFightCapabilities.getEntityPatch(attacker,LivingEntityPatch.class);
            if(attackerPatch==null)return false;
            var player=attackerPatch.getAnimator().getPlayerFor(attack.getAccessor());
            var phase=attack.getPhaseByTime(player.getElapsedTime());
            return sweep(defender,box,attackerPatch,attack,phase,Math.max(phase.preDelay,player.getPrevElapsedTime()),Math.min(phase.contact,player.getElapsedTime()),Math.max(.1F,attack.getPlaySpeed(attackerPatch,attack)));
        }
        // Vanilla attacks have no animated weapon collider. Test their physical strike path instead.
        var from=source.getEntity() instanceof LivingEntity attacker?attacker.getEyePosition():source.getSourcePosition();
        return from!=null && box.beforeBody(from,defender.position().add(0,defender.getBbHeight()*.66,0),defender.getBoundingBox(),0);
    }
    public static boolean sweep(LivingEntity defender,ShieldBox box,LivingEntityPatch<?> attacker,AttackAnimation animation,AttackAnimation.Phase phase,float from,float to,float speed) {
        if(to<from)return false;
        final int[] sample={0},shield={Integer.MAX_VALUE},body={Integer.MAX_VALUE};
        var previous=SAMPLE.get();
        try {
            SAMPLE.set(collider->{
                int index=sample[0]++;boolean touches=false;
                if(collider instanceof OBBCollider obb)touches=obb.isCollide(box.collider());
                else if(collider instanceof LineCollider line) {
                    var start=ShieldBox.toWorld(((ShieldColliderAccess)line).riftfall$center());
                    var end=start.add(ShieldBox.toWorld(((ShieldLineAccess)line).riftfall$vector()));
                    touches=Double.isFinite(box.entry(start,end,0));
                }else if(collider instanceof PlaneCollider plane) {
                    var start=ShieldBox.toWorld(((ShieldColliderAccess)plane).riftfall$center());
                    var vectors=((ShieldPlaneAccess)plane).riftfall$vectors();
                    var a=start.add(ShieldBox.toWorld(vectors[0]));var b=start.add(ShieldBox.toWorld(vectors[1]));var corner=a.add(b.subtract(start));
                    touches=box.triangle(start,a,b)||box.triangle(a,corner,b);
                }
                if(touches)shield[0]=Math.min(shield[0],index);
                if(collider.isCollide(defender))body[0]=Math.min(body[0],index);
            });
            phase.getCollidingEntities(attacker,animation,from,to,speed);
        }finally {if(previous==null)SAMPLE.remove();else SAMPLE.set(previous);}
        if(shield[0]==Integer.MAX_VALUE || body[0]<shield[0])return false;
        return shield[0]<body[0] || box.beforeBody(attacker.getOriginal().getEyePosition(),box.center(),defender.getBoundingBox(),0);
    }
    public static boolean sampling(){return SAMPLE.get()!=null;}
    public static List<net.minecraft.world.entity.Entity> includeShieldContacts(AttackAnimation.Phase phase,List<net.minecraft.world.entity.Entity> bodyHits) {
        var current=currentSweep();
        if(sampling() || current==null || current.phase!=phase || current.bounds==null || current.attacker.isLogicalClient())return bodyHits;
        var result=new LinkedHashSet<>(bodyHits);var attacker=current.attacker.getOriginal();
        for(var defender:attacker.level().getEntitiesOfClass(LivingEntity.class,current.bounds.inflate(2),entity->entity!=attacker && entity.isAlive() && !attacker.isAlliedTo(entity))) {
            if(result.contains(defender))continue;
            var held=held(defender);
            if(held!=null && sweep(defender,box(held,1),current.attacker,current.animation,phase,Math.max(phase.preDelay,current.from),Math.min(phase.contact,current.to),Math.max(.1F,current.animation.getPlaySpeed(current.attacker,current.animation)))) {
                result.add(defender);current.shieldOnly.add(defender.getId());
            }
        }
        return new ArrayList<>(result);
    }
    public static boolean shieldOnlyMiss(LivingEntity defender,DamageSource source) {
        var current=currentSweep();
        return current!=null && source.getEntity()==current.attacker.getOriginal() && current.shieldOnly.contains(defender.getId()) && !contact(defender,source);
    }
    private ShieldCollisions(){}
}
