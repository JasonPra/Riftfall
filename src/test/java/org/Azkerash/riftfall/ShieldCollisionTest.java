package org.Azkerash.riftfall;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.Azkerash.riftfall.shield.*;
import org.junit.jupiter.api.Test;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import static org.junit.jupiter.api.Assertions.*;

class ShieldCollisionTest {
    private ShieldBox shield(){return new ShieldBox(new AABB(-.3,-.3,-.05,.3,.3,.05),OpenMatrix4f.createTranslation(0,1,-.4F));}
    @Test void blocksAPathThroughTheBoardButNotBesideAboveBelowOrBehindIt(){
        var shield=shield();var body=new AABB(-.25,0,-.25,.25,1.8,.25);
        assertTrue(shield.beforeBody(new Vec3(0,1,2),new Vec3(0,1,0),body,0));
        assertFalse(shield.beforeBody(new Vec3(1,1,2),new Vec3(1,1,0),body,0));
        assertFalse(shield.beforeBody(new Vec3(0,1.8,2),new Vec3(0,1.8,0),body,0));
        assertFalse(shield.beforeBody(new Vec3(0,.2,2),new Vec3(0,.2,0),body,0));
        assertFalse(shield.beforeBody(new Vec3(0,1,-2),new Vec3(0,1,.4),body,0));
    }
    @Test void epicFightWeaponBoxesMustActuallyTouchTheShield(){
        var shield=shield();
        var strike=new OBBCollider(.04,.04,.5,0,0,0);strike.transform(OpenMatrix4f.createTranslation(0,1,-.4F));
        assertTrue(strike.isCollide(shield.collider()));
        var low=new OBBCollider(.04,.04,.5,0,0,0);low.transform(OpenMatrix4f.createTranslation(0,.2F,-.4F));
        assertFalse(low.isCollide(shield.collider()));
    }
    @Test void planeWeaponsMustIntersectTheBoardRatherThanJustItsSurroundings(){
        var shield=shield();
        assertTrue(shield.triangle(new Vec3(-1,1,.4),new Vec3(1,1,.4),new Vec3(0,1.2,.4)));
        assertFalse(shield.triangle(new Vec3(-1,2,.4),new Vec3(1,2,.4),new Vec3(0,3,.4)));
    }
    @Test void derivesBucklerDimensionsAndKeepsThemSmallerThanTheVanillaShield(){
        var buckler=ShieldModels.elements(ShieldModels.resource("assets/riftfall/models/item/goblin_buckler.json"));
        assertNotNull(buckler);assertEquals(10D/16,buckler.getXsize(),1E-6);assertEquals(10D/16,buckler.getYsize(),1E-6);
        assertEquals(4D/16,buckler.getZsize(),1E-6);
        var padded=buckler.inflate(1D/16);assertEquals(12D/16,padded.getXsize(),1E-6);assertEquals(12D/16,padded.getYsize(),1E-6);
        var vanilla=ShieldModels.resource("data/minecraft/shield_colliders/shield.json");
        assertEquals(22,vanilla.getAsJsonArray("to").get(1).getAsInt()-vanilla.getAsJsonArray("from").get(1).getAsInt());
        assertTrue(padded.getYsize()<24D/16);
    }
    @Test void inheritsModelGeometryAndTransformsRatherThanAssumingOneSizeForEveryShield(){
        var parent=ResourceLocation.parse("test:item/base");var child=ResourceLocation.parse("test:item/tower");
        var models=Map.of(parent,JsonParser.parseString("{\"elements\":[{\"from\":[0,0,7],\"to\":[16,32,9]}],\"display\":{\"thirdperson_righthand\":{\"scale\":[1,1,1]}}}").getAsJsonObject(),
            child,JsonParser.parseString("{\"parent\":\"test:item/base\",\"display\":{\"thirdperson_lefthand\":{\"scale\":[.5,.5,.5]}}}").getAsJsonObject());
        var model=ShieldModels.inherit(child,models::get,new HashSet<>());var box=ShieldModels.elements(model);
        assertEquals(2,box.getYsize(),1E-6);assertTrue(model.getAsJsonObject("display").has("thirdperson_righthand"));assertTrue(model.getAsJsonObject("display").has("thirdperson_lefthand"));
    }
}
