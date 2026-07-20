package dev.x341.mrw.mod.client;

import dev.x341.mrw.mod.Init;
import dev.x341.mrw.mod.registry.Items;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.ItemRegistryObject;
import org.mtr.mapping.registry.RegistryClient;
import org.mtr.mod.item.ItemBlockClickingBase;

/**
 * Shared client-side entry point (renderers, key bindings, colours). Like {@link Init}, this is
 * copied verbatim into the Forge module and only uses the {@code org.mtr.mapping.*} abstraction.
 */
public final class InitClient {

	public static final RegistryClient REGISTRY_CLIENT = new RegistryClient(Init.REGISTRY);

	private InitClient() {
	}

	/** Called by each loader's client bootstrap. */
	public static void init() {
		REGISTRY_CLIENT.setupPackets(new Identifier(Init.MOD_ID, "packet"));

		// Swap to the "selected" texture once the first node is clicked, like MTR's own creators
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_1_3);
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_1_5);
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_1_7);
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_1_9);
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_2_3);
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_2_5);
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_2_7);
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_2_9);
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_3_3);
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_3_5);
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_3_7);
		registerSelectedPredicate(Items.BRIDGE_WALL_CREATOR_3_9);

		// Rail Worker tracks click progress client-side (see RailWorkerClickState) instead of the
		// TAG_POS the other node-clicking items store on the stack itself
		REGISTRY_CLIENT.registerItemModelPredicate(Items.RAIL_WORKER, new Identifier(Init.MOD_ID, "selected"), (itemStack, clientWorld, livingEntity) -> RailWorkerClickState.getInstance().getTextureIndex());

		REGISTRY_CLIENT.init();
	}

	private static void registerSelectedPredicate(ItemRegistryObject itemRegistryObject) {
		REGISTRY_CLIENT.registerItemModelPredicate(itemRegistryObject, new Identifier(Init.MOD_ID, "selected"), (itemStack, clientWorld, livingEntity) -> itemStack.getOrCreateTag().contains(ItemBlockClickingBase.TAG_POS) ? 1 : 0);
	}
}
