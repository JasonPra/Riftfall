package org.Azkerash.riftfall.confidence;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConfidenceTuningCacheTest {
    @Test void cachedProfilesRespectBaseIdentityAndReloadedOverrides()throws Exception{
        var field=ConfidenceConfig.class.getDeclaredField("overrides");field.setAccessible(true);Object original=field.get(null);
        try{
            field.set(null,Map.of("test:actor",Map.of("boldPoints",12D)));
            var base=ConfidenceConfig.settings();var first=ConfidenceConfig.forEntity("test:actor",base);
            assertSame(first,ConfidenceConfig.forEntity("test:actor",base));assertEquals(12,first.boldPoints());
            field.set(null,Map.of("test:actor",Map.of("boldPoints",25D)));
            var changed=ConfidenceConfig.forEntity("test:actor",base);assertNotSame(first,changed);assertEquals(25,changed.boldPoints());
            var newBase=new ConfidenceConfig.Settings(base.profile(),base.boldPoints(),base.woundedFraction(),base.vulnerableFraction(),base.retreatingMultiplier());
            assertNotSame(changed,ConfidenceConfig.forEntity("test:actor",newBase));
        }finally{field.set(null,original);}
    }
}
