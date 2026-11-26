package dev.x341.mrw.mod.client;

import dev.x341.mrw.mod.blockentity.BlockEntityShapeDesigner;
import dev.x341.mrw.mod.screen.ShapeDesignerScreen;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;

public class ShapeDesignerClientHelper {

    private ShapeDesignerClientHelper() {}

    public static void openScreen(BlockPos blockPos) {
        final MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.getWorldMapped() == null) {
            return;
        }

        final BlockEntity blockEntity = minecraftClient.getWorldMapped().getBlockEntity(blockPos);
        if (blockEntity != null && blockEntity.data instanceof BlockEntityShapeDesigner) {
            minecraftClient.openScreen(new Screen(new ShapeDesignerScreen(blockPos, (BlockEntityShapeDesigner) blockEntity.data)));
        }
    }
}
