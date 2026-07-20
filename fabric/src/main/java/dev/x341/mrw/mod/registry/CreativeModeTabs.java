package dev.x341.mrw.mod.registry;

import dev.x341.mrw.mod.Init;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.ItemConvertible;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.registry.CreativeModeTabHolder;

/**
 * This mod's own creative tab, holding the Bridge Wall Creator family and Rail Worker. Previously
 * these lived in MTR's own {@code CORE} tab; moved here to group MRW's tools together.
 */
public final class CreativeModeTabs {

	public static final CreativeModeTabHolder RAIL_WORKS = Init.REGISTRY.createCreativeModeTabHolder(new Identifier(Init.MOD_ID, "rail_works"), () -> new ItemStack(new ItemConvertible(Items.RAIL_WORKER.get().data)));

	private CreativeModeTabs() {
	}
}
