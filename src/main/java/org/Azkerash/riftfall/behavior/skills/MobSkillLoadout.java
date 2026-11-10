package org.Azkerash.riftfall.behavior.skills;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.*;

/** Five player-equivalent active slots, persistent per mob, with per-slot cooldowns. */
public final class MobSkillLoadout {
    public static final List<SkillSlots> ACTIVE_SLOTS=List.of(SkillSlots.DODGE,SkillSlots.GUARD,SkillSlots.WEAPON_INNATE,SkillSlots.MOVER,SkillSlots.IDENTITY);
    private static final String DATA="RiftfallMobSkills",COOLDOWNS="RiftfallMobSkillCooldowns";
    private static final Map<ResourceLocation,MobActiveSkill> ADAPTERS=new ConcurrentHashMap<>();
    public static void registerAdapter(ResourceLocation skill,MobActiveSkill adapter){ADAPTERS.put(skill,adapter);}
    public static MobActiveSkill adapter(Skill skill){return skill instanceof MobActiveSkill own?own:ADAPTERS.get(skill.getRegistryName());}
    private static CompoundTag slots(Mob mob){return mob.getPersistentData().getCompound(DATA);}
    public static Skill equipped(Mob mob,SkillSlots slot){
        String value=slots(mob).getString(slot.name());var id=ResourceLocation.tryParse(value);
        return id==null?null:EpicFightRegistries.SKILL.get(id);
    }
    public static void equip(Mob mob,SkillSlots slot,Skill skill){
        if(!ACTIVE_SLOTS.contains(slot) || skill.getCategory()!=slot.category())throw new IllegalArgumentException("Skill does not fit active slot "+slot);
        if(adapter(skill)==null)throw new IllegalArgumentException("Skill needs an NPC execution adapter: "+skill.getRegistryName());
        var data=slots(mob);data.putString(slot.name(),skill.getRegistryName().toString());mob.getPersistentData().put(DATA,data);install(mob,slot);
    }
    public static boolean equipFromBook(Mob mob,SkillSlots slot,net.minecraft.world.item.ItemStack book){
        var skill=yesman.epicfight.world.item.SkillBookItem.getContainSkill(book);
        if(skill.isEmpty())return false;equip(mob,slot,skill.get().value());return true;
    }
    public static void clear(Mob mob,SkillSlots slot){var data=slots(mob);data.remove(slot.name());mob.getPersistentData().put(DATA,data);}
    public static boolean ready(Mob mob,SkillSlots slot){return mob.level().getGameTime()>=mob.getPersistentData().getCompound(COOLDOWNS).getLong(slot.name());}
    static void used(Mob mob,SkillSlots slot,int ticks){
        var data=mob.getPersistentData().getCompound(COOLDOWNS);data.putLong(slot.name(),mob.level().getGameTime()+ticks);mob.getPersistentData().put(COOLDOWNS,data);
    }
    private static void install(Mob mob,SkillSlots slot){
        if(mob instanceof BehaviorSkillUser)return;
        if(mob.goalSelector.getAvailableGoals().stream().noneMatch(w->w.getGoal() instanceof MobSkillGoal goal && goal.slot()==slot))
            mob.goalSelector.addGoal(1,new MobSkillGoal(mob,slot));
    }
    @SubscribeEvent public static void loaded(EntityJoinLevelEvent event){
        if(!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob)
            for(var slot:ACTIVE_SLOTS)if(equipped(mob,slot)!=null)install(mob,slot);
    }
    private MobSkillLoadout(){}
}
