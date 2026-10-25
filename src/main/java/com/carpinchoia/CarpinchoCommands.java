package com.carpinchoia;

import com.carpinchoia.entity.CarpinchoEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = CarpinchoIA.ID)
public class CarpinchoCommands {

    static List<CarpinchoEntity> find(MinecraftServer s, String name) {
        List<CarpinchoEntity> r = new ArrayList<>();
        for (ServerLevel l : s.getAllLevels())
            for (CarpinchoEntity c : l.getEntities(EntityTypeTest.forClass(CarpinchoEntity.class), e -> true))
                if (name == null || c.getName().getString().equalsIgnoreCase(name)) r.add(c);
        return r;
    }

    static RequiredArgumentBuilder<CommandSourceStack, String> nameArg() {
        return Commands.argument("nombre", StringArgumentType.string()).suggests((ctx, b) -> {
            Set<String> n = new LinkedHashSet<>();
            for (CarpinchoEntity c : find(ctx.getSource().getServer(), null)) n.add(c.getName().getString());
            return SharedSuggestionProvider.suggest(n, b);
        });
    }

    static boolean canEdit(CommandSourceStack src, CarpinchoEntity c) {
        return src.hasPermission(2) || (src.getEntity() instanceof net.minecraft.world.entity.player.Player p && c.isOwner(p));
    }

    interface Action { void run(CommandSourceStack src, CarpinchoEntity c) throws Exception; }

    static int each(CommandSourceStack src, String name, boolean needEdit, Action a) {
        List<CarpinchoEntity> l = find(src.getServer(), name);
        if (l.isEmpty()) { src.sendFailure(Component.literal("No encontré ningún carpincho con ese nombre.")); return 0; }
        int n = 0;
        for (CarpinchoEntity c : l) {
            if (needEdit && !canEdit(src, c)) continue;
            try { a.run(src, c); n++; } catch (Exception ex) { src.sendFailure(Component.literal("Error: " + ex.getMessage())); }
        }
        if (n == 0) src.sendFailure(Component.literal("No tenés permiso sobre ese carpincho."));
        return n;
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();

        // /carpinchotp [nombre]  (solo admin)
        d.register(Commands.literal("carpinchotp").requires(s -> s.hasPermission(2))
            .executes(c -> tp(c.getSource(), null))
            .then(nameArg().executes(c -> tp(c.getSource(), StringArgumentType.getString(c, "nombre")))));

        // /momentocarpincho [nombre]  (solo admin)
        d.register(Commands.literal("momentocarpincho").requires(s -> s.hasPermission(2))
            .executes(c -> momento(c.getSource(), null))
            .then(nameArg().executes(c -> momento(c.getSource(), StringArgumentType.getString(c, "nombre")))));

        // /carpinchoeliminar <nombre>  (solo admin)
        d.register(Commands.literal("carpinchoeliminar").requires(s -> s.hasPermission(2))
            .then(nameArg().executes(c -> each(c.getSource(), StringArgumentType.getString(c, "nombre"), false, (src, car) -> {
                car.discard();
                src.sendSuccess(() -> Component.literal("§cCarpincho eliminado."), true);
            }))));

        // /carpincho <nombre> dueno add|quitar <jugador> | zona <pos> <radio> | alerta on|off
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("carpincho");
        root.then(nameArg()
            .then(Commands.literal("dueno")
                .then(Commands.literal("add").then(Commands.argument("jugador", EntityArgument.player())
                    .executes(c -> {
                        ServerPlayer p = EntityArgument.getPlayer(c, "jugador");
                        return each(c.getSource(), StringArgumentType.getString(c, "nombre"), true, (s, car) -> {
                            car.addOwner(p.getUUID());
                            s.sendSuccess(() -> Component.literal("§a" + p.getName().getString() + " ahora es dueño."), false);
                        });
                    })))
                .then(Commands.literal("quitar").then(Commands.argument("jugador", EntityArgument.player())
                    .executes(c -> {
                        ServerPlayer p = EntityArgument.getPlayer(c, "jugador");
                        return each(c.getSource(), StringArgumentType.getString(c, "nombre"), true, (s, car) -> {
                            car.removeOwner(p.getUUID());
                            s.sendSuccess(() -> Component.literal("§e" + p.getName().getString() + " ya no es dueño."), false);
                        });
                    }))))
            .then(Commands.literal("zona").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("radio", IntegerArgumentType.integer(1, 64)).executes(c -> {
                    var pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
                    int r = IntegerArgumentType.getInteger(c, "radio");
                    return each(c.getSource(), StringArgumentType.getString(c, "nombre"), true, (s, car) -> {
                        car.setZone(pos, r);
                        s.sendSuccess(() -> Component.literal("§aZona: " + pos.toShortString() + " radio " + r), false);
                    });
                }))))
            .then(Commands.literal("alerta")
                .then(Commands.literal("on").executes(c -> alerta(c.getSource(), StringArgumentType.getString(c, "nombre"), true)))
                .then(Commands.literal("off").executes(c -> alerta(c.getSource(), StringArgumentType.getString(c, "nombre"), false))))
        );
        d.register(root);
    }

    static int alerta(CommandSourceStack src, String name, boolean on) {
        return each(src, name, true, (s, car) -> {
            car.setAlerts(on);
            s.sendSuccess(() -> Component.literal("Alerta de base: " + (on ? "§aACTIVADA" : "§cDESACTIVADA")), false);
        });
    }

    static int tp(CommandSourceStack src, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        return each(src, name, false, (s, c) -> {
            c.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), Set.of(), p.getYRot(), p.getXRot());
            c.getNavigation().stop();
            s.sendSuccess(() -> Component.literal("§a" + c.getName().getString() + " teletransportado hacia vos."), false);
        });
    }

    static int momento(CommandSourceStack src, String name) {
        return each(src, name, false, (s, c) -> {
            c.startMomento();
            if (s.getEntity() instanceof ServerPlayer sp && !c.isOwner(sp)) Net.totem(sp);
            s.sendSuccess(() -> Component.literal("§6Momento Carpincho activado para " + c.getName().getString()), true);
        });
    }
}
