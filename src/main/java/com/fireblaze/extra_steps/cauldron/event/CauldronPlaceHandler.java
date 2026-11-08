package com.fireblaze.extra_steps.cauldron.event;

import com.fireblaze.extra_steps.ExtraSteps;
import com.fireblaze.extra_steps.cauldron.data.CauldronMixingData;
import com.fireblaze.extra_steps.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtraSteps.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CauldronPlaceHandler {
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();

        if (state.is(ModTags.Blocks.VALID_MIXING_CAULDRONS)) {

            CauldronMixingData data = CauldronMixingData.get(level);
            if (data == null) return;

            data.activeCauldrons.add(pos.asLong());
            data.setDirty();
        }
    }
}
