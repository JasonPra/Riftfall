package org.Azkerash.riftfall.client;
import net.neoforged.neoforge.common.ModConfigSpec;
/** Local preferences only; no server camera setting is forced outside a backstab scene. */
public final class CameraConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue DISTANCE,SHOULDER,HEIGHT,RECOVERY;
    static{
        var b=new ModConfigSpec.Builder();ENABLED=b.define("enhancedThirdPerson",true);
        DISTANCE=b.defineInRange("distance",4.5D,1.0D,10.0D);SHOULDER=b.comment("Positive is right shoulder, negative is left.").defineInRange("shoulderOffset",.35D,-1.5D,1.5D);
        HEIGHT=b.defineInRange("heightOffset",.15D,-.5D,1.0D);RECOVERY=b.comment("Boom recovery rate after leaving a wall; rotation stays responsive.").defineInRange("recoverySpeed",10D,1D,30D);SPEC=b.build();
    }
    private CameraConfig(){}
}
