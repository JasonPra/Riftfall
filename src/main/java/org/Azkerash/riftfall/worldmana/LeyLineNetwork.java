package org.Azkerash.riftfall.worldmana;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-dimension tree. A line is only an endpoint relationship: no entities, blocks, particles or rendering. */
public final class LeyLineNetwork extends SavedData {
    private static final Factory<LeyLineNetwork> FACTORY=new Factory<>(LeyLineNetwork::new,LeyLineNetwork::load,null);
    public static final double GEYSER_BASE_REGENERATION=5.0;
    public record Node(int chunkX,int chunkZ,int y,Long parent,int matureCrystals){
        public Node {if(matureCrystals<0)throw new IllegalArgumentException("Negative crystal count");}
        public double strength(){return GEYSER_BASE_REGENERATION+ChunkMana.MATURE_CRYSTAL_BONUS*matureCrystals;}
        public long id(){return ChunkPos.asLong(chunkX,chunkZ);}
        public double x(){return chunkX*16.0+8;}
        public double z(){return chunkZ*16.0+8;}
    }
    private final Map<Long,Node> nodes=new LinkedHashMap<>();
    public static LeyLineNetwork get(ServerLevel level){return level.getDataStorage().computeIfAbsent(FACTORY,"riftfall_ley_lines");}
    public int size(){return nodes.size();}
    public Optional<Node> node(int chunkX,int chunkZ){return Optional.ofNullable(nodes.get(ChunkPos.asLong(chunkX,chunkZ)));}
    public Collection<Node> nodes(){return List.copyOf(nodes.values());}
    public boolean discover(int chunkX,int chunkZ,int y,long randomness){
        long id=ChunkPos.asLong(chunkX,chunkZ);if(nodes.containsKey(id))return false;
        // Only runs when a very rare node is discovered. Keep the nearest three in constant auxiliary space.
        var nearest=new ArrayList<Node>(4);
        Comparator<Node> order=Comparator.comparingDouble((Node n)->Math.hypot((double)n.chunkX()-chunkX,(double)n.chunkZ()-chunkZ)).thenComparingLong(Node::id);
        for(var n:nodes.values()){nearest.add(n);nearest.sort(order);if(nearest.size()>3)nearest.removeLast();}
        Long parent=nearest.isEmpty()?null:nearest.get(Math.floorMod(LeyLinePlacement.mix(randomness^id),nearest.size())).id();
        nodes.put(id,new Node(chunkX,chunkZ,y,parent,0));setDirty();return true;
    }
    public void updateCrystals(int chunkX,int chunkZ,int count){
        var old=nodes.get(ChunkPos.asLong(chunkX,chunkZ));if(old==null||old.matureCrystals()==count)return;
        nodes.put(old.id(),new Node(old.chunkX(),old.chunkZ(),old.y(),old.parent(),count));setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        ListTag list=new ListTag();for(var n:nodes.values()){
            CompoundTag entry=new CompoundTag();entry.putInt("ChunkX",n.chunkX());entry.putInt("ChunkZ",n.chunkZ());entry.putInt("Y",n.y());entry.putInt("MatureCrystals",n.matureCrystals());
            if(n.parent()!=null)entry.putLong("Parent",n.parent());list.add(entry);
        }tag.putInt("Version",2);tag.put("Nodes",list);return tag;
    }
    public static LeyLineNetwork load(CompoundTag tag,HolderLookup.Provider provider){
        var network=new LeyLineNetwork();var list=tag.getList("Nodes",10);
        for(int i=0;i<list.size();i++){
            var entry=list.getCompound(i);var node=new Node(entry.getInt("ChunkX"),entry.getInt("ChunkZ"),entry.getInt("Y"),entry.contains("Parent",4)?entry.getLong("Parent"):null,entry.getInt("MatureCrystals"));
            // Our saved insertion order always puts parents before children, making cycle checks linear.
            if(network.nodes.containsKey(node.id())||node.parent()!=null&&!network.nodes.containsKey(node.parent())||node.parent()==null&&!network.nodes.isEmpty())throw new IllegalArgumentException("Invalid ley-line tree data");
            network.nodes.put(node.id(),node);
        }return network;
    }
}
