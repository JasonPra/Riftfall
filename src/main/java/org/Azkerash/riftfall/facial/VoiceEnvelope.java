package org.Azkerash.riftfall.facial;

/** Reusable audio-amplitude curve; time is source seconds, so pitch changes animation speed too. */
public final class VoiceEnvelope {
    private final float duration;private final float[] samples;
    public VoiceEnvelope(float duration,float... samples){
        if(!Float.isFinite(duration)||duration<=0||samples.length<2)throw new IllegalArgumentException("Invalid envelope");
        this.duration=duration;this.samples=samples.clone();
        for(float value:samples)if(!Float.isFinite(value)||value<0||value>1)throw new IllegalArgumentException("Invalid amplitude");
    }
    public float duration(){return duration;}
    public float opening(double seconds){
        if(!Double.isFinite(seconds)||seconds<0||seconds>=duration)return 0;
        double position=seconds/duration*(samples.length-1);int i=(int)position;
        float value=samples[i]+(samples[Math.min(i+1,samples.length-1)]-samples[i])*(float)(position-i);
        return value*(float)Math.min(1,Math.min(seconds/.05,(duration-seconds)/.1));
    }
}
