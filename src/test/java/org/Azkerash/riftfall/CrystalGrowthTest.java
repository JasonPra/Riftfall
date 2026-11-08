package org.Azkerash.riftfall;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalGrowthTest {
    @Test void ordinaryChunksNeedMoreThanOneHundredEssence() {
        assertFalse(CrystalGrowth.canFund(100, 600, 10));
        assertTrue(CrystalGrowth.canFund(101, 600, 10));
    }

    @Test void criticalMassQualifiesEvenWhenConfiguredBelowOneHundred() {
        assertTrue(CrystalGrowth.canFund(50, 50, 10));
        assertFalse(CrystalGrowth.canFund(49, 50, 10));
    }

    @Test void neitherGrowthNorSpreadingCanOverspend() {
        assertFalse(CrystalGrowth.canFund(110, 600, 120));
        assertTrue(CrystalGrowth.canFund(120, 600, 120));
        assertFalse(CrystalGrowth.canFund(0, 600, 10));
    }
}
