package org.Azkerash.riftfall.worldmana.geysers;

/** Full-height region labels available to future zone-specific biome sets. */
public enum GeyserZone {
    OUTSIDE("Outside"),CORE("White core"),INNER("Inner ring"),MIDDLE("Middle ring"),OUTER("Outer fringe");
    private final String title;
    GeyserZone(String title){this.title=title;}
    public String title(){return title;}
}
