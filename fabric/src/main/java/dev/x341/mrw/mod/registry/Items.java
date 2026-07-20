package dev.x341.mrw.mod.registry;

import dev.x341.mrw.mod.Init;
import dev.x341.mrw.mod.item.ItemBridgeWallCreator;
import dev.x341.mrw.mod.item.ItemRailWorker;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.Item;
import org.mtr.mapping.registry.ItemRegistryObject;

public final class Items {

    static {
        BRIDGE_WALL_CREATOR_1_3 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_1_3"), itemSettings -> new Item(new ItemBridgeWallCreator(1, 3, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        BRIDGE_WALL_CREATOR_1_5 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_1_5"), itemSettings -> new Item(new ItemBridgeWallCreator(1, 5, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        BRIDGE_WALL_CREATOR_1_7 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_1_7"), itemSettings -> new Item(new ItemBridgeWallCreator(1, 7, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        BRIDGE_WALL_CREATOR_1_9 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_1_9"), itemSettings -> new Item(new ItemBridgeWallCreator(1, 9, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        BRIDGE_WALL_CREATOR_2_3 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_2_3"), itemSettings -> new Item(new ItemBridgeWallCreator(2, 3, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        BRIDGE_WALL_CREATOR_2_5 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_2_5"), itemSettings -> new Item(new ItemBridgeWallCreator(2, 5, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        BRIDGE_WALL_CREATOR_2_7 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_2_7"), itemSettings -> new Item(new ItemBridgeWallCreator(2, 7, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        BRIDGE_WALL_CREATOR_2_9 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_2_9"), itemSettings -> new Item(new ItemBridgeWallCreator(2, 9, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        BRIDGE_WALL_CREATOR_3_3 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_3_3"), itemSettings -> new Item(new ItemBridgeWallCreator(3, 3, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        BRIDGE_WALL_CREATOR_3_5 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_3_5"), itemSettings -> new Item(new ItemBridgeWallCreator(3, 5, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        BRIDGE_WALL_CREATOR_3_7 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_3_7"), itemSettings -> new Item(new ItemBridgeWallCreator(3, 7, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        BRIDGE_WALL_CREATOR_3_9 = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "bridge_wall_creator_3_9"), itemSettings -> new Item(new ItemBridgeWallCreator(3, 9, itemSettings)), CreativeModeTabs.RAIL_WORKS);
        RAIL_WORKER = Init.REGISTRY.registerItem(new Identifier(Init.MOD_ID, "rail_worker"), itemSettings -> new Item(new ItemRailWorker(itemSettings)), CreativeModeTabs.RAIL_WORKS);
    }

    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_1_3;
    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_1_5;
    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_1_7;
    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_1_9;
    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_2_3;
    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_2_5;
    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_2_7;
    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_2_9;
    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_3_3;
    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_3_5;
    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_3_7;
    public static final ItemRegistryObject BRIDGE_WALL_CREATOR_3_9;
    public static final ItemRegistryObject RAIL_WORKER;

    public static void init() {
        Init.LOGGER.info("Registering MRW items");
    }

}
