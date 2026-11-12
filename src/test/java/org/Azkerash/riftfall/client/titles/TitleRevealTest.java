package org.Azkerash.riftfall.client.titles;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TitleRevealTest {
    @Test void sweepIsContinuousMonotonicAndCompleteBeforeChimeEnds() {
        assertEquals(0F, TitleReveal.progress(-1));
        assertEquals(1F, TitleReveal.progress(22));
        float previous = 0;
        for (float age = 0; age <= 22; age += .125F) {
            float p = TitleReveal.progress(age);
            assertTrue(p >= previous && p <= 1);
            assertTrue(p - previous < .01F);
            previous = p;
        }
    }
    @Test void starsRemainDeterministicAndExpireBeforeTitleFades() {
        for (int i = 0; i < TitleReveal.SPARKS; i++) {
            assertNull(TitleReveal.spark(i, -1, 123));
            assertNull(TitleReveal.spark(i, 50, 123));
            for (int age = 0; age < 50; age++) {
                var spark = TitleReveal.spark(i, age, 123);
                assertEquals(spark, TitleReveal.spark(i, age, 123));
                if (spark == null) continue;
                assertTrue(spark.progress() >= 0 && spark.progress() <= 1);
                assertTrue(spark.opacity() >= 0 && spark.opacity() <= 1);
                assertTrue(spark.size() > 0 && spark.size() <= 3.5F);
            }
        }
    }
}
