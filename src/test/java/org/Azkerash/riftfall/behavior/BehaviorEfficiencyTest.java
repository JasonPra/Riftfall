package org.Azkerash.riftfall.behavior;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BehaviorEfficiencyTest {
    @Test void fastSelectionMatchesExplainedSelectionAcrossRandomSituationsAndIncumbents(){
        var random=new Random(12345);var rules=new ArrayList<BehaviorRule>();
        for(int i=0;i<30;i++){String ready="ready."+i;rules.add(new BehaviorRule("action"+i,i*3-20,Map.of("confidence",random.nextDouble()*100-50,"fear",random.nextDouble()*100-50,"health",20D),c->c.active(ready)));}
        var engine=new BehaviorEngine(rules,2);
        for(int n=0;n<10000;n++){
            var values=new HashMap<String,Double>();values.put("confidence",random.nextDouble());values.put("fear",random.nextDouble());values.put("health",random.nextDouble());
            for(int i=0;i<30;i++)values.put("ready."+i,random.nextBoolean()?1D:0D);
            var context=new BehaviorContext(values);String current=n%4==0?null:"action"+random.nextInt(30);
            assertEquals(engine.select(context,current).selected().orElse(null),engine.selectAction(context,current));
        }
    }
    @Test void compensatedScoreMatchesPreviousStreamSummation(){
        var weights=new LinkedHashMap<String,Double>();weights.put("a",1D);weights.put("b",1D);weights.put("c",1D);
        var rule=new BehaviorRule("test",3,weights,c->true);var random=new Random(12);
        for(int i=0;i<10000;i++){
            var ctx=new BehaviorContext(Map.of("a",random.nextDouble()*1e16,"b",-random.nextDouble()*1e16,"c",random.nextDouble()));
            double old=rule.base()+rule.weights().entrySet().stream().mapToDouble(e->e.getValue()*ctx.get(e.getKey())).sum();
            assertEquals(Double.doubleToLongBits(old),Double.doubleToLongBits(rule.score(ctx)));
        }
    }
    @Test void sensingWorkIsEvenlySpreadWithoutDelayingPastItsInterval(){
        for(int interval:new int[]{5,10,20})for(long now:new long[]{0,1,19,2000}){
            var buckets=new int[interval];
            for(int id=0;id<interval*10;id++){long next=SensingSchedule.next(now,interval,id);assertTrue(next>now&&next<=now+interval);buckets[(int)(next-now-1)]++;}
            for(int count:buckets)assertEquals(10,count);
        }
    }
    @Test void contextStillCopiesInputsAndRejectsNonFiniteValues(){
        var values=new HashMap<String,Double>();values.put("health",1D);var context=new BehaviorContext(values);values.put("health",0D);
        assertEquals(1,context.get("health"));assertEquals(0,context.get("missing"));
        assertThrows(IllegalArgumentException.class,()->new BehaviorContext(Map.of("health",Double.NaN)));
    }
}
