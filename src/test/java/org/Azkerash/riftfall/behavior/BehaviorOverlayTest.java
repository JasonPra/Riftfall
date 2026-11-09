package org.Azkerash.riftfall.behavior;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BehaviorOverlayTest {
    @Test void displayExplainsNegativeRawConfidenceAndIndividualContributions() {
        var data=new CompoundTag();data.putString("Action","watch");data.putDouble("Points",-10);data.putDouble("Confidence",0);
        var details=new CompoundTag();details.putDouble("EngagePoints",12);data.put("ConfidenceDetails",details);
        var contributions=new CompoundTag();contributions.putDouble("factor.combat.armor_advantage",-15);contributions.putDouble("allies.riftfall:goblin",10);data.put("Contributions",contributions);
        var text=String.join("\n",BehaviorOverlay.lines("Goblin",data));
        assertTrue(text.contains("watch"));assertTrue(text.contains("-10.0 pts"));assertTrue(text.contains("advance 12.0"));
        assertTrue(text.contains("combat.armor_advantage -15.0"));assertTrue(text.contains("allies.riftfall:goblin +10.0"));
    }
    @Test void oversizedPayloadsAreRejectedBeforeSending() {
        assertThrows(IllegalArgumentException.class,()->new BehaviorOverlayPayload.Entry(1,java.util.UUID.randomUUID(),java.util.List.of("x".repeat(257))));
        assertThrows(IllegalArgumentException.class,()->new BehaviorOverlayPayload(java.util.Collections.nCopies(33,new BehaviorOverlayPayload.Entry(1,java.util.UUID.randomUUID(),java.util.List.of()))));
    }
    @Test void debugFramesStayBoundedForLargeCustomProfiles() {
        var data=new CompoundTag();var contributions=new CompoundTag();
        for (int i=0;i<100;i++) contributions.putDouble("custom."+i,1);data.put("Contributions",contributions);
        var lines=BehaviorOverlay.lines("x".repeat(300),data);
        assertEquals(48,lines.size());assertTrue(lines.stream().allMatch(line->line.length()<=256));
    }
}
