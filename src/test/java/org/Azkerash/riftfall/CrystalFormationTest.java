package org.Azkerash.riftfall;

import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalFormationTest {
    @Test void visiblePointsMatchHarvestedCrystalCountAtEveryStage() {
        for (int count=1;count<=5;count++) for (int variant=0;variant<3;variant++) {
            var crystals=CrystalFormation.crystals(count,variant);
            assertEquals(count,crystals.size());
            for (var prism : crystals) {
                assertTrue(prism.tip().y>prism.root().y);
                assertTrue(prism.root().distanceTo(crystals.getFirst().root())<3,"Daughter crystals grow from the shared stem");
            }
        }
    }
    @Test void allAttachmentDirectionsKeepMineralInsideItsBlock() {
        for (int count=1;count<=5;count++) for (int variant=0;variant<3;variant++)
            for (var prism : CrystalFormation.crystals(count,variant)) for (Direction facing : Direction.values()) {
                inside(CrystalFormation.orient(prism.tip(),facing));
                for (int i=0;i<6;i++) {
                    inside(CrystalFormation.orient(prism.vertex(i,false),facing));
                    inside(CrystalFormation.orient(prism.vertex(i,true),facing));
                }
            }
    }
    @Test void neighboringBedsMeetAtIdenticalEdgesWithoutInternalFacesOrRails() {
        for (int mask=0;mask<16;mask++) {
            var bed=CrystalFormation.bed(mask);
            if ((mask&1)!=0) assertEquals(Set.of(new Vec3(5,.15,0),new Vec3(11,.15,0)),bed.stream().filter(p->p.z==0).collect(Collectors.toSet()));
            if ((mask&2)!=0) assertEquals(Set.of(new Vec3(16,.15,5),new Vec3(16,.15,11)),bed.stream().filter(p->p.x==16).collect(Collectors.toSet()));
            if ((mask&4)!=0) assertEquals(Set.of(new Vec3(5,.15,0),new Vec3(11,.15,0)),bed.stream().filter(p->p.z==16).map(p->p.add(0,0,-16)).collect(Collectors.toSet()));
            if ((mask&8)!=0) assertEquals(Set.of(new Vec3(16,.15,5),new Vec3(16,.15,11)),bed.stream().filter(p->p.x==0).map(p->p.add(16,0,0)).collect(Collectors.toSet()));
            bed.forEach(CrystalFormationTest::inside);
        }
        assertTrue(CrystalFormation.bed(0).stream().allMatch(p->p.x>0 && p.x<16 && p.z>0 && p.z<16));
    }
    @Test void matureFormationsHaveVariedHeightAndSilhouette() {
        assertNotEquals(CrystalFormation.crystals(5,0),CrystalFormation.crystals(5,1));
        assertNotEquals(CrystalFormation.crystals(5,0).getFirst().tip().y,CrystalFormation.crystals(5,2).getFirst().tip().y);
    }
    private static void inside(Vec3 p) {
        assertTrue(p.x>=0 && p.y>=0 && p.z>=0 && p.x<=16 && p.y<=16 && p.z<=16,p.toString());
    }
}
