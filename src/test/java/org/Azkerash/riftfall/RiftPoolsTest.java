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

    @Test void bossesAndGuardiansAreExcludedButOrdinaryHostilesAreAllowed() {
        for (String name : new String[]{"wither","ender_dragon","guardian","elder_guardian","warden"}) assertFalse(RiftMobs.allowedId("minecraft:"+name));
        assertTrue(RiftMobs.allowedId("minecraft:wither_skeleton"));
        assertTrue(RiftMobs.allowedId("minecraft:skeleton"));
    }

    @Test void roomTrapAndRewardBudgetsGrowWithDanger() {
        assertEquals(100,RiftProgression.minimumCost(6,1));
        assertTrue(RiftProgression.minimumCost(6,2)>RiftProgression.minimumCost(6,1));
        assertEquals(0,RiftProgression.traps(1));assertEquals(1,RiftProgression.traps(2));
        assertEquals(6,RiftProgression.traps(10));
    }
    @Test void everyPurchasedTrapHasItsOwnLocationInsideTheDungeon() {
        for (int radius : new int[]{10,12,20,26}) {
            var positions = new java.util.HashSet<BlockPos>();
            for (int i=0;i<6;i++) {
                BlockPos pos = RiftDungeons.trapPosition(BlockPos.ZERO,radius,i);
                assertTrue(Math.abs(pos.getX())<radius && Math.abs(pos.getZ())<radius);
                positions.add(pos);
            }
            assertEquals(6,positions.size());
        }
    }

}
