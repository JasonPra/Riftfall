package org.Azkerash.riftfall.crystals;

import java.util.*;

public enum CrystalTier {
    RED("Red",0xB94D57),ORANGE("Orange",0xCB8450),YELLOW("Yellow",0xD0BB67),GREEN("Green",0x619A72),
    CYAN("Cyan",0x65A8B4),BLUE("Blue",0x526FAE),VIOLET("Violet",0x8A64AD),WHITE("White",0xD8DFE8);
    private final String title;private final int color;
    CrystalTier(String title,int color){this.title=title;this.color=color;}
    public String id(){return name().toLowerCase(Locale.ROOT);}
    public String title(){return title;}
    public int color(){return color;}
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
