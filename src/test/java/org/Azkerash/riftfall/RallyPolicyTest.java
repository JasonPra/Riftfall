package org.Azkerash.riftfall;

import static org.junit.jupiter.api.Assertions.*;

import org.Azkerash.riftfall.confidence.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

class RallyPolicyTest {
    @Test
    void actsBeforePanicAndRespectsSpeciesThreshold() {
        assertTrue(RallyPolicy.atRisk(0, -15));
        assertFalse(RallyPolicy.atRisk(.01, -15));
        assertTrue(RallyPolicy.atRisk(-30, -15));
        assertFalse(RallyPolicy.atRisk(0, -40));
    }

    @Test
    void choosesGroupOnlyWhenNeededAndAffordable() {
        assertEquals(0, RallyPolicy.level(0, 160, 80, 140, 2));
        assertEquals(0, RallyPolicy.level(2, 79, 80, 140, 2));
        assertEquals(1, RallyPolicy.level(1, 160, 80, 140, 2));
        assertEquals(1, RallyPolicy.level(3, 100, 80, 140, 2));
        assertEquals(2, RallyPolicy.level(2, 140, 80, 140, 2));
        assertEquals(1, RallyPolicy.level(3, 160, 80, 140, 1));
    }

    @Test
    void ralliedConfidenceIsVisibleInTheNormalContributionBreakdown() {
        var config = ConfidenceConfig.defaults();
        var plain = ConfidenceEngine.evaluate(config.profile(), Map.of(), Map.of("base", 1D));
        var rallied =
                ConfidenceEngine.evaluate(
                        config.profile(), Map.of(), Map.of("base", 1D, "self.rallied", 1D));
        assertEquals(40, rallied.points() - plain.points());
        assertEquals(40D, rallied.contributions().get("factor.self.rallied"));
        assertEquals(
                0D,
                ConfidenceConfig.withStatusDefaults(Map.of("self.rallied", 0D))
                        .get("self.rallied"));
    }
}
