package org.Azkerash.riftfall;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        for (Rift r : java.util.List.copyOf(data.rifts)) {
            if (server.getTickCount() % 20 == 0) {
                double available = SoulEssenceEvents.availableEssence(world, r.pos);
                // Preserve a completed opening charge while a dormant gate waits for
                // its cooldown or an active-rift slot, rather than consuming it first.
                double intake = r.dormant && available >= Config.CRITICAL_MASS.get() ? 0
                    : Math.min(Config.RIFT_AMBIENT_INTAKE.get(), Math.max(0, available));
                if (intake > 0) {
                    SoulEssenceEvents.spend(world, r.pos, intake);
                    r.energy += intake; r.essence += intake;
                }
            }
            if ((r.sourceBiome.isEmpty() && world.hasChunkAt(r.pos)) || (!r.sourceBiome.isEmpty() && r.rosterVersion<RiftEcology.ROSTER_VERSION)) RiftEcology.capture(world,r);
            RiftDungeons.migrate(arena,r);
            for (UUID id : java.util.Set.copyOf(r.enemies)) {
                var mob=RiftMobs.find(arena,r,id);
                if (mob!=null && (!RiftMobs.eligible(mob.getType()) || !RiftEcology.matches(mob.getType(),r))
                    && !(RiftBosses.isBoss(mob.getType()) && id.equals(r.boss) && r.theme.equals("frozen"))) {
                    mob.discard(); r.enemies.remove(id); r.spawned.remove(id); arena.getScoreboard().removePlayerFromTeam(id.toString());
                    if (id.equals(r.boss)) { r.boss=null; RiftBosses.clearBar(r); }
                }
            }
            RiftBosses.tick(arena,r);
            if (r.dormant) {
                if (r.dormantTicks > 0) r.dormantTicks--;
                if (r.dormantTicks == 0 && data.activeCount() < Config.MAX_RIFTS.get()
                    && SoulEssenceEvents.availableEssence(world, r.pos) >= Config.CRITICAL_MASS.get()) reopen(server, r);
                continue;
            }
            r.occupants.removeIf(id -> {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                return p == null || !p.isAlive() || p.isSpectator() || p.level().dimension() != ARENA || !r.contains(p.getX(), p.getZ());
            });
            if (r.built) for (ServerPlayer p : arena.players())
                if (p.isAlive() && !p.isSpectator() && r.contains(p.getX(), p.getZ())) r.occupants.add(p.getUUID());
            if (!r.cleared && r.remaining > 0 && --r.remaining == 0)
                announce(server, "Dungeon break at " + coordinates(r.pos) + "! Clear the arena to stop the breach.");
            if (r.built && r.entityGraceTicks > 0) r.entityGraceTicks--;
            if (r.built && !r.cleared && r.entityGraceTicks == 0 && server.getTickCount() % 100 == 0)
                r.enemies.removeIf(id -> {
                    if (RiftMobs.find(arena,r,id) != null) return false;
                    r.spawned.remove(id);
                    arena.getScoreboard().removePlayerFromTeam(id.toString());
                    return true;
                });
            RiftReplenishment.tick(arena, r);
            if (r.built && !r.cleared) r.emptyEnemyTicks = r.enemies.isEmpty() ? r.emptyEnemyTicks+1 : 0;
            // Death animations can spawn offspring a second later (notably slimes).
            if (r.built && !r.cleared && r.emptyEnemyTicks >= 40) {
                r.cleared = true;
                RiftProgression.rewards(arena, r);
                announce(server, "Rift at " + coordinates(r.pos) + " cleared! Collect the northern reward chest, then walk out through the exit.");
            }
            if (world.hasChunkAt(r.pos)) {
                if (server.getTickCount() % 10 == 0) portal(world, r, false);
                if (!r.cleared && r.remaining == 0 && ++r.waveTicks >= Config.WAVE_SECONDS.get() * 20) {
                    r.waveTicks = 0;
                    for (int i = 0; i < 2 && r.outside.size() < Config.MAX_OUTSIDE.get(); i++) {
                        int x = r.pos.getX() + world.random.nextInt(9) - 4;
                        int z = r.pos.getZ() + world.random.nextInt(9) - 4;
                        BlockPos pos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                        if (safe(world, pos)) {
                            var spawned = RiftMobs.spawn(world,pos,r.level(),r.energy,r);
                            if (spawned != null) { r.energy-=spawned.cost(); r.outside.add(spawned.mob().getUUID()); RiftMobs.team(world,spawned.mob(),r.id); }
                        }
                    }
                }
                if (!r.cleared) for (ServerPlayer player : java.util.List.copyOf(world.players())) {
                    if (canEnter(player, r.pos)) {
                        if (!r.built && !build(arena, r)) {
                            player.displayClientMessage(Component.literal("The rift is gathering essence to construct its encounter."), true);
                            continue;
                        }
                        travel(player, arena, r.entry(), r.portalYaw(true));
                        r.occupants.add(player.getUUID());
                        player.sendSystemMessage(Component.literal("Defeat all " + r.enemies.size() + " enemies (rift level " + r.encounterLevel + "). Collect rewards and leave through the southern exit when ready."));
                    }
                }
            }
            if (r.built) {
                if (!r.cleared && server.getTickCount() % 20 == 0) RiftMobs.control(arena,r);
                if (server.getTickCount() % 10 == 0) portal(arena, r, true);
                for (ServerPlayer player : java.util.List.copyOf(arena.players()))
                    if (canEnter(player, r.exit())) { r.occupants.remove(player.getUUID()); travel(player, world, r.pos); }
                if (r.cleared && r.occupants.isEmpty()) close(server, data, r);
            }
        }
        // Return disconnected visitors safely if their old encounter went dormant.
        for (ServerPlayer player : java.util.List.copyOf(arena.players())) {
            if (!player.isAlive()) continue;
            Rift owner = data.rifts.stream().filter(r -> r.contains(player.getX(), player.getZ())).findFirst().orElse(null);
            if (owner == null || owner.dormant || !owner.built)
                travel(player, world, owner == null ? world.getSharedSpawnPos().above() : owner.pos);
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
        travel(p, level, pos, p.getYRot());
    }

    static void travel(ServerPlayer p, ServerLevel level, BlockPos pos, float yaw) {
        if (level.dimension() == Level.OVERWORLD && !safe(level, pos)) {
            BlockPos landing = null;
            for (int x = -8; x <= 8 && landing == null; x++) for (int z = -8; z <= 8 && landing == null; z++) {
                BlockPos candidate = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.offset(x, 0, z));
                if (safe(level, candidate)) landing = candidate;
            }
            if (landing != null) pos = landing;
            else pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level.getSharedSpawnPos());
        }
        p.teleportTo(level, pos.getX() + .5, pos.getY(), pos.getZ() + .5, yaw, p.getXRot());
        p.fallDistance = 0;
        COOLDOWNS.put(p.getUUID(), 100);
    }

    static void portal(ServerLevel level, Rift r, boolean exit) {
        BlockPos pos = exit ? r.exit() : r.pos;
        var existing = level.getEntitiesOfClass(RiftEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2),
            entity -> entity.belongsTo(r.id, exit));
        if (!existing.isEmpty()) {
            existing.getFirst().setBroken(!exit && !r.cleared && r.remaining == 0);
            // Keep one visual per endpoint, even if an earlier load created a duplicate.
            for (int i = 1; i < existing.size(); i++) existing.get(i).discard();
            return;
        }
        RiftEntity entity = RiftEntities.RIFT.get().create(level);
        if (entity != null) {
            entity.configure(r.id, exit, pos, !exit && !r.cleared && r.remaining == 0, r.portalYaw(exit));
            level.addFreshEntity(entity);
        }
    }

    static void removePortal(ServerLevel level, Rift r, boolean exit) {
        BlockPos pos = exit ? r.exit() : r.pos;
        level.getEntitiesOfClass(RiftEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2),
            entity -> entity.belongsTo(r.id, exit)).forEach(RiftEntity::discard);
    }

    static void forceArena(ServerLevel level, Rift r, boolean force) {
        for (int x = (r.arena().getX() - r.radius) >> 4; x <= (r.arena().getX() + r.radius) >> 4; x++)
            for (int z = (-r.radius >> 4); z <= (r.radius >> 4); z++) level.setChunkForced(x, z, force);
    }

    static boolean build(ServerLevel level, Rift r) { return RiftDungeons.build(level, r); }

    static void reopen(MinecraftServer server, Rift r) {
        r.opening=true;
        try { openingBurst(server.overworld(), r.pos); } finally { r.opening=false; }
        r.energy += Math.max(0, SoulEssenceEvents.availableEssence(server.overworld(), r.pos));
        SoulEssenceEvents.consume(server.overworld(), r.pos);
        r.dormant = false; r.cleared = false; r.built = false; r.cycle++;
        r.replenishTicks=0; r.enemyCapacity=-1;
        r.remaining = Config.BREAK_SECONDS.get() * 20; r.waveTicks = 0;
        RiftBosses.clearBar(r); r.boss = null;
        r.enemies.clear(); r.spawned.clear(); r.occupants.clear();
        portal(server.overworld(), r, false);
        announce(server, "Rift at " + coordinates(r.pos) + " reawakened (level " + r.level() + ")!");
    }

    static void create(MinecraftServer server, BlockPos pos) {
        RiftData data = RiftData.get(server);
        Rift r = new Rift(data.nextId++, pos, Config.BREAK_SECONDS.get() * 20);
        RiftEcology.capture(server.overworld(), r);
        r.exteriorYaw = server.overworld().random.nextFloat() * 360F;
        r.energy = Math.max(Config.CRITICAL_MASS.get(), SoulEssenceEvents.availableEssence(server.overworld(), pos));
        data.addRift(r); data.setDirty();
        r.opening=true;
        try { openingBurst(server.overworld(), pos); } finally { r.opening=false; }
        SoulEssenceEvents.consume(server.overworld(), pos);
        portal(server.overworld(), r, false);
        announce(server, "A rift opened at " + coordinates(pos) + ". Enter the rift portal; dungeon break in " + Config.BREAK_SECONDS.get() + " seconds.");
    }

    static void openingBurst(ServerLevel world, BlockPos pos) {
        double x = pos.getX() + .5, y = pos.getY() + 1, z = pos.getZ() + .5;
        // NONE preserves blocks while retaining the blast's entity damage and knockback.
        world.explode(null, x, y, z, 2.0F, Level.ExplosionInteraction.NONE);
        world.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 80, .8, 1.0, .8, .06);
        world.sendParticles(ParticleTypes.FLAME, x, y, z, 60, .6, .9, .6, .12);
        // A normal positional sound packet, never the Wither's global level event.
        world.playSound(null, x, y, z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 1.0F);
    }

    @SubscribeEvent public static void joined(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel arena) || arena.dimension()!=ARENA
            || !(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)) return;
        RiftData data=RiftData.get(arena.getServer());
        String species=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        if (RiftEcology.overworldOnly(species) && mob instanceof io.redspace.ironsspellbooks.api.entity.IMagicEntity) {
            for (Rift r : data.rifts) { r.enemies.remove(mob.getUUID()); r.spawned.remove(mob.getUUID()); if (mob.getUUID().equals(r.boss)) r.boss=null; }
            event.setCanceled(true); data.setDirty(); return;
        }
        if (!(RiftMobs.eligible(mob.getType()) || RiftBosses.isBoss(mob.getType()))) return;
        for (Rift r : data.rifts) if (r.retired.remove(mob.getUUID())) {
            arena.getScoreboard().removePlayerFromTeam(mob.getStringUUID());
            event.setCanceled(true); data.setDirty(); return;
        }
        for (Rift r : data.rifts) if (r.contains(mob.getX(),mob.getZ()) && !r.dormant
            && (r.refitting || ((!r.built || r.cleared) && !mob.getPersistentData().getBoolean("RiftfallPaid")))) {
            event.setCanceled(true); return;
        }
        Rift owner=data.rifts.stream().filter(r -> r.built && !r.dormant && !r.cleared && r.contains(mob.getX(),mob.getZ())).findFirst().orElse(null);
        if (owner==null) return;
        if (owner.enemies.contains(mob.getUUID())) { owner.spawned.put(mob.getUUID(),mob); RiftMobs.hostile(mob); RiftSpellcasting.restore(mob); RiftMobs.team(arena,mob,owner.id); return; }
        if (RiftBosses.isBoss(mob.getType()) && !mob.getPersistentData().getBoolean("RiftfallBoss")) { event.setCanceled(true); return; }
        if (owner.enemies.size()>=128 || (!mob.getPersistentData().getBoolean("RiftfallPaid") && owner.energy<10)) { event.setCanceled(true); return; }
        if (!mob.getPersistentData().getBoolean("RiftfallPaid") && owner.boss != null && RiftBosses.inRoom(owner,mob.getX(),mob.getZ()))
            mob.getPersistentData().putBoolean("RiftfallBossMinion",true);
        if (!mob.getPersistentData().getBoolean("RiftfallPaid")) {
            owner.energy-=10;
            RiftProgression.equip(mob,owner.encounterLevel);
        }
        owner.spawned.put(mob.getUUID(),mob);
        owner.enemies.add(mob.getUUID()); mob.addTag("riftfall_rift_"+owner.id);
        mob.setPersistenceRequired(); RiftMobs.hostile(mob); RiftMobs.team(arena,mob,owner.id); data.setDirty();
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        RiftData data = RiftData.get(level.getServer());
        if (event.getEntity() instanceof ServerPlayer player) {
            var killer = event.getSource().getEntity();
            for (Rift r : data.rifts) {
                boolean ownedMob = killer != null && (r.enemies.contains(killer.getUUID()) || r.outside.contains(killer.getUUID())
                    || (!(killer instanceof ServerPlayer) && killer.getPersistentData().contains("RiftfallOwner") && killer.getPersistentData().getInt("RiftfallOwner")==r.id));
                boolean dungeonHazard = level.dimension()==ARENA && r.built && !r.dormant
                    && r.contains(player.getX(),player.getZ()) && !(killer instanceof ServerPlayer);
                boolean openingBlast = r.opening && level.dimension()==Level.OVERWORLD && player.distanceToSqr(r.pos.getX()+.5,r.pos.getY()+1,r.pos.getZ()+.5)<64;
                if (!ownedMob && !dungeonHazard && !openingBlast) continue;
                r.essence += Config.RIFT_DEATH_ESSENCE.get(); r.energy += Config.RIFT_DEATH_ESSENCE.get();
                data.setDirty();
                announce(level.getServer(), "Rift at " + coordinates(r.pos) + " absorbed a fallen delver's essence. Its next encounter can reach level " + r.level() + ".");
                break;
            }
        }
        for (Rift r : data.rifts) {
            if (event.getEntity() instanceof ServerPlayer) r.occupants.remove(event.getEntity().getUUID());
            if (r.enemies.remove(event.getEntity().getUUID()) | r.outside.remove(event.getEntity().getUUID())) {
                r.spawned.remove(event.getEntity().getUUID());
                level.getScoreboard().removePlayerFromTeam(event.getEntity().getStringUUID());
                data.setDirty();
            }
        }
    }

    static void close(MinecraftServer server, RiftData data, Rift r) {
        ServerLevel arena = server.getLevel(ARENA);
        if (arena == null) return;
        removePortal(server.overworld(), r, false);
        removePortal(arena, r, true);
        forceArena(arena, r, false);
        RiftBosses.clearBar(r);
        r.dormant = true;
        r.spawned.clear();
        r.dormantTicks = Config.RIFT_DORMANT_SECONDS.get() * 20;
        data.setDirty();
        announce(server, "Rift at " + coordinates(r.pos) + " went dormant. It can reopen after " + Config.RIFT_DORMANT_SECONDS.get() + " seconds if its chunk is critical again.");
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
                if (data.activeCount() >= Config.MAX_RIFTS.get()) { c.getSource().sendFailure(Component.literal("Maximum active rifts reached.")); return 0; }
                BlockPos pos = BlockPos.containing(c.getSource().getPosition());
                if (!safe(world, pos) || !RiftPlacement.available(data, pos)) {
                    c.getSource().sendFailure(Component.literal("Stand on solid ground with clear space, more than 63 chunks horizontally from every existing rift (including dormant rifts).")); return 0;
                }
                create(server, pos); return 1;
            }))
            .then(Commands.literal("status").executes(c -> {
                RiftData data = RiftData.get(c.getSource().getServer());
                c.getSource().sendSuccess(() -> Component.literal("Active rifts: " + data.activeCount() + " | Dormant: " + (data.rifts.size() - data.activeCount())), false);
                for (Rift r : data.rifts) c.getSource().sendSuccess(() -> Component.literal("#" + r.id + " at " + coordinates(r.pos) + " | " + (r.dormant ? "DORMANT " + (r.dormantTicks + 19) / 20 + "s" : r.cleared ? "CLEARED — awaiting exit" : r.remaining == 0 ? "BROKEN" : (r.remaining + 19) / 20 + "s") + " | level " + r.level() + " | power " + (Math.round(r.essence * 100) / 100.0) + " | bank " + Math.round(r.energy) + " | arena: " + (r.built ? r.enemies.size() + " enemies" : "unvisited")), false);
                return 1;
            }))
            .then(Commands.literal("roster").then(Commands.argument("id",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0)).executes(c -> {
                int id=com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"id");
                var r=RiftData.get(c.getSource().getServer()).rifts.stream().filter(v->v.id==id).findFirst().orElse(null);
                if (r==null) { c.getSource().sendFailure(Component.literal("Unknown rift.")); return 0; }
                c.getSource().sendSuccess(()->Component.literal("Rift #"+id+" | "+r.sourceBiome+" | "+r.theme+" | normal weights: "+r.roster),false);
                return 1;
            })))
            .then(Commands.literal("leave").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                if (p.level().dimension() != ARENA) { c.getSource().sendFailure(Component.literal("You are not inside a rift.")); return 0; }
                RiftData data = RiftData.get(p.server);
                Rift nearest = data.rifts.stream().min(java.util.Comparator.comparingDouble(r -> p.distanceToSqr(r.arena().getX(), 81, 0))).orElse(null);
                if (nearest != null) nearest.occupants.remove(p.getUUID());
                travel(p, p.server.overworld(), nearest == null ? p.server.overworld().getSharedSpawnPos().above() : nearest.pos);
                return 1;
            })));
    }
}
