package com.fireblaze.extra_steps.block;

import com.fireblaze.extra_steps.config.ModConfigHandler;
import com.fireblaze.extra_steps.item.WoolBrushItem;
import com.fireblaze.extra_steps.processing.ProcessingMode;
import com.fireblaze.extra_steps.processing.ProcessingRecipe;
import com.fireblaze.extra_steps.processing.ProcessingItemRegistry;
import com.fireblaze.extra_steps.util.ScrapingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.fireblaze.extra_steps.blockentity.DryingRackBlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public class DryingRackBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DryingRackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static final VoxelShape SHAPE = Block.box(0, 0, 7, 16, 16, 9);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> null;
            case UP -> null;
            case NORTH -> Block.box(0, 0, 7, 16, 16, 9);
            case SOUTH -> Block.box(0, 0, 7, 16, 16, 9); // gespiegelt
            case WEST  -> Block.box(7, 0, 0, 9, 16, 16);               // X-Achse
            case EAST  -> Block.box(7, 0, 0, 9, 16, 16);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide) return InteractionResult.CONSUME;

        var be = level.getBlockEntity(pos);

        if (!(be instanceof DryingRackBlockEntity rack))
            return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        ItemStack offhandHeld = player.getOffhandItem();
        ItemStack insertableItem = null;
        ItemStack stored = rack.getItemInSlot0();

        if (ProcessingItemRegistry.isValidRackItem(held)) insertableItem = held;
        else if (ProcessingItemRegistry.isValidRackItem(offhandHeld)) insertableItem = offhandHeld;

        if (insertableItem != null && stored.isEmpty()) {
            // Item auf Server einfügen
            rack.getInventoryHandler().insertItem(0, insertableItem.split(1), false);
            rack.markForRenderUpdate();
            level.playSound(null, pos,
                    SoundEvents.ITEM_FRAME_ADD_ITEM,
                    SoundSource.BLOCKS,
                    0.4f, 1.0f
            );

            return InteractionResult.SUCCESS;
        }

        if(!stored.isEmpty()) {

            if(rack.pickupCooldown > 0) {
                return InteractionResult.SUCCESS;
            }

            // --- Brushing ---
            Optional<ProcessingRecipe> recipe = rack.getRecipeByMode(ProcessingMode.BRUSHING);
            if (recipe.isPresent()) {
                if (recipe.get().isToolValid(held)) {
                    rack.interaction(player, recipe.get());
                    return InteractionResult.SUCCESS;
                }
            }

            // --- Scraping ---
            if (rack.getRecipeByMode(ProcessingMode.SCRAPING).isPresent()) {
                Optional<ProcessingRecipe> scrapingRecipe = rack.getRecipeByMode(ProcessingMode.SCRAPING);

                if(ScrapingHelper.canScrape(stored, player, scrapingRecipe)) {
                    rack.interaction(player, rack.getRecipeByMode(ProcessingMode.SCRAPING).get());
                    return InteractionResult.SUCCESS;
                }
            }

            // --- Normales Aufheben ---
            ItemStack toPickup = stored.copy();

            level.playSound(null, pos,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS,
                    0.4f, 1.0f
            );

            boolean added = player.getInventory().add(toPickup);

            if(!added) {
                player.drop(toPickup, false);
            }

            rack.getInventoryHandler().extractItem(0, stored.getCount(), false);
            rack.markForRenderUpdate();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DryingRackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof DryingRackBlockEntity rack) {
                rack.tick();
            }
        };
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) { // nur, wenn der Block wirklich wegkommt
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof DryingRackBlockEntity rack) {
                // Alle Items droppen
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