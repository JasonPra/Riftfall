package org.Azkerash.riftfall.worldmana;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LeyLineFlowTest {
    private LeyLineFlow.Reservoir node(long id,double amount,double strength){return new LeyLineFlow.Reservoir(id,amount,2000,strength,1);}
    private LeyLineFlow.Connection edge(long a,long b){return new LeyLineFlow.Connection(a,b);}
    @Test void equalStrengthsAreIdleRegardlessOfStoredMana(){
        assertTrue(LeyLineFlow.plan(List.of(node(1,2000,5),node(2,0,5)),List.of(edge(1,2))).isEmpty());
    }
    @Test void directionDependsOnGenerationAndReversesWhenStrengthChanges(){
        var first=LeyLineFlow.plan(List.of(node(1,100,6),node(2,1000,5)),List.of(edge(1,2)));
        assertEquals(List.of(new LeyLineFlow.Transfer(1,2,6)),first);
        var second=LeyLineFlow.plan(List.of(node(1,1000,5),node(2,100,6)),List.of(edge(1,2)));
        assertEquals(List.of(new LeyLineFlow.Transfer(2,1,6)),second);
    }
    @Test void oneOutputBudgetIsSharedRatherThanDuplicatedAcrossBranches(){
        var plan=LeyLineFlow.plan(List.of(node(1,100,6),node(2,0,5),node(3,0,5)),List.of(edge(1,2),edge(1,3)));
        assertEquals(2,plan.size());assertEquals(6,plan.stream().mapToDouble(LeyLineFlow.Transfer::amount).sum());assertTrue(plan.stream().allMatch(t->t.amount()==3));
    }
    @Test void noOverdrawingAndMissingOrUnloadedEndpointsCannotReceive(){
        assertEquals(List.of(new LeyLineFlow.Transfer(1,2,2)),LeyLineFlow.plan(List.of(node(1,2,10),node(2,0,5)),List.of(edge(1,2),edge(1,99))));
        assertTrue(LeyLineFlow.plan(List.of(node(1,100,10)),List.of(edge(1,99))).isEmpty());
    }
    @Test void receiverCapacityIsSharedFairlyAmongMultipleSources(){
        var plan=LeyLineFlow.plan(List.of(node(1,100,10),node(2,100,10),node(3,1999,5)),List.of(edge(1,3),edge(2,3)));
        assertEquals(2,plan.size());assertTrue(plan.stream().allMatch(t->t.amount()==.5));assertEquals(1,plan.stream().mapToDouble(LeyLineFlow.Transfer::amount).sum());
    }
    @Test void fullRecipientsBlockFlowAndLeaveTheOutputAtTheSource(){
        var plan=LeyLineFlow.plan(List.of(node(1,100,10),node(2,2000,5),node(3,0,5)),List.of(edge(1,2),edge(1,3)));
        assertEquals(List.of(new LeyLineFlow.Transfer(1,3,10)),plan);
    }
    @Test void receivedManaCannotBeForwardedUsingAnOrderDependentSameTickBudget(){
        var plan=LeyLineFlow.plan(List.of(node(1,100,10),node(2,0,6),node(3,0,5)),List.of(edge(1,2),edge(2,3)));
        assertEquals(List.of(new LeyLineFlow.Transfer(1,2,10)),plan);
    }
    @Test void partialLoadedIntervalsLimitOutputAndDuplicateEdgesCannotMultiplyIt(){
        var a=new LeyLineFlow.Reservoir(1,100,2000,10,.25);
        assertEquals(List.of(new LeyLineFlow.Transfer(1,2,2.5)),LeyLineFlow.plan(List.of(a,node(2,0,5)),List.of(edge(1,2),edge(2,1),edge(1,2))));
    }
    @Test void planningDoesNotDependOnCollectionOrder(){
        var nodes=new ArrayList<>(List.of(node(1,100,10),node(2,100,8),node(3,1990,5),node(4,0,5)));var edges=new ArrayList<>(List.of(edge(1,3),edge(2,3),edge(1,4)));
        var expected=LeyLineFlow.plan(nodes,edges);Collections.reverse(nodes);Collections.reverse(edges);assertEquals(expected,LeyLineFlow.plan(nodes,edges));
    }
    @Test void geyserGenerationIsFiveAndCrystalsAreTheOnlyStrengthBonus(){
        var mana=new ChunkMana();mana.regenerate(20,LeyLineNetwork.GEYSER_BASE_REGENERATION);assertEquals(5,mana.amount());mana.setMatureCrystals(2);assertEquals(6,mana.regenerationPerSecond(5));
        var network=new LeyLineNetwork();network.discover(0,0,70,100);assertEquals(5,network.node(0,0).orElseThrow().strength());network.updateCrystals(0,0,2);var node=network.node(0,0).orElseThrow();assertEquals(6,node.strength());
        mana.add(1000);assertEquals(6,mana.regenerationPerSecond(5));assertEquals(6,node.strength());
    }
    @Test void savedGeyserStrengthAndLegacyNetworksLoadCorrectly(){
        var network=new LeyLineNetwork();network.discover(0,0,70,100);network.updateCrystals(0,0,4);
        var data=network.save(new CompoundTag(),null);assertEquals(7,LeyLineNetwork.load(data,null).node(0,0).orElseThrow().strength());
        data.getList("Nodes",10).getCompound(0).remove("MatureCrystals");data.putInt("Version",1);assertEquals(5,LeyLineNetwork.load(data,null).node(0,0).orElseThrow().strength());
    }
    @Test void randomTreesConserveManaAndNeverOverdrawOrOverfill(){
        var random=new Random(42);
        for(int sample=0;sample<1000;sample++){
            List<LeyLineFlow.Reservoir> nodes=new ArrayList<>();List<LeyLineFlow.Connection> edges=new ArrayList<>();
            for(int i=0;i<20;i++){nodes.add(node(i,random.nextDouble()*2000,5+random.nextInt(20)*.5));if(i>0)edges.add(edge(i,random.nextInt(i)));}
            double[] amounts=nodes.stream().mapToDouble(LeyLineFlow.Reservoir::amount).toArray();double before=Arrays.stream(amounts).sum();
            for(var transfer:LeyLineFlow.plan(nodes,edges)){amounts[(int)transfer.source()]-=transfer.amount();amounts[(int)transfer.destination()]+=transfer.amount();}
            assertEquals(before,Arrays.stream(amounts).sum(),1e-7);for(double amount:amounts)assertTrue(amount>=-1e-9&&amount<=2000+1e-9);
        }
    }
    @Test void futureRefinementCanRequireTheWholeManaCostWithoutPartialConsumption(){
        var mana=new ChunkMana();mana.add(100);assertFalse(mana.tryExtract(150));assertEquals(100,mana.amount());assertTrue(mana.tryExtract(100));assertEquals(0,mana.amount());assertThrows(IllegalArgumentException.class,()->mana.tryExtract(Double.NaN));
    }
    @Test void invalidReservoirsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->node(1,-1,5));assertThrows(IllegalArgumentException.class,()->node(1,2001,5));assertThrows(IllegalArgumentException.class,()->node(1,0,Double.NaN));
    }
}
