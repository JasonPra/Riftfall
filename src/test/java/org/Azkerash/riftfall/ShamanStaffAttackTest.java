package org.Azkerash.riftfall;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShamanStaffAttackTest {
    @Test void staffDamagePreservesFullyBlockedHitsAndScalesAddonWeapons() {
        for (var attack:GoblinTactics.StaffAttack.values()) {
            var modifier=yesman.epicfight.api.utils.math.ValueModifier.calculator().attach(attack.damageModifier());
            assertEquals(0,modifier.getResult(0));
            assertEquals(attack.damage,modifier.getResult(5),.001F);
            assertEquals(attack.damage*2,modifier.getResult(10),.001F);
        }
    }
    private static JsonObject resource(String path) {
        var stream=ShamanStaffAttackTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream,path);
        try (var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException error) { throw new AssertionError(error); }
    }
    private static Matrix4f matrix(com.google.gson.JsonArray values) {
        float[] raw=new float[16]; for (int i=0;i<16;i++) raw[i]=values.get(i).getAsFloat();
        return new Matrix4f().set(raw).transpose();
    }
    private static Matrix4f joint(JsonObject node, Matrix4f parent, JsonObject clip,float time,String wanted) {
        String name=node.get("name").getAsString();
        Matrix4f local=matrix(node.getAsJsonArray("transform"));
        for (var entry:clip.getAsJsonArray("animation")) {
            var channel=entry.getAsJsonObject(); if (!channel.get("name").getAsString().equals(name)) continue;
            var times=channel.getAsJsonArray("time"); int key=-1;
            for (int i=0;i<times.size();i++) if (Math.abs(times.get(i).getAsFloat()-time)<.0001) key=i;
            assertTrue(key>=0); local=matrix(channel.getAsJsonArray("transform").get(key).getAsJsonArray()); break;
        }
        var world=new Matrix4f(parent).mul(local);
        if (name.equals(wanted)) return world;
        for (var child:node.getAsJsonArray("children")) {
            var found=joint(child.getAsJsonObject(),world,clip,time,wanted); if (found!=null) return found;
        }
        return null;
    }
    private static Matrix4f tool(String clip,float time,String hand) {
        var rig=resource("assets/epicfight/animmodels/entity/biped.json").getAsJsonObject("armature").getAsJsonArray("hierarchy").get(0).getAsJsonObject();
        return joint(rig,new Matrix4f(),resource("assets/riftfall/animmodels/animations/biped/"+clip+".json"),time,"Tool_"+hand);
    }
    @Test void bothAttacksTelegraphForOverASecondAndNeverIncreaseTheOldKnockback() {
        for (var attack:GoblinTactics.StaffAttack.values()) {
            assertTrue(attack.impact>=20);
            assertTrue(attack.duration>attack.impact+10);
            assertTrue(attack.knockback<=.9);
            String name=attack==GoblinTactics.StaffAttack.SWEEP ? "staff_sweep" : "staff_smash";
            var times=resource("assets/riftfall/animmodels/animations/biped/"+name+".json").getAsJsonArray("animation").get(0).getAsJsonObject().getAsJsonArray("time");
            assertEquals(attack.duration/20F,times.get(times.size()-1).getAsFloat(),.001);
            assertTrue(times.asList().stream().anyMatch(t->Math.abs(t.getAsFloat()-attack.impact/20F)<.001));
        }
        assertEquals(.9,GoblinTactics.StaffAttack.SWEEP.knockback);
    }
    @Test void overheadSmashKeepsTheSecondHandOnTheActualStaffShaft() {
        for (float time:new float[]{.35F,.85F,2.0F,2.15F,2.3F,2.7F}) {
            var secondaryGrip=tool("staff_smash",time,"R").transformPosition(new Vector3f(-.01F,-.021875F,.153125F));
            var palm=tool("staff_smash",time,"L").getTranslation(new Vector3f());
            assertTrue(palm.distance(secondaryGrip)<.015,"Secondary hand must grip the shaft at "+time);
        }
    }
    @Test void smashDropsTheStaffHeadAndSweepCrossesInFrontOfTheBody() {
        Vector3f top=new Vector3f(-.01F,-.021875F,-.983125F);
        float raised=tool("staff_smash",2.0F,"R").transformPosition(new Vector3f(top)).z;
        float impact=tool("staff_smash",2.15F,"R").transformPosition(new Vector3f(top)).z;
        float follow=tool("staff_smash",2.3F,"R").transformPosition(new Vector3f(top)).z;
        assertTrue(raised>impact+.5 && impact>follow+.5);
        assertTrue(Math.abs(follow)<.2,"Full follow-through reaches ground level");
        var start=tool("staff_sweep",2.0F,"R").transformPosition(new Vector3f(top));
        var end=tool("staff_sweep",2.1F,"R").transformPosition(new Vector3f(top));
        assertTrue(start.x>.7 && end.x<-.5);
        assertTrue(start.y>0 && end.y>0);
    }
    @Test void sweepWaitsTwoSecondsThenCrossesInOneTenthOfASecond() {
        assertEquals(42,GoblinTactics.StaffAttack.SWEEP.impact);
        var channels=resource("assets/riftfall/animmodels/animations/biped/staff_sweep.json").getAsJsonArray("animation");
        for (var entry:channels) {
            var channel=entry.getAsJsonObject();
            var times=channel.getAsJsonArray("time");
            assertEquals(2F,times.get(3).getAsFloat(),.001);
            assertEquals(.1F,times.get(4).getAsFloat()-times.get(3).getAsFloat(),.001);
            assertEquals(channel.getAsJsonArray("transform").get(2),channel.getAsJsonArray("transform").get(3),"Hold the warning pose before the fast swing");
        }
    }


}
