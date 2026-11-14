package org.Azkerash.riftfall.wood;
import org.Azkerash.riftfall.worldmana.geysers.GeyserLayout;
/** Mature canopy anchors plus younger trees in the intervening understory. */
final class HeartwoodGroveLayout {
 static String stage(long seed,int cx,int cz){
  long local=GeyserLayout.mix(seed^cx*0x632be59bd9b4e019L^cz*0x9e3779b97f4a7c15L);
  // Diagonally staggered adults overlap their crowns; rare omissions leave small clearings.
  if(Math.floorMod(cx+cz,2)==(int)((seed>>>4)&1)&&(local&15)!=15)return "adult";
  return (local&3)==0?((local&4)==0?"young":"intermediate"):"none";
 }
 static long sampleHash(long seed,int cx,int cz){return GeyserLayout.mix(seed^cx*731L^cz*997L^0x41474553L);}
 private HeartwoodGroveLayout(){}
}
