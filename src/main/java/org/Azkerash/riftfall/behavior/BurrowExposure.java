package org.Azkerash.riftfall.behavior;

/** Shared thresholds for the current crouch-and-sink animation, on both logical sides. */
public final class BurrowExposure {
    // Sinking starts at 65%: by 75% about half the kneeling body is concealed.
    public static boolean breaksLock(double progress){return progress>=.75;}
    // The final 10% is entirely below the ground, before the audio-wait stage begins.
    public static boolean fullyBuried(double progress){return progress>=.9F;}
    public static float offset(float progress) {
        return .43F*Math.clamp(progress*8,0,1)+1.8F*Math.clamp((progress-.65F)/.35F,0,1);
    }
    private BurrowExposure() {}
}
