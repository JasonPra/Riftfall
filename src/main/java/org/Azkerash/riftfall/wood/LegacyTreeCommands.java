package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.api.worldgen.LevelContext;
import com.dtteam.dynamictrees.tree.species.Species;
import com.dtteam.dynamictrees.worldgen.DynamicTreeGenerationContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Inspect living growth, export native layouts, or preview a captured grown-stage tree. */
@EventBusSubscriber(modid="riftfall")
public final class LegacyTreeCommands {
    private record Observation(long tick,long fingerprint){}
    private static final java.util.Map<ServerLevel,java.util.LinkedHashMap<BlockPos,Observation>> OBSERVATIONS=new java.util.WeakHashMap<>();
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("legacytree").requires(source->source.hasPermission(2))
            .then(Commands.literal("inspect").executes(context->inspect(context.getSource(),lookAt(context.getSource())))
                .then(Commands.argument("tree",BlockPosArgument.blockPos()).executes(context->inspect(context.getSource(),BlockPosArgument.getLoadedBlockPos(context,"tree")))))
            .then(Commands.literal("export").then(Commands.argument("tree",BlockPosArgument.blockPos()).executes(context->export(context.getSource(),BlockPosArgument.getLoadedBlockPos(context,"tree")))))
            .then(Commands.argument("soil",BlockPosArgument.blockPos()).executes(context->{
                var source=context.getSource();var pos=BlockPosArgument.getLoadedBlockPos(context,"soil");
                if(!generate(source.getLevel(),pos)){
                    source.sendFailure(Component.literal("Heartwood tree needs open space above a soil block."));return 0;
                }
                source.sendSuccess(()->Component.literal("Heartwood grown-reference tree placed at "+pos.toShortString()+"."),true);
                return 1;
            })));
    }
    private static BlockPos lookAt(net.minecraft.commands.CommandSourceStack source)throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var hit=source.getEntityOrException().pick(32,0,false);
        return hit instanceof net.minecraft.world.phys.BlockHitResult block?block.getBlockPos():BlockPos.containing(source.getPosition());
    }
    private static int inspect(net.minecraft.commands.CommandSourceStack source,BlockPos target){
        var level=source.getLevel();
        if(level.getBlockState(target).getBlock() instanceof com.dtteam.dynamictrees.block.sapling.DynamicSaplingBlock){
            source.sendSuccess(()->Component.literal("Sapling — not yet a branching tree. Dynamic Trees has no fixed final growth-stage flag."),false);return 1;
        }
        try{
            var sample=LegacyTreeSurvey.measure(level,LegacyTreeSurvey.root(level,target));
            var history=OBSERVATIONS.computeIfAbsent(level,key->new java.util.LinkedHashMap<>());
            long now=level.getGameTime();var previous=history.put(sample.root(),new Observation(now,sample.fingerprint()));
            if(history.size()>128)history.remove(history.keySet().iterator().next());
            String observation=previous==null?"Inspect again later to compare growth.":
                (previous.fingerprint()!=sample.fingerprint()?"Branch structure/thickness changed":"No measured branch change")+" over "+Math.max(0,(now-previous.tick())/20)+" seconds; this does not prove growth is finished.";
            String text=sample.species().getRegistryName()+" | root "+sample.root().toShortString()+"\n"
                +"Living tree. Final/adult stage: not defined by Dynamic Trees.\n"
                +String.format(java.util.Locale.ROOT,"Trunk diameter: %.2f blocks (configured maximum %.2f); max thickness reached: %s.\n",sample.trunkRadius()/8.0,sample.species().getMaxBranchRadius()/8.0,sample.trunkRadius()>=sample.species().getMaxBranchRadius()?"yes — this is not a final-stage flag":"no")
                +"Branch height: "+sample.height()+"; branch spread: "+sample.width()+" × "+sample.depth()+"; branches: "+sample.nodes().size()+"; tips: "+sample.tips()+".\n"
                +(sample.species().getRegistryName().equals(LegacyTrees.ID)?"Captured grown references: "+LegacyGrownProfiles.referenceRange()+" (not a final-stage threshold).\n":"")
                +"Soil fertility: "+sample.fertility()+"/15"+(sample.fertility()==0?" (depleted soil can pause growth; fertilize to test further growth)":"")+"; growth multiplier: "+sample.growthRate()+". "+(sample.complete()?"":"PARTIAL: some branches are outside loaded chunks. ")+observation;
            source.sendSuccess(()->Component.literal(text).withStyle(style->style
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD,text))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,Component.literal("Click to copy this tree report")))),false);return 1;
        }catch(IllegalArgumentException error){source.sendFailure(Component.literal(error.getMessage()));return 0;}
    }
    private static int export(net.minecraft.commands.CommandSourceStack source,BlockPos target){
        try{
            var level=source.getLevel();var sample=LegacyTreeSurvey.measure(level,LegacyTreeSurvey.root(level,target));
            if(!sample.complete())throw new IllegalArgumentException("Load the complete tree before exporting.");
            var json=new com.google.gson.JsonObject();json.addProperty("source","living_tree_capture");json.addProperty("species",sample.species().getRegistryName().toString());
            json.addProperty("seed",level.getSeed());json.addProperty("soil",sample.root().toShortString());
            json.addProperty("soil_fertility",sample.fertility());json.addProperty("recorded_at_tick",level.getGameTime());
            json.addProperty("jo_code",com.dtteam.dynamictrees.tree.TreeHelper.getJoCode(level,sample.root(),Direction.NORTH,false).orElseThrow().toString());
            json.addProperty("roots_jo_code",com.dtteam.dynamictrees.tree.TreeHelper.getRootsJoCode(level,sample.root()).map(Object::toString).orElse(""));
            var nodes=new com.google.gson.JsonArray();int reach=0;
            for(var node:sample.nodes()){var row=new com.google.gson.JsonArray();row.add(node.x());row.add(node.y());row.add(node.z());row.add(node.radius());nodes.add(row);reach=Math.max(reach,Math.max(Math.abs(node.x()),Math.abs(node.z())));}
            json.add("branches",nodes);json.addProperty("height",sample.height());json.addProperty("branch_reach",reach);
            var folder=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("legacy-tree-captures");java.nio.file.Files.createDirectories(folder);
            var root=sample.root();var file=folder.resolve(root.getX()+"_"+root.getY()+"_"+root.getZ()+"_"+level.getGameTime()+".json");
            java.nio.file.Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
            source.sendSuccess(()->Component.literal("Exported native tree layout to "+file+". Growth age/origin is not stored by Dynamic Trees; use a planted reference when updating generation samples."),false);return 1;
        }catch(Exception error){source.sendFailure(Component.literal("Tree export failed: "+error.getMessage()));return 0;}
    }
    public static boolean generate(ServerLevel level,BlockPos soil) {
        Species species=Species.findSpecies(LegacyTrees.ID);
        if(!species.isValid()||!species.isAcceptableSoil(level.getBlockState(soil)))return false;
        boolean generated=species.generate(new DynamicTreeGenerationContext(LevelContext.create(level),species,soil,soil.mutable(),
            level.getBiome(soil),Direction.NORTH,8,false));
        return generated&&com.dtteam.dynamictrees.tree.TreeHelper.getBranchOpt(level.getBlockState(soil.above()))
            .filter(branch->branch.getFamily()==species.getFamily()).isPresent();
    }
    private LegacyTreeCommands() {}
}
