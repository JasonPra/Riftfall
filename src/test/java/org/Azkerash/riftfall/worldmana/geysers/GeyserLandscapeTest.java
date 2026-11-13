package org.Azkerash.riftfall.worldmana.geysers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeyserLandscapeTest {
    @Test void routedWaterHasOneSourceAndNeverClimbsDownstream(){
        for(int i=0;i<6;i++){
            var g=new GeyserRegion(i*512,-512,193,1000,1);var route=GeyserDrainage.of(g);
            int previous=999,actualPrevious=999,river=0,lake=0;
            for(var point:route.path()){
                assertTrue(point.water()<=previous);previous=point.water();
                var c=GeyserTerrain.column(42,g,(int)Math.round(point.x()),(int)Math.round(point.z()));
                assertTrue(c.wet(),"Dry planned route at "+point);
                assertTrue(c.water()<=actualPrevious,"Actual water climbs at "+point+" from "+actualPrevious+" to "+c.water());actualPrevious=c.water();
                if(c.kind()==GeyserTerrain.WaterKind.LAKE)lake++;else river++;
            }
            assertTrue(river>0&&lake>0);
        }
    }
    @Test void springAndReceivingPoolEdgesGradeIntoGround(){
        var g=new GeyserRegion(0,0,193,1000,1);var route=GeyserDrainage.of(g);var p=route.source();
        int previous=GeyserTerrain.height(42,g,(int)p.x()-70,(int)p.z());
        for(int x=(int)p.x()-69;x<=(int)p.x()+70;x++){
            int height=GeyserTerrain.height(42,g,x,(int)p.z());
            assertTrue(Math.abs(height-previous)<=2,"Abrupt shore cutoff at "+x);previous=height;
        }
    }
    @Test void floodedDepressionShorelinesHaveNoExcavatedVerticalCutoff(){
        var g=new GeyserRegion(0,0,193,1000,1);var route=GeyserDrainage.of(g);int shores=0;
        for(var point:route.path())for(int dx=-80;dx<=80;dx+=4)for(int dz=-80;dz<=80;dz+=4){
            int x=(int)Math.round(point.x())+dx,z=(int)Math.round(point.z())+dz;
            var a=GeyserTerrain.column(42,g,x,z);if(!a.wet()||a.kind()!=GeyserTerrain.WaterKind.LAKE)continue;
            var b=GeyserTerrain.column(42,g,x+1,z);if(b.wet())continue;
            assertTrue(Math.abs(a.floor()-b.floor())<=2,"Hard lake edge at "+x+","+z);shores++;
        }
        assertTrue(shores>0,"No lake shore exercised");
    }
    @Test void shallowBankApronDoesNotBecomeAnotherWideWaterChannel(){
        for(double ground:new double[]{68,74,80})for(double width:new double[]{3,4.5,6}){
            int wet=0;
            for(int x=-45;x<=45;x++){
                var c=GeyserTerrain.riverColumn(ground,74,width,Math.abs(x),2);
                if(c.wet()){wet++;assertTrue(Math.abs(x)<width+2,"Water floods the dry bank apron");}
            }
            assertTrue(wet>=5&&wet<=15,"Actual wet width was "+wet);
        }
    }
    @Test void waterBiomesKeepZoneIdentityWithoutReplacingLandEverywhere(){
        var keys=new java.util.HashSet<>();
        for(var zone:new GeyserZone[]{GeyserZone.CORE,GeyserZone.INNER,GeyserZone.MIDDLE,GeyserZone.OUTER}){
            assertTrue(keys.add(GeyserBiomes.key(zone)));assertTrue(keys.add(GeyserBiomes.waterKey(zone,false)));assertTrue(keys.add(GeyserBiomes.waterKey(zone,true)));
        }
        assertEquals(12,keys.size());assertThrows(IllegalArgumentException.class,()->GeyserBiomes.waterKey(GeyserZone.OUTSIDE,true));
        var g=new GeyserRegion(-512,512,193,1000,1);
        int wet=0,dry=0;
        for(int z=(int)g.centerZ()-500;z<g.centerZ()+500;z+=4)for(int dx:new int[]{0,40}){
            int x=(int)Math.round(GeyserTerrain.riverCenter(g,z))+dx;var c=GeyserTerrain.column(42,g,x,z);
            var zone=GeyserShape.zone(g,x,z);
            assertEquals(c.wet()?GeyserBiomes.waterKey(zone,c.kind()==GeyserTerrain.WaterKind.LAKE):GeyserBiomes.key(zone),GeyserBiomes.surfaceKey(42,g,x,z));
            if(c.wet())wet++;else dry++;
        }
        assertTrue(wet>0&&dry>0);
    }
}
