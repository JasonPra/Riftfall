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
        if (name.equals("Tool_R")) return world;
        for (var child : node.getAsJsonArray("children")) {
            var found=tool(child.getAsJsonObject(),world,clip,time); if (found!=null) return found;
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
    @Test void everyBackstabPointsDownwardIntoThePlayerFacingSide() {
        for (float time : new float[]{1,1.8F,2.6F,3.4F}) {
            var direction=blade("goblin_grapple",time);
            assertTrue(direction.y>0,"Stab must point toward the attached player");
            assertTrue(direction.z<-.8F,"Stab must cut downward");
        }
    }
}
