package dev.x341.mrw.mod.mixin;

import org.mtr.core.data.Rail;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mod.item.ItemNodeModifierSelectableBlockBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ItemNodeModifierSelectableBlockBase.class, remap = false)
public interface ItemNodeModifierSelectableBlockBaseAccessor {

    @Accessor("radius")
    int mrw$getRadius();

    @Accessor("height")
    int mrw$getHeight();

    @Accessor("canSaveBlock")
    boolean mrw$getCanSaveBlock();

    @Invoker("onConnect")
    void mrw$invokeOnConnect(Rail rail, ServerPlayerEntity serverPlayerEntity, ItemStack itemStack, int radius, int height);

    @Invoker("getSavedState")
    BlockState mrw$invokeGetSavedState(ItemStack itemStack);
}
