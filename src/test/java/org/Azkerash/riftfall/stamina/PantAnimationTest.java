package org.Azkerash.riftfall.stamina;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PantAnimationTest {
    @Test void breathingLoopClosesWithoutJumpingAndAnimatesTorsoAndArms() throws IOException {
        var source=getClass().getResourceAsStream("/assets/riftfall/animmodels/animations/biped/goblin_pant.json");assertNotNull(source);
        try(var reader=new InputStreamReader(source,StandardCharsets.UTF_8)){
            var channels=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("animation");
            var changed=new java.util.HashSet<String>();
            for(var entry:channels){
                var channel=entry.getAsJsonObject();var times=channel.getAsJsonArray("time");var transforms=channel.getAsJsonArray("transform");
                assertEquals(0,times.get(0).getAsDouble());assertEquals(2,times.get(times.size()-1).getAsDouble());
                assertEquals(transforms.get(0),transforms.get(transforms.size()-1),channel.get("name").getAsString());
                if(!transforms.get(0).equals(transforms.get(10)))changed.add(channel.get("name").getAsString());
                for(var transform:transforms)for(var number:transform.getAsJsonArray())assertTrue(Double.isFinite(number.getAsDouble()));
            }
            assertTrue(changed.containsAll(java.util.Set.of("Root","Torso","Chest","Head","Arm_R","Arm_L")));
        }
    }
}
