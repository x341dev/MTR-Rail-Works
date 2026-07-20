package dev.x341.mrw.mod.packet;

import dev.x341.mrw.mod.data.RailWorkerMode;
import dev.x341.mrw.mod.item.ItemRailWorker;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

/**
 * Sent whenever {@code RailWorkerConfigScreen} closes. The server re-validates every field rather
 * than trusting the client outright (a modified client could otherwise set an invalid width or
 * both Walls modes at once).
 */
public final class PacketUpdateRailWorkerConfig extends PacketHandler {

	private static final int[] ALLOWED_WIDTHS = {3, 5, 7, 9};

	private final int mode;
	private final boolean replace;
	private final int width;
	private final int height;

	public PacketUpdateRailWorkerConfig(PacketBufferReceiver packetBufferReceiver) {
		mode = packetBufferReceiver.readInt();
		replace = packetBufferReceiver.readBoolean();
		width = packetBufferReceiver.readInt();
		height = packetBufferReceiver.readInt();
	}

	public PacketUpdateRailWorkerConfig(int mode, boolean replace, int width, int height) {
		this.mode = mode;
		this.replace = replace;
		this.width = width;
		this.height = height;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeInt(mode);
		packetBufferSender.writeBoolean(replace);
		packetBufferSender.writeInt(width);
		packetBufferSender.writeInt(height);
	}

	@Override
	public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
		final ItemStack itemStack = serverPlayerEntity.getMainHandStack();
		if (!(itemStack.getItem().data instanceof ItemRailWorker)) {
			return;
		}

		final CompoundTag tag = itemStack.getOrCreateTag();
		final int sanitizedMode = RailWorkerMode.sanitize(mode);
		final int snappedWidth = snapWidth(width);
		final int clampedHeight = Math.max(1, Math.min(8, height));
		tag.putInt(ItemRailWorker.TAG_MODE, sanitizedMode);
		tag.putBoolean(ItemRailWorker.TAG_REPLACE, replace);
		tag.putInt(ItemRailWorker.TAG_WIDTH, snappedWidth);
		tag.putInt(ItemRailWorker.TAG_HEIGHT, clampedHeight);

		if (dev.x341.mrw.mod.MrwDebug.isEnabled()) {
			dev.x341.mrw.mod.Init.LOGGER.info("[MRW debug] Rail Worker config saved: mode={} (raw {}) replace={} width={} (raw {}) height={} (raw {})",
					sanitizedMode, mode, replace, snappedWidth, width, clampedHeight, height);
		}
	}

	private static int snapWidth(int width) {
		int best = ALLOWED_WIDTHS[0];
		int bestDiff = Integer.MAX_VALUE;
		for (final int candidate : ALLOWED_WIDTHS) {
			final int diff = Math.abs(candidate - width);
			if (diff < bestDiff) {
				bestDiff = diff;
				best = candidate;
			}
		}
		return best;
	}
}
