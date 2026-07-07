package dev.x341.mrw.mod.mixin;

import dev.x341.mrw.mod.data.RailActionBatchTracker;
import org.mtr.core.data.Rail;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.ServerWorld;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.data.RailAction;
import org.mtr.mod.data.RailActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.UUID;


@Mixin(value = RailAction.class, remap = false)
public abstract class RailActionMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mrw$trackEnqueue(ServerWorld serverWorld, ServerPlayerEntity serverPlayerEntity, RailActionType railActionType, Rail rail, int radius, int height, @Nullable BlockState state, CallbackInfo ci) {
        RailActionBatchTracker.onEnqueue(serverPlayerEntity.getUuid());
    }

    @Inject(method = "sendProgressMessage", at = @At("HEAD"), cancellable = true)
    private void mrw$batchProgress(float percentage, CallbackInfo ci) {
        final RailActionAccessor accessor = (RailActionAccessor) this;
        final UUID uuid = accessor.mrw$getUuid();
        final int[] batch = RailActionBatchTracker.get(uuid);

        if (batch != null && batch[1] > 1) {
            final PlayerEntity playerEntity = accessor.mrw$getServerWorld().getPlayerByUuid(uuid);
            if (playerEntity != null) {
                final String message = accessor.mrw$getRailActionType().progressTranslation.getString(percentage) + " / (" + (batch[0] + 1) + "/" + batch[1] + ")";
                playerEntity.sendMessage(new Text(TextHelper.literal(message).data), true);
            }
            ci.cancel();
        }

        if (percentage >= 100) {
            RailActionBatchTracker.onComplete(uuid);
        }
    }
}
