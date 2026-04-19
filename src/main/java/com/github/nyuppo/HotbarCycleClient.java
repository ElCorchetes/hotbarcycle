package com.github.nyuppo;

import com.github.nyuppo.compat.Clicker;
import com.github.nyuppo.compat.IPNClicker;
import com.github.nyuppo.compat.VanillaClicker;
import com.github.nyuppo.config.ClothConfigHotbarCycleConfig;
import com.github.nyuppo.config.DefaultHotbarCycleConfig;
import com.github.nyuppo.config.HotbarCycleConfig;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BooleanSupplier;

public class HotbarCycleClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("hotbarcycle");
    private static final HotbarCycleConfig CONFIG;
    private static final KeyMapping.Category HOTBARCYCLE_CATEGORY = KeyMapping.Category.MISC;
    private static final int HOTBAR_SIZE = 9;
    private static final int[] ROW_START_SLOTS_DOWN = {9, 18, 27};
    private static final int[] ROW_START_SLOTS_UP = {27, 18, 9};
    private static final BooleanSupplier[] COLUMN_CHECKS;

    private static KeyMapping cycleKeyBinding;
    private static KeyMapping singleCycleKeyBinding;
    private static Clicker clicker;

    static {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            CONFIG = AutoConfig.register(ClothConfigHotbarCycleConfig.class, GsonConfigSerializer::new).getConfig();
        } else {
            LOGGER.warn("Cloth Config no encontrado. Usando configuración por defecto.");
            CONFIG = new DefaultHotbarCycleConfig();
        }

        COLUMN_CHECKS = new BooleanSupplier[] {
            CONFIG::getEnableColumn0,
            CONFIG::getEnableColumn1,
            CONFIG::getEnableColumn2,
            CONFIG::getEnableColumn3,
            CONFIG::getEnableColumn4,
            CONFIG::getEnableColumn5,
            CONFIG::getEnableColumn6,
            CONFIG::getEnableColumn7,
            CONFIG::getEnableColumn8
        };
    }

    @Override
    public void onInitializeClient() {
        clicker = getClicker();
        registerKeyBindings();
        registerTickHandler();
    }

    public static HotbarCycleConfig getConfig() {
        return CONFIG;
    }

    public static KeyMapping getCycleKeyBinding() {
        return cycleKeyBinding;
    }

    public static KeyMapping getSingleCycleKeyBinding() {
        return singleCycleKeyBinding;
    }

    private void registerKeyBindings() {
        cycleKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.hotbarcycle.cycle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            HOTBARCYCLE_CATEGORY
        ));
        singleCycleKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.hotbarcycle.single_cycle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            HOTBARCYCLE_CATEGORY
        ));
    }

    private void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || CONFIG.getHoldAndScroll()) {
                return;
            }

            while (cycleKeyBinding.consumeClick()) {
                shiftRows(client, Direction.DOWN);
            }

            while (singleCycleKeyBinding.consumeClick()) {
                shiftSingle(client, client.player.getInventory().getSelectedSlot(), Direction.DOWN);
            }
        });
    }

    public enum Direction {
        UP,
        DOWN;

        public Direction reverse(boolean reversed) {
            return reversed ? (this == UP ? DOWN : UP) : this;
        }
    }

    public static void shiftRows(Minecraft client, Direction requestedDirection) {
        if (client.player == null || clicker == null) {
            return;
        }

        boolean row1Enabled = CONFIG.getEnableRow1();
        boolean row2Enabled = CONFIG.getEnableRow2();
        boolean row3Enabled = CONFIG.getEnableRow3();
        boolean hasEnabledRow = false;
        Direction effectiveDirection = requestedDirection.reverse(CONFIG.getReverseCycleDirection());

        for (int rowStartSlot : getRowStartSlots(effectiveDirection)) {
            if (!isRowEnabled(rowStartSlot, row1Enabled, row2Enabled, row3Enabled)) {
                continue;
            }

            hasEnabledRow = true;
            for (int hotbarSlot = 0; hotbarSlot < HOTBAR_SIZE; hotbarSlot++) {
                if (COLUMN_CHECKS[hotbarSlot].getAsBoolean()) {
                    clicker.swap(client, rowStartSlot + hotbarSlot, hotbarSlot);
                }
            }
        }

        if (hasEnabledRow) {
            playCycleSound(client, 1.5f);
        }
    }

    public static void shiftSingle(Minecraft client, int hotbarSlot, Direction requestedDirection) {
        if (client.player == null || clicker == null) {
            return;
        }

        boolean row1Enabled = CONFIG.getEnableRow1();
        boolean row2Enabled = CONFIG.getEnableRow2();
        boolean row3Enabled = CONFIG.getEnableRow3();
        boolean hasEnabledRow = false;
        Direction effectiveDirection = requestedDirection.reverse(CONFIG.getReverseCycleDirection());

        for (int rowStartSlot : getRowStartSlots(effectiveDirection)) {
            if (!isRowEnabled(rowStartSlot, row1Enabled, row2Enabled, row3Enabled)) {
                continue;
            }

            hasEnabledRow = true;
            clicker.swap(client, rowStartSlot + hotbarSlot, hotbarSlot);
        }

        if (hasEnabledRow) {
            playCycleSound(client, 1.8f);
        }
    }

    private static int[] getRowStartSlots(Direction direction) {
        return direction == Direction.DOWN ? ROW_START_SLOTS_DOWN : ROW_START_SLOTS_UP;
    }

    private static boolean isRowEnabled(
            int rowStartSlot,
            boolean row1Enabled,
            boolean row2Enabled,
            boolean row3Enabled
    ) {
        return switch (rowStartSlot) {
            case 9 -> row1Enabled;
            case 18 -> row2Enabled;
            case 27 -> row3Enabled;
            default -> false;
        };
    }

    private static void playCycleSound(Minecraft client, float pitch) {
        if (!CONFIG.getPlaySound() || client.level == null || client.player == null) {
            return;
        }

        client.level.playLocalSound(client.player, SoundEvents.BOOK_PAGE_TURN, SoundSource.MASTER, 0.5f, pitch);
    }

    private static Clicker getClicker() {
        return FabricLoader.getInstance().isModLoaded("inventoryprofilesnext")
                ? new IPNClicker()
                : new VanillaClicker();
    }
}
