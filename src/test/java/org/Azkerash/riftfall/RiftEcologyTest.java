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
        assertFalse(RiftEcology.matches("minecraft:blaze","woodland",Set.of("minecraft:skeleton")));
        assertTrue(RiftEcology.matches("minecraft:blaze","nether",Set.of("minecraft:blaze")));
        assertFalse(RiftEcology.matches("minecraft:stray","arid",Set.of("minecraft:stray")));
        assertTrue(RiftEcology.matches("minecraft:husk","arid",Set.of()));
        assertFalse(RiftEcology.matches("riftfall:goblin","aquatic",Set.of()));
        assertTrue(RiftEcology.matches("riftfall:goblin_shaman","woodland",Set.of()));
    }
    @Test void namedBossesCannotEnterNormalPoolsEvenIfABiomeListsThem() {
        for (String boss : RiftEcology.BOSS_ONLY) {
            assertFalse(RiftMobs.allowedId(boss));
            for (String theme : Set.of("woodland","frozen","temperate","swamp"))
                assertFalse(RiftEcology.matches(boss,theme,Set.of(boss)));
        }
        assertFalse(RiftEcology.matches("irons_spellbooks:summoned_skeleton","woodland",Set.of("irons_spellbooks:summoned_skeleton")));
    }
    @Test void bossRoomsRequirePowerFundsAndAMatchingBiome() {
        var r=new RiftData.Rift(1,BlockPos.ZERO,100);
        r.theme="frozen";
        assertFalse(RiftBosses.canBuild(r,4,1000));
        assertFalse(RiftBosses.canBuild(r,5,199));
        assertTrue(RiftBosses.canBuild(r,5,200));
        assertEquals("irons_spellbooks:ice_spider",RiftBosses.species(r.theme));
        r.theme="woodland";
        assertEquals("irons_spellbooks:archevoker",RiftBosses.species(r.theme));
        r.theme="arid";
        assertFalse(RiftBosses.canBuild(r,10,1000));
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
        r.nativeMobs.clear();r.nativeMobs.add("minecraft:stray");r.boss=java.util.UUID.randomUUID();
        data.rifts.add(r);
        var restored=RiftData.load(data.save(new CompoundTag(),null),null).rifts.getFirst();
        assertEquals(r.sourceBiome,restored.sourceBiome);assertEquals(r.theme,restored.theme);
        assertEquals(r.nativeMobs,restored.nativeMobs);assertEquals(r.boss,restored.boss);
    }
}
