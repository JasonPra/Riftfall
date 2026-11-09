package org.Azkerash.riftfall;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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
            var frames=channel.getAsJsonArray("transform");
            if (key>=0) local=matrix(frames.get(key).getAsJsonArray());
            else {
                int from=0;
                while (from+1<times.size()-1 && times.get(from+1).getAsFloat()<time) from++;
                float u=(time-times.get(from).getAsFloat())/(times.get(from+1).getAsFloat()-times.get(from).getAsFloat());
                var a=matrix(frames.get(from).getAsJsonArray());var b=matrix(frames.get(from+1).getAsJsonArray());
                local=new Matrix4f().translation(a.getTranslation(new Vector3f()).lerp(b.getTranslation(new Vector3f()),u))
                    .rotate(a.getUnnormalizedRotation(new Quaternionf()).slerp(b.getUnnormalizedRotation(new Quaternionf()),u))
                    .scale(a.getScale(new Vector3f()).lerp(b.getScale(new Vector3f()),u));
            }
            break;
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
    private static Vector3f frenzyTip(float time) {
        var rig=resource("assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
        var clip=resource("assets/riftfall/animmodels/animations/biped/goblin_frenzy.json");
        return tool(rig,new Matrix4f(),clip,time).transformPosition(new Vector3f(0,.08995792F,-.04705085F))
            .add(blade("goblin_frenzy",time).mul(.675F));
    }
    @Test void oneHandedFlurryMovesTheArmWithoutChangingTheBladePathOrDisplacingSockets() {
        var rig=resource("assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
        var clip=resource("assets/riftfall/animmodels/animations/biped/goblin_frenzy.json");
        float minX=10,maxX=-10,minZ=10,maxZ=-10;
        for (var time:clip.getAsJsonArray("animation").get(0).getAsJsonObject().getAsJsonArray("time")) {
            float t=time.getAsFloat();
            var primary=tool(rig,new Matrix4f(),clip,t).transformPosition(new Vector3f(0,.08995792F,-.04705085F));
            var primaryPalm=joint(rig,new Matrix4f(),clip,t,"Hand_R").transformPosition(new Vector3f(0,.272858F,0));
            assertTrue(primary.distance(primaryPalm)<.01F,"Hilt must remain in the sword hand");
            var freeHand=joint(rig,new Matrix4f(),clip,t,"Tool_L").getTranslation(new Vector3f());
            assertTrue(primary.distance(freeHand)>.35F,"Empty hand must not grip the sword");
            assertTrue(primaryPalm.z<1.7F,"Keep the arm below the old displaced overhead pose");
            minX=Math.min(minX,primaryPalm.x);maxX=Math.max(maxX,primaryPalm.x);
            minZ=Math.min(minZ,primaryPalm.z);maxZ=Math.max(maxZ,primaryPalm.z);
        }
        assertTrue(maxX-minX>.5F && maxZ-minZ>.5F,"The entire sword arm must sweep laterally and down");
        for (var entry:clip.getAsJsonArray("animation")) {
            var channel=entry.getAsJsonObject();String name=channel.get("name").getAsString();
            var transforms=channel.getAsJsonArray("transform");
            for (var frame:transforms) {
                if (name.equals("Tool_R")) {
                    var reference=matrix(transforms.get(0).getAsJsonArray());var current=matrix(frame.getAsJsonArray());
                    var axis=new Vector3f(0,-(float)Math.sin(Math.toRadians(10)),-(float)Math.cos(Math.toRadians(10)));
                    assertTrue(reference.transformDirection(new Vector3f(axis)).distance(current.transformDirection(new Vector3f(axis)))<.001F,
                        "Grip roll must not steer the blade away from the arm-driven path");
                    var grip=new Vector3f(0,.08995792F,-.04705085F);
                    assertTrue(reference.transformPosition(new Vector3f(grip)).distance(current.transformPosition(new Vector3f(grip)))<.001F,
                        "Roll must preserve the hilt in the fist");
                }
                if (java.util.Set.of("Shoulder_R","Shoulder_L").contains(name))
                    assertEquals(transforms.get(0),frame,"No independent weapon steering or raised clavicle: "+name);
                if (java.util.Set.of("Arm_R","Hand_R").contains(name))
                    assertEquals(matrix(transforms.get(0).getAsJsonArray()).getTranslation(new Vector3f()),
                        matrix(frame.getAsJsonArray()).getTranslation(new Vector3f()),"Arm bones must stay connected");
            }
        }
    }
    @Test void flurryRepeatsCrossedDescendingCutsInAFigureEight() {
        for (float[] pair:new float[][]{{.12F,.3F},{.37F,.55F},{.62F,.8F},{.87F,1.05F}}) {
            var wind=frenzyTip(pair[0]);var cut=frenzyTip(pair[1]);
            assertTrue(wind.x*cut.x<0,"Rendered blade crosses the body");
            assertTrue(wind.z-cut.z>.5F,"Arm-driven cut descends diagonally");
        }
        assertTrue(frenzyTip(.3F).distance(frenzyTip(.8F))<.08F);
        assertTrue(frenzyTip(.55F).distance(frenzyTip(1.05F))<.08F);
    }
    @Test void flurryLoopsWideAroundTheBodyAndReturnsToAnUprightGrip() {
        var rig=resource("assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
        var clip=resource("assets/riftfall/animmodels/animations/biped/goblin_frenzy.json");
        float min=0,max=0;
        for (var time:clip.getAsJsonArray("animation").get(0).getAsJsonObject().getAsJsonArray("time")) {
            float t=time.getAsFloat();
            var tip=tool(rig,new Matrix4f(),clip,t).transformPosition(new Vector3f(0,.08995792F,-.04705085F))
                .add(blade("goblin_frenzy",t).mul(.675F));
            min=Math.min(min,tip.x);max=Math.max(max,tip.x);
        }
        assertTrue(max-min>1.5F,"Full rendered blade must sweep broadly past both shoulders");
        assertTrue(blade("goblin_frenzy",.335F).x<-.7F,"Loop outside the left cut");
        assertTrue(blade("goblin_frenzy",.585F).x>.9F,"Loop outside the right cut");
        assertTrue(blade("goblin_frenzy",0).z>.5F);
        assertTrue(blade("goblin_frenzy",1.4F).z>.5F);
    }
    @Test void frenzyKeepsSteppingAndTransfersBodyWeightThroughEveryCut() {
        var clip=resource("assets/riftfall/animmodels/animations/biped/goblin_frenzy.json");
        for (String name:new String[]{"Thigh_R","Leg_R","Thigh_L","Leg_L","Root","Torso","Chest"}) {
            var channel=clip.getAsJsonArray("animation").asList().stream().map(v->v.getAsJsonObject())
                .filter(c->c.get("name").getAsString().equals(name)).findFirst().orElseThrow();
            var times=channel.getAsJsonArray("time");var frames=channel.getAsJsonArray("transform");
            for (float[] cut:new float[][]{{.18F,.26F},{.43F,.51F},{.68F,.76F},{.93F,1.01F}}) {
                int from=-1,to=-1;
                for (int i=0;i<times.size();i++) {
                    if (Math.abs(times.get(i).getAsFloat()-cut[0])<.001F) from=i;
                    if (Math.abs(times.get(i).getAsFloat()-cut[1])<.001F) to=i;
                }
                assertNotEquals(frames.get(from),frames.get(to),"No frozen lower body during "+name+" cut "+cut[0]);
            }
        }
        var coord=clip.getAsJsonArray("animation").asList().stream().map(v->v.getAsJsonObject())
            .filter(c->c.get("name").getAsString().equals("Coord")).findFirst().orElseThrow().getAsJsonArray("transform");
        assertEquals(.28F,matrix(coord.get(coord.size()-1).getAsJsonArray()).m31(),.001F);
    }
    @Test void outgoingFrenzyCutsKeepTheirExistingEdgeAlignment() {
        var rig=resource("assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
        var clip=resource("assets/riftfall/animmodels/animations/biped/goblin_frenzy.json");
        for (float start:new float[]{.18F,.68F}) {
            for (int i=1;i<16;i++) {
                float t=start+i*.005F;
                var direction=blade("goblin_frenzy",t);
                var travel=frenzyTip(t+.0005F).sub(frenzyTip(t-.0005F));
                travel.sub(new Vector3f(direction).mul(travel.dot(direction))).normalize();
                var face=tool(rig,new Matrix4f(),clip,t).transformDirection(new Vector3f(1,0,0)).normalize();
                assertTrue(Math.abs(face.dot(travel))<.08F,"Cut must not strike with its flat at "+t);
            }
        }
    }
    @Test void returningFrenzyCutsPresentTheEdgeToTheOpponentThroughoutTheStroke() {
        var rig=resource("assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
        var clip=resource("assets/riftfall/animmodels/animations/biped/goblin_frenzy.json");
        for (float start:new float[]{.37F,.87F}) {
            for (int i=1;i<36;i++) {
                float t=start+i*.005F;
                var direction=blade("goblin_frenzy",t);
                var towardOpponent=new Vector3f(0,1,0);
                towardOpponent.sub(new Vector3f(direction).mul(towardOpponent.dot(direction))).normalize();
                var face=tool(rig,new Matrix4f(),clip,t).transformDirection(new Vector3f(1,0,0)).normalize();
                assertTrue(Math.abs(face.dot(towardOpponent))<.08F,"Return cut must not present the flat toward the opponent at "+t);
            }
        }
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
