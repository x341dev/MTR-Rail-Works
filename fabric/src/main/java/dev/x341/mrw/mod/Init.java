package dev.x341.mrw.mod;

import dev.x341.mrw.mod.packet.PacketApplyRailAction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.Registry;

/**
 * Shared, loader-agnostic entry point. Written once in the Fabric module and copied verbatim
 * into the Forge module by the Forge {@code setupFiles} task. Only references
 * {@code org.mtr.mapping.*} — never {@code net.minecraft.*} or a loader API — so the same
 * bytecode compiles under both Fabric and Forge and across every supported Minecraft version.
 */
public final class Init {

    public static final String MOD_ID = "mrw";
    public static final Registry REGISTRY = new Registry();

    private Init() {
    }

    /** Called by each loader's bootstrap ({@code MRWFabric} / {@code MRWForge}). */
    public static void init() {
        // Register blocks, items, block entities, entities, particles, sounds, packets and
        // commands on REGISTRY here, e.g.:
        //   REGISTRY.registerItem(new Identifier(MOD_ID, "example"), ExampleItem::new);
        REGISTRY.setupPackets(new Identifier(MOD_ID, "packet"));
        REGISTRY.registerPacket(PacketApplyRailAction.class, PacketApplyRailAction::new);
        REGISTRY.init();
    }
}
