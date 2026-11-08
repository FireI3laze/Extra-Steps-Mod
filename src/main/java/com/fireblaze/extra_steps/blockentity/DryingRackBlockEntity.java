package com.fireblaze.extra_steps.blockentity;

import com.fireblaze.extra_steps.blockentity.machine.MachineBlockEntity;
import com.fireblaze.extra_steps.processing.Byproduct;
import com.fireblaze.extra_steps.processing.ByproductTrigger;
import com.fireblaze.extra_steps.processing.ProcessingMode;
import com.fireblaze.extra_steps.processing.ProcessingRecipe;
import com.fireblaze.extra_steps.registry.ModBlockEntities;
import com.fireblaze.extra_steps.registry.ModRecipeTypes;
import com.fireblaze.extra_steps.util.DryingEnvironment;
import com.fireblaze.extra_steps.util.GenericColorHelper;
import com.fireblaze.extra_steps.util.InteractionCounter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Objects;
import java.util.Optional;

public class DryingRackBlockEntity extends MachineBlockEntity {

    public float dryingTime = 0;
    public float clientSpeed = 0f;
    public int interactionCooldown = 15;
    public int pickupCooldown = 0;
    public boolean showBrushFinishedParticles = false;
    private int brushHits = 0;
    public final InteractionCounter.State state = new InteractionCounter.State();

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);

            ItemStack stack = inventory.getStackInSlot(slot);
            if(!stack.isEmpty() && stack.hasTag() && stack.getTag().contains("dryingProgress")) {
                // Progress von Item in dryingTime umrechnen
                Optional<ProcessingRecipe> recipe = getRecipeByMode(ProcessingMode.DRYING);
                if(recipe.isPresent()) {
                    float progress = stack.getTag().getFloat("dryingProgress"); // 0.0 -> 1.0
                    dryingTime = progress * recipe.get().getTime();
                }
            }

            markForRenderUpdate(); // BlockEntity Update
        }
    };

    private final LazyOptional<IItemHandler> inventoryCap = LazyOptional.of(() -> inventory);

    public ItemStack getItemInSlot0() {
        return inventory.getStackInSlot(0);
    }

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRYING_RACK_BE.get(), pos, state);
    }

    @Override
    protected Optional<ProcessingRecipe> getRecipe() {

        if(level == null) return Optional.empty();

        if (state.cooldown > 0) {
            state.cooldown--;
        }

        return level.getRecipeManager().getRecipeFor(
                ModRecipeTypes.PROCESSING.get(),
                new SimpleContainer(inventory.getStackInSlot(0)),
                level
        );
    }

    public void tick() {
        if(level == null || level.isClientSide) return;

        if (state.cooldown > 0) {
            state.cooldown--;
        }

        // Cooldown runterzählen
        if(pickupCooldown > 0) {
            pickupCooldown--;
        }

        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) {
            brushHits = 0;
            state.hits = 0;
            state.cooldown = 0;
            return;
        }

        // --- Drying Mode ---
        Optional<ProcessingRecipe> dryingRecipe = getRecipeByMode(ProcessingMode.DRYING);

        if (dryingRecipe.isEmpty()) {
            dryingTime = 0;
            return;
        }

        ProcessingRecipe recipe = dryingRecipe.get();

        float speed = getProcessingSpeed();
        clientSpeed = speed;

        dryingTime += speed;
        dryingTime = Math.max(dryingTime, -200);

        updateDryingProgress(stack, dryingTime, recipe.getTime());
        markForRenderUpdate();

        if (dryingTime >= recipe.getTime()) {

            dropByproducts(
                    recipe,
                    ByproductTrigger.ON_COMPLETE
            );

            processItem(recipe);

            dryingTime = 0;
        }
    }

    private void processItem(ProcessingRecipe recipe) {

        ItemStack input = inventory.extractItem(0, 1, false);
        ItemStack result = recipe.getResultItem(null).copy();

        // Farbe übertragen
        if (GenericColorHelper.hasColor(input)) {
            GenericColorHelper.copyColor(input, result);
        }

        if (result.hasTag()) {

            CompoundTag tag = result.getTag();

            if (tag != null) {
                tag.remove("processingProgress");
                tag.remove("dryingProgress");

                if (tag.isEmpty()) {
                    result.setTag(null);
                }
            }
        }

        // Prüfen ob Item Haltbarkeit hat
        if (input.isDamageableItem()) {

            // Haltbarkeit reduzieren
            input.setDamageValue(input.getDamageValue() + 1);

            // Prüfen ob kaputt
            boolean broken = input.getDamageValue() >= input.getMaxDamage();

            // Nur zurücklegen wenn NICHT kaputt
            if (!broken) {
                inventory.insertItem(0, input, false);
            }

            // Ergebnis droppen
            if (level != null && !level.isClientSide) {
                Containers.dropItemStack(
                        level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.0,
                        worldPosition.getZ() + 0.5,
                        result
                );
            }

        } else {
            // Farbe vom Input auf das Ergebnis übertragen, falls vorhanden
            if (GenericColorHelper.hasColor(input)) {
                GenericColorHelper.copyColor(input, result);
            }

            // Ergebnis ins Inventar einfügen
            inventory.insertItem(0, result, false);

            setChanged();

            // BlockUpdate an Client senden
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    private void dropByproducts(
            ProcessingRecipe recipe,
            ByproductTrigger trigger
    ) {

        if (level == null || level.isClientSide)
            return;

        for (Byproduct byproduct : recipe.getByproducts()) {

            // falscher Trigger → skip
            if (byproduct.trigger() != trigger)
                continue;

            // Chance failed → skip
            if (!byproduct.roll(level.random))
                continue;

            Containers.dropItemStack(
                    level,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5,
                    byproduct.stack().copy()
            );
        }
    }

    public void interaction(Player player, ProcessingRecipe recipe) {
        ItemStack stack = getItemInSlot0();
        if (stack.isEmpty()) return;

        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 8, false, false));

        InteractionCounter.handle(
                state,
                player,
                stack,
                recipe,
                interactionCooldown,
                p -> {
                    sound(recipe);

                    dropByproducts(
                            recipe,
                            ByproductTrigger.PER_INTERACTION
                    );

                    damageTool(p);
                },
                () -> processFinished(recipe)
        );

        markForRenderUpdate();
    }

    private boolean processFinishedParticlePending = false;
    private void processFinished(ProcessingRecipe recipe) {

        dropByproducts(
                recipe,
                ByproductTrigger.ON_COMPLETE
        );

        processItem(recipe);

        pickupCooldown = 2;

        if (level != null && !level.isClientSide) {
            processFinishedParticlePending = true;
            markForRenderUpdate();
        }
    }

    public void damageTool(Player player) {
        ItemStack tool = player.getMainHandItem();
        tool.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
    }

    public void sound(ProcessingRecipe recipe) {
        if (recipe.getMode().equals(ProcessingMode.BRUSHING)) {
            if(level != null) {
                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.BRUSH_GENERIC,
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        0.5f,
                        1f
                );
            }
        }

        if (recipe.getMode().equals(ProcessingMode.SCRAPING)) {
            if(level != null) {
                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.ARMOR_EQUIP_LEATHER,
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        0.4f,
                        2f
                );
            }
        }
    }
    public void markForRenderUpdate() {
        if (level != null && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            setChanged(); // markiert BlockEntity dirty
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

            // BE-Paket an alle Spieler im Chunk senden
            ChunkPos chunkPos = new ChunkPos(worldPosition);
            serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)
                    .forEach(player -> player.connection.send(ClientboundBlockEntityDataPacket.create(this)));
        }
    }

    private void updateDryingProgress(ItemStack stack, float progress, float maxProgress) {
        if(stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putFloat("dryingProgress", progress / maxProgress);
        stack.setTag(tag);
    }

    public IItemHandler getInventoryHandler() {
        return inventory;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putFloat("dryingTime", dryingTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        dryingTime = tag.getFloat("dryingTime");
        clientSpeed = tag.getFloat("clientSpeed");
    }

    @Override
    public float getProcessingSpeed() {

        if(level == null)
            return 1f;

        return DryingEnvironment.getDryingSpeed(level, worldPosition);
    }


    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        tag.putFloat("clientSpeed", clientSpeed);

        // Serverseitig setzen, Client bekommt es über Paket
        tag.putBoolean("brushFinishedParticlePending", processFinishedParticlePending);
        processFinishedParticlePending = false; // nur ein Paket nötig

        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        CompoundTag tag = pkt.getTag();
        handleUpdateTag(tag);

        // Client empfängt Paket → einmaliger Partikel-Trigger
        if(tag.contains("brushFinishedParticlePending") && tag.getBoolean("brushFinishedParticlePending")) {
            showBrushFinishedParticles = true; // Clientseitiger Trigger
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        // Paket für BlockEntity Sync an Client
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public Optional<ProcessingRecipe> getRecipeByMode(ProcessingMode mode) {

        if(level == null) return Optional.empty();

        SimpleContainer container = new SimpleContainer(inventory.getStackInSlot(0));

        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.PROCESSING.get())
                .stream()
                .filter(recipe -> recipe.getMode() == mode)
                .filter(recipe -> recipe.matches(container, level))
                .findFirst();
    }
}