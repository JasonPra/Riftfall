package org.Azkerash.riftfall;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BackstabAnimationTest {
    private JsonArray clip(String name){
        var stream=getClass().getResourceAsStream("/assets/riftfall/animmodels/animations/biped/"+name+".json");assertNotNull(stream);
        try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){return JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("animation");}
        catch(IOException e){throw new AssertionError(e);}
    }
    private JsonObject joint(String clip,String joint){for(var v:clip(clip))if(v.getAsJsonObject().get("name").getAsString().equals(joint))return v.getAsJsonObject();throw new AssertionError(joint);}
    private Matrix4f at(JsonObject channel,double t){
        var times=channel.getAsJsonArray("time");for(int i=0;i<times.size();i++)if(Math.abs(times.get(i).getAsDouble()-t)<1E-5){
            var values=channel.getAsJsonArray("transform").get(i).getAsJsonArray();float[] matrix=new float[16];for(int j=0;j<16;j++)matrix[j]=values.get(j).getAsFloat();return new Matrix4f().set(matrix).transpose();
        }throw new AssertionError("Missing time "+t);
    }
    @Test void victimDropsToKneesThenFallsAndReturnsToTheOriginalPose(){
        var root=joint("backstab_fall","Root");
        assertTrue(at(root,.68).m32()<at(root,0).m32()-.2F);
        assertTrue(at(root,1.45).m32()<at(root,.68).m32()-.02F);
        assertEquals(0,at(root,1.45).m12(),.001F); // Long body axis lies horizontally after faceplant.
        assertTrue(at(root,4.6).equals(at(root,0),.0001F));
        assertTrue(BackstabSequence.RECOVERY_TICKS>=Math.ceil(4.6*20)+2);
    }
    @Test void captureFlowsIntoFallWithoutAJointPosePop(){
        for(var value:clip("backstab_caught")){
            var channel=value.getAsJsonObject();var name=channel.get("name").getAsString();
            assertTrue(at(channel,1.8).equals(at(joint("backstab_fall",name),0),.0001F),name);
        }
    }
    private Matrix4f world(JsonObject node,Matrix4f parent,double time,String wanted){return world("backstab_fall",node,parent,time,wanted);}
    private Matrix4f world(String clip,JsonObject node,Matrix4f parent,double time,String wanted){
        var current=new Matrix4f(parent).mul(at(joint(clip,node.get("name").getAsString()),time));
        if(node.get("name").getAsString().equals(wanted))return current;
        for(var child:node.getAsJsonArray("children")){var found=world(clip,child.getAsJsonObject(),current,time,wanted);if(found!=null)return found;}
        return null;
    }
    @Test void kneesAndHandsActuallyReachTheGroundDuringFallAndRecovery(){
        try(var reader=new InputStreamReader(getClass().getResourceAsStream("/assets/epicfight/animmodels/entity/biped.json"),StandardCharsets.UTF_8)){
            var rig=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
            float knee=world(rig,new Matrix4f(),.68,"Leg_R").m32();assertTrue(knee>.04 && knee<.18);
            for(double time:new double[]{1.45,3.0})for(var hand:new String[]{"Tool_R","Tool_L"}){
                float height=world(rig,new Matrix4f(),time,hand).m32();assertTrue(height>.05 && height<.22,hand+" at "+time+": "+height);
            }
        }catch(IOException ex){throw new AssertionError(ex);}
    }
    @Test void greatswordSelectionUsesEpicFightCategoryRatherThanItemNames(){
        assertTrue(BackstabSequence.greatsword(yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories.GREATSWORD));
        assertFalse(BackstabSequence.greatsword(yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories.SWORD));
        assertFalse(BackstabSequence.greatsword(yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories.AXE));
    }
    @Test void greatswordBladePointsIntoTargetAndBothHandsFollowTheLongGrip() throws IOException {
        try(var reader=new InputStreamReader(getClass().getResourceAsStream("/assets/epicfight/animmodels/entity/biped.json"),StandardCharsets.UTF_8)){
            var rig=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
            // Actual Epic Fight item display: 45-degree diagonal texture, 2x scale, 11.25px translation.
            var item=new Matrix4f().translate(0,0,-.13F).rotateX(-(float)Math.PI/2).translate(0,11.25F/16,0)
                .rotateY(-(float)Math.PI/2).rotateZ((float)Math.PI/4).scale(2,2,1);
            float draw=0;
            for(double time:new double[]{.38,.58,.72}){
                var tool=world("greatsword_backstab",rig,new Matrix4f(),time,"Tool_R");
                var sword=new Matrix4f(tool).mul(item);
                var grip=sword.transformPosition(new org.joml.Vector3f(6.5F/32-.5F,1-26.5F/32-.5F,0));
                if(time==.38){draw=grip.y;continue;}
                var blade=sword.transformDirection(new org.joml.Vector3f(1,1,0)).normalize();
                assertTrue(blade.y>.99F && Math.abs(blade.z)<.02F,"blade must thrust forward at torso height");
                assertTrue(grip.y-draw>.6F,"full arm/body drive, not a wrist flick");
                var support=world("greatsword_backstab",rig,new Matrix4f(),time,"Tool_L").getTranslation(new org.joml.Vector3f());
                var rearGrip=new org.joml.Vector3f(grip).fma(-.2F,blade);
                assertTrue(rearGrip.distance(support)<.035F,"support fist must stay on the rear grip");
            }
        }
    }
    @Test void pairedImpaleLiftsOnlySlightlyThenTipsAndDropsBeforeRecovery() throws IOException {
        try(var reader=new InputStreamReader(getClass().getResourceAsStream("/assets/epicfight/animmodels/entity/biped.json"),StandardCharsets.UTF_8)){
            var rig=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
            for(double t:new double[]{0,.18,.38,.58,.72}){
                var tool=world("greatsword_backstab",rig,new Matrix4f(),t,"Tool_R");
                var forward=tool.transformDirection(new org.joml.Vector3f(0,0,-1)).normalize();
                assertTrue(forward.y>.99 && Math.abs(forward.z)<.02,"plunge must stay level, not chop downward");
            }
            var hilt=world("greatsword_backstab",rig,new Matrix4f(),.72,"Tool_R");
            var lift=world("greatsword_backstab",rig,new Matrix4f(),.9,"Tool_R");
            assertEquals(.045,lift.m32()-hilt.m32(),.003);
            var tip=world("greatsword_backstab",rig,new Matrix4f(),1.3,"Tool_R").transformDirection(new org.joml.Vector3f(0,0,-1)).normalize();
            assertTrue(tip.y>.85 && tip.z<-.4,"sword tips forward only after the lift");
            var victim=joint("greatsword_backstab_fall","Root");
            assertEquals(.045,at(victim,.38).m32()-at(victim,.18).m32(),.001);
            assertEquals(0,at(victim,1.8).m12(),.001);
            assertTrue(at(joint("greatsword_backstab_death","Root"),1.9).equals(at(victim,1.8),.0001F));
            assertTrue(BackstabSequence.GREATSWORD_ATTACK_TICKS>=Math.ceil(2.4*20)+2);
            assertTrue(BackstabSequence.GREATSWORD_RECOVERY_TICKS>=Math.ceil(5.0*20)+2);
            assertTrue(BackstabSequence.GREATSWORD_DEATH_TICKS>=Math.ceil(1.9*20)+2);
        }
    }
    @Test void clipsHaveFiniteRigidTransformsAndSmoothVictimKeySpacing(){
        for(var name:new String[]{"backstab_caught","backstab_fall","goblin_shield_backstab","greatsword_backstab","greatsword_backstab_fall","greatsword_backstab_death"})for(var value:clip(name)){
            var channel=value.getAsJsonObject();var times=channel.getAsJsonArray("time");var transforms=channel.getAsJsonArray("transform");assertEquals(times.size(),transforms.size());
            for(int i=0;i<times.size();i++){
                var time=times.get(i).getAsDouble();var matrix=at(channel,time);assertTrue(matrix.isFinite());assertEquals(1,matrix.determinant(),.001F);
                if(i>0){double dt=time-times.get(i-1).getAsDouble();assertTrue(dt>0 && dt<=.05001,name);}
            }
        }
    }
}
