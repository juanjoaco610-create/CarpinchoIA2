package com.carpinchoia;

import com.carpinchoia.client.ClientHooks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Net {
    private static final String V = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(CarpinchoIA.ID, "main"), () -> V, V::equals, V::equals);

    public static class TotemPacket {}

    public static void init() {
        CH.registerMessage(0, TotemPacket.class, (p, b) -> {}, b -> new TotemPacket(), (p, ctx) -> {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::totem));
            ctx.get().setPacketHandled(true);
        });
    }

    public static void totem(ServerPlayer p) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), new TotemPacket());
    }
}
