package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.block.leaves.LeavesProperties;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictrees.tree.species.UndergroundRootsSpecies;
import com.dtteam.dynamictrees.worldgen.DynamicTreeGenerationContext;
import com.dtteam.dynamictrees.worldgen.JoCode;
import net.minecraft.resources.ResourceLocation;

/** An ancient crown needs a larger leaf volume than DT's ordinary eight-block grove radius. */
public final class LegacyTreeSpecies extends UndergroundRootsSpecies {
    public static final int CROWN_RADIUS=26;
    public LegacyTreeSpecies(ResourceLocation name,Family family,LeavesProperties leaves) {super(name,family,leaves);}

    @Override public JoCode getJoCode(String code) {
        return new JoCode(code) {
            @Override public void generate(DynamicTreeGenerationContext context) {
                // This prototype is available through seeds and the preview command. Biome placement
                // is a separate portion: large crowns need spacing before enabling world generation.
                super.generate(new DynamicTreeGenerationContext(context.levelContext(),context.species(),
                    context.originPos(),context.rootPos(),context.biome(),context.facing(),context.radius(),context.isWorldGen()) {
                    @Override public int radius(){return CROWN_RADIUS;}
                    @Override public boolean secondChanceRegen(){return context.secondChanceRegen();}
                    @Override public void secondChance(){context.secondChance();}
                });
            }
        };
    }
}
