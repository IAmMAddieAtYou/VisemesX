package net.visemesx.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.visemesx.VisemesX;
import net.visemesx.audio.VisemeMapper;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class VisemeRenderer<T extends LivingEntity, M extends EntityModel<T> & ModelWithHead> extends FeatureRenderer<T, M> {
    private final ModelPart base;
    private final ModelPartData data;

    public VisemeRenderer(FeatureRendererContext<T, M> context) {
        super(context);

        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        data = modelPartData;
        ModelPartBuilder modelPartBuilder  = ModelPartBuilder.create();
        List<ModelCuboidData> cuboidData = modelPartBuilder.cuboidData;
        Set<Direction> set = Set.of();
        cuboidData.add(new CustomModelCuboidData("mouthm",0,0,-4.0F, -7.8F, 4.54F, 8, 8, 8,new Dilation(0.0f),true,1,1, set));


        modelPartData.addChild("mouth",modelPartBuilder,ModelTransform.rotation(0,0,0));

        TexturedModelData texturedModelData = TexturedModelData.of(modelData, 640, 640);


        this.base = texturedModelData.createModel().getChild("mouth");
        System.out.println("[VisemesX] Render Initialized.");
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        ModelPart head = getContextModel().getHead();
        // Draw a 128x128 cube
        float size = 640 / 16f; // Convert pixels to Minecraft units
        float offset = size / 2;

        // Get the current viseme

        String currentViseme = VisemesX.getInstance().getVisemeMapper().currentviseme;
        if(Objects.equals(currentViseme, "default")){currentViseme = "closed";}
        // Map visemes to colors
        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;


        switch (currentViseme) {
            case "closed":
                red = 1.0f;
                green = 0.0f;
                blue = 0.0f; // Red
                break;
            case "teeth":
                red = 0.0f;
                green = 1.0f;
                blue = 0.0f; // Green
                break;
            case "tongue":
                red = 0.0f;
                green = 0.0f;
                blue = 1.0f; // Blue
                break;
            case "smile":
                red = 1.0f;
                green = 1.0f;
                blue = 0.0f; // Yellow
                break;
            case "round":
                red = 1.0f;
                green = 0.0f;
                blue = 1.0f; // Magenta
                break;
            case "open":
                red = 0.0f;
                green = 1.0f;
                blue = 1.0f; // Cyan
                break;
            case "neutral":
                red = 0.5f;
                green = 0.5f;
                blue = 0.5f; // Grey
                break;
            default:
                red = 1.0f;
                green = 1.0f;
                blue = 1.0f; // White
                break;
        }




        RenderLayer renderLayer = RenderLayer.getEntityTranslucent(Identifier.of(VisemesX.MOD_ID, "textures/" + currentViseme +".png"));

        VertexConsumer vertices = vertexConsumers.getBuffer(renderLayer);
        //vertices.color(red,green,blue,1f);

        float hoffsetScale = 250.0f;
        // make changes to the matrix


        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));

        matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(-head.yaw));
        matrices.multiply(RotationAxis.NEGATIVE_X.rotation(head.pitch));
        matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(-head.roll));

        //matrices.translate(-hoffsetScale, hoffsetScale, hoffsetScale);
        matrices.scale(1, 1, 1);
        if(entity.isSneaking()) {
            matrices.translate(0, .26, 0);
        }
        // render
        //System.out.println(this.base);
        this.base.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV);

        matrices.scale(1, 1, 1);

        //matrices.scale(1.0f, 1.0f, 1.0f);
        //matrices.translate(( -hoffsetScale) * -1.0f, (hoffsetScale) * -1.0f, (hoffsetScale) * -1.0f);

        matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(head.roll));
        matrices.multiply(RotationAxis.NEGATIVE_X.rotation(-head.pitch));
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(head.yaw));

        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(-180.0F));

        //matrices.translate(0, 0, 0.03);

        //System.out.println("[VisemesX] "+ currentViseme+" Render Rendered.");
    }





}