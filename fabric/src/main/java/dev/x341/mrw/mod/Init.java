package dev.x341.mrw.mod;

import dev.x341.mrw.mod.data.RailActionModuleMrw;
import dev.x341.mrw.mod.packet.PacketApplyRailAction;
import dev.x341.mrw.mod.packet.PacketApplyRailWorkerBuild;
import dev.x341.mrw.mod.packet.PacketCycleRailWorkerTargetSlot;
import dev.x341.mrw.mod.packet.PacketUpdateRailWorkerConfig;
import dev.x341.mrw.mod.registry.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.ServerWorld;
import org.mtr.mapping.mapper.MinecraftServerHelper;
import org.mtr.mapping.registry.Registry;

import java.util.function.Consumer;

/**
 * Shared, loader-agnostic entry point. Written once in the Fabric module and copied verbatim
 * into the Forge module by the Forge {@code setupFiles} task. Only references
 * {@code org.mtr.mapping.*} — never {@code net.minecraft.*} or a loader API — so the same
 * bytecode compiles under both Fabric and Forge and across every supported Minecraft version.
 */
public final class Init {

	public static final String MOD_ID = "mrw";
	public static final Registry REGISTRY = new Registry();

	public static final Logger LOGGER = LogManager.getLogger("MTRRailWorks");

	private static final Object2ObjectArrayMap<ServerWorld, RailActionModuleMrw> RAIL_ACTION_MODULES = new Object2ObjectArrayMap<>();

	public static void init() {
		LOGGER.info("Hello from MRW OwO");

		Items.init();

		REGISTRY.setupPackets(new Identifier(MOD_ID, "packet"));
		REGISTRY.registerPacket(PacketApplyRailAction.class, PacketApplyRailAction::new);
		REGISTRY.registerPacket(PacketApplyRailWorkerBuild.class, PacketApplyRailWorkerBuild::new);
		REGISTRY.registerPacket(PacketUpdateRailWorkerConfig.class, PacketUpdateRailWorkerConfig::new);
		REGISTRY.registerPacket(PacketCycleRailWorkerTargetSlot.class, PacketCycleRailWorkerTargetSlot::new);

		REGISTRY.registerCommand("mrw", commandBuilder -> commandBuilder.then("debug", debugBuilder -> {
			debugBuilder.permissionLevel(2);
			debugBuilder.then("on", onBuilder -> onBuilder.executes(context -> {
				MrwDebug.setEnabled(true);
				context.sendSuccess("command.mrw.debug_on", true);
				return 1;
			}));
			debugBuilder.then("off", offBuilder -> offBuilder.executes(context -> {
				MrwDebug.setEnabled(false);
				context.sendSuccess("command.mrw.debug_off", true);
				return 1;
			}));
		}));

		REGISTRY.eventRegistry.registerServerStarted(minecraftServer -> {
			RAIL_ACTION_MODULES.clear();
			MinecraftServerHelper.iterateWorlds(minecraftServer, serverWorld -> RAIL_ACTION_MODULES.put(serverWorld, new RailActionModuleMrw(serverWorld)));
		});

		REGISTRY.eventRegistry.registerEndWorldTick(serverWorld -> {
			final RailActionModuleMrw railActionModule = RAIL_ACTION_MODULES.get(serverWorld);
			if (railActionModule != null) {
				railActionModule.tick();
			}
		});

		REGISTRY.init();
	}

	public static void getRailActionModule(ServerWorld serverWorld, Consumer<RailActionModuleMrw> consumer) {
		final RailActionModuleMrw railActionModule = RAIL_ACTION_MODULES.get(serverWorld);
		if (railActionModule != null) {
			consumer.accept(railActionModule);
		}
	}

}
