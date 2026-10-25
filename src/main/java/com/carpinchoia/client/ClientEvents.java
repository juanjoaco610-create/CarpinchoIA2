package com.carpinchoia.client;

import com.carpinchoia.CarpinchoIA;
import com.carpinchoia.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CarpinchoIA.ID, value = Dist.CLIENT)
public class ClientEvents {
    private static final ResourceLocation STEP = new ResourceLocation(CarpinchoIA.ID, "carpincho_step");
    private static final ResourceLocation ALERT = new ResourceLocation(CarpinchoIA.ID, "carpincho_alert");

    /** Botón "Carpincho" en Opciones (junto a Sonido/Gráficos). */
    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post e) {
        if (e.getScreen() instanceof OptionsScreen screen) {
            e.addListener(Button.builder(Component.literal("Carpincho..."),
                b -> Minecraft.getInstance().setScreen(new CarpinchoOptionsScreen(screen)))
                .bounds(screen.width / 2 - 100, screen.height - 52, 200, 20).build());
        }
    }

    /** Aplica el volumen configurado a pasos y alertas del carpincho. */
    @SubscribeEvent
    public static void onSound(PlaySoundEvent e) {
        SoundInstance s = e.getSound();
        if (s == null) return;
        double v;
        if (s.getLocation().equals(STEP)) v = Config.STEP_VOLUME.get();
        else if (s.getLocation().equals(ALERT)) v = Config.ALERT_VOLUME.get();
        else return;
        if (v <= 0) { e.setSound(null); return; }
        if (v >= 1) return;
        e.setSound(new SimpleSoundInstance(s.getLocation(), s.getSource(), s.getVolume() * (float) v, s.getPitch(),
            RandomSource.create(), false, 0, SoundInstance.Attenuation.LINEAR, s.getX(), s.getY(), s.getZ(), false));
    }
}
