package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.*;
import com.dtteam.dynamictrees.systems.GrowSignal;
import com.dtteam.dynamictrees.utility.CoordUtils;
import net.minecraft.core.Direction;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Living limbs support separated, irregular canopy layers with short lateral fans. */
public final class LegacyGrowthLogic extends GrowthLogicKit {
    public static final int MAX_TRUNK_HEIGHT=27,MAX_BRANCH_HEIGHT=29,MAX_HORIZONTAL_REACH=23,BRANCH_ENERGY=28;
    private static final int LAYER_SPACING=6;
    // A growth pulse follows one connected path. Remember its fork height while it travels;
    // nothing is saved on the tree and both planted and generated branches use this kit.
    private static final class Limb {
        final Direction outward;
        final int height,run,riseAt,fanSpacing;
        final boolean rises,leftFavored;
        boolean child,lifted;
        Limb(Direction outward,int height,int run,int hash){
            this.outward=outward;this.height=height;this.run=run;
            riseAt=3+Math.floorMod(hash/5,3);fanSpacing=3+Math.floorMod(hash/11,2);
            rises=(hash&4)!=0;leftFavored=(hash&8)!=0;
        }
    }
    private final Map<GrowSignal,Limb> limbs=Collections.synchronizedMap(new WeakHashMap<>());
    public LegacyGrowthLogic(){super(LegacyTrees.ID);}
    private static int firstLayer(GrowSignal signal){return 10+Math.floorMod(CoordUtils.coordHashCode(signal.rootPos,2),2);}
    private static int layerHeight(GrowSignal signal,int layer){
        int variation=layer==0?0:Math.floorMod(CoordUtils.coordHashCode(signal.rootPos,2)/7+layer*5,2);
        return firstLayer(signal)+layer*LAYER_SPACING+variation;
    }
    private static int forkHeight(GrowSignal signal,int layer,Direction direction){
        int stagger=Math.floorMod(CoordUtils.coordHashCode(signal.rootPos,2)+direction.get2DDataValue()*31+layer*17,2);
        return layerHeight(signal,layer)+stagger;
    }
    private static boolean connected(DirectionManipulationContext context,Direction direction){
        var state=context.level().getBlockState(context.pos().relative(direction));
        return state.getBlock() instanceof com.dtteam.dynamictrees.block.branch.BranchBlock branch
            &&branch.getFamily()==context.species().getFamily();
    }
    @Override public Direction selectNewDirection(GrowthLogicKitConfiguration configuration,DirectionSelectionContext context){
        var signal=context.signal();
        Direction next=super.selectNewDirection(configuration,context);
        if(signal.isInTrunk()&&next.getAxis().isHorizontal()){
            int layer=Math.max(0,(signal.delta.getY()-firstLayer(signal))/LAYER_SPACING);
            int hash=CoordUtils.coordHashCode(signal.rootPos,2)+next.get2DDataValue()*19+layer*37;
            int run=(layer==2?5:6)+Math.floorMod(hash,5);
            limbs.put(signal,new Limb(next,signal.delta.getY(),run,hash));
            signal.energy=BRANCH_ENERGY+Math.floorMod(hash,4);
        }else{
            var limb=limbs.get(signal);
            if(limb!=null&&!limb.child&&next!=limb.outward){
                int distance=signal.delta.getX()*limb.outward.getStepX()+signal.delta.getZ()*limb.outward.getStepZ();
                if(next==Direction.UP&&limb.rises&&!limb.lifted&&distance==limb.riseAt&&signal.delta.getY()==limb.height){
                    limb.lifted=true;
                }else{
                    limb.child=true;
                    signal.energy=6+Math.floorMod(CoordUtils.coordHashCode(context.pos(),2)+next.ordinal()*13,5);
                }
            }
        }
        if(next==Direction.UP&&signal.delta.getY()>=(signal.isInTrunk()?layerHeight(signal,2)+1:MAX_BRANCH_HEIGHT))signal.energy=0;
        return next;
    }
    @Override public int[] populateDirectionProbabilityMap(GrowthLogicKitConfiguration configuration,DirectionManipulationContext context){
        int[] map=super.populateDirectionProbabilityMap(configuration,context);
        int[] nativeMap=map.clone();var signal=context.signal();map[Direction.DOWN.ordinal()]=0;
        if(signal.isInTrunk()){
            for(Direction direction:Direction.Plane.HORIZONTAL){
                if(connected(context,direction))continue;
                boolean fork=false;
                for(int layer=0;layer<3;layer++)
                    fork|=signal.delta.getY()==forkHeight(signal,layer,direction)&&(context.radius()>1||layer==2);
                map[direction.ordinal()]=fork?2:0;
            }
            map[Direction.UP.ordinal()]=signal.delta.getY()<firstLayer(signal)?8:signal.delta.getY()<layerHeight(signal,2)+1?Math.max(6,nativeMap[Direction.UP.ordinal()]):0;
        }else{
            var limb=limbs.get(signal);
            if(limb!=null){
                for(Direction direction:Direction.values())map[direction.ordinal()]=0;
                int distance=signal.delta.getX()*limb.outward.getStepX()+signal.delta.getZ()*limb.outward.getStepZ();
                if(!limb.child){
                    if(distance<limb.run)map[limb.outward.ordinal()]=6;
                    // Unequal fans and occasional one-block arches break up planar spokes.
                    if(distance>=3&&(Math.floorMod(distance-limb.riseAt,limb.fanSpacing)==0||distance>=limb.run)){
                        map[limb.outward.getClockWise().ordinal()]=limb.leftFavored?1:3;
                        map[limb.outward.getCounterClockWise().ordinal()]=limb.leftFavored?3:1;
                    }
                    if(limb.rises&&!limb.lifted&&distance==limb.riseAt&&signal.delta.getY()==limb.height)map[Direction.UP.ordinal()]=2;
                    if(distance>=limb.run&&signal.delta.getY()==limb.height)map[Direction.UP.ordinal()]=1;
                }else if(signal.delta.getY()<limb.height+1&&signal.dir==Direction.UP){
                    map[Direction.UP.ordinal()]=4;
                }else{
                    // Offshoots spread within their parent's layer instead of rising into a cone.
                    for(Direction direction:Direction.Plane.HORIZONTAL)map[direction.ordinal()]=1;
                    map[signal.dir.getAxis().isHorizontal()?signal.dir.ordinal():limb.outward.ordinal()]+=3;
                    if(signal.delta.getY()==limb.height&&Math.floorMod(CoordUtils.coordHashCode(context.pos(),2),7)==0)
                        map[Direction.UP.ordinal()]=1;
                }
                // Existing connections retain native routing, including older tree layouts.
                for(Direction direction:Direction.values())if(connected(context,direction))map[direction.ordinal()]=Math.max(map[direction.ordinal()],nativeMap[direction.ordinal()]);
            }
            if(signal.delta.getY()>=MAX_BRANCH_HEIGHT)map[Direction.UP.ordinal()]=0;
            for(Direction direction:Direction.Plane.HORIZONTAL){
                var next=context.pos().relative(direction);
                if(Math.abs(next.getX()-signal.rootPos.getX())>MAX_HORIZONTAL_REACH||Math.abs(next.getZ()-signal.rootPos.getZ())>MAX_HORIZONTAL_REACH)map[direction.ordinal()]=0;
            }
        }
        map[signal.dir.getOpposite().ordinal()]=0;
        return map;
    }
    @Override public float getEnergy(GrowthLogicKitConfiguration configuration,PositionalSpeciesContext context){
        return context.species().getSignalEnergy()+Math.floorMod(CoordUtils.coordHashCode(context.pos(),2),7);
    }
}
