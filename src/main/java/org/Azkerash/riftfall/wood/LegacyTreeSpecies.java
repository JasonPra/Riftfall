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
        // Closed-canopy adult anchors retain their population; young trees fill between them.
        var chunk=context.level() instanceof net.minecraft.server.level.WorldGenRegion region?region.getCenter():new net.minecraft.world.level.ChunkPos(context.originPos());
        int cx=chunk.x,cz=chunk.z;
        long seed=((net.minecraft.server.level.ServerLevel)context.levelContext().level()).getSeed();
        if(HeartwoodGroveLayout.stage(seed,cx,cz).equals("none"))return false;
        int x=cx*16+8,z=cz*16+8;
        int top=context.level().getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z)-1;
        BlockPos root=null;
        // Existing crowns and underbrush must not be mistaken for the terrain surface.
        for(int y=top;y>=Math.max(context.level().getMinBuildHeight(),top-64);y--){
            var p=new BlockPos(x,y,z);var state=context.level().getBlockState(p);
            if(com.dtteam.dynamictrees.tree.TreeHelper.isRooty(state))return false;
            if(isAcceptableSoil(state)&&context.level().getFluidState(p.above()).isEmpty()){root=p;break;}
            if(!(state.isAir()||state.canBeReplaced()||state.is(net.minecraft.tags.BlockTags.REPLACEABLE_BY_TREES)||state.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock||state.getBlock() instanceof LegacyBranchBlock))return false;
        }
        if(root==null||!context.level().getUncachedNoiseBiome(x>>2,root.getY()>>2,z>>2).is(
            org.Azkerash.riftfall.worldmana.geysers.GeyserBiomes.key(org.Azkerash.riftfall.worldmana.geysers.GeyserZone.CORE)))return false;
        context.rootPos().set(root);
        var sample=LegacyGrownProfiles.choose(HeartwoodGroveLayout.stage(seed,cx,cz),HeartwoodGroveLayout.sampleHash(seed,cx,cz));
        if(sample==null)return false;
        // Native JoCode obstruction checks follow the captured stage's actual height.
        // The generic species entry point demands a 50-block clear column even for seedlings.
        getJoCode(sample.code()).setCareful(true).generate(context);
        if(!(context.level().getBlockState(root.above()).getBlock() instanceof LegacyBranchBlock))return false;
        if(!sample.roots().isBlank())getRootsJoCode(sample.roots()).setCareful(true).generate(context);
        return true;
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
                new CrownCode(selected(context,toString(),false),context.rootPos().immutable(),context.isWorldGen()).setCareful(careful).generate(expanded);
            }
        };
    }

    private String selected(DynamicTreeGenerationContext context,String fallback,boolean roots){
        if(!context.isWorldGen())return fallback;
        int cx=Math.floorDiv(context.rootPos().getX(),16),cz=Math.floorDiv(context.rootPos().getZ(),16);
        long seed=((net.minecraft.server.level.ServerLevel)context.levelContext().level()).getSeed();
        var sample=LegacyGrownProfiles.choose(HeartwoodGroveLayout.stage(seed,cx,cz),HeartwoodGroveLayout.sampleHash(seed,cx,cz));
        return sample==null?fallback:roots?sample.roots():sample.code();
    }
    @Override public com.dtteam.dynamictrees.worldgen.RootsJoCode getRootsJoCode(String code){
        return new com.dtteam.dynamictrees.worldgen.RootsJoCode(code){
            @Override public void generate(DynamicTreeGenerationContext context){
                String chosen=selected(context,toString(),true);
                if(!chosen.isBlank())new com.dtteam.dynamictrees.worldgen.RootsJoCode(chosen).setCareful(careful).generate(context);
            }
        };
    }

    /** Per-generation bounds: tree templates are shared by parallel chunk workers. */
    private static final class CrownCode extends JoCode {
        private final BlockPos root;
        private final boolean worldGen;
        private final java.util.Set<BlockPos> placed=new java.util.HashSet<>();
        CrownCode(String code,BlockPos root,boolean worldGen){super(code);this.root=root;this.worldGen=worldGen;}
        private boolean inside(BlockPos pos){return Math.abs(pos.getX()-root.getX())<=23&&Math.abs(pos.getZ()-root.getZ())<=23;}
        @Override protected boolean setBlockForGeneration(LevelAccessor level,com.dtteam.dynamictrees.tree.species.Species species,BlockPos pos,net.minecraft.core.Direction direction,boolean careful,boolean rootBranch){
            if(level.getBlockState(pos).getBlock() instanceof LegacyBranchBlock)return true;
            for(net.minecraft.core.Direction side:net.minecraft.core.Direction.values()){var neighbor=pos.relative(side);if(level.getBlockState(neighbor).getBlock() instanceof com.dtteam.dynamictrees.block.branch.BranchBlock&&!placed.contains(neighbor))return true;}
            boolean blocked=super.setBlockForGeneration(level,species,pos,direction,careful,rootBranch);
            if(!blocked)placed.add(pos.immutable());return blocked;
        }
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
                    if(placed.contains(pos)&&state.getBlock() instanceof com.dtteam.dynamictrees.block.branch.BranchBlock branch&&branch.getFamily()==context.species().getFamily())
                        branch.setRadius(context.level(),pos,node.radius(),null,2);
                }
                var cluster=context.species().getLeavesProperties().getCellKit().getLeafCluster();
                for(var node:profile){
                    var pos=node.position(root,context.facing());var state=context.level().getBlockState(pos);
                    if(placed.contains(pos)&&state.getBlock() instanceof com.dtteam.dynamictrees.block.branch.BranchBlock branch&&branch.getFamily()==context.species().getFamily()&&branch.getRadius(state)==1)
                        leaves.blitMax(pos,cluster);
                }
                for(var node:profile){
                    var pos=node.position(root,context.facing());var state=context.level().getBlockState(pos);
                    if(placed.contains(pos)&&state.getBlock() instanceof com.dtteam.dynamictrees.block.branch.BranchBlock branch&&branch.getFamily()==context.species().getFamily())
                        leaves.setVoxel(pos,(byte)(branch.getRadius(state)==1?16:32));
                }
            }
            // DT's ageing pass can grow a leaf one block beyond the initial map. Leave that
            // margin inside vanilla's 3x3 FEATURES write area instead of issuing far-chunk writes.
            if(generation){
                // Physically bound the ageing map: overlapping neighbors can otherwise
                // hydrate cells past a zeroed rim in later native ageing iterations.
                var safe=new com.dtteam.dynamictrees.api.voxmap.SimpleVoxmap(45,leaves.getLenY(),45)
                    .setCenter(new BlockPos(22-root.getX(),leaves.getCenter().getY(),22-root.getZ()));
                for(var cell:leaves.getAllNonZeroCells())
                    if(Math.abs(cell.getPos().getX()-root.getX())<=22&&Math.abs(cell.getPos().getZ()-root.getZ())<=22)
                        safe.setVoxel(cell.getPos(),cell.getValue());
                super.generateAndAgeLeaves(context,safe,true);return;
            }
            super.generateAndAgeLeaves(context,leaves,generation);
        }
    }
}
