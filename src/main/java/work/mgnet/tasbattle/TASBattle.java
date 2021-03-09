package work.mgnet.tasbattle;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.ByteBufUtil;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.WorldBorderS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.apache.commons.io.FileUtils;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.exceptions.InvalidConfigurationException;
import work.mgnet.tasbattle.generator.VoidChunkGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TASBattle implements ModInitializer {

    public static int tickrate = 20;
    public static int gameTickrate = 20;
    public static final Identifier tickrateChannel = new Identifier("tickratechanger");
    public static final Identifier atmoschannel = new Identifier("atmosphere");
    public static YamlFile config;
    public static String map = "TheNile";
    public static String kit = "tactical";

    public static boolean rl = false;

    public static boolean isRunning;
    public static ArrayList<ServerPlayerEntity> players = new ArrayList<>();

    public static ArrayList<ChunkPos> chunks = new ArrayList<>();

    public static StatsUtils stats;

    public static RegistryKey<World> worldReg = RegistryKey.of(Registry.DIMENSION, new Identifier("arena", "void"));
    public static boolean isNether;
    public static boolean hideParticles;

    @Override
    public void onInitialize() {
        // Load Config
        config = new YamlFile("config.yml");
        try {
            config.createNewFile(false);
            config.load();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stats = new StatsUtils();
        if (!new File("statistics/").exists()) new File("statistics/").mkdir();
        try {
            stats.loadStats(new File("statistics/"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        gameTickrate = config.getInt("tickrate");

        Registry.register(Registry.CHUNK_GENERATOR, new Identifier("arena", "void"), VoidChunkGenerator.CODEC);

        // Add Tickrate Command
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("ticks").requires((serverCommandSource -> {
                        return serverCommandSource.hasPermissionLevel(2);
                    })).then(literal("set").then(argument("tickrate", IntegerArgumentType.integer())
                            .executes(context -> {
                                context.getSource().sendFeedback(new LiteralText("§b» §7" + "Tickrate has been set to " + getInteger(context, "tickrate")), true);
                                tickrate = getInteger(context, "tickrate");
                                PacketByteBuf buf = PacketByteBufs.create();
                                buf.writeInt(tickrate);
                                for (ServerPlayerEntity user : context.getSource().getMinecraftServer().getPlayerManager().getPlayerList()) {
                                    ServerPlayNetworking.send(user, tickrateChannel, buf);
                                }
                                return 1;
                            })
                    )).then(literal("save").then(argument("tickrate", IntegerArgumentType.integer())
                    .executes(context -> {
                        context.getSource().sendFeedback(new LiteralText("§b» §7" + "Game Tickrate has been updated to " + getInteger(context, "tickrate")), true);
                        gameTickrate = getInteger(context, "tickrate");
                        config.set("tickrate", gameTickrate);
                        try {
                            config.save();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return 1;
                    })
            )));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("kit").requires(serverCommandSource -> {
                return serverCommandSource.hasPermissionLevel(2);
            }).then(literal("save").then(argument("kitname", StringArgumentType.string()).executes(context -> {
                if (!new File("kits").exists()) new File("kits").mkdir();
                try {
                    InventoryManaging.saveInventory(StringArgumentType.getString(context, "kitname"), context.getSource().getPlayer());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 1;
            }))).then(literal("load").then(argument("kitname", StringArgumentType.string()).executes(context -> {
                if (!new File("kits").exists()) new File("kits").mkdir();
                try {
                    InventoryManaging.loadInventory(StringArgumentType.getString(context, "kitname"), context.getSource().getPlayer());
                    kit = StringArgumentType.getString(context, "kitname");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 1;
            }))).then(literal("list").executes(context -> {
                if (!new File("kits").exists()) new File("kits").mkdir();
                for (File f : new File("kits").listFiles()) {
                    context.getSource().sendFeedback(new LiteralText(f.getName()), false);
                }
                return 1;
            })));
        });



        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("map").requires((serverCommandSource -> {
                return serverCommandSource.hasPermissionLevel(2);
            })).then(literal("reload").executes(context -> {
                reloadMap(context.getSource().getMinecraftServer());
                return 1;
                    })
            ).then(literal("set").then(argument("mapname", StringArgumentType.string()).executes(context -> {
                        map = StringArgumentType.getString(context, "mapname");
                        try {
                            reloadMap(context.getSource().getMinecraftServer());
                        } catch (Exception e) {

                        }
                        return 1;
            }))));
        });

        ServerPlayNetworking.registerGlobalReceiver(new Identifier("version"), new ServerPlayNetworking.PlayChannelHandler() {
            @Override
            public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
                System.out.println(player.getName().getString() + " joined with version " + buf.readByte());
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("top")
                    .executes((context) -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        ArrayList<StatsUtils.Stats> statistics = new ArrayList<>(stats.stats);

                        Collections.sort(statistics, (obj1, obj2) -> {
                            int score1 = obj1.points;
                            int score2 = obj2.points;
                            return score2 - score1;
                        });
                        int index = 1;
                        try {for (StatsUtils.Stats s : statistics.subList(0, 5)) {
                            player.sendMessage(new LiteralText("§b» §7Rank " + index + " (§b" + player.getServer().getUserCache().getByUuid(s.uuid).getName() + "§7): §e" + s.points + " Points"), false);
                            index++;
                        }} catch (Exception e) {
                            for (StatsUtils.Stats s : statistics) {
                                player.sendMessage(new LiteralText("§b» §7Rank " + index + " (§b" + player.getServer().getUserCache().getByUuid(s.uuid).getName() + "§7): §e" + s.points + " Points"), false);
                                index++;
                            }
                        }
                        return 1;
                    })
            );
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("stats")
                            .executes((context) -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                try {
                                    StatsUtils.Stats stats2 = stats.getStats(player.getUuid());
                                    player.sendMessage(new LiteralText("§b» §7Showing Stats of " + player.getName().getString()), false);
                                    player.sendMessage(new LiteralText("§b» §7Kills: §b" + stats2.kills), false);
                                    player.sendMessage(new LiteralText("§b» §7Deaths: §b" + stats2.deaths), false);
                                    Double kd = (double) stats2.kills / (double) stats2.deaths;
                                    String kdStr = kd.toString().length() >= 5 ? kd.toString().substring(0, 5) : kd.toString();
                                    player.sendMessage(new LiteralText("§b» §7K/D: §b" + kdStr), false);
                                    player.sendMessage(new LiteralText("§b» §7Games played: §b" + stats2.games), false);
                                    player.sendMessage(new LiteralText("§b» §7Games won: §b" + stats2.gamesWon), false);
                                    Double winChance = (double) stats2.gamesWon / (double) stats2.games * 100;
                                    String winChanceStr = winChance.toString().length() >= 5 ? winChance.toString().substring(0, 5) : winChance.toString();
                                    player.sendMessage(new LiteralText("§b» §7Win chance: §b" + winChanceStr + "%"), false);
                                    player.sendMessage(new LiteralText("§b» §7Points: §b" + stats2.points), false);
                                } catch (ArithmeticException e1) {
                                    player.sendMessage(new LiteralText("§b» §7Not enough data!"), false);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    player.sendMessage(new LiteralText("§b» §7Couldn't show stats!"), false);
                                }
                                return 1;
                            })
            );
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("startworldborder").requires((serverCommandSource -> {
                        return serverCommandSource.hasPermissionLevel(2);
                    }))
                            .executes((context) -> {
                                if (!isRunning) context.getSource().sendFeedback(new LiteralText("§b» §7" + "The Game is currently not running"), false);
                                else {
                                    WorldBorder border = context.getSource().getMinecraftServer().getWorld(worldReg).getWorldBorder();
                                    context.getSource().getMinecraftServer().getPlayerManager().sendToAll(new WorldBorderS2CPacket(border, WorldBorderS2CPacket.Type.LERP_SIZE));
                                    border.setCenter(0, 0);
                                    border.setSize(500);
                                    border.interpolateSize(500, 5, 900000);
                                }
                                return 1;
                            })
            );
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("hideparticles").requires((serverCommandSource -> {
                        return serverCommandSource.hasPermissionLevel(2);
                    }))
                            .executes((context) -> {
                                hideParticles = !hideParticles;
                                context.getSource().sendFeedback(new LiteralText("§b» §7" + (hideParticles ? "Some Particles are Invisible now" : "Some Particles are visible again.")), false);
                                return 1;
                            })
            );
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("forcestart").requires((serverCommandSource -> {
                        return serverCommandSource.hasPermissionLevel(2);
                    }))
                    .executes((context) -> {
                        if (isRunning) context.getSource().sendFeedback(new LiteralText("§b» §7" + "The Game is currently running"), false);
                        else {
                            players.clear();
                            players.addAll(context.getSource().getMinecraftServer().getPlayerManager().getPlayerList());
                            try {
                                startGame(context.getSource());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return 1;
                    })
            );
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("forceend").requires((serverCommandSource -> {
                        return serverCommandSource.hasPermissionLevel(2);
                    }))
                    .executes((context) -> {
                        if (!isRunning) context.getSource().sendFeedback(new LiteralText("§b» §7" + "The Game is currently not running"), false);
                        else {
                            players.clear();
                            players.addAll(context.getSource().getMinecraftServer().getPlayerManager().getPlayerList());
                            try {
                                stopGame(context.getSource().getPlayer());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return 1;
                    })
            );
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("overworld").requires((serverCommandSource -> {
                return serverCommandSource.hasPermissionLevel(2);
            })).executes(context -> {
                if (!isRunning) {
                    context.getSource().sendFeedback(new LiteralText("§b» §7" + "This Command can only be ran in-game"), false);
                } else {
                    context.getSource().getPlayer().getServerWorld().getDimension().natural = true;
                    context.getSource().getPlayer().getServerWorld().getDimension().bedWorks = true;
                    context.getSource().getPlayer().getServerWorld().getDimension().respawnAnchorWorks = false;
                    context.getSource().getPlayer().getServerWorld().getDimension().ultrawarm = false;
                    isNether = false;
                    for (ServerPlayerEntity e : context.getSource().getPlayer().getServer().getPlayerManager().getPlayerList()) {
                        PacketByteBuf b = PacketByteBufs.create();
                        b.writeString("overworld");
                        ServerPlayNetworking.send(e, atmoschannel, b);
                    }
                }
                return 1;
            }));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("nether").requires((serverCommandSource -> {
                return serverCommandSource.hasPermissionLevel(2);
            })).executes(context -> {
                if (!isRunning) {
                    context.getSource().sendFeedback(new LiteralText("§b» §7" + "This Command can only be ran in-game"), false);
                } else {
                    context.getSource().getPlayer().getServerWorld().getDimension().natural = false;
                    context.getSource().getPlayer().getServerWorld().getDimension().bedWorks = false;
                    context.getSource().getPlayer().getServerWorld().getDimension().respawnAnchorWorks = true;
                    context.getSource().getPlayer().getServerWorld().getDimension().ultrawarm = true;
                    isNether = true;
                    for (ServerPlayerEntity e : context.getSource().getPlayer().getServer().getPlayerManager().getPlayerList()) {
                        PacketByteBuf b = PacketByteBufs.create();
                        b.writeString("nether");
                        ServerPlayNetworking.send(e, atmoschannel, b);
                    }

                }
                return 1;
            }));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("spec").requires((serverCommandSource -> {
                return serverCommandSource.hasPermissionLevel(2);
            })).executes(context -> {
                if (!isRunning) {
                    context.getSource().sendFeedback(new LiteralText("§b» §7" + "The Game is currently not running"), false);
                } else {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    player.setGameMode(GameMode.SPECTATOR);
                    player.inventory.clear();
                    players.remove(player);
                    player.teleport(0, 100, 0);
                    player.setExperienceLevel(0);
                }
                return 1;
            }));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) dispatcher.register(literal("ready")
                .executes((context) -> {
                    if (context.getSource().getMinecraftServer().getCurrentPlayerCount() < 2) {
                        context.getSource().sendFeedback(new LiteralText("§b» §cThere have to be at least 2 players online"), false);
                        return 1;
                    }
                    if (isRunning) context.getSource().sendFeedback(new LiteralText("§b» §7" + "The Game is currently running"), false);
                    else if (players.contains(context.getSource().getPlayer())) {
                        players.remove(context.getSource().getPlayer());
                        for (ServerPlayerEntity user : context.getSource().getMinecraftServer().getPlayerManager().getPlayerList()) {
                            user.sendMessage(new LiteralText("§b» §7" + "§b" + context.getSource().getPlayer().getName().getString() + " §7is no longer ready"), false);
                            user.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 0.5f, 0.5f);
                        }
                        context.getSource().getPlayer().sendMessage(new LiteralText("§b» §7" + "§cYou are no longer ready"), true);
                    } else {
                        players.add(context.getSource().getPlayer());
                        for (ServerPlayerEntity user : context.getSource().getMinecraftServer().getPlayerManager().getPlayerList()) {
                            user.sendMessage(new LiteralText("§b» §7" + "§b" + context.getSource().getPlayer().getName().getString() + " §7is now ready!"), false);
                            user.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.0f);
                        }
                        context.getSource().getPlayer().sendMessage(new LiteralText("§b» §7" + "§aYou are now ready"), true);
                        if (players.size() >= (context.getSource().getMinecraftServer().getPlayerManager().getPlayerList().size())) {
                            try {
                                startGame(context.getSource());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return 1;
                })
            );
        });
    }

    public static void reloadMap(MinecraftServer server) {
        File source = new File("world/dimensions/" + map);
        if (!source.exists()) {
            return;
        }
        try {final ServerWorld world = server.getWorld(worldReg);
            world.getWorldBorder().setSize(500);
        for (ServerPlayerEntity remove : world.getPlayers()) {
            remove.teleport(world.getServer().getOverworld(), 0, 100, 0, 0, 0);
        }
        DimensionType dimensiontype = world.getDimension();
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        try {
            world.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File target = new File("world/dimensions/arena");
        target.delete();
        FileUtils.copyDirectory(source, target);

        server.worlds.remove(worldReg);
        ServerWorld redoWorld = new ServerWorld(server, server.workerExecutor, server.session, server.saveProperties.getMainWorldProperties(), worldReg, dimensiontype, server.worldGenerationProgressListenerFactory.create(11), generator, false, 0L, ImmutableList.of(), false);
        server.worlds.put(worldReg, redoWorld);
        for (Entity entity : redoWorld.iterateEntities()) {
            redoWorld.removeEntity(entity);
        } } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startGame(ServerCommandSource source) throws CommandSyntaxException, IOException {
        if (isRunning) return;

        // Set World Stuff
        source.getMinecraftServer().setDifficulty(Difficulty.HARD, true);

        tickrate = gameTickrate;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(tickrate);
        for (ServerPlayerEntity user : source.getMinecraftServer().getPlayerManager().getPlayerList()) {
            user.teleport(source.getMinecraftServer().getWorld(worldReg), 0, 100, 0, 0, 0);
        }
        source.getMinecraftServer().getWorld(worldReg).getWorldBorder().setSize(500);
        source.getMinecraftServer().getPlayerManager().sendToAll(new WorldBorderS2CPacket(source.getMinecraftServer().getWorld(worldReg).getWorldBorder(), WorldBorderS2CPacket.Type.SET_SIZE));

        // Spread Players
        SpreadplayersTheft.spread(source.getMinecraftServer().getCommandSource(), source.getMinecraftServer().getPlayerManager().getPlayerList(), source.getMinecraftServer().getWorld(worldReg));

        // Set Player Stuff
        for (ServerPlayerEntity p : players) {
            p.setGameMode(GameMode.SURVIVAL);
            p.clearStatusEffects();
            p.setExperienceLevel(0);
            stats.updateStats(p, 0, 0, 1, 0);
            stats.saveStats();
            p.getHungerManager().setFoodLevel(20);
            p.setHealth(20);
            InventoryManaging.loadInventory(kit, p);
            p.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
            p.sendMessage(new LiteralText("§b» §7" + "The Game has begun. Kill everyone to win"), false);
            ServerPlayNetworking.send(p, tickrateChannel, buf);
            PacketByteBuf b2 = PacketByteBufs.create();
            b2.writeString(isNether ? "nether" : "overworld");
            ServerPlayNetworking.send(p, atmoschannel, b2);
        }
        isRunning = true;
    }

    public static void stopGame(ServerPlayerEntity player) throws IOException {
        if (!isRunning) return;
        // Clear Player List
        players.clear();

        // World stuff
        player.getServer().setDifficulty(Difficulty.PEACEFUL, true);

        tickrate = 20;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(tickrate);
        for (ServerPlayerEntity user : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(user, tickrateChannel, buf);
        }

        // Set Player Stuff
        for (ServerPlayerEntity p : player.getCommandSource().getMinecraftServer().getPlayerManager().getPlayerList()) {
            p.setGameMode(GameMode.ADVENTURE);
            p.clearStatusEffects();
            p.inventory.clear();
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 255, false, false, false));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 255, false, false, false));
            p.sendMessage(new LiteralText("§b» §7" + "§eThe Game has ended."), false);
            p.sendMessage(new LiteralText("§b» §7" + "§7Type §a/ready§7 once you are ready."), true);
            p.teleport(0, 100, 0);
            p.setExperienceLevel(0);
            p.playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST_FAR, SoundCategory.MASTER, 1.0f, 1.0f);
            p.getHungerManager().setFoodLevel(20);
            p.setHealth(20);
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                for (ServerPlayerEntity user : player.getServer().getPlayerManager().getPlayerList()) {
                    user.teleport(player.getServer().getOverworld(), 0, 100, 0, 0, 0);
                    user.clearStatusEffects();
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 255, false, false, false));
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 255, false, false, false));
                }
            }
        }, 2000L);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                rl = true;
            }
        }, 3000L);

        isRunning = false;
    }

    public static void playerJoin(ServerPlayerEntity p) {
        p.clearStatusEffects();
        p.inventory.clear();
        p.setExperienceLevel(0);
        p.getHungerManager().setFoodLevel(20);
        p.setHealth(20);

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(tickrate);
        ServerPlayNetworking.send(p, tickrateChannel, buf);

        if (isRunning) {
            p.setGameMode(GameMode.SPECTATOR);
            p.teleport(p.getServer().getWorld(worldReg), 0, 100, 0, 0, 0);
            p.sendMessage(new LiteralText("§b» §7" + "§7A Game is currently running."), true);
        } else {
            p.setGameMode(GameMode.ADVENTURE);
            p.teleport(p.getServer().getOverworld(), 0, 100, 0, 0, 0);
            p.teleport(0, 100, 0);
            p.sendMessage(new LiteralText("§b» §7" + "§7Type §a/ready§7 once you are ready."), true);
        }
    }

    public static void playerDeath(ServerPlayerEntity player, double x, double y, double z) throws IOException {
        if (!isRunning) return;
        stats.updateStats(player, 0, 1, 0, 0);
        player.setGameMode(GameMode.SPECTATOR);
        player.inventory.clear();
        players.remove(player);
        player.teleport(x, y, z);
        player.setExperienceLevel(0);

        LivingEntity killer = player.getAttacker();
        if (killer != null) {
            if (killer instanceof ServerPlayerEntity) {
                stats.updateStats((ServerPlayerEntity) killer, 1, 0, 0, 0);
                RankingUtils.onKill((ServerPlayerEntity) killer, player);
            }
        } else {
            RankingUtils.onDeath(player);
        }
        String deathMessage = "§b» §b" + player.getName().getString() + (killer == null ? "§7 died." :"§7 was killed by §b" + killer.getName().getString() + "§7.");
        for (ServerPlayerEntity p : player.getCommandSource().getMinecraftServer().getPlayerManager().getPlayerList()) {
            p.sendMessage(new LiteralText(deathMessage), false);
        }
        if (players.size() == 1) {
            stats.updateStats(players.get(0), 0, 0, 0, 1);
            RankingUtils.onWin(players.get(0));
            for (ServerPlayerEntity p : player.getCommandSource().getMinecraftServer().getPlayerManager().getPlayerList()) {
                p.sendMessage(new LiteralText("§b» §e" + players.get(0).getName().getString() + " won!"), false);
            }
            stopGame(player);
        }
        stats.saveStats();
    }

}
