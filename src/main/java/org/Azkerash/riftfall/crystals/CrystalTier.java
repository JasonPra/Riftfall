package org.Azkerash.riftfall.crystals;

import java.util.*;

public enum CrystalTier {
    RED("Red",0xB94D57,0xFF4764),ORANGE("Orange",0xCB8450,0xFF953F),YELLOW("Yellow",0xD0BB67,0xFFE35C),GREEN("Green",0x619A72,0x68F18C),
    CYAN("Cyan",0x65A8B4,0x5CE9FF),BLUE("Blue",0x526FAE,0x6C9EFF),VIOLET("Violet",0x8A64AD,0xC28AFF),WHITE("White",0xD8DFE8,0xF0FCFF);
    private final String title;private final int color,glowColor;
    CrystalTier(String title,int color,int glowColor){this.title=title;this.color=color;this.glowColor=glowColor;}
    public String id(){return name().toLowerCase(Locale.ROOT);}
    public String title(){return title;}
    public int color(){return color;}
    public int glowColor(){return glowColor;}
    public Optional<CrystalTier> next(){return ordinal()<7?Optional.of(values()[ordinal()+1]):Optional.empty();}
    public boolean canRefine(int layers){return layers==8&&next().isPresent();}
    public CrystalTier seedTier(double draw){
        if(!Double.isFinite(draw)||draw<0||draw>=1)throw new IllegalArgumentException("Invalid seed draw");
        int drop=draw<.70?1:draw<.95?2:3;return values()[Math.max(0,ordinal()-drop)];
    }
    /** Fraction inward from the field's stable outer fringe; draw is a seeded value in [0,1). */
    public static CrystalTier natural(double inward,double power,double draw,boolean white){
        if(!Double.isFinite(inward)||!Double.isFinite(power)||!Double.isFinite(draw)||draw<0||draw>=1||power<=0)throw new IllegalArgumentException("Invalid crystal tier selection");
        if(inward<.34)return draw<.70?RED:draw<.95?ORANGE:YELLOW;
        if(inward<.70)return draw<.35?YELLOW:draw<.85?GREEN:CYAN;
        double boost=Math.min(2,Math.max(1,power));
        if(white&&draw<.0002*boost)return WHITE;
        if(draw<.01*boost)return VIOLET;
        if(draw<.08*boost)return BLUE;
        return draw<.60?CYAN:GREEN;
    }
}
