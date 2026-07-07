package dev.x341.mrw.mod.mixin;

import dev.x341.mrw.mod.data.RailActionBatchTracker;
import org.mtr.mod.data.RailAction;
import org.mtr.mod.data.RailActionModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = RailActionModule.class, remap = false)
public abstract class RailActionModuleMixin {

    @Inject(method = "removeRailAction", at = @At("HEAD"))
    private void mrw$trackRemoval(long id, CallbackInfo ci) {
        for (final RailAction railAction : ((RailActionModuleAccessor) this).mrw$getRailActions()) {
            if (railAction.id == id) {
                RailActionBatchTracker.onRemove(((RailActionAccessor) railAction).mrw$getUuid());
                break;
            }
        }
    }
}
