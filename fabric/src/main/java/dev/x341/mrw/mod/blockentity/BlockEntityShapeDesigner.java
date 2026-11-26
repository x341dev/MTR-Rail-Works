package dev.x341.mrw.mod.blockentity;

import dev.x341.mrw.mod.data.ShapeProfile;
import dev.x341.mrw.mod.registry.BlockEntityTypes;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.mapper.BlockEntityExtension;

public class BlockEntityShapeDesigner extends BlockEntityExtension {

    private ShapeProfile profile = new ShapeProfile();

    public  BlockEntityShapeDesigner(BlockPos blockPos, BlockState blockState) {
        super(BlockEntityTypes.SHAPE_DESIGNER.get(), blockPos, blockState);
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        profile = ShapeProfile.readTag(compoundTag);
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        profile.writeTag(compoundTag);
    }

    public ShapeProfile getProfile() {
        return profile;
    }

    public void setProfile(ShapeProfile profile) {
        this.profile = profile;
        markDirty2();
    }
}
