package org.Azkerash.riftfall;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Validate the rendered mesh's front, rather than assuming an export coordinate convention. */
class GoblinSwordDirectionTest {
    private static JsonObject resource(String path) {
        var stream=GoblinSwordDirectionTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream,path);
        try (var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) { throw new AssertionError(exception); }
    }
    private static Matrix4f matrix(com.google.gson.JsonArray values) {
        float[] raw=new float[16]; for (int i=0;i<16;i++) raw[i]=values.get(i).getAsFloat();
        return new Matrix4f().set(raw).transpose();
    }
    private static Matrix4f tool(JsonObject node,Matrix4f parent,JsonObject clip,float time) {
        return joint(node,parent,clip,time,"Tool_R");
    }
    private static Matrix4f joint(JsonObject node,Matrix4f parent,JsonObject clip,float time,String wanted) {
        String name=node.get("name").getAsString();
        Matrix4f local=matrix(node.getAsJsonArray("transform"));
        for (var value : clip.getAsJsonArray("animation")) {
            var channel=value.getAsJsonObject(); if (!channel.get("name").getAsString().equals(name)) continue;
            var times=channel.getAsJsonArray("time"); int key=-1;
            for (int i=0;i<times.size();i++) if (Math.abs(times.get(i).getAsFloat()-time)<.0001) key=i;
            assertTrue(key>=0,"Expected an authored keyframe at "+time);
            local=matrix(channel.getAsJsonArray("transform").get(key).getAsJsonArray()); break;
        }
        Matrix4f world=new Matrix4f(parent).mul(local);
        if (name.equals(wanted)) return world;
        for (var child : node.getAsJsonArray("children")) {
            var found=joint(child.getAsJsonObject(),world,clip,time,wanted); if (found!=null) return found;
        }
        return null;
    }
    private static Vector3f blade(String name,float time) {
        var rig=resource("assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
        var clip=resource("assets/riftfall/animmodels/animations/biped/"+name+".json");
        // Vanilla handheld sword's diagonal, including its display rotation and Epic Fight item correction.
        Vector3f axis=new Vector3f(0,-(float)Math.sin(Math.toRadians(10)),-(float)Math.cos(Math.toRadians(10)));
        return tool(rig,new Matrix4f(),clip,time).transformDirection(axis).normalize();
    }
    @Test void swordCutsFaceTheSameWayAsTheActualGoblinMesh() {
        var vertices=resource("assets/riftfall/animmodels/entity/goblin.json").getAsJsonObject("vertices").getAsJsonObject("positions").getAsJsonArray("array");
        float min=0,max=0;
        for (int i=1;i<vertices.size();i+=3) { min=Math.min(min,vertices.get(i).getAsFloat()); max=Math.max(max,vertices.get(i).getAsFloat()); }
        assertTrue(max>Math.abs(min),"The projecting nose establishes +Y as the mesh's front");
        assertTrue(blade("goblin_slash",.2F).x>.2F);
        assertTrue(blade("goblin_slash",.4F).x<-.2F);
        assertTrue(blade("goblin_backswing",.2F).x<-.2F);
        assertTrue(blade("goblin_backswing",.4F).x>.2F);
        for (var sample : java.util.List.of(new Object[]{"goblin_slash",.4F},new Object[]{"goblin_backswing",.4F},
                new Object[]{"goblin_flurry",.3F},new Object[]{"goblin_flurry",.85F},new Object[]{"goblin_flurry",1.4F}))
            assertTrue(blade((String)sample[0],(Float)sample[1]).y>0,"Cuts must sweep across the front, not behind the back");
    }
    @Test void cutsLeadWithTheEdgeRatherThanTheBroadFace() {
        var rig=resource("assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
        for (var sample : java.util.List.of(new Object[]{"goblin_slash",.4F},new Object[]{"goblin_backswing",.4F},
                new Object[]{"goblin_flurry",.3F},new Object[]{"goblin_flurry",.85F},new Object[]{"goblin_flurry",1.4F})) {
            var clip=resource("assets/riftfall/animmodels/animations/biped/"+sample[0]+".json");
            // A vanilla sword's broad-face normal becomes the tool's X axis after its item transforms.
            var face=tool(rig,new Matrix4f(),clip,(Float)sample[1]).transformDirection(new Vector3f(1,0,0)).normalize();
            assertTrue(Math.abs(face.z)>.95F,"The broad face must face up/down during a lateral cut, not into its sweep");
        }
    }
    @Test void basicCutsKeepTheShieldArmInGuardWithoutAFeint() {
        for (String name:new String[]{"goblin_slash","goblin_backswing"}) {
            var clip=resource("assets/riftfall/animmodels/animations/biped/"+name+".json");
            for (var entry:clip.getAsJsonArray("animation")) {
                var channel=entry.getAsJsonObject();
                if (!java.util.Set.of("Arm_L","Hand_L","Tool_L").contains(channel.get("name").getAsString())) continue;
                var frames=channel.getAsJsonArray("transform");
                for (var frame:frames) assertEquals(frames.get(0),frame,"Offhand must stay tucked during "+name);
            }
        }
    }
    @Test void twoHandedFlurryKeepsTheEmptyHandOnTheSwordHilt() {
        var rig=resource("assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
        var clip=resource("assets/riftfall/animmodels/animations/biped/goblin_twohand_flurry.json");
        for (var time:clip.getAsJsonArray("animation").get(0).getAsJsonObject().getAsJsonArray("time")) {
            float t=time.getAsFloat();
            var tool=tool(rig,new Matrix4f(),clip,t);
            var primary=tool.transformPosition(new Vector3f(0,.08995792F,-.04705085F));
            var primaryPalm=joint(rig,new Matrix4f(),clip,t,"Hand_R").transformPosition(new Vector3f(0,.272858F,0));
            assertTrue(primary.distance(primaryPalm)<.01F,"Primary fist must hold the hilt, not the blade: "+t);
            var secondary=new Vector3f(primary)
                .sub(blade("goblin_twohand_flurry",t).mul(.075F));
            var palm=joint(rig,new Matrix4f(),clip,t,"Tool_L").getTranslation(new Vector3f());
            assertTrue(palm.distance(secondary)<.02F,"Grip at "+t);
        }
        for (float t:new float[]{.3F,.55F,.8F,1.05F})
            assertTrue(blade("goblin_twohand_flurry",t).z<-.3F,"Each cut rakes downwards");
    }
    @Test void flurryRepeatsCrossedDescendingCutsInAFigureEight() {
        for (float[] pair:new float[][]{{.12F,.3F},{.37F,.55F},{.62F,.8F},{.87F,1.05F}}) {
            var wind=blade("goblin_twohand_flurry",pair[0]);
            var cut=blade("goblin_twohand_flurry",pair[1]);
            assertTrue(wind.x*cut.x<-.25F,"Cut crosses the body");
            assertTrue(wind.z>.3F && cut.z<-.3F,"Cut descends diagonally");
        }
        assertTrue(blade("goblin_twohand_flurry",.3F).x<0);
        assertTrue(blade("goblin_twohand_flurry",.55F).x>0);
        assertEquals(blade("goblin_twohand_flurry",.3F),blade("goblin_twohand_flurry",.8F));
        assertEquals(blade("goblin_twohand_flurry",.55F),blade("goblin_twohand_flurry",1.05F));
    }
    @Test void flurryLoopsWideAroundTheBodyAndReturnsToAnUprightGrip() {
        var rig=resource("assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
        var clip=resource("assets/riftfall/animmodels/animations/biped/goblin_twohand_flurry.json");
        float min=0,max=0;
        for (var time:clip.getAsJsonArray("animation").get(0).getAsJsonObject().getAsJsonArray("time")) {
            float t=time.getAsFloat();
            var tip=tool(rig,new Matrix4f(),clip,t).transformPosition(new Vector3f(0,.08995792F,-.04705085F))
                .add(blade("goblin_twohand_flurry",t).mul(.675F));
            min=Math.min(min,tip.x);max=Math.max(max,tip.x);
        }
        assertTrue(max-min>1.5F,"Full rendered blade must sweep broadly past both shoulders");
        assertTrue(blade("goblin_twohand_flurry",.335F).x<-.9F,"Loop outside the left cut");
        assertTrue(blade("goblin_twohand_flurry",.585F).x>.9F,"Loop outside the right cut");
        assertTrue(blade("goblin_twohand_flurry",0).z>.5F);
        assertTrue(blade("goblin_twohand_flurry",1.4F).z>.5F);
    }
    @Test void shieldOverheadRaisesThenCutsDownInFront() {
        var raised=blade("goblin_shield_overhead",.4F);
        var cut=blade("goblin_shield_overhead",.55F);
        assertTrue(raised.z>.6F && cut.z<-.5F);
        assertTrue(raised.y>.4F && cut.y>.5F);
    }
    @Test void everyBackstabPointsDownwardIntoThePlayerFacingSide() {
        for (float time : new float[]{1,1.8F,2.6F,3.4F}) {
            var direction=blade("goblin_grapple",time);
            assertTrue(direction.y>0,"Stab must point toward the attached player");
            assertTrue(direction.z<-.8F,"Stab must cut downward");
        }
    }
}
