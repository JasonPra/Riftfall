package org.Azkerash.riftfall.crystals;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Stable, nonoverlapping 2x2 visual formations; all four members must be mature and match. */
public final class CrystalLargeGroups {
    public record Member(CrystalTier tier,int layers,Direction face,int variant){}
    public record Group(BlockPos anchor,int preset,int u,int v){public int quarter(){return u+2*v;}}
    private record Candidate(BlockPos anchor,long rank,int preset){}
    public static Group find(BlockPos pos,Direction face,Function<BlockPos,Member> lookup){
        var members=new HashMap<BlockPos,Member>();Function<BlockPos,Member> read=p->{if(!members.containsKey(p))members.put(p,lookup.apply(p));return members.get(p);};
        var own=read.apply(pos);if(own==null||own.layers()!=8||own.face()!=face)return null;
        var x=CrystalSurfaceConnections.tangent(face,1);var z=CrystalSurfaceConnections.tangent(face,2);var candidates=new HashMap<BlockPos,Candidate>();
        Function<BlockPos,Candidate> candidate=anchor->{
            if(candidates.containsKey(anchor))return candidates.get(anchor);
            long seed=anchor.asLong()^((long)face.ordinal()*0x9e3779b97f4a7c15L);boolean valid=true;
            for(int u=0;u<2;u++)for(int v=0;v<2;v++){
                var member=read.apply(anchor.relative(x,u).relative(z,v));
                if(member==null||member.layers()!=8||member.face()!=face||member.tier()!=own.tier()){valid=false;break;}
                seed^=mix((long)(member.variant()+1)*(u+2*v+1)*0x632be59bd9b4e019L);
            }
            long rank=mix(seed);var result=valid&&Long.remainderUnsigned(rank,100)<35?new Candidate(anchor,rank,(int)Long.remainderUnsigned(mix(rank),6)):null;
            candidates.put(anchor,result);return result;
        };
        for(int u=0;u<2;u++)for(int v=0;v<2;v++){
            var anchor=pos.relative(x,-u).relative(z,-v);var current=candidate.apply(anchor);if(current==null)continue;boolean winner=true;
            for(int a=-1;a<=1&&winner;a++)for(int b=-1;b<=1;b++){
                if(a==0&&b==0)continue;var other=candidate.apply(anchor.relative(x,a).relative(z,b));
                if(other!=null&&(Long.compareUnsigned(other.rank(),current.rank())<0||(other.rank()==current.rank()&&other.anchor().compareTo(anchor)<0))){winner=false;break;}
            }
            if(winner)return new Group(anchor,current.preset(),u,v);
        }return null;
    }
    private static long mix(long x){x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;return x^(x>>>31);}
    private CrystalLargeGroups(){}
}
