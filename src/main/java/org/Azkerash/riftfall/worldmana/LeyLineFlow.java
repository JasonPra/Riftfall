package org.Azkerash.riftfall.worldmana;

import java.util.*;

/** Plans transfers from one snapshot. Received mana never changes strength or this interval's budget. */
public final class LeyLineFlow {
    public record Reservoir(long id,double amount,double capacity,double strength,double elapsedSeconds) {
        public Reservoir {
            for(double value:new double[]{amount,capacity,strength,elapsedSeconds})
                if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid ley-line reservoir");
            if(amount>capacity)throw new IllegalArgumentException("Reservoir exceeds capacity");
        }
    }
    public record Connection(long a,long b) {}
    public record Transfer(long source,long destination,double amount) {}
    public static List<Transfer> plan(Collection<Reservoir> reservoirs,Collection<Connection> connections) {
        Map<Long,Reservoir> nodes=new TreeMap<>();
        for(var reservoir:reservoirs)if(nodes.put(reservoir.id(),reservoir)!=null)throw new IllegalArgumentException("Duplicate reservoir ID");
        Map<Long,Set<Long>> outgoing=new TreeMap<>();
        for(var edge:connections){
            var a=nodes.get(edge.a());var b=nodes.get(edge.b());
            if(a==null||b==null||a.strength()==b.strength())continue;
            var source=a.strength()>b.strength()?a:b;
            var destination=source==a?b:a;
            if(destination.amount()>=destination.capacity())continue;
            outgoing.computeIfAbsent(source.id(),id->new TreeSet<>()).add(destination.id());
        }
        List<Transfer> proposed=new ArrayList<>();Map<Long,Double> demand=new HashMap<>();
        for(var entry:outgoing.entrySet()){
            var source=nodes.get(entry.getKey());
            double budget=Math.min(source.amount(),source.strength()*source.elapsedSeconds());
            double share=budget/entry.getValue().size();if(share<=0)continue;
            for(long destination:entry.getValue()){
                proposed.add(new Transfer(source.id(),destination,share));demand.merge(destination,share,Double::sum);
            }
        }
        List<Transfer> result=new ArrayList<>();
        for(var transfer:proposed){
            var destination=nodes.get(transfer.destination());
            double fraction=Math.min(1,(destination.capacity()-destination.amount())/demand.get(destination.id()));
            double amount=transfer.amount()*fraction;
            if(amount>0)result.add(new Transfer(transfer.source(),transfer.destination(),amount));
        }
        return List.copyOf(result);
    }
    private LeyLineFlow() {}
}
