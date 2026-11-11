package org.Azkerash.riftfall.crystals;

import java.util.*;

/** Pure chunk-level event simulation. No world, block entities, tick loops or terrain mutation. */
public final class RefinementBatch {
    public record Settings(double baseTicks,double manaCost,double tierMultiplier){
        public Settings {if(!Double.isFinite(baseTicks)||baseTicks<=0||!Double.isFinite(manaCost)||manaCost<=0||manaCost>2000||!Double.isFinite(tierMultiplier)||tierMultiplier<1||tierMultiplier>10)throw new IllegalArgumentException("Invalid refinement settings");}
        public double ticks(CrystalTier tier){return baseTicks*Math.pow(tierMultiplier,tier.ordinal());}
    }
    /** worldTier is the last physically applied tier; tier can include deferred upgrades. */
    public record Crystal(long position,CrystalTier worldTier,CrystalTier tier,double progress){
        public Crystal {Objects.requireNonNull(worldTier);Objects.requireNonNull(tier);if(!Double.isFinite(progress)||progress<0||tier.ordinal()<worldTier.ordinal())throw new IllegalArgumentException("Invalid refinement progress");}
    }
    public record Chunk(long id,double exposure,List<Crystal> crystals) {
        public Chunk {if(!Double.isFinite(exposure)||exposure<=0)throw new IllegalArgumentException("Invalid exposure");crystals=List.copyOf(crystals);}
    }
    public record Result(List<Chunk> chunks,double mana,int upgrades,Set<Long> served){}
    private record Event(double due,int index,int chunk,long position){}
    /** Input chunks are ordered center-out. Served persists across calls and saves within a round. */
    public static Result advance(List<Chunk> chunks,double elapsed,double mana,double capacity,double perTick,Settings settings,Set<Long> priorServed){
        if(!Double.isFinite(elapsed)||elapsed<0||!Double.isFinite(capacity)||capacity<=0||!Double.isFinite(mana)||mana<0||mana>capacity||!Double.isFinite(perTick)||perTick<0)throw new IllegalArgumentException("Invalid refinement simulation");
        List<Crystal> input=new ArrayList<>();List<Integer> owners=new ArrayList<>();
        for(int c=0;c<chunks.size();c++)for(var crystal:chunks.get(c).crystals()){input.add(crystal);owners.add(c);}
        int size=input.size();var tiers=new CrystalTier[size];double[] start=new double[size],carry=new double[size];
        Set<Long> positions=new HashSet<>(),served=new HashSet<>(priorServed);
        var future=new PriorityQueue<Event>(Comparator.comparingDouble(Event::due).thenComparingInt(Event::chunk).thenComparingLong(Event::position));
        var ready=new TreeMap<Integer,PriorityQueue<Event>>();
        for(int i=0;i<size;i++){
            var crystal=input.get(i);if(!positions.add(crystal.position()))throw new IllegalArgumentException("Duplicate crystal position");tiers[i]=crystal.tier();carry[i]=Math.min(settings.ticks(tiers[i]),crystal.progress());
            if(tiers[i].next().isPresent())future.add(new Event((settings.ticks(tiers[i])-carry[i])/chunks.get(owners.get(i)).exposure(),i,owners.get(i),crystal.position()));
        }
        double wall=0,active=0;int upgrades=0;
        while(!future.isEmpty()||!ready.isEmpty()){
            if(mana<settings.manaCost()){
                if(perTick<=0)break;
                double wait=(settings.manaCost()-mana)/perTick;
                if(wait>elapsed-wall)break;
                wall+=wait;mana=settings.manaCost();
            }
            if(ready.isEmpty()){
                double time=Math.max(0,future.peek().due()-active);
                if(time>elapsed-wall)break;
                wall+=time;active+=time;mana=Math.min(capacity,mana+time*perTick);
            }
            while(!future.isEmpty()&&future.peek().due()<=active+1e-8){var event=future.remove();ready.computeIfAbsent(event.chunk(),k->new PriorityQueue<>(Comparator.comparingLong(Event::position))).add(event);}
            Integer selected=null;
            for(int c:ready.keySet())if(!served.contains(chunks.get(c).id())){selected=c;break;}
            if(selected==null){served.clear();selected=ready.firstKey();}
            var queue=ready.get(selected);var event=queue.remove();if(queue.isEmpty())ready.remove(selected);
            served.add(chunks.get(selected).id());mana=Math.max(0,mana-settings.manaCost());
            int i=event.index();tiers[i]=tiers[i].next().orElseThrow();carry[i]=0;start[i]=active;upgrades++;
            if(tiers[i].next().isPresent())future.add(new Event(active+settings.ticks(tiers[i])/chunks.get(selected).exposure(),i,selected,event.position()));
        }
        double remaining=Math.max(0,elapsed-wall);
        if(!future.isEmpty()||!ready.isEmpty()){
            double wait=mana>=settings.manaCost()?0:perTick>0?(settings.manaCost()-mana)/perTick:Double.POSITIVE_INFINITY;
            active+=Math.max(0,remaining-wait);
        }
        mana=Math.min(capacity,mana+remaining*perTick);
        List<Chunk> output=new ArrayList<>();int i=0;
        for(var chunk:chunks){var values=new ArrayList<Crystal>();for(var old:chunk.crystals()){
            double progress=tiers[i]==CrystalTier.WHITE?0:Math.min(settings.ticks(tiers[i]),carry[i]+Math.max(0,active-start[i])*chunk.exposure());
            values.add(new Crystal(old.position(),old.worldTier(),tiers[i],progress));i++;
        }output.add(new Chunk(chunk.id(),chunk.exposure(),values));}
        return new Result(List.copyOf(output),mana,upgrades,Set.copyOf(served));
    }
    private RefinementBatch(){}
}
