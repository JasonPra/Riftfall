package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.block.leaves.LeavesProperties;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictrees.tree.species.UndergroundRootsSpecies;
import com.dtteam.dynamictrees.worldgen.DynamicTreeGenerationContext;
import com.dtteam.dynamictrees.worldgen.JoCode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import org.Azkerash.riftfall.worldmana.geysers.GeyserLayout;

/** An ancient crown needs a larger leaf volume than DT's ordinary eight-block grove radius. */
public final class LegacyTreeSpecies extends UndergroundRootsSpecies {
    public static final int CROWN_RADIUS=26;
    public LegacyTreeSpecies(ResourceLocation name,Family family,LeavesProperties leaves) {super(name,family,leaves);}

    @Override public boolean generate(DynamicTreeGenerationContext context) {
        if(!context.isWorldGen())return super.generate(context);
        // The normal DT feature invokes the species. Choose one jittered anchor per 4x4-chunk
        // grove cell: adjacent crowns remain at least 48 blocks apart, rather than using the
        // small-tree Poisson spacing. Centering also fits each crown in vanilla's write region.
        int cx=Math.floorDiv(context.originPos().getX(),16),cz=Math.floorDiv(context.originPos().getZ(),16);
        int gx=Math.floorDiv(cx,4),gz=Math.floorDiv(cz,4);
        long hash=GeyserLayout.mix(((net.minecraft.server.level.ServerLevel)context.levelContext().level()).getSeed()^gx*731L^gz*997L);
        if(Math.floorMod(cx,4)!=1+(int)(hash&1)||Math.floorMod(cz,4)!=1+(int)((hash>>>1)&1))return false;
        int x=cx*16+8,z=cz*16+8;
        int y=context.level().getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;
        var root=new BlockPos(x,y,z);
        if(!context.level().getUncachedNoiseBiome(x>>2,y>>2,z>>2).is(
            org.Azkerash.riftfall.worldmana.geysers.GeyserBiomes.key(org.Azkerash.riftfall.worldmana.geysers.GeyserZone.CORE))
            ||!isAcceptableSoil(context.level().getBlockState(root)))return false;
        context.rootPos().set(root);
        return super.generate(context);
    }

    @Override public JoCode getJoCode(String code) {
        return new JoCode(code) {
            @Override public void generate(DynamicTreeGenerationContext context) {
                var expanded=new DynamicTreeGenerationContext(context.levelContext(),context.species(),
                    context.originPos(),context.rootPos(),context.biome(),context.facing(),context.radius(),context.isWorldGen()) {
                    @Override public int radius(){return CROWN_RADIUS;}
                    @Override public boolean secondChanceRegen(){return context.secondChanceRegen();}
                    @Override public void secondChance(){context.secondChance();}
                };
                new CrownCode(toString(),context.rootPos().immutable(),context.isWorldGen()).setCareful(careful).generate(expanded);
            }
        };
    }

    /** Per-generation bounds: tree templates are shared by parallel chunk workers. */
    private static final class CrownCode extends JoCode {
        private final BlockPos root;
        private final boolean worldGen;
        CrownCode(String code,BlockPos root,boolean worldGen){super(code);this.root=root;this.worldGen=worldGen;}
        private boolean inside(BlockPos pos){return Math.abs(pos.getX()-root.getX())<=23&&Math.abs(pos.getZ()-root.getZ())<=23;}
        @Override protected boolean isFreeToSetBlock(LevelAccessor level,BlockPos pos,com.dtteam.dynamictrees.tree.species.Species species){
            return (!worldGen||inside(pos))&&super.isFreeToSetBlock(level,pos,species);
        }
        @Override protected void generateAndAgeLeaves(DynamicTreeGenerationContext context,com.dtteam.dynamictrees.api.voxmap.SimpleVoxmap leaves,boolean generation){
            var profile=LegacyGrownProfiles.find(toString());
            if(!profile.isEmpty()){
                // JoCodes store paths but omit radii. Restore the actual grown branch
                // states before foliage is evaluated, rather than inventing a second taper.
                leaves.fill((byte)0);
                for(var node:profile){
                    var pos=node.position(root,context.facing());var state=context.level().getBlockState(pos);
                    if(state.getBlock() instanceof com.dtteam.dynamictrees.block.branch.BranchBlock branch&&branch.getFamily()==context.species().getFamily())
                        branch.setRadius(context.level(),pos,node.radius(),null,2);
                }
                var cluster=context.species().getLeavesProperties().getCellKit().getLeafCluster();
                for(var node:profile){
                    var pos=node.position(root,context.facing());var state=context.level().getBlockState(pos);
                    if(state.getBlock() instanceof com.dtteam.dynamictrees.block.branch.BranchBlock branch&&branch.getFamily()==context.species().getFamily()&&branch.getRadius(state)==1)
                        leaves.blitMax(pos,cluster);
                }
                for(var node:profile){
                    var pos=node.position(root,context.facing());var state=context.level().getBlockState(pos);
                    if(state.getBlock() instanceof com.dtteam.dynamictrees.block.branch.BranchBlock branch&&branch.getFamily()==context.species().getFamily())
                        leaves.setVoxel(pos,(byte)(branch.getRadius(state)==1?16:32));
                }
            }
            // DT's ageing pass can grow a leaf one block beyond the initial map. Leave that
            // margin inside vanilla's 3x3 FEATURES write area instead of issuing far-chunk writes.
            if(generation)for(var cell:leaves.getAllNonZeroCells())
                if(Math.abs(cell.getPos().getX()-root.getX())>22||Math.abs(cell.getPos().getZ()-root.getZ())>22)
                    leaves.setVoxel(cell.getPos(),(byte)0);
            super.generateAndAgeLeaves(context,leaves,generation);
        }
    }
}
