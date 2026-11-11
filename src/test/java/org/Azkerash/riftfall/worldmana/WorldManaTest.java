package org.Azkerash.riftfall.worldmana;

import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldManaTest {
    @Test void eachReservoirStartsEmptyAndLoadedSecondsRegenerateAtTheAgreedRate(){
        var mana=new ChunkMana();assertEquals(0,mana.amount());assertEquals(.5,mana.regenerationPerSecond());
        mana.regenerate(20);assertEquals(.5,mana.amount());mana.regenerate(10);assertEquals(.75,mana.amount());
        mana.setMatureCrystals(3);assertEquals(2,mana.regenerationPerSecond());mana.regenerate(20);assertEquals(2.75,mana.amount());
    }
    @Test void capacityAndWithdrawalsAreBoundedEvenForVeryLargeFiniteInputs(){
        var mana=new ChunkMana();assertEquals(2000,mana.add(Double.MAX_VALUE));assertEquals(0,mana.add(1));
        assertEquals(2000,mana.extract(Double.MAX_VALUE));assertEquals(0,mana.extract(1));
        mana.setMatureCrystals(Integer.MAX_VALUE);mana.regenerate(Long.MAX_VALUE);assertEquals(2000,mana.amount());assertTrue(Double.isFinite(mana.amount()));
    }
    @Test void invalidRequestsNeverCorruptTheReservoir(){
        var mana=new ChunkMana();mana.add(100);
        for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}){assertThrows(IllegalArgumentException.class,()->mana.add(invalid));assertThrows(IllegalArgumentException.class,()->mana.extract(invalid));}
        assertThrows(IllegalArgumentException.class,()->mana.setMatureCrystals(-1));assertThrows(IllegalArgumentException.class,()->mana.regenerate(-1));assertEquals(100,mana.amount());
    }
    @Test void attachmentCodecPreservesFractionalManaAndCrystalCount(){
        var mana=new ChunkMana();mana.add(123.456);mana.setMatureCrystals(7);
        var json=ChunkMana.CODEC.encodeStart(JsonOps.INSTANCE,mana).getOrThrow();var restored=ChunkMana.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();assertEquals(mana.snapshot(),restored.snapshot());
    }
    private Set<LeyLinePlacement.Candidate> region(long seed){
        Set<LeyLinePlacement.Candidate> result=new LinkedHashSet<>();for(int x=-15;x<=15;x++)for(int z=-15;z<=15;z++)LeyLinePlacement.inCell(seed,x,z).ifPresent(result::add);return result;
    }
    @Test void placementIsSparseAndAlwaysEnforcesTheCircular63ChunkExclusion(){
        for(long seed:new long[]{0,12345,-100}){
            var candidates=new ArrayList<>(region(seed));assertTrue(candidates.size()>10);assertTrue(candidates.size()<250);
            for(int i=0;i<candidates.size();i++)for(int j=i+1;j<candidates.size();j++){
                var a=candidates.get(i);var b=candidates.get(j);long x=(long)a.chunkX()-b.chunkX(),z=(long)a.chunkZ()-b.chunkZ();assertTrue(x*x+z*z>63L*63);
            }
        }
    }
    @Test void negativeChunkCentersAndDiscoveryOrderAreDeterministic(){
        var expected=region(12345);assertNotEquals(expected,region(12346));var reversed=new ArrayList<>(expected);Collections.reverse(reversed);
        for(var point:reversed){assertEquals(point,LeyLinePlacement.at(12345,point.chunkX(),point.chunkZ()).orElseThrow());assertEquals(8,point.x()-point.chunkX()*16.0);assertEquals(8,point.z()-point.chunkZ()*16.0);assertTrue(LeyLinePlacement.at(12345,point.chunkX()+1,point.chunkZ()).isEmpty());}
    }
    @Test void allDiscoveredNodesFormOneTreeAndRediscoveryDoesNotAddEdges(){
        var network=new LeyLineNetwork();var points=region(100);
        for(var point:points)assertTrue(network.discover(point.chunkX(),point.chunkZ(),70,point.priority()));
        assertEquals(points.size(),network.size());assertEquals(1,network.nodes().stream().filter(n->n.parent()==null).count());
        Map<Long,LeyLineNetwork.Node> byID=new HashMap<>();network.nodes().forEach(n->byID.put(n.id(),n));
        for(var point:points)assertFalse(network.discover(point.chunkX(),point.chunkZ(),99,999));
        for(var node:network.nodes()){
            Set<Long> visited=new HashSet<>();var current=node;
            while(current!=null){assertTrue(visited.add(current.id()),"cycle");current=current.parent()==null?null:byID.get(current.parent());}
            assertTrue(visited.contains(network.nodes().iterator().next().id()));
        }
    }
    @Test void savingAndReloadingPreservesEveryNodeAndConnection(){
        var network=new LeyLineNetwork();for(var point:region(55))network.discover(point.chunkX(),point.chunkZ(),81,point.priority());
        var tag=network.save(new CompoundTag(),null);var restored=LeyLineNetwork.load(tag,null);assertEquals(network.nodes(),restored.nodes());
        for(var node:restored.nodes())assertEquals(node,restored.node(node.chunkX(),node.chunkZ()).orElseThrow());
    }
}
