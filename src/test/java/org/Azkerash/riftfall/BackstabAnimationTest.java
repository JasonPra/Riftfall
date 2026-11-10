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
            for(double time:new double[]{.38,.58,.76}){
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
    @Test void clipsHaveFiniteRigidTransformsAndSmoothVictimKeySpacing(){
        for(var name:new String[]{"backstab_caught","backstab_fall","goblin_shield_backstab","greatsword_backstab"})for(var value:clip(name)){
            var channel=value.getAsJsonObject();var times=channel.getAsJsonArray("time");var transforms=channel.getAsJsonArray("transform");assertEquals(times.size(),transforms.size());
            for(int i=0;i<times.size();i++){
                var time=times.get(i).getAsDouble();var matrix=at(channel,time);assertTrue(matrix.isFinite());assertEquals(1,matrix.determinant(),.001F);
                if(i>0){double dt=time-times.get(i-1).getAsDouble();assertTrue(dt>0 && dt<=.05001,name);}
            }
        }
    }
}
