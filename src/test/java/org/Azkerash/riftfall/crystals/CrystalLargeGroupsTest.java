package org.Azkerash.riftfall.crystals;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalLargeGroupsTest {
    private Map<BlockPos,CrystalLargeGroups.Member> patch(BlockPos anchor,Direction face){
        var map=new HashMap<BlockPos,CrystalLargeGroups.Member>();
        for(int u=0;u<2;u++)for(int v=0;v<2;v++)map.put(anchor.relative(CrystalSurfaceConnections.tangent(face,1),u).relative(CrystalSurfaceConnections.tangent(face,2),v),new CrystalLargeGroups.Member(CrystalTier.BLUE,8,face,(u+2*v)%6));
        return map;
    }
    @Test void allFourMembersAgreeAndAllSixVariationsCanAppearOnEveryFace(){
        for(var face:Direction.values()){
            var presets=new HashSet<Integer>();int formed=0;
            for(int i=0;i<100;i++){
                var anchor=new BlockPos(i*4,30,40);var map=patch(anchor,face);var group=CrystalLargeGroups.find(anchor,face,map::get);
                if(group==null)continue;formed++;presets.add(group.preset());var quarters=new HashSet<Integer>();
                for(var pos:map.keySet()){var member=CrystalLargeGroups.find(pos,face,map::get);assertNotNull(member);assertEquals(group.anchor(),member.anchor());assertEquals(group.preset(),member.preset());quarters.add(member.quarter());assertEquals(member,CrystalLargeGroups.find(pos,face,map::get));}
                assertEquals(Set.of(0,1,2,3),quarters);
            }assertTrue(formed>15&&formed<60);assertEquals(6,presets.size());
        }
    }
    @Test void seedsPartialGrowthMixedTiersAndDifferentFacesCannotMerge(){
        var anchor=BlockPos.ZERO;var map=patch(anchor,Direction.UP);
        for(int attempt=0;attempt<100&&CrystalLargeGroups.find(anchor,Direction.UP,map::get)==null;attempt++){anchor=anchor.offset(4,0,0);map=patch(anchor,Direction.UP);}
        assertNotNull(CrystalLargeGroups.find(anchor,Direction.UP,map::get));
        for(int layer=1;layer<8;layer++){map.put(anchor,new CrystalLargeGroups.Member(CrystalTier.BLUE,layer,Direction.UP,0));for(var pos:map.keySet())assertNull(CrystalLargeGroups.find(pos,Direction.UP,map::get));}
        map.put(anchor,new CrystalLargeGroups.Member(CrystalTier.RED,8,Direction.UP,0));for(var pos:map.keySet())assertNull(CrystalLargeGroups.find(pos,Direction.UP,map::get));
        map.put(anchor,new CrystalLargeGroups.Member(CrystalTier.BLUE,8,Direction.NORTH,0));for(var pos:map.keySet())assertNull(CrystalLargeGroups.find(pos,Direction.UP,map::get));
        map.remove(anchor);for(var pos:map.keySet())assertNull(CrystalLargeGroups.find(pos,Direction.UP,map::get));
    }
    @Test void overlappingGroupsNeverClaimTheSameBlock(){
        Function<BlockPos,CrystalLargeGroups.Member> field=p->p.getY()==0?new CrystalLargeGroups.Member(CrystalTier.RED,8,Direction.UP,0):null;
        var claimed=new HashMap<BlockPos,BlockPos>();
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){
            var pos=new BlockPos(x,0,z);var group=CrystalLargeGroups.find(pos,Direction.UP,field);if(group==null)continue;
            for(int u=0;u<2;u++)for(int v=0;v<2;v++){
                var member=group.anchor().offset(u,0,v);var previous=claimed.putIfAbsent(member,group.anchor());assertTrue(previous==null||previous.equals(group.anchor()));
            }
        }assertFalse(claimed.isEmpty());
    }
}
