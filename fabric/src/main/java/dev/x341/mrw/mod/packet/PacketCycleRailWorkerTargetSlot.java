package dev.x341.mrw.mod.packet;

import dev.x341.mrw.mod.item.ItemRailWorker;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

/**
 * Sent by the sprint+scroll handlers (one per loader — see the Rail Worker implementation plan)
 * to flip which block slot (floor/wall) sneak-clicking a block will save into. No payload: the
 * client already flipped its own local NBT copy optimistically for instant feedback: this just
 * applies the same flip to the server's authoritative copy.
 */
public final class PacketCycleRailWorkerTargetSlot extends PacketHandler {

	public PacketCycleRailWorkerTargetSlot() {
	}

	public PacketCycleRailWorkerTargetSlot(PacketBufferReceiver packetBufferReceiver) {
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
	}

	@Override
	public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
		final ItemStack itemStack = serverPlayerEntity.getMainHandStack();
		if (!(itemStack.getItem().data instanceof ItemRailWorker)) {
			return;
		}

		final CompoundTag tag = itemStack.getOrCreateTag();
		final int current = tag.getInt(ItemRailWorker.TAG_TARGET_SLOT);
		tag.putInt(ItemRailWorker.TAG_TARGET_SLOT, current == ItemRailWorker.TARGET_WALL ? ItemRailWorker.TARGET_FLOOR : ItemRailWorker.TARGET_WALL);
	}
}
