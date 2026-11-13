package org.Azkerash.riftfall.worldmana.geysers;

import java.util.Optional;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** A real indexed structure, with placement based on the geyser terrain rather than the original noise surface. */
public final class GoblinCampStructure extends Structure {
    public static final MapCodec<GoblinCampStructure> CODEC=simpleCodec(GoblinCampStructure::new);
    public GoblinCampStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return GoblinCampStructures.CAMP.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        if(!(context.heightAccessor() instanceof ServerLevel level))return Optional.empty();
        var cp=context.chunkPos();var field=GeyserBiomes.field(level,cp.x,cp.z);
        if(field==null||field.zone(cp.x,cp.z)!=GeyserZone.OUTER
            ||!GoblinCampLayout.eligible(context.seed(),field)||!GoblinCampLayout.candidate(context.seed(),cp.x,cp.z))return Optional.empty();
        long terrain=GeyserLayout.mix(context.seed()^0x474559534552L);
        int x=cp.getMinBlockX()+8,z=cp.getMinBlockZ()+8,min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=5;dz++){
            int floor=GeyserTerrain.height(terrain,field,x+dx,z+dz);min=Math.min(min,floor);max=Math.max(max,floor);
            if(max-min>1||floor<level.getSeaLevel()+2||GeyserShape.zone(field,x+dx,z+dz)!=GeyserZone.OUTER
                ||GeyserTerrain.entranceColumn(terrain,field,x+dx,z+dz)!=null)return Optional.empty();
        }
        var origin=new BlockPos(x,max+1,z);
        return Optional.of(new GenerationStub(origin,builder->builder.addPiece(new Piece(origin))));
    }
    /** Geysers suppress vanilla starts; insert just this allowed structure at the normal starts stage. */
    public static void generateStart(ServerLevel level,ChunkGenerator generator,net.minecraft.world.level.chunk.ChunkAccess chunk){
        var structure=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
            .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("riftfall","goblin_camp"));
        if(structure==null)return;
        var start=structure.generate(level.registryAccess(),generator,generator.getBiomeSource(),level.getChunkSource().randomState(),
            level.getStructureManager(),level.getSeed(),chunk.getPos(),0,level,biome->true);
        if(start.isValid())chunk.setStartForStructure(structure,start);
    }
    public static final class Piece extends StructurePiece {
        private final BlockPos origin;
        Piece(BlockPos origin){super(GoblinCampStructures.PIECE.get(),0,new BoundingBox(origin.getX()-6,origin.getY()-1,origin.getZ()-6,origin.getX()+6,origin.getY()+4,origin.getZ()+5));this.origin=origin;}
        Piece(CompoundTag tag){super(GoblinCampStructures.PIECE.get(),tag);origin=new BlockPos(tag.getInt("CampX"),tag.getInt("CampY"),tag.getInt("CampZ"));}
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag){tag.putInt("CampX",origin.getX());tag.putInt("CampY",origin.getY());tag.putInt("CampZ",origin.getZ());}
        @Override public void postProcess(WorldGenLevel world,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox bounds,ChunkPos chunk,BlockPos anchor){
            if(chunk.x==origin.getX()>>4&&chunk.z==origin.getZ()>>4)GoblinCampBuilder.build(world,origin);
        }
    }
}
