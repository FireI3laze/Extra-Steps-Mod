package com.fireblaze.extra_steps.block;

import com.fireblaze.extra_steps.blockentity.BasketBlockEntity;
import com.fireblaze.extra_steps.blockentity.DryingRackBlockEntity;
import com.fireblaze.extra_steps.processing.ProcessingMode;
import com.fireblaze.extra_steps.registry.ModBlockEntities;
import com.fireblaze.extra_steps.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.IItemHandler;

public class BasketBlock extends Block implements EntityBlock {

    public BasketBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(level.isClientSide) return InteractionResult.SUCCESS;

        var be = level.getBlockEntity(pos);
        if(!(be instanceof BasketBlockEntity basket)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        IItemHandler inv = basket.getInventoryHandler();

        // --- Items mit leerer Hand entnehmen ---
        if(held.isEmpty()) {
            for(int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.extractItem(i, 64, false);
                if(!stack.isEmpty()) {
                    player.addItem(stack);
                }
            }
            return InteractionResult.SUCCESS;
        }

        var recipe = basket.getRecipeForItem(held, ProcessingMode.COMPRESSING).orElse(null);
        if (recipe == null) return InteractionResult.PASS;
        boolean matchesIngredient = recipe.getIngredients().stream()
                .anyMatch(ingredient -> ingredient.test(held));
        if(matchesIngredient) {
            ItemStack inSlot = inv.getStackInSlot(0);

            // --- Korb ist leer → einfach einfüllen ---
            if(inSlot.isEmpty()) {
                inv.insertItem(0, held.split(held.getCount()), false);

            }
            // --- Korb ist teilweise gefüllt → stapeln ---
            else if(ItemStack.isSameItemSameTags(inSlot, held)) {
                int space = 64 - inSlot.getCount();
                int toInsert = Math.min(space, held.getCount());
                inv.insertItem(0, held.split(toInsert), false);

            }
            // --- Korb ist gefüllt mit anderem Item → tauschen ---
            else {
                // nur wenn Spieler in der Lage ist, das bestehende Item aufzunehmen
                ItemStack temp = inSlot.copy();
                inv.extractItem(0, inSlot.getCount(), false); // entfernen
                inv.insertItem(0, held.split(held.getCount()), false); // neues Item rein
                player.setItemInHand(hand, temp); // altes Item zurück auf Hand
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public BasketBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BasketBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(
                Block.box(0, 0, 0, 16, 4, 16),   // Boden des Korbs
                Block.box(0, 0, 0, 1, 8, 16),   // Westwand
                Block.box(15, 0, 0, 16, 8, 16), // Ostwand
                Block.box(1, 0, 0, 15, 8, 1),   // Nordwand
                Block.box(1, 0, 15, 15, 8, 16)  // Südwand
        );
    }


    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if(be instanceof BasketBlockEntity basket) {
                basket.tick();
            }
        };
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) { // nur, wenn der Block wirklich wegkommt
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof BasketBlockEntity rack) {
                var handler = rack.getInventoryHandler(); // dein Inventar-Handler
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stack = handler.getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        popResource(world, pos, stack);
                    }
                }
            }
            super.onRemove(state, world, pos, newState, isMoving);
        }
    }
}