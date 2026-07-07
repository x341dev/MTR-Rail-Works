package dev.x341.mrw.mod.packet;

import dev.x341.mrw.mod.mixin.ItemNodeModifierSelectableBlockBaseAccessor;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.item.ItemNodeModifierBase;
import org.mtr.mod.item.ItemNodeModifierSelectableBlockBase;

public final class PacketApplyRailAction extends PacketHandler {

    private final ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> railPairs;

    public PacketApplyRailAction(PacketBufferReceiver packetBufferReceiver) {
        railPairs = new ObjectArrayList<>();
        final int count = packetBufferReceiver.readInt();
        for (int i = 0; i < count; i++) {
            final BlockPos start = BlockPos.fromLong(packetBufferReceiver.readLong());
            final BlockPos end = BlockPos.fromLong(packetBufferReceiver.readLong());
            railPairs.add(new ObjectObjectImmutablePair<>(start, end));
        }
    }

    public PacketApplyRailAction(ObjectArrayList<ObjectObjectImmutablePair<BlockPos, BlockPos>> railPairs) {
        this.railPairs = railPairs;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeInt(railPairs.size());
        for (final ObjectObjectImmutablePair<BlockPos, BlockPos> pair : railPairs) {
            packetBufferSender.writeLong(pair.left().asLong());
            packetBufferSender.writeLong(pair.right().asLong());
        }
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        final ItemStack itemStack = serverPlayerEntity.getMainHandStack();
        if (!(itemStack.getItem().data instanceof ItemNodeModifierSelectableBlockBase)) {
            return;
        }
        final ItemNodeModifierSelectableBlockBaseAccessor item = (ItemNodeModifierSelectableBlockBaseAccessor) itemStack.getItem().data;
        final int capturedRadius = item.mrw$getRadius();
        final int capturedHeight = item.mrw$getHeight();
        for (final ObjectObjectImmutablePair<BlockPos, BlockPos> pair : railPairs) {
            ItemNodeModifierBase.getRail(
                    serverPlayerEntity.getEntityWorld(),
                    pair.left(),
                    pair.right(),
                    serverPlayerEntity,
                    rail -> item.mrw$invokeOnConnect(rail, serverPlayerEntity, itemStack, capturedRadius, capturedHeight)
            );
        }
    }
}
