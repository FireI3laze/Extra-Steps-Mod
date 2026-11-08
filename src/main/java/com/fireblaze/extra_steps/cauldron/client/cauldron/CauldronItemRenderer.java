package com.fireblaze.extra_steps.cauldron.client.cauldron;

import com.fireblaze.extra_steps.cauldron.client.ClientCauldronCache;
import com.fireblaze.extra_steps.config.ModConfigHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class CauldronItemRenderer {

    public static void render(BlockPos pos, PoseStack poseStack, MultiBufferSource buffer) {

        NonNullList<ItemStack> inv = ClientCauldronCache.getInventory(pos);
        if (inv == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        ItemRenderer itemRenderer = mc.getItemRenderer();

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition();

        int size = 0;
        for (ItemStack s : inv) {
            if (!s.isEmpty()) size++;
        }

        if (size == 0) return;

        int index = 0;

        // 🧪 Waterlevel (0–3 angenommen; falls du hast: ersetzen!)
        int waterLevel = 0;

        boolean isEmptyCauldron = true;

        var state = mc.level.getBlockState(pos);

        if (state.hasProperty(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL)) {
            waterLevel = state.getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL);

            isEmptyCauldron = false;
        }

        float waterFactor = 0.3f + (waterLevel * 0.22f);

        float radius = 0.35f;
        float angleStep = (float) (Math.PI * 2 / size);

        for (ItemStack stack : inv) {
            if (stack.isEmpty()) continue;

            float angle = index * angleStep;

            float xOffset = (float) Math.cos(angle) * radius;
            float zOffset = (float) Math.sin(angle) * radius;

            poseStack.pushPose();

            // 🌊 WORLD -> CAMERA space
            poseStack.translate(
                    pos.getX() + 0.5 - cam.x + xOffset / 1.5,
                    pos.getY() + waterFactor - cam.y + ((double) (index % 2) / 100),
                    pos.getZ() + 0.5 - cam.z + zOffset / 1.5
            );

            // 📦 scale
            poseStack.scale(0.4f, 0.4f, 0.4f);

            // 🪨 leerer Cauldron → Items liegen flach
            if (isEmptyCauldron) {

                float flatRotation = getFlatRotation(pos, index);

                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90f));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(flatRotation));
            } else {

                float time = (mc.level.getGameTime() + mc.getFrameTime()) * 0.05f;

                float phase = index * 1.7f;

                float bob = (float) Math.sin(time + phase) * 0.03f;

                poseStack.translate(0, bob, 0);

                float seed = hash(pos, index);

                // 🌊 YAW (facing direction)
                float yawBase = seed * 360f;
                float yawWobble = (float)Math.sin(time + seed * 6f) * 10f;

                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yawBase + yawWobble));

                // 🎯 tilt
                float rotXBase = (seed - 0.5f) * 10f;
                float rotZBase = (hash(pos, index + 17) - 0.5f) * 25f;

                float wobbleX = (float)Math.sin(time + seed * 10f) * 5f;
                float wobbleZ = (float)Math.cos(time + seed * 10f) * 5f;

                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotXBase + wobbleX));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotZBase + wobbleZ));
            }

            int skyLight = mc.level.getBrightness(LightLayer.SKY, pos);
            int blockLight = mc.level.getBrightness(LightLayer.BLOCK, pos);

            int packedLight = (skyLight << 20) | (blockLight << 4);

            var model = itemRenderer.getModel(stack, mc.level, mc.player, 0);

            int count = stack.getCount();
            int renderCount = Math.min(4, 1 + (count / 16));


            if (!ModConfigHandler.enableCauldronIngredientStackSizeRendering.get()) {

                itemRenderer.render(
                        stack,
                        ItemDisplayContext.FIXED,
                        false,
                        poseStack,
                        buffer,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        model
                );
            } else {
                boolean isBlock = stack.getItem() instanceof net.minecraft.world.item.BlockItem;

                float seed = hash(pos, index);

                if (isBlock) {

                    poseStack.pushPose();

                    // 1–64 → 0.45 → 0.85 scale (smooth growth)
                    float t = Math.min(count / 64f, 1f);
                    t = t * t; // easing (optional aber nicer)

                    float scale = 0.8f + t * 0.2f;

                    poseStack.scale(scale, scale, scale);

                    // optional: stabiler als Items
                    poseStack.translate(0, 0.02f, 0);

                    itemRenderer.render(
                            stack,
                            ItemDisplayContext.FIXED,
                            false,
                            poseStack,
                            buffer,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            model
                    );

                    poseStack.popPose();
                }

                else {
                    for (int i = 0; i < renderCount; i++) {

                        poseStack.pushPose();
                        float scale = 0.8f;

                        poseStack.scale(scale, scale, scale);
                        float yOffset = i * 0.03f + ((i % 2 == 0) ? 0.05f : -0.05f);

                        poseStack.translate(
                                (seed - 0.5f) * 0.07f,
                                yOffset,
                                (seed - 0.5f) * 0.07f * i * 4
                        );

                        itemRenderer.render(
                                stack,
                                ItemDisplayContext.FIXED,
                                false,
                                poseStack,
                                buffer,
                                packedLight,
                                OverlayTexture.NO_OVERLAY,
                                model
                        );

                        poseStack.popPose();
                    }
                }
            }

            poseStack.popPose();

            index++;
        }
    }

    private static float hash(BlockPos pos, int index) {
        long seed = pos.asLong() ^ (index * 31L);
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        return (seed & 0xFFFF) / 65535f;
    }

    private static float getFlatRotation(BlockPos pos, int index) {
        float v = hash(pos, index); // 0.0 - 1.0

        int step = (int)(v * 2); // 0,1
        return step * 180f;
    }
}