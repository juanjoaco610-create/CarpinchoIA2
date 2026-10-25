package com.carpinchoia.client;

import com.carpinchoia.CarpinchoIA;
import com.carpinchoia.entity.CarpinchoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CarpinchoRenderer extends MobRenderer<CarpinchoEntity, CarpinchoModel> {
    private static final ResourceLocation MALE = new ResourceLocation(CarpinchoIA.ID, "textures/entity/carpincho.png");
    private static final ResourceLocation FEMALE = new ResourceLocation(CarpinchoIA.ID, "textures/entity/carpincha.png");

    public CarpinchoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CarpinchoModel(ctx.bakeLayer(CarpinchoModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CarpinchoEntity e) { return e.isFemale() ? FEMALE : MALE; }
}
