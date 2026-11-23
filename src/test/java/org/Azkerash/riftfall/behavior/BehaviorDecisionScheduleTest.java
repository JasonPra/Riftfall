package org.Azkerash.riftfall.behavior;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BehaviorDecisionScheduleTest {
    @Test
    void firstDecisionIsImmediateAndLaterDecisionsStayWithinTheirBudget() {
        for (int id = 0; id < 30; id++) {
            var clock = new BehaviorDecisionSchedule();
            assertTrue(clock.due(100));
            clock.completed(100, id, true);
            int first = 101;
            while (!clock.due(first)) {
                first++;
            }
            assertTrue(first <= 103);
            clock.completed(first, id, true);
            assertFalse(clock.due(first + 2));
            assertTrue(clock.due(first + 3));
            clock.completed(first + 3, id, false);
            int idle = first + 4;
            while (!clock.due(idle)) {
                idle++;
            }
            assertTrue(idle <= first + 13);
        }
    }

    @Test
    void steadyCombatAndIdleWorkIsStaggeredAcrossEntities() {
        for (boolean combat : new boolean[] {true, false}) {
            int interval = combat ? 3 : 10;
            int[] counts = new int[interval * 3 + 1];
            for (int id = 0; id < 300; id++) {
                var schedule = new BehaviorDecisionSchedule();
                schedule.completed(0, id, combat);
                for (int t = 1; t < counts.length; t++) {
                    if (schedule.due(t)) {
                        counts[t]++;
                        schedule.completed(t, id, combat);
                    }
                }
            }
            for (int t = 1; t < counts.length; t++) {
                assertEquals(300 / interval, counts[t]);
            }
        }
    }

    @Test
    void resetAndSkippedTicksDoNotLeaveTheBrainWaiting() {
        var clock = new BehaviorDecisionSchedule();
        clock.completed(100, 1, false);
        assertTrue(clock.due(1000));
        clock.completed(1000, 1, false);
        clock.reset();
        assertTrue(clock.due(1000));
    }

    @Test
    void intervalsAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> new BehaviorDecisionSchedule(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new BehaviorDecisionSchedule(4, 3));
    }
}
