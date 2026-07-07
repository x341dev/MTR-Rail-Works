package dev.x341.mrw.mod.mixin;

import org.mtr.mapping.holder.ItemUsageContext;
import org.mtr.mod.item.ItemNodeModifierBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ItemNodeModifierBase.class, remap = false)
public interface ItemNodeModifierBaseInvoker {

    @Invoker("clickCondition")
    boolean mrw$invokeClickCondition(ItemUsageContext context);
}
