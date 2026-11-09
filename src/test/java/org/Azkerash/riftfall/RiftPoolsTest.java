package org.Azkerash.riftfall;

import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiftPoolsTest {
    @Test void thousandBlockExclusionIgnoresHeightAndIncludesTheBoundary() {
        BlockPos origin = new BlockPos(0,0,0);
        assertTrue(RiftPlacement.tooClose(origin,new BlockPos(1000,300,0)));
        assertTrue(RiftPlacement.tooClose(origin,new BlockPos(600,-60,800)));
        assertFalse(RiftPlacement.tooClose(origin,new BlockPos(1001,0,0)));
        assertFalse(RiftPlacement.tooClose(origin,new BlockPos(-1001,0,0)));
    }

    @Test void dormantRiftsKeepTheirReservedArea() {
        RiftData data = new RiftData();
        RiftData.Rift r = new RiftData.Rift(0,new BlockPos(0,64,0),0);
        r.dormant=true; data.rifts.add(r);
        assertFalse(RiftPlacement.available(data,new BlockPos(300,10,300)));
        assertTrue(RiftPlacement.available(data,new BlockPos(1001,64,0)));
    }

    @Test void connectedChunksHaveOneOwnerAndDisconnectedChunksHaveNone() {
        long a=ChunkPos.asLong(0,0), b=ChunkPos.asLong(1,0), c=ChunkPos.asLong(2,0), d=ChunkPos.asLong(3,0), e=ChunkPos.asLong(4,0);
        var owners=RiftEssencePools.assign(Set.of(a,b,c,d,e,ChunkPos.asLong(20,20)),Map.of(a,1,e,2));
        assertEquals(1,owners.get(a));assertEquals(1,owners.get(b));assertEquals(1,owners.get(c));
        assertEquals(2,owners.get(d));assertEquals(2,owners.get(e));
        assertFalse(owners.containsKey(ChunkPos.asLong(20,20)));
        var broken=RiftEssencePools.assign(Set.of(a,c),Map.of(a,1));
        assertFalse(broken.containsKey(c));
    }

    @Test void donorTransfersCannotOverdrawOrOverflowTheRoot() {
        assertEquals(3,RiftEssencePools.transfer(100,3,10,600));
        assertEquals(10,RiftEssencePools.transfer(100,50,10,600));
        assertEquals(1,RiftEssencePools.transfer(599,50,10,600));
        assertEquals(0,RiftEssencePools.transfer(600,50,10,600));
    }

    @Test void catchmentsStopAtThousandBlocksEvenWithAContinuousLoadedPath() {
        var loaded = new java.util.HashSet<Long>();
        for (int x = 0; x <= 70; x++) loaded.add(ChunkPos.asLong(x, 0));
        var owned = RiftEssencePools.assign(loaded, Map.of(ChunkPos.asLong(0, 0), 1));
        assertEquals(1, owned.get(ChunkPos.asLong(62, 0))); // 992 blocks
        assertFalse(owned.containsKey(ChunkPos.asLong(63, 0))); // 1008 blocks
        assertFalse(RiftEssencePools.withinRadius(new ChunkPos(0, 0), new ChunkPos(50, 50)));
    }

    @Test void parallelDonorsRefillTenTimesFasterWithoutCreatingEssence() {
        double root = 0, withdrawn = 0;
        double[] donors = new double[12];
        java.util.Arrays.fill(donors, 600);
        for (int second = 0; second < 6; second++) {
            double budget = RiftEssencePools.drawBudget(donors.length, 10, 100);
            for (int i = 0; i < donors.length; i++) {
                double amount = RiftEssencePools.transfer(root, donors[i], Math.min(10, budget), 600);
                root += amount; donors[i] -= amount; budget -= amount; withdrawn += amount;
            }
        }
        assertEquals(600, root);
        assertEquals(root, withdrawn);
        assertEquals(7200, root + java.util.Arrays.stream(donors).sum());
        assertEquals(10, RiftEssencePools.drawBudget(1, 10, 100));
        assertEquals(0, RiftEssencePools.drawBudget(12, 0, 100));
        assertEquals(0, RiftEssencePools.drawBudget(12, 10, 0));
    }

    @Test void dormantRootsReserveTheirOpeningChargeFromCrystalSpending() {
        for (double balance : new double[]{120, 300, 599, 600})
            assertFalse(RiftEssencePools.canFundCrystal(balance, 600, 10, true));
        assertTrue(RiftEssencePools.canFundCrystal(610, 600, 10, true));
        assertTrue(RiftEssencePools.canFundCrystal(120, 600, 10, false));
    }

    @Test void bossesAndGuardiansAreExcludedButOrdinaryHostilesAreAllowed() {
        for (String name : new String[]{"wither","ender_dragon","guardian","elder_guardian","warden","silverfish","endermite"}) assertFalse(RiftMobs.allowedId("minecraft:"+name));
        assertTrue(RiftMobs.allowedId("minecraft:wither_skeleton"));
        assertTrue(RiftMobs.allowedId("minecraft:skeleton"));
    }

    @Test void arenaBudgetsOnlyChargeForSimpleShellMonstersAndRewards() {
        for (int level=1;level<=10;level++) {
            assertEquals(20+20*level,RiftProgression.fixedCost(6,level));
            assertEquals(20+20*level+RiftProgression.enemies(6,level)*10,RiftProgression.minimumCost(6,level));
        }
    }
}
