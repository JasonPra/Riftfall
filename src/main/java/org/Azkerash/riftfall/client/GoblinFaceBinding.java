package org.Azkerash.riftfall.client;

import org.Azkerash.riftfall.facial.AtlasPanel;

/** Skin ca4692a: the six-row face was moved from (8,10) to (32,0), without changing its proportions. */
public final class GoblinFaceBinding {
    public static final int FACE_U=32,FACE_V=0;
    // Center the five visible mouth pixels, excluding the asymmetric transparent padding.
    public static final AtlasPanel MOUTH=new AtlasPanel(64,64,FACE_U+1,FACE_V+4,5,1,
        -2.75F,-2.125F,5.5F,1.0625F,-4.31F,true);
    public static final ArticulatedFaceRig.Layout LAYOUT=new ArticulatedFaceRig.Layout(2.2F,-4.78125F,-4.35F);
    public static final ArticulatedFaceRig.Style STYLE=new ArticulatedFaceRig.Style(1.3F,1F,1F,.42F,.12F,.16F,.12F,3.1F,.65F);
    // Epic Fight's skinned head uses different bounds from the vanilla head cuboid. These are derived
    // from goblin.json (head height .54m, front .265m) and the BIPED Head bind origin (1.513972m).
    public static final float EPIC_Y_SCALE=8.64F/8.5F;
    public static final float EPIC_Y_OFFSET=1.023552F;
    public static final float EPIC_Z_OFFSET=.075136F;
    private GoblinFaceBinding() {}
}
