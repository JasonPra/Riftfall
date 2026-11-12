package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeyserShapeTest {
    private final GeyserRegion field = new GeyserRegion(256,256,129,1000,1);
    @Test void circularFieldHasNoSquareCornersAndRetainsItsEcosystemBudget() {
        assertFalse(field.contains(256+64,256+64));
        assertTrue(field.contains(256+64,256));
        assertEquals(129*129,field.chunkCount());
        assertEquals(129*129*2000D,GeyserLedger.capacity(field));
        assertTrue(Math.PI*Math.pow(GeyserShape.radius(field),2)>=Math.pow(field.blockWidth(),2));
        double sum=0;int e=field.extentChunks();
        for(int x=-e;x<=e;x++)for(int z=-e;z<=e;z++)sum+=field.outputAt(256+x,256+z);
        assertEquals(field.totalOutput(),sum,1e-8);
    }
    @Test void everyRayHasAnOuterEdgeNearTheCircleWithBroadAndFineVariation() {
        double min=Double.MAX_VALUE,max=-Double.MAX_VALUE;
        for(int degrees=0;degrees<360;degrees+=3){
            double angle=Math.toRadians(degrees),r=GeyserShape.radius(field),low=r-40,high=r+40;
            for(int i=0;i<24;i++){
                double mid=(low+high)/2;
                if(GeyserShape.distance(field,field.centerX()+Math.cos(angle)*mid,field.centerZ()+Math.sin(angle)*mid)<0)low=mid;else high=mid;
            }
            double boundary=(low+high)/2;
            assertTrue(Math.abs(boundary-r)<=GeyserShape.EDGE_NOISE+.01);
            min=Math.min(min,boundary);max=Math.max(max,boundary);
        }
        assertTrue(max-min>10,"edge must not be a perfectly smooth circle");
    }
    @Test void widenedCoreKeepsAtLeastTheOldSquareCoresAreaAfterNoise() {
        int cells=0;
        for(int dx=-180;dx<=180;dx+=4)for(int dz=-180;dz<=180;dz+=4)
            if(GeyserShape.zone(field,field.centerX()+dx,field.centerZ()+dz)==GeyserZone.CORE)cells++;
        assertTrue(cells*16>=272*272);
        for(int angle=0;angle<360;angle++){
            double a=Math.toRadians(angle);
            assertEquals(GeyserZone.CORE,GeyserShape.zone(field,field.centerX()+Math.cos(a)*156,field.centerZ()+Math.sin(a)*156));
        }
    }
    @Test void everyRingRetainsItsOldAreaEvenWithWorstCaseBoundaryNoise() {
        for(int width:new int[]{129,193,257}){
            var g=new GeyserRegion(256,256,width,1000,1);
            double[] radii={GeyserShape.coreRadius(g),GeyserShape.innerRadius(g),GeyserShape.middleRadius(g),GeyserShape.radius(g)};
            double[] amplitudes={11.2,22.4,32,32};
            int[] old={g.coreRadiusChunks(),g.innerRadiusChunks(),g.middleRadiusChunks(),g.halfWidth()};
            double previousMax=0,previousArea=0;
            for(int ring=0;ring<4;ring++){
                double oldArea=Math.pow((old[ring]*2+1)*16.0,2);
                double stableArea=Math.PI*(Math.pow(radii[ring]-amplitudes[ring],2)-previousMax*previousMax);
                assertTrue(stableArea+1e-6>=oldArea-previousArea,"lost area in ring "+ring+" of "+width);
                previousMax=radii[ring]+amplitudes[ring];previousArea=oldArea;
            }
        }
    }
    @Test void eachCircularZoneRetainsAtLeastItsPreviousChunkCenterCount() {
        for(int width:new int[]{129,193,257}){
            var g=new GeyserRegion(-256,256,width,1000,1);
            var count=new java.util.EnumMap<GeyserZone,Integer>(GeyserZone.class);int e=g.extentChunks();
            for(int x=-e;x<=e;x++)for(int z=-e;z<=e;z++)count.merge(g.zone(g.centerChunkX()+x,g.centerChunkZ()+z),1,Integer::sum);
            int previous=0;int[] old={g.coreRadiusChunks(),g.innerRadiusChunks(),g.middleRadiusChunks(),g.halfWidth()};
            var zones=new GeyserZone[]{GeyserZone.CORE,GeyserZone.INNER,GeyserZone.MIDDLE,GeyserZone.OUTER};
            for(int ring=0;ring<4;ring++){
                int area=(old[ring]*2+1)*(old[ring]*2+1);
                assertTrue(count.getOrDefault(zones[ring],0)>=area-previous,"lost chunk centers in "+zones[ring]);
                previous=area;
            }
        }
    }
    @Test void bordersCanCrossInsideOneChunkAndUseContinuousWorldCoordinates() {
        boolean mixed=false;
        for(int cx=field.centerChunkX();cx<=field.centerChunkX()+field.extentChunks();cx++){
            var zones=new java.util.HashSet<GeyserZone>();
            for(int x=0;x<4;x++)for(int z=0;z<4;z++)zones.add(GeyserShape.zone(field,cx*16+x*4+2,field.centerChunkZ()*16+z*4+2));
            mixed|=zones.size()>1;
        }
        assertTrue(mixed);
        for(int x=-200;x<200;x++){
            double a=GeyserShape.fray(field,x,-x),b=GeyserShape.fray(field,x+.01,-x);
            assertTrue(Math.abs(a-b)<.02);
        }
    }
    @Test void expandedCircleAndTransitionFitInsidePlacementCells() {
        var layout=new GeyserLayout(42);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)layout.cell(x,z).ifPresent(g->{
            double margin=GeyserShape.radius(g)+GeyserShape.EDGE_NOISE+GeyserTransition.WIDTH+12;
            double localX=Math.floorMod(g.centerChunkX(),512)*16+8,localZ=Math.floorMod(g.centerChunkZ(),512)*16+8;
            assertTrue(localX-margin>0&&localX+margin<8192&&localZ-margin>0&&localZ+margin<8192);
        });
    }
    @Test void previousSquareDescriptorsAndNewCirclesBothSurviveReload() {
        var old=new CompoundTag();old.putInt("Version",2);old.putLong("Seed",42);
        var fields=new ListTag();var row=new CompoundTag();row.putInt("X",256);row.putInt("Z",256);row.putInt("Width",129);row.putDouble("Output",1000);row.putBoolean("Legacy",false);fields.add(row);old.put("Fields",fields);
        var saved=GeyserRegions.load(old,null).discovered().iterator().next();
        assertFalse(saved.circular());assertTrue(saved.contains(320,320));
        var regions=new GeyserRegions(42);var g=regions.candidate(0,0).orElseGet(()->regions.candidate(1,0).orElseThrow());
        regions.discover(g.centerChunkX(),g.centerChunkZ());
        var restored=GeyserRegions.load(regions.save(new CompoundTag(),null),null).discovered().iterator().next();
        assertEquals(g,restored);assertTrue(restored.circular());
    }
}
