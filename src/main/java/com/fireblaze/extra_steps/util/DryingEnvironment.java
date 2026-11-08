package com.fireblaze.extra_steps.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class DryingEnvironment {

    public static float getDryingSpeed(Level level, BlockPos pos) {

        float speed = 0.0f;

        // Day bonus
        if(level.isDay()) {
            speed += 0.4f;
        }

        // Sky access
        if(level.canSeeSky(pos.above())) {
            speed += 0.25f;
        }

        // Rain penalty
        if(level.isRainingAt(pos.above())) {
            speed -= 0.5f;
        }

        // Biome temperature
        Biome biome = level.getBiome(pos).value();
        float temp = biome.getBaseTemperature();

        if(temp > 1.0f)
            speed += 0.3f;
        else if(temp < 0.3f)
            speed -= 0.2f;

        return Math.max(speed, -0.5f);
    }

}