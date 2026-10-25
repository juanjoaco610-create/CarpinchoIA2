package com.carpinchoia.client;

import com.carpinchoia.CarpinchoIA;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CarpinchoIA.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(CarpinchoModel.LAYER, CarpinchoModel::createBodyLayer);
    }
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(CarpinchoIA.CARPINCHO.get(), CarpinchoRenderer::new);
        e.registerEntityRenderer(CarpinchoIA.CARPINCHA.get(), CarpinchoRenderer::new);
    }
}
