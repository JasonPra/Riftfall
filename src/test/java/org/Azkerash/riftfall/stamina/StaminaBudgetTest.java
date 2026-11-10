package org.Azkerash.riftfall.stamina;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StaminaBudgetTest {
    @Test void completeAnimationCoversAllItsPhasesButExpires(){
        var b=new StaminaBudget();b.animation(100,30);assertTrue(b.covered(100));assertTrue(b.covered(129));assertFalse(b.covered(130));assertFalse(b.skillCovered());
    }
    @Test void paidSkillCoversNestedAttackAndProjectileHooksAndAlwaysReleases(){
        var b=new StaminaBudget();b.beginSkill();b.beginSkill();assertTrue(b.covered(1000));b.endSkill();assertTrue(b.skillCovered());b.endSkill();assertFalse(b.covered(1000));b.endSkill();assertFalse(b.skillCovered());
    }
    @Test void multiProjectileVolleyAndDamageCallbacksOnlyPayOncePerTick(){
        var b=new StaminaBudget();assertFalse(b.sameContactTick(50));b.contact(50);assertTrue(b.sameContactTick(50));assertFalse(b.sameContactTick(51));
    }
    @Test void loadingRestoresResourceButClearsTransientPaymentWindows(){
        var s=new StaminaState();s.budget().beginSkill();s.budget().animation(100,300);s.budget().contact(100);
        s.restore(12,false,20,40);assertFalse(s.budget().covered(100));assertFalse(s.budget().sameContactTick(100));assertEquals(12,s.current(40));
    }
}
