package dev.x341.mrw.mod.mixin;

import org.mtr.mapping.holder.ServerWorld;
import org.mtr.mod.data.RailAction;
import org.mtr.mod.data.RailActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(value = RailAction.class, remap = false)
public interface RailActionAccessor {

    @Accessor("uuid")
    UUID mrw$getUuid();

    @Accessor("serverWorld")
    ServerWorld mrw$getServerWorld();

    @Accessor("railActionType")
    RailActionType mrw$getRailActionType();
}
