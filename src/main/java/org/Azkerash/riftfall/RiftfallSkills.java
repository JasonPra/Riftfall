package org.Azkerash.riftfall;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.*;

final class RiftfallSkills {
    static final DeferredRegister<Skill> SKILLS=DeferredRegister.create(EpicFightRegistries.SKILL,Riftfall.MODID);
    static final net.neoforged.neoforge.registries.DeferredHolder<Skill,BackstabSkill> BACKSTAB=SKILLS.register("backstab",()->
        new BackstabBuilder().setCategory(SkillCategories.IDENTITY)
            .setResource(Skill.Resource.COOLDOWN).build(ResourceLocation.fromNamespaceAndPath(Riftfall.MODID,"backstab")));
    private static final class BackstabBuilder extends SkillBuilder<BackstabBuilder>{BackstabBuilder(){super(BackstabSkill::new);}}
    private RiftfallSkills(){}
}
