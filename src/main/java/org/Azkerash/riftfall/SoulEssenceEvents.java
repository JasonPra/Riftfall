package org.Azkerash.riftfall;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class SoulEssenceEvents {
    static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Riftfall.MODID);
    static final DeferredHolder<AttachmentType<?>, AttachmentType<SoulEssence>> ESSENCE = TYPES.register("soul_essence",
        () -> AttachmentType.builder(() -> SoulEssence.EMPTY).serialize(SoulEssence.CODEC).build());
    private static final Set<Long> LOADED = ConcurrentHashMap.newKeySet();
    private static int ticks;
    private static int rollSeconds;
    private static int caveSeconds;

    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        // Load events may precede FULL promotion. Only track coordinates here; interact
        // with chunks on the server tick, using getChunkNow to avoid loading neighbors.
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD
            && event.getChunk() instanceof LevelChunk) LOADED.add(event.getChunk().getPos().toLong());
    }

    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD)
            LOADED.remove(event.getChunk().getPos().toLong());
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        LOADED.clear(); CrystalCultivation.clear(); RiftEssencePools.clear(); ticks = 0; rollSeconds = 0; caveSeconds = 0;
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        ServerLevel world = event.getServer().overworld();
        RiftData data = RiftData.get(event.getServer());
        double criticalMass = Config.CRITICAL_MASS.get();
        if (++ticks < 20) return;
        ticks = 0;
        boolean roll = ++rollSeconds >= Config.ESSENCE_ROLL_SECONDS.get();
        if (roll) rollSeconds = 0;
        boolean seedCaves = ++caveSeconds >= Config.CRYSTAL_CAVE_SECONDS.get();
        if (seedCaves) caveSeconds = 0;
        var chunks = new ArrayList<>(LOADED);
        // Avoid preferring the same chunks whenever the active-rift cap is nearly full.
        if (roll) Collections.shuffle(chunks, new java.util.Random(world.random.nextLong()));
        for (long packed : chunks) {
            ChunkPos pos = new ChunkPos(packed);
            LevelChunk chunk = world.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null) continue;
            SoulEssence previous = chunk.getData(ESSENCE);
            SoulEssence essence = previous.accumulate(CrystalCultivation.rate(Config.BASE_ESSENCE_PER_SECOND.get(), Config.MATURE_CRYSTAL_BONUS.get(), CrystalCultivation.count(world, pos)), criticalMass);
            if (!essence.equals(previous)) set(chunk, essence);
            if (seedCaves && world.random.nextInt(Config.CRYSTAL_CAVE_RARITY.get()) == 0
                && canSpend(world, pos.getWorldPosition(), Config.CRYSTAL_SEED_COST.get())
                && world.random.nextDouble() < Config.CRYSTAL_CAVE_CHANCE.get()) {
                BlockPos site = CrystalGrowth.caveSite(world, pos, world.random, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
                if (site != null) {
                    var seed = CrystalGrowth.seedState(world, site, net.minecraft.core.Direction.UP);
                    if (seed != null && world.setBlock(site, seed, 3)) spend(world, site, Config.CRYSTAL_SEED_COST.get());
                }
                essence = chunk.getData(ESSENCE);
            }
            if (!roll || !essence.isCritical(criticalMass) || data.activeCount() >= Config.MAX_RIFTS.get()) continue;
            if (data.rifts.stream().anyMatch(r -> new ChunkPos(r.pos).equals(pos))) continue;
            if (world.random.nextDouble() >= Config.ESSENCE_SPAWN_CHANCE.get()) continue;
            BlockPos site = findSite(world, chunk, data);
            if (site != null) RiftEvents.create(event.getServer(), site);
            // create() consumes essence only after finding a valid site. Failed rolls,
            // blocked terrain, and the global cap leave a critical chunk ready to retry.
        }
        RiftEssencePools.tick(world, data, LOADED);
    }

    private static BlockPos findSite(ServerLevel world, LevelChunk chunk, RiftData data) {
        for (int attempt = 0; attempt < 12; attempt++) {
            // Leave room for the eruption and portal without querying adjacent chunks.
            int x = chunk.getPos().getMinBlockX() + 4 + world.random.nextInt(8);
            int z = chunk.getPos().getMinBlockZ() + 4 + world.random.nextInt(8);
            BlockPos site = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (RiftEvents.safe(world, site) && RiftPlacement.available(data, site)) return site;
        }
        return null;
    }

    static void consume(ServerLevel world, BlockPos pos) {
        LevelChunk chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null) set(chunk, SoulEssence.EMPTY);
    }

    static double availableEssence(ServerLevel world, BlockPos pos) {
        LevelChunk chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? -1 : chunk.getData(ESSENCE).points();
    }

    static void add(ServerLevel world, BlockPos pos, double amount) {
        LevelChunk chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null) set(chunk, new SoulEssence(chunk.getData(ESSENCE).points() + amount));
    }

    static boolean canSpend(ServerLevel world, BlockPos pos, double cost) {
        LevelChunk chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return false;
        boolean dormantRoot = RiftData.get(world.getServer()).rifts.stream()
            .anyMatch(r -> r.dormant && new ChunkPos(r.pos).equals(chunk.getPos()));
        return RiftEssencePools.canFundCrystal(chunk.getData(ESSENCE).points(), Config.CRITICAL_MASS.get(), cost, dormantRoot);
    }

    static void spend(ServerLevel world, BlockPos pos, double cost) {
        LevelChunk chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null) set(chunk, new SoulEssence(Math.max(0, chunk.getData(ESSENCE).points() - cost)));
    }

    private static void set(LevelChunk chunk, SoulEssence essence) {
        chunk.setData(ESSENCE, essence);
        chunk.setUnsaved(true);
    }

    private static LevelChunk commandChunk(CommandSourceStack source) {
        if (source.getLevel().dimension() != Level.OVERWORLD) {
            source.sendFailure(Component.literal("Soul essence accumulates in overworld chunks.")); return null;
        }
        BlockPos pos = BlockPos.containing(source.getPosition());
        LevelChunk chunk = source.getLevel().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) source.sendFailure(Component.literal("That chunk is not loaded."));
        return chunk;
    }

    private static int report(CommandSourceStack source) {
        LevelChunk chunk = commandChunk(source);
        if (chunk == null) return 0;
        SoulEssence essence = chunk.getData(ESSENCE);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
            "Chunk [%d, %d]: %.1f / %.1f soul essence%s", chunk.getPos().x, chunk.getPos().z,
            essence.points(), Config.CRITICAL_MASS.get(), essence.isCritical(Config.CRITICAL_MASS.get()) ? " (CRITICAL)" : "")), false);
        int mature = CrystalCultivation.count(source.getLevel(), chunk.getPos());
        double rate = CrystalCultivation.rate(Config.BASE_ESSENCE_PER_SECOND.get(), Config.MATURE_CRYSTAL_BONUS.get(), mature);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Cultivation: %.3f/s (%d mature clusters)", rate, mature)), false);
        Integer owner = RiftEssencePools.owner(source.getLevel(), chunk.getPos());
        if (owner != null) source.sendSuccess(() -> Component.literal("Connected essence pool: rift #" + owner), false);
        return 1;
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        // Registering another child of the existing /rift root merges command nodes.
        event.getDispatcher().register(Commands.literal("rift").then(Commands.literal("essence")
            .executes(c -> report(c.getSource()))
            .then(Commands.literal("set").requires(s -> s.hasPermission(2))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0, 1_000_000_000))
                    .executes(c -> {
                        LevelChunk chunk = commandChunk(c.getSource());
                        if (chunk == null) return 0;
                        set(chunk, new SoulEssence(DoubleArgumentType.getDouble(c, "amount")));
                        return report(c.getSource());
                    })))));
    }
}
