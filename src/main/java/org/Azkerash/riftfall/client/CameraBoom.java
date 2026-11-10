package org.Azkerash.riftfall.client;
/** Frame-rate-independent distance smoothing: collision retracts immediately, never through a wall. */
public final class CameraBoom {
    private double length=Double.NaN;
    public double advance(double allowed,double dt,double speed){
        allowed=Math.max(0,allowed);
        if(Double.isNaN(length)||allowed<length)length=allowed;
        else length+= (allowed-length)*(1-Math.exp(-speed*Math.clamp(dt,0,.1)));
        return length;
    }
    public void reset(){length=Double.NaN;}
}
