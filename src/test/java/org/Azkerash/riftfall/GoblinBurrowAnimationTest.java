package org.Azkerash.riftfall;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoblinBurrowAnimationTest {
    private JsonObject json(String path) throws Exception {
        try(var stream=getClass().getResourceAsStream(path)){assertNotNull(stream,path);return JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();}
    }
    private double[][] matrix(JsonArray a){double[][] m=new double[4][4];for(int i=0;i<16;i++)m[i/4][i%4]=a.get(i).getAsDouble();return m;}
    private double[][] multiply(double[][] a,double[][] b){double[][] out=new double[4][4];for(int i=0;i<4;i++)for(int j=0;j<4;j++)for(int k=0;k<4;k++)out[i][j]+=a[i][k]*b[k][j];return out;}
    private double[] point(double[][] m,double x,double y,double z){double[] p={x,y,z,1},out=new double[3];for(int i=0;i<3;i++)for(int k=0;k<4;k++)out[i]+=m[i][k]*p[k];return out;}
    private void walk(JsonObject node,double[][] parent,Map<String,JsonObject> channels,int frame,Map<String,double[][]> out){
        String name=node.get("name").getAsString();var local=matrix(channels.get(name).getAsJsonArray("transform").get(frame).getAsJsonArray());
        var global=multiply(parent,local);out.put(name,global);
        for(var child:node.getAsJsonArray("children"))walk(child.getAsJsonObject(),global,channels,frame,out);
    }
    private void bind(JsonObject node,double[][] parent,Map<String,double[][]> out){
        String name=node.get("name").getAsString();var global=multiply(parent,matrix(node.getAsJsonArray("transform")));out.put(name,global);
        for(var child:node.getAsJsonArray("children"))bind(child.getAsJsonObject(),global,out);
    }
    private double[][] inverseRigid(double[][] m){
        double[][] inverse=new double[4][4];inverse[3][3]=1;
        for(int r=0;r<3;r++){for(int c=0;c<3;c++)inverse[r][c]=m[c][r];for(int c=0;c<3;c++)inverse[r][3]-=inverse[r][c]*m[c][3];}
        return inverse;
    }
    @Test void damageProtectionBeginsOnlyAfterTheAnimatedMeshIsBelowGround() throws Exception {
        var armature=json("/assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature");
        var root=armature.getAsJsonArray("hierarchy").get(0).getAsJsonObject();var names=armature.getAsJsonArray("joints");
        Map<String,JsonObject> channels=new HashMap<>();
        json("/assets/riftfall/animmodels/animations/biped/goblin_burrow.json").getAsJsonArray("animation")
            .forEach(v->channels.put(v.getAsJsonObject().get("name").getAsString(),v.getAsJsonObject()));
        var identity=new double[][]{{1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}};
        Map<String,double[][]> rest=new HashMap<>();bind(root,identity,rest);
        var mesh=json("/assets/riftfall/animmodels/entity/goblin.json").getAsJsonObject("vertices");
        var positions=mesh.getAsJsonObject("positions").getAsJsonArray("array");
        var counts=mesh.getAsJsonObject("vcounts").getAsJsonArray("array");
        var indices=mesh.getAsJsonObject("vindices").getAsJsonArray("array");
        var weights=mesh.getAsJsonObject("weights").getAsJsonArray("array");
        var times=channels.get("Root").getAsJsonArray("time");int protectedFrames=0;
        for(int frame=0;frame<times.size();frame++) {
            double progress=times.get(frame).getAsDouble()/2.5;
            if(!org.Azkerash.riftfall.behavior.BurrowExposure.fullyBuried(progress))continue;
            Map<String,double[][]> pose=new HashMap<>();walk(root,identity,channels,frame,pose);int influence=0;
            for(int vertex=0;vertex<counts.size();vertex++) {
                double height=0;
                for(int j=0;j<counts.get(vertex).getAsInt();j++) {
                    String joint=names.get(indices.get(influence++).getAsInt()).getAsString();
                    double weight=weights.get(indices.get(influence++).getAsInt()).getAsDouble();
                    var transform=multiply(pose.get(joint),inverseRigid(rest.get(joint)));
                    height+=weight*point(transform,positions.get(vertex*3).getAsDouble(),positions.get(vertex*3+1).getAsDouble(),positions.get(vertex*3+2).getAsDouble())[2];
                }
                assertTrue(height<org.Azkerash.riftfall.behavior.BurrowExposure.offset((float)progress),"Protected vertex still above the ground");
            }
            protectedFrames++;
        }
        assertTrue(protectedFrames>=5);
    }
    @Test void diggingPlantsBothKneesAndUsesAFreeHandAndDownwardKnifeInFrontOfTheBody() throws Exception {
        var root=json("/assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
        var animation=json("/assets/riftfall/animmodels/animations/biped/goblin_burrow.json").getAsJsonArray("animation");
        Map<String,JsonObject> channels=new HashMap<>();animation.forEach(v->channels.put(v.getAsJsonObject().get("name").getAsString(),v.getAsJsonObject()));
        var times=channels.get("Root").getAsJsonArray("time");assertEquals(2.5,times.get(times.size()-1).getAsDouble());
        double minForward=Double.POSITIVE_INFINITY,maxForward=Double.NEGATIVE_INFINITY;
        for(int i=0;i<times.size();i++){
            double t=times.get(i).getAsDouble();if(t<.45||t>1.5)continue;
            Map<String,double[][]> pose=new HashMap<>();walk(root,new double[][]{{1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}},channels,i,pose);
            for(String side:new String[]{"R","L"}){
                double knee=pose.get("Leg_"+side)[2][3]-.43;
                assertTrue(knee>-.02&&knee<.12,"Knee must stay planted, not in a standing squat: "+knee);
            }
            double[] hand=point(pose.get("Hand_L"),0,.272858,0);
            assertTrue(hand[2]-.43>-.03&&hand[2]-.43<.30,"Free hand must scoop at ground level");
            assertTrue(hand[1]>.60,"Hand must reach forward, not behind the goblin");
            minForward=Math.min(minForward,hand[1]);maxForward=Math.max(maxForward,hand[1]);
            double[][] tool=pose.get("Tool_R");double down=tool[2][1]*-Math.sin(Math.toRadians(10))+tool[2][2]*-Math.cos(Math.toRadians(10));
            assertTrue(down<-.75,"Knife must point into the soil");
        }
        assertTrue(maxForward-minForward>.15,"Digging strokes must have substantial travel");
    }
}
