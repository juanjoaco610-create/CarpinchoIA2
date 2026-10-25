package com.carpinchoia.client;

import com.carpinchoia.CarpinchoIA;
import com.carpinchoia.entity.CarpinchoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Carpincho bípedo (2 patas, 2 brazos). Textura 64x64. */
public class CarpinchoModel extends EntityModel<CarpinchoEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CarpinchoIA.ID, "carpincho"), "main");
    private final ModelPart root, head, rightLeg, leftLeg, rightArm, leftArm;

    public CarpinchoModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition h = r.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
            .texOffs(24, 0).addBox(-2, -3, -7, 4, 3, 3), PartPose.ZERO);
        h.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(32, 0).addBox(-1, -2, -0.5F, 2, 2, 1), PartPose.offset(-3, -8, 0));
        h.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(32, 0).addBox(-1, -2, -0.5F, 2, 2, 1), PartPose.offset(3, -8, 0));
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-5, 0, -3, 10, 12, 6), PartPose.ZERO);
        r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-2.5F, 12, 0));
        r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(2.5F, 12, 0));
        r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 10, 4), PartPose.offset(-6, 2, 0));
        r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1, -2, -2, 4, 10, 4), PartPose.offset(6, 2, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(CarpinchoEntity e, float swing, float amount, float age, float yaw, float pitch) {
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        rightLeg.xRot = Mth.cos(swing * 0.6662F) * 1.4F * amount;
        leftLeg.xRot = Mth.cos(swing * 0.6662F + Mth.PI) * 1.4F * amount;
        rightArm.xRot = Mth.cos(swing * 0.6662F + Mth.PI) * 0.8F * amount;
        leftArm.xRot = Mth.cos(swing * 0.6662F) * 0.8F * amount;
        if (e.isMomento()) { rightArm.xRot = -2.2F; leftArm.xRot = -2.2F; } // brazos en alto
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(ps, vc, light, overlay, r, g, b, a);
    }
}
