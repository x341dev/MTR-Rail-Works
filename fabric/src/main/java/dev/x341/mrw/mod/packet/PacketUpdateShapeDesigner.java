package dev.x341.mrw.mod.packet;

import dev.x341.mrw.mod.blockentity.BlockEntityShapeDesigner;
import dev.x341.mrw.mod.data.ShapeBlocks;
import dev.x341.mrw.mod.data.ShapeProfile;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public final class PacketUpdateShapeDesigner extends PacketHandler {

    private static final double MAX_DISTANCE_SQUARED = 8 * 8;

    private final long blockPos;
    private final String palette;
    private final String cells;

    public PacketUpdateShapeDesigner(PacketBufferReceiver packetBufferReceiver) {
        blockPos = packetBufferReceiver.readLong();
        palette = packetBufferReceiver.readString();
        cells = packetBufferReceiver.readString();
    }

    public PacketUpdateShapeDesigner(BlockPos blockPos, ShapeProfile profile) {
        this.blockPos = blockPos.asLong();
        palette = String.join(",", profile.getPalette());
        final StringBuilder stringBuilder = new StringBuilder();
        for (int row = 0; row < ShapeProfile.SIZE; row++) {
            for (int column = 0; column < ShapeProfile.SIZE; column++) {
                final byte cell = profile.getCell(column, row);
                stringBuilder.append(cell == ShapeProfile.EMPTY ? '-' : (char) ('0' + cell));
            }
        }
        cells = stringBuilder.toString();
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos);
        packetBufferSender.writeString(palette);
        packetBufferSender.writeString(cells);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        final BlockPos pos = BlockPos.fromLong(blockPos);
        final double dx = serverPlayerEntity.getX() - (pos.getX() + 0.5);
        final double dy = serverPlayerEntity.getY() - (pos.getY() + 0.5);
        final double dz = serverPlayerEntity.getZ() - (pos.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > MAX_DISTANCE_SQUARED || cells.length() != ShapeProfile.SIZE * ShapeProfile.SIZE) {
            return;
        }

        final World world = serverPlayerEntity.getEntityWorld();
        final BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null || !(blockEntity.data instanceof BlockEntityShapeDesigner)) {
            return;
        }

        final ShapeProfile profile = new ShapeProfile();
        if (!palette.isEmpty()) {
            for (final String rawId : palette.split(",")) {
                final String id = ShapeBlocks.normalize(rawId);
                if (id == null || ShapeBlocks.toState(id) == null || profile.getOrAddPaletteIndex(id) < 0) {
                    return;
                }
            }
        }
        for (int i = 0; i < cells.length(); i++) {
            final char character = cells.charAt(i);
            if (character == '-') {
                continue;
            }
            final int index = character - '0';
            if (index < 0 || index >= profile.getPalette().size()) {
                return;
            }
            profile.setCell(i % ShapeProfile.SIZE, i / ShapeProfile.SIZE, (byte) index);
        }

        final BlockEntityShapeDesigner shapeDesigner = (BlockEntityShapeDesigner) blockEntity.data;
        shapeDesigner.setProfile(profile);
        final BlockState state = shapeDesigner.getCachedState2();
        world.updateListeners(pos, state, state, 3);
    }
}