/*
 * Copyright © 2021 antD97
 * Licensed under the MIT License https://antD.mit-license.org/
 */
package com.antd.railtransportplus.mixin;

import com.antd.railtransportplus.CartVisualState;
import com.antd.railtransportplus.interfaceinject.RtpAbstractMinecartEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MinecartEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.MinecartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecartEntityRenderer.class)
public abstract class MinecartEntityRendererMixin<T extends AbstractMinecartEntity> extends EntityRenderer<T> {

    protected MinecartEntityRendererMixin(EntityRendererFactory.Context ctx, EntityModel<T> model2) {
        super(ctx);
    }

    private static final Identifier FRONT_TEXTURE =
            new Identifier("railtransportplus", "textures/entity/front.png");
    private static final Identifier FRONT_BOOST_TEXTURE =
            new Identifier("railtransportplus", "textures/entity/front_boost.png");
    private static final Identifier FRONT_BOOST_TAILLESS_TEXTURE =
            new Identifier("railtransportplus", "textures/entity/front_tailless_boost.png");
    private static final Identifier TRAILING_TEXTURE =
            new Identifier("railtransportplus", "textures/entity/trailing.png");
    private static final Identifier TRAILING_BOOST_TEXTURE =
            new Identifier("railtransportplus", "textures/entity/trailing_boost.png");

    private EntityModel<T> model2;

/* ----------------------------------------------------- Inject ----------------------------------------------------- */

    @Inject(at = @At("RETURN"), method = "<init>")
    public void constructor(EntityRendererFactory.Context ctx, EntityModelLayer layer, CallbackInfo ci) {
        this.model2 = new MinecartEntityModel(ctx.getPart(layer));
    }

    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V",
                    ordinal = 1
            ),
            method = "render(Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;" +
                    "FFLnet/minecraft/client/util/math/MatrixStack;" +
                    "Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
    )
    public void render(T abstractMinecartEntity, float f, float g, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {

        final var cartType =
                ((RtpAbstractMinecartEntity) abstractMinecartEntity).railtransportplus$getVisualState();

        if (cartType != CartVisualState.REGULAR) {

            matrixStack.scale(1.01F, 1.01F, 1.01F);
            VertexConsumer vertexConsumer = null;

            switch (cartType) {
                case FRONT -> vertexConsumer = vertexConsumerProvider
                        .getBuffer(this.model2.getLayer(FRONT_TEXTURE));
                case FRONT_BOOST -> vertexConsumer = vertexConsumerProvider
                        .getBuffer(this.model2.getLayer(FRONT_BOOST_TEXTURE));
                case FRONT_TAILLESS_BOOST -> vertexConsumer = vertexConsumerProvider
                        .getBuffer(this.model2.getLayer(FRONT_BOOST_TAILLESS_TEXTURE));
                case TRAILING -> vertexConsumer = vertexConsumerProvider
                        .getBuffer(this.model2.getLayer(TRAILING_TEXTURE));
                case TRAILING_BOOST -> vertexConsumer = vertexConsumerProvider
                        .getBuffer(this.model2.getLayer(TRAILING_BOOST_TEXTURE));
            }

            this.model2.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV,
                    1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
