package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.Azkerash.riftfall.Riftfall;

public final class GoblinCampStructures {
    private static final DeferredRegister<StructureType<?>> TYPES=DeferredRegister.create(Registries.STRUCTURE_TYPE,Riftfall.MODID);
    private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,Riftfall.MODID);
    public static final java.util.function.Supplier<StructureType<GoblinCampStructure>> CAMP=TYPES.register("goblin_camp",()->()->GoblinCampStructure.CODEC);
    public static final java.util.function.Supplier<StructurePieceType> PIECE=PIECES.register("goblin_camp",()->(StructurePieceType.ContextlessType)GoblinCampStructure.Piece::new);
    public static void register(IEventBus bus){TYPES.register(bus);PIECES.register(bus);}
    private GoblinCampStructures(){}
}
