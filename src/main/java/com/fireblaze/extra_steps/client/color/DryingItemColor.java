package com.fireblaze.extra_steps.client.color;

import com.fireblaze.extra_steps.processing.ProcessingItemRegistry;
import com.fireblaze.extra_steps.registry.ModItems;
import com.fireblaze.extra_steps.util.GenericColorHelper;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

public class DryingItemColor implements ItemColor {

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        //if (!ProcessingItemRegistry.isValidDryingItem(stack)) return 0xFFFFFF; // kein Tinting für andere Items

        // Ziel-Farbe: aus NBT oder Fallback auf GenericColorHelper
        int dryRGB = GenericColorHelper.getColor(stack);
        int dryR = (dryRGB >> 16) & 0xFF;
        int dryG = (dryRGB >> 8) & 0xFF;
        int dryB = dryRGB & 0xFF;

        int r, g, b;

        if (ProcessingItemRegistry.isValidDryingItem(stack)) {
            if (tintIndex != 0) return 0xFFFFFF; // nur Basisfarbe wird getintet

            // Neutraler Ausgangston (z. B. nass / roh)
            int neutralR = 160, neutralG = 130, neutralB = 100;
            if (stack.is(ModItems.PULP_BLOCK.get())) {
                neutralR = 185;
                neutralG = 190;
                neutralB = 180;
            }

            // Trocknungsfortschritt: -1 = nassblau, 0-1 = Trocknung
            float progress = 0f;
            if(stack.hasTag() && stack.getTag().contains("dryingProgress")) {
                progress = stack.getTag().getFloat("dryingProgress");
            }

            if (progress < 0f) {
                // Nassbereich: neutral -> nassblau
                float t = Math.min(-progress, 1f);
                int wetBlueR = 130, wetBlueG = 150, wetBlueB = 200;
                r = (int) lerp(neutralR, wetBlueR, t);
                g = (int) lerp(neutralG, wetBlueG, t);
                b = (int) lerp(neutralB, wetBlueB, t);
            } else {
                // Trocknungsbereich: neutral -> Ziel-Farbe
                float t = Math.min(progress, 1f);
                r = (int) lerp(neutralR, dryR, t);
                g = (int) lerp(neutralG, dryG, t);
                b = (int) lerp(neutralB, dryB, t);
            }
        } else {
            r = dryR;
            g = dryG;
            b = dryB;
        }
        return (r << 16) | (g << 8) | b;
    }

    private float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }
}