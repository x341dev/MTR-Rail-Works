package dev.x341.mrw.mod.client;

import dev.x341.mrw.mod.Init;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.RegistryClient;

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
        // Register block-entity / entity / particle renderers, key bindings and colour providers
        // on REGISTRY_CLIENT here.
        REGISTRY_CLIENT.setupPackets(new Identifier(Init.MOD_ID, "packet"));
        REGISTRY_CLIENT.init();
    }
}
