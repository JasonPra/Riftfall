package org.Azkerash.riftfall.mana;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ManaBudgetTest {
    @Test void attackAndHealingShareOneBudgetAndCannotOverdraw(){
        var b=new ManaBudget();double mana=160;
        mana=b.spend(mana,30,40);mana=b.spend(mana,100,40);assertEquals(30,mana);
        assertFalse(b.canSpend(mana,31));assertEquals(30,b.spend(mana,31,40));assertEquals(40,b.delay());
    }
    @Test void regenWaitsThenHonorsEquipmentAdjustedRateAndCap(){
        var b=new ManaBudget();double mana=b.spend(160,100,40);
        for(int i=0;i<40;i++)mana=b.tick(mana,210,6,false);assertEquals(60,mana);
        for(int i=0;i<20;i++)mana=b.tick(mana,210,6,false);assertEquals(66,mana,1E-8);
        assertEquals(50,b.tick(mana,50,6,false));
        assertEquals(210,b.tick(209.9,210,6,false));
    }
    @Test void castingPausesRegenAndFailedSpendDoesNotRestartDelay(){
        var b=new ManaBudget();assertEquals(10,b.tick(10,100,4,true));
        assertEquals(10,b.spend(10,11,40));assertEquals(0,b.delay());
        assertFalse(b.canSpend(100,-1));assertFalse(b.canSpend(100,Double.NaN));
    }
    @Test void remainingDelaySurvivesReload(){var b=new ManaBudget();b.restoreDelay(19);assertEquals(10,b.tick(10,100,4,false));assertEquals(18,b.delay());}
}
