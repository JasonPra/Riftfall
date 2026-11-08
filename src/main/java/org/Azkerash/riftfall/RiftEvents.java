package org.Azkerash.riftfall;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.Azkerash.riftfall.RiftData.Rift;

public final class RiftEvents {
    static final ResourceKey<Level> ARENA = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("riftfall", "arena"));
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { COOLDOWNS.clear(); }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel world = server.overworld();
        ServerLevel arena = server.getLevel(ARENA);
        if (arena == null) return;
        RiftData data = RiftData.get(server);
        COOLDOWNS.replaceAll((id, ticks) -> ticks - 1);
        COOLDOWNS.values().removeIf(t -> t <= 0);
        if (++data.spawnTicks >= Config.SPAWN_SECONDS.get() * 20) {
            data.spawnTicks = 0;
            var players = world.players().stream().filter(p -> !p.isSpectator()).toList();
            if (!players.isEmpty() && data.rifts.size() < Config.MAX_RIFTS.get()) {
                ServerPlayer player = players.get(world.random.nextInt(players.size()));
                for (int attempt = 0; attempt < 12; attempt++) {
                    int x = player.getBlockX() + world.random.nextInt(129) - 64;
                    int z = player.getBlockZ() + world.random.nextInt(129) - 64;
                    if (!world.hasChunk(x >> 4, z >> 4)) continue;
                    BlockPos pos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                    if (safe(world, pos) && data.rifts.stream().noneMatch(r -> r.pos.distSqr(pos) < 256)) {
                        create(server, pos); break;
                    }
                }
            }
        }
        for (Rift r : java.util.List.copyOf(data.rifts)) {
            if (r.remaining > 0 && --r.remaining == 0) {
                announce(server, "Dungeon break at " + coordinates(r.pos) + "! Clear the arena to close the rift.");
            }
            if (r.built && server.getTickCount() > 200 && server.getTickCount() % 100 == 0) {
                // Reconcile enemies removed without a death event (e.g. Peaceful mode).
                // Forced chunks plus a startup grace period allow entity loading first.
                r.enemies.removeIf(id -> arena.getEntity(id) == null);
            }
            if (r.built && r.enemies.isEmpty()) { close(server, data, r); continue; }
            if (world.hasChunkAt(r.pos)) {
                if (server.getTickCount() % 10 == 0) portal(world, r.pos, r.remaining == 0);
                if (r.remaining == 0 && ++r.waveTicks >= Config.WAVE_SECONDS.get() * 20) {
                    r.waveTicks = 0;
                    for (int i = 0; i < 2 && r.outside.size() < Config.MAX_OUTSIDE.get(); i++) {
                        int x = r.pos.getX() + world.random.nextInt(9) - 4;
                        int z = r.pos.getZ() + world.random.nextInt(9) - 4;
                        BlockPos pos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                        if (safe(world, pos)) {
                            Zombie mob = zombie(world, pos);
                            if (mob != null) r.outside.add(mob.getUUID());
                        }
                    }
                }
                for (ServerPlayer player : java.util.List.copyOf(world.players())) {
                    if (canEnter(player, r.pos)) {
                        if (!r.built) build(arena, r);
                        travel(player, arena, r.exit());
                        player.sendSystemMessage(Component.literal("Defeat all " + r.enemies.size() + " arena enemies. The purple marker returns you outside."));
                    }
                }
            }
            if (r.built) {
                if (server.getTickCount() % 10 == 0) portal(arena, r.exit(), false);
                for (ServerPlayer player : java.util.List.copyOf(arena.players())) {
                    if (canEnter(player, r.exit())) travel(player, world, r.pos);
                }
            }
        }
        // Recover players who logged out inside an arena that another player cleared.
        for (ServerPlayer player : java.util.List.copyOf(arena.players())) {
            boolean active = data.rifts.stream().anyMatch(r -> Math.abs(player.getX() - r.arena().getX()) <= 11 && Math.abs(player.getZ()) <= 11);
            if (!active) travel(player, world, world.getSharedSpawnPos().above());
        }
        // Persist countdowns and tracked mobs, even if no player is near the rift.
        data.setDirty();
    }

    static boolean safe(ServerLevel level, BlockPos pos) {
        return level.getWorldBorder().isWithinBounds(pos) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
            && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    static boolean canEnter(ServerPlayer p, BlockPos pos) {
        return p.isAlive() && !p.isSpectator() && !COOLDOWNS.containsKey(p.getUUID())
            && p.distanceToSqr(pos.getX() + .5, pos.getY(), pos.getZ() + .5) < 2.25;
    }

    static void travel(ServerPlayer p, ServerLevel level, BlockPos pos) {
        if (level.dimension() == Level.OVERWORLD && !safe(level, pos)) {
            BlockPos landing = null;
            for (int x = -8; x <= 8 && landing == null; x++) for (int z = -8; z <= 8 && landing == null; z++) {
                BlockPos candidate = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.offset(x, 0, z));
                if (safe(level, candidate)) landing = candidate;
            }
            if (landing != null) pos = landing;
            else pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level.getSharedSpawnPos());
        }
        p.teleportTo(level, pos.getX() + .5, pos.getY(), pos.getZ() + .5, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        COOLDOWNS.put(p.getUUID(), 100);
    }

    static void portal(ServerLevel level, BlockPos pos, boolean broken) {
        level.sendParticles(broken ? ParticleTypes.FLAME : ParticleTypes.PORTAL,
            pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, 35, .45, 1, .45, .02);
    }

    static Zombie zombie(ServerLevel level, BlockPos pos) {
        Zombie mob = EntityType.ZOMBIE.create(level);
        if (mob == null) return null;
        mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
        mob.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD, 0);
        return level.addFreshEntity(mob) ? mob : null;
    }

    static void build(ServerLevel level, Rift r) {
        BlockPos c = r.arena();
        // Each arena occupies separate chunks. Keep enemies ticking while players leave.
        for (int x = (c.getX() - 10) >> 4; x <= (c.getX() + 10) >> 4; x++)
            for (int z = -1; z <= 0; z++) level.setChunkForced(x, z, true);
        for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) for (int y = 0; y <= 7; y++) {
            boolean shell = y == 0 || y == 7 || Math.abs(x) == 10 || Math.abs(z) == 10;
            level.setBlockAndUpdate(c.offset(x, y, z), (shell ? (y == 7 ? Blocks.SEA_LANTERN : Blocks.BEDROCK) : Blocks.AIR).defaultBlockState());
        }
        for (int i = 0; i < Config.ENEMIES.get(); i++) {
            Zombie mob = zombie(level, c.offset(level.random.nextInt(11) - 3, 1, level.random.nextInt(15) - 7));
            if (mob != null) r.enemies.add(mob.getUUID());
        }
        r.built = true;
    }

    static void create(MinecraftServer server, BlockPos pos) {
        RiftData data = RiftData.get(server);
        Rift r = new Rift(data.nextId++, pos, Config.BREAK_SECONDS.get() * 20);
        data.rifts.add(r); data.setDirty();
        announce(server, "A rift opened at " + coordinates(pos) + ". Enter the purple particles; dungeon break in " + Config.BREAK_SECONDS.get() + " seconds.");
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        RiftData data = RiftData.get(level.getServer());
        for (Rift r : data.rifts) {
            if (r.enemies.remove(event.getEntity().getUUID()) | r.outside.remove(event.getEntity().getUUID())) data.setDirty();
        }
    }

    static void close(MinecraftServer server, RiftData data, Rift r) {
        ServerLevel arena = server.getLevel(ARENA);
        if (arena == null) return;
        for (ServerPlayer player : java.util.List.copyOf(arena.players())) {
            if (Math.abs(player.getX() - r.arena().getX()) <= 11 && Math.abs(player.getZ()) <= 11)
                travel(player, server.overworld(), r.pos);
        }
        for (int x = (r.arena().getX() - 10) >> 4; x <= (r.arena().getX() + 10) >> 4; x++)
            for (int z = -1; z <= 0; z++) arena.setChunkForced(x, z, false);
        data.rifts.remove(r); data.setDirty();
        announce(server, "Rift at " + coordinates(r.pos) + " cleared and closed!");
    }

    static String coordinates(BlockPos p) { return p.getX() + ", " + p.getY() + ", " + p.getZ(); }
    static void announce(MinecraftServer server, String message) { server.getPlayerList().broadcastSystemMessage(Component.literal("[Riftfall] " + message), false); }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rift")
            .then(Commands.literal("spawn").requires(s -> s.hasPermission(2)).executes(c -> {
                ServerLevel world = c.getSource().getLevel();
                MinecraftServer server = c.getSource().getServer();
                if (world.dimension() != Level.OVERWORLD) { c.getSource().sendFailure(Component.literal("Spawn rifts in the overworld.")); return 0; }
                RiftData data = RiftData.get(server);
                if (data.rifts.size() >= Config.MAX_RIFTS.get()) { c.getSource().sendFailure(Component.literal("Maximum active rifts reached.")); return 0; }
                BlockPos pos = BlockPos.containing(c.getSource().getPosition());
                if (!safe(world, pos) || data.rifts.stream().anyMatch(r -> r.pos.distSqr(pos) < 256)) {
                    c.getSource().sendFailure(Component.literal("Stand on solid ground with clear space, at least 16 blocks from other rifts.")); return 0;
                }
                create(server, pos); return 1;
            }))
            .then(Commands.literal("status").executes(c -> {
                RiftData data = RiftData.get(c.getSource().getServer());
                c.getSource().sendSuccess(() -> Component.literal("Active rifts: " + data.rifts.size()), false);
                for (Rift r : data.rifts) c.getSource().sendSuccess(() -> Component.literal("#" + r.id + " at " + coordinates(r.pos) + " | " + (r.remaining == 0 ? "BROKEN" : (r.remaining + 19) / 20 + "s") + " | arena: " + (r.built ? r.enemies.size() + " enemies" : "unvisited")), false);
                return 1;
            }))
            .then(Commands.literal("leave").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                if (p.level().dimension() != ARENA) { c.getSource().sendFailure(Component.literal("You are not inside a rift.")); return 0; }
                RiftData data = RiftData.get(p.server);
                Rift nearest = data.rifts.stream().min(java.util.Comparator.comparingDouble(r -> p.distanceToSqr(r.arena().getX(), 81, 0))).orElse(null);
                travel(p, p.server.overworld(), nearest == null ? p.server.overworld().getSharedSpawnPos().above() : nearest.pos);
                return 1;
            })));
    }
}
