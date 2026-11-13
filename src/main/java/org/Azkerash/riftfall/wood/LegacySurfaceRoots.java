package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.api.voxmap.SimpleVoxmap;
import com.dtteam.dynamictrees.systems.genfeature.RootsGenFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** Uneven, tapered surface roots; DT handles support, terrain steps and disconnection. */
public final class LegacySurfaceRoots extends RootsGenFeature {
    public LegacySurfaceRoots(){super(ResourceLocation.fromNamespaceAndPath("riftfall","legacy_surface_roots"));}
    @Override protected boolean isReplaceableWithRoots(net.minecraft.world.level.LevelAccessor level,net.minecraft.world.level.block.state.BlockState state,BlockPos pos){
        return state.getBlock() instanceof com.dtteam.dynamictrees.block.branch.SurfaceRootBlock root&&root.getFamily() instanceof LegacyTreeFamily
            ||super.isReplaceableWithRoots(level,state,pos);
    }
    @Override protected SimpleVoxmap[] createRootMaps(){
        var maps=new SimpleVoxmap[6];
        for(int variant=0;variant<maps.length;variant++){
            int size=17,center=8;byte[] data=new byte[size*size];
            var random=RandomSource.create(38719L+variant*731L);
            int count=6+random.nextInt(3);double phase=random.nextDouble()*Math.PI*2;
            for(int root=0;root<count;root++){
                double angle=phase+(root+random.nextDouble()*.4)*Math.PI*2/count;
                double dx=Math.cos(angle),dz=Math.sin(angle);int x=0,z=0;
                int length=5+random.nextInt(3);
                for(int step=1;step<=length;step++){
                    int tx=(int)Math.round(dx*step),tz=(int)Math.round(dz*step);
                    while(x!=tx||z!=tz){
                        if(Math.abs(tx-x)>=Math.abs(tz-z))x+=Integer.signum(tx-x);else z+=Integer.signum(tz-z);
                        int radius=8-Math.abs(x)-Math.abs(z);
                        if(radius<=0)break;
                        data[(z+center)*size+x+center]=(byte)Math.max(data[(z+center)*size+x+center],radius);
                    }
                    if(Math.abs(x)+Math.abs(z)>=7)break;
                }
            }
            maps[variant]=new SimpleVoxmap(size,1,size,data).setCenter(new BlockPos(center,0,center));
        }
        return maps;
    }
}
