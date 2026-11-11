package org.Azkerash.riftfall.crystals;

import java.util.*;

/** Pure chunk-level event simulation. No world, block entities, tick loops or terrain mutation. */
public final class RefinementBatch {
    public record Settings(double baseTicks,double manaCost,double tierMultiplier,RefinementBoostTable boosts){
        public Settings(double baseTicks,double manaCost,double tierMultiplier){this(baseTicks,manaCost,tierMultiplier,RefinementBoostTable.defaults());}
        public Settings {Objects.requireNonNull(boosts);if(!Double.isFinite(baseTicks)||baseTicks<=0||!Double.isFinite(manaCost)||manaCost<=0||manaCost>2000||!Double.isFinite(tierMultiplier)||tierMultiplier<1||tierMultiplier>10)throw new IllegalArgumentException("Invalid refinement settings");}
        public double ticks(CrystalTier tier){return baseTicks*Math.pow(tierMultiplier,tier.ordinal());}
    }
    /** worldTier is the last physically applied tier; tier can include deferred upgrades. */
    public record Crystal(long position,CrystalTier worldTier,CrystalTier tier,double progress,int contacts){
        public Crystal(long position,CrystalTier worldTier,CrystalTier tier,double progress){this(position,worldTier,tier,progress,0);}
        public Crystal {for(int side=0;side<6;side++)if(((contacts>>>(side*4))&15)>8)throw new IllegalArgumentException("Invalid neighbor tier");if((contacts>>>24)!=0)throw new IllegalArgumentException("Invalid neighbor contacts");Objects.requireNonNull(worldTier);Objects.requireNonNull(tier);if(!Double.isFinite(progress)||progress<0||tier.ordinal()<worldTier.ordinal())throw new IllegalArgumentException("Invalid refinement progress");}
    }
    public record Chunk(long id,double exposure,List<Crystal> crystals) {
        public Chunk {if(!Double.isFinite(exposure)||exposure<=0)throw new IllegalArgumentException("Invalid exposure");crystals=List.copyOf(crystals);}
    }
    public record Result(List<Chunk> chunks,double mana,int upgrades,Set<Long> served){}
    private record Event(double due,int index,int chunk,long position,int version){}
    /** Input chunks are ordered center-out. Served persists across calls and saves within a round. */
    public static Result advance(List<Chunk> chunks,double elapsed,double mana,double capacity,double perTick,Settings settings,Set<Long> priorServed){
        if(!Double.isFinite(elapsed)||elapsed<0||!Double.isFinite(capacity)||capacity<=0||!Double.isFinite(mana)||mana<0||mana>capacity||!Double.isFinite(perTick)||perTick<0)throw new IllegalArgumentException("Invalid refinement simulation");
        List<Crystal> input=new ArrayList<>();List<Integer> owners=new ArrayList<>();
        for(int c=0;c<chunks.size();c++)for(var crystal:chunks.get(c).crystals()){input.add(crystal);owners.add(c);}
        int size=input.size();var tiers=new CrystalTier[size];double[] start=new double[size],carry=new double[size],rate=new double[size];
        int[] versions=new int[size];boolean[] pending=new boolean[size];
        var index=new HashMap<Long,Integer>();for(int i=0;i<size;i++)index.put(input.get(i).position(),i);
        int[][] adjacent=new int[size][6];
        for(int i=0;i<size;i++)for(int side=0;side<6;side++)adjacent[i][side]=index.getOrDefault(RefinementNeighbors.position(input.get(i).position(),side),-1);
        for(int i=0;i<size;i++)tiers[i]=input.get(i).tier();
        Set<Long> positions=new HashSet<>(),served=new HashSet<>(priorServed);
        var future=new PriorityQueue<Event>(Comparator.comparingDouble(Event::due).thenComparingInt(Event::chunk).thenComparingLong(Event::position));
        var ready=new TreeMap<Integer,PriorityQueue<Event>>();
        for(int i=0;i<size;i++){
            var crystal=input.get(i);if(!positions.add(crystal.position()))throw new IllegalArgumentException("Duplicate crystal position");tiers[i]=crystal.tier();carry[i]=Math.min(settings.ticks(tiers[i]),crystal.progress());
            rate[i]=speed(i,tiers,input,adjacent,settings)*chunks.get(owners.get(i)).exposure();
            if(tiers[i].next().isPresent())future.add(new Event((settings.ticks(tiers[i])-carry[i])/rate[i],i,owners.get(i),crystal.position(),versions[i]));
        }
        double wall=0,active=0;int upgrades=0;
        while(true){
            discardStale(future,versions);
            if(future.isEmpty()&&ready.isEmpty())break;
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
            while(!future.isEmpty()&&future.peek().due()<=active+1e-8){
                var event=future.remove();if(event.version()!=versions[event.index()])continue;
                pending[event.index()]=true;ready.computeIfAbsent(event.chunk(),k->new PriorityQueue<>(Comparator.comparingLong(Event::position))).add(event);
            }
            Integer selected=null;
            for(int c:ready.keySet())if(!served.contains(chunks.get(c).id())){selected=c;break;}
            if(selected==null){served.clear();selected=ready.firstKey();}
            var queue=ready.get(selected);var event=queue.remove();if(queue.isEmpty())ready.remove(selected);
            served.add(chunks.get(selected).id());mana=Math.max(0,mana-settings.manaCost());
            int i=event.index();tiers[i]=tiers[i].next().orElseThrow();pending[i]=false;carry[i]=0;start[i]=active;upgrades++;
            // Only this mother and its six neighbors need new rates after an upgrade.
            var affected=new ArrayList<Integer>();affected.add(i);for(int neighbor:adjacent[i])if(neighbor>=0)affected.add(neighbor);
            for(int j:affected){
                if(j!=i){carry[j]=Math.min(settings.ticks(tiers[j]),carry[j]+Math.max(0,active-start[j])*rate[j]);start[j]=active;}
                rate[j]=speed(j,tiers,input,adjacent,settings)*chunks.get(owners.get(j)).exposure();
                if(!pending[j]){
                    versions[j]++;
                    if(tiers[j].next().isPresent())future.add(new Event(active+Math.max(0,settings.ticks(tiers[j])-carry[j])/rate[j],j,owners.get(j),input.get(j).position(),versions[j]));
                }
            }
        }
        discardStale(future,versions);
        double remaining=Math.max(0,elapsed-wall);
        if(!future.isEmpty()||!ready.isEmpty()){
            double wait=mana>=settings.manaCost()?0:perTick>0?(settings.manaCost()-mana)/perTick:Double.POSITIVE_INFINITY;
            active+=Math.max(0,remaining-wait);
        }
        mana=Math.min(capacity,mana+remaining*perTick);
        List<Chunk> output=new ArrayList<>();int i=0;
        for(var chunk:chunks){var values=new ArrayList<Crystal>();for(var old:chunk.crystals()){
            double progress=tiers[i]==CrystalTier.WHITE?0:Math.min(settings.ticks(tiers[i]),carry[i]+Math.max(0,active-start[i])*rate[i]);
            values.add(new Crystal(old.position(),old.worldTier(),tiers[i],progress,old.contacts()));i++;
        }output.add(new Chunk(chunk.id(),chunk.exposure(),values));}
        return new Result(List.copyOf(output),mana,upgrades,Set.copyOf(served));
    }
    private static void discardStale(PriorityQueue<Event> events,int[] versions){while(!events.isEmpty()&&events.peek().version()!=versions[events.peek().index()])events.remove();}
    private static double speed(int i,CrystalTier[] tiers,List<Crystal> crystals,int[][] adjacent,Settings settings){
        var highest=tiers[i];
        for(int side=0;side<6;side++){
            int neighbor=adjacent[i][side];var tier=neighbor>=0?tiers[neighbor]:RefinementNeighbors.tier(crystals.get(i).contacts(),side);
            if(tier!=null&&tier.ordinal()>highest.ordinal())highest=tier;
        }
        return settings.boosts().speed(tiers[i],highest);
    }
    private RefinementBatch(){}
}
