package com.fireblaze.extra_steps.client.render;

import com.fireblaze.extra_steps.blockentity.LyeWaterCauldronBlockEntity;
import com.fireblaze.extra_steps.config.ModConfigHandler;
import com.fireblaze.extra_steps.util.GenericColorHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LyeWaterCauldronRenderer implements BlockEntityRenderer<LyeWaterCauldronBlockEntity> {

    private final float spacing = 0.12f;
    private final float itemScale = 0.12f;
    private final float maxDistance = 5f; // Items werden nach 5 Blöcken unsichtbar

    public LyeWaterCauldronRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(LyeWaterCauldronBlockEntity cauldron, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = cauldron.getBlockPos();
        var player = mc.player;

        if (player == null) return;

        // Spielerabstand prüfen
        double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distance > ModConfigHandler.maxDistanceColorItemRendering.get() * ModConfigHandler.maxDistanceColorItemRendering.get()) return; // Items unsichtbar

        List<Integer>[] colorLists = cauldron.getUsedColors();
        int maxSlotsPerLevel = 3;
        int totalColors = cauldron.getUsedSlots();
        if (totalColors <= 0) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.95, 0.5);
        poseStack.scale(2f, 2f, 2f);

        // Rotation in Richtung Spieler
        boolean rotateToPlayer = false;
        float targetYaw = cauldron.itemYaw;
        if (distance < 25.0) { // innerhalb Sichtbereich
            rotateToPlayer = true;
            double dx = player.getX() - (pos.getX() + 0.5);
            double dz = player.getZ() - (pos.getZ() + 0.5);
            float rawYaw = (float) Math.toDegrees(Math.atan2(dx, dz));

            // Auf 90°-Schritte runden
            targetYaw = Math.round(rawYaw / 90f) * 90f;
        }

        if (rotateToPlayer) {
            cauldron.itemYaw += Mth.wrapDegrees(targetYaw - cauldron.itemYaw) * 0.1f;
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(cauldron.itemYaw));

        ItemRenderer itemRenderer = mc.getItemRenderer();
        int slotIndex = 0;

        for (int level = 0; level < colorLists.length; level++) {
            List<Integer> levelColors = colorLists[level];

            for (int colorInt : levelColors) {
                poseStack.pushPose();

                int xIndex = slotIndex % maxSlotsPerLevel;
                float xOffset = (xIndex - 1) * spacing;
                int yRow = slotIndex / maxSlotsPerLevel;
                float yOffset = -0.10f - (yRow * 0.08f);
                float zOffset = 0.25f;

                poseStack.translate(xOffset, yOffset, zOffset);
                poseStack.scale(itemScale, itemScale, itemScale);

                ItemStack dyeStack = GenericColorHelper.getDyeItemFromRGB(colorInt);
                BakedModel model = itemRenderer.getModel(dyeStack, cauldron.getLevel(), null, 0);

                itemRenderer.render(
                        dyeStack,
                        net.minecraft.world.item.ItemDisplayContext.FIXED,
                        false,
                        poseStack,
                        buffer,
                        combinedLight,
                        combinedOverlay,
                        model
                );

                poseStack.popPose();
                slotIndex++;
            }
        }

        poseStack.popPose();
    }
}