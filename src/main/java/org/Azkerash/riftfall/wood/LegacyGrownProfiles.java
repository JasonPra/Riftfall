package org.Azkerash.riftfall.wood;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Normal branch states captured from living seedlings, not another shape generator. */
final class LegacyGrownProfiles {
    record Node(int x,int y,int z,int radius){
        BlockPos position(BlockPos soil,Direction facing){
            return switch(facing){
                case SOUTH->soil.offset(-x,y,-z);
                case EAST->soil.offset(-z,y,x);
                case WEST->soil.offset(z,y,-x);
                default->soil.offset(x,y,z);
            };
        }
    }
    record Sample(String code,String roots,String stage,List<Node> nodes){}
    private static final java.util.List<Sample> SAMPLES=new java.util.ArrayList<>();
    private static final Map<String,List<Node>> PROFILES=load();
    static Sample choose(String stage,long hash){
        var choices=SAMPLES.stream().filter(s->s.stage().equals(stage)).toList();
        return choices.isEmpty()?null:choices.get(Math.floorMod(hash,choices.size()));
    }
    static List<Node> find(String code){return PROFILES.getOrDefault(code,List.of());}
    static String referenceRange(){
        var counts=SAMPLES.stream().filter(s->s.stage().equals("adult")).mapToInt(s->s.nodes().size()).summaryStatistics();
        return counts.getMin()+"–"+counts.getMax()+" branches";
    }
    private static Map<String,List<Node>> load(){
        try(var stream=LegacyGrownProfiles.class.getResourceAsStream("/trees/riftfall/jo_codes/vermillion_legacy_profiles.json")){
            if(stream==null)throw new IllegalStateException("Missing grown Legacy branch profiles");
            var profiles=new HashMap<String,List<Node>>();
            for(var entry:JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonArray()){
                var sample=entry.getAsJsonObject();var nodes=new ArrayList<Node>();
                for(var value:sample.getAsJsonArray("branches")){
                    var a=value.getAsJsonArray();int radius=a.get(3).getAsInt();
                    if(radius<1||radius>24)throw new IllegalStateException("Invalid captured branch radius");
                    nodes.add(new Node(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt(),radius));
                }
                String code=sample.get("jo_code").getAsString();
                profiles.put(code,List.copyOf(nodes));
                SAMPLES.add(new Sample(code,sample.has("roots_jo_code")?sample.get("roots_jo_code").getAsString():"",sample.has("growth_stage")?sample.get("growth_stage").getAsString():"adult",List.copyOf(nodes)));
            }
            return Map.copyOf(profiles);
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot read grown Legacy profiles",error);}
    }
    private LegacyGrownProfiles(){}
}
