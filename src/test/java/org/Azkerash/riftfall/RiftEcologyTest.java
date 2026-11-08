package org.Azkerash.riftfall;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiftEcologyTest {
    @Test void frozenWaterBiomesStayFrozenAndExcludeAquaticOrDesertMobs() {
        assertEquals("frozen",RiftEcology.theme("minecraft:frozen_ocean",true,true,false,false,false));
        var natives=Set.of("minecraft:drowned","minecraft:husk","minecraft:stray","minecraft:skeleton");
        assertFalse(RiftEcology.matches("minecraft:drowned","frozen",natives));
        assertFalse(RiftEcology.matches("minecraft:husk","frozen",natives));
        assertTrue(RiftEcology.matches("minecraft:stray","frozen",natives));
        assertTrue(RiftEcology.matches("minecraft:skeleton","frozen",natives));
        assertFalse(RiftEcology.matches("irons_spellbooks:pyromancer","frozen",natives));
    }
    @Test void nativeSpawnListsDoNotLeakBetweenBiomes() {
        assertFalse(RiftEcology.matches("minecraft:blaze","vanilla",Set.of("minecraft:skeleton")));
        assertTrue(RiftEcology.matches("minecraft:blaze","nether",Set.of("minecraft:blaze")));
        assertFalse(RiftEcology.matches("minecraft:stray","plains",Set.of("minecraft:stray")));
        assertTrue(RiftEcology.matches("minecraft:husk","vanilla",Set.of("minecraft:husk")));
        assertFalse(RiftEcology.matches("riftfall:goblin","aquatic",Set.of()));
        assertTrue(RiftEcology.matches("riftfall:goblin_shaman","plains",Set.of()));
    }
    @Test void namedBossesCannotEnterNormalPoolsEvenIfABiomeListsThem() {
        for (String boss : RiftEcology.BOSS_ONLY) {
            assertFalse(RiftMobs.allowedId(boss));
            for (String theme : Set.of("vanilla","frozen","temperate","swamp"))
                assertFalse(RiftEcology.matches(boss,theme,Set.of(boss)));
        }
        assertFalse(RiftEcology.matches("irons_spellbooks:summoned_skeleton","vanilla",Set.of("irons_spellbooks:summoned_skeleton")));
    }
    @Test void bossRoomsRequirePowerFundsAndAMatchingBiome() {
        var r=new RiftData.Rift(1,BlockPos.ZERO,100);
        r.theme="frozen";
        assertFalse(RiftBosses.canBuild(r,4,1000));
        assertFalse(RiftBosses.canBuild(r,5,199));
        assertTrue(RiftBosses.canBuild(r,5,200));
        assertEquals("irons_spellbooks:ice_spider",RiftBosses.species(r.theme));
        r.theme="vanilla";
        assertEquals("",RiftBosses.species(r.theme));
        r.theme="arid";
        assertFalse(RiftBosses.canBuild(r,10,1000));
    }
    @Test void plainsUseTheDedicatedGoblinRosterAndWizardsAreAlwaysOverworldOnly() {
        assertEquals("plains",RiftEcology.theme("minecraft:plains",false,false,false,false,false));
        var r=new RiftData.Rift(1,BlockPos.ZERO,1);r.theme="plains";
        assertTrue(RiftEcology.weight("riftfall:goblin",r)>RiftEcology.weight("minecraft:zombie",r));
        assertEquals(0,RiftEcology.weight("minecraft:witch",r));
        for (String id : Set.of("irons_spellbooks:pyromancer","irons_spellbooks:cryomancer","irons_spellbooks:archevoker","irons_spellbooks:necromancer","irons_spellbooks:cultist")) {
            assertTrue(RiftEcology.overworldOnly(id));
            for (String theme : Set.of("plains","frozen","vanilla")) assertFalse(RiftEcology.matches(id,theme,Set.of(id)));
        }
    }
    @Test void onlyApprovedLivingHumanoidsLearnRiftSpells() {
        assertTrue(RiftSpellcasting.canLearn("minecraft:pillager",false));
        assertTrue(RiftSpellcasting.canLearn("riftfall:goblin_shaman",false));
        assertFalse(RiftSpellcasting.canLearn("riftfall:goblin",false));
        for (String id : Set.of("minecraft:zombie","minecraft:skeleton","minecraft:zombified_piglin","minecraft:spider","minecraft:cow"))
            assertFalse(RiftSpellcasting.canLearn(id,id.contains("zombie") || id.contains("skeleton")));
        assertFalse(RiftSpellcasting.canLearn("minecraft:pillager",true));
    }
    @Test void sourceEcologyAndBossIdentitySurviveRestart() {
        var data=new RiftData();var r=new RiftData.Rift(2,BlockPos.ZERO,100);
        r.sourceBiome="minecraft:snowy_plains";r.theme="frozen";
        r.nativeMobs.clear();r.nativeMobs.add("minecraft:stray");r.roster.put("minecraft:stray",80);r.rosterVersion=2;r.layoutVersion=2;r.boss=java.util.UUID.randomUUID();
        data.rifts.add(r);
        var restored=RiftData.load(data.save(new CompoundTag(),null),null).rifts.getFirst();
        assertEquals(r.sourceBiome,restored.sourceBiome);assertEquals(r.theme,restored.theme);
        assertEquals(r.nativeMobs,restored.nativeMobs);assertEquals(r.boss,restored.boss);
        assertEquals(r.roster,restored.roster);assertEquals(2,restored.rosterVersion);assertEquals(2,restored.layoutVersion);
    }
}
