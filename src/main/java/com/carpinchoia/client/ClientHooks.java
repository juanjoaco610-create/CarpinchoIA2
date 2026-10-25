package com.carpinchoia.client;

import com.carpinchoia.CarpinchoIA;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public class ClientHooks {
    /** Animación del tótem, pero con la cara del carpincho (ítem carpincho_totem). */
    public static void totem() {
        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.displayItemActivation(new ItemStack(CarpinchoIA.TOTEM.get()));
        if (mc.player != null && mc.level != null)
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1F, 1F, false);
    }
}
