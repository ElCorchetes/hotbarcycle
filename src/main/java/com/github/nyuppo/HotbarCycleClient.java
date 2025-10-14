package com.github.nyuppo;

import com.github.nyuppo.compat.Clicker;
import com.github.nyuppo.compat.IPNClicker;
import com.github.nyuppo.compat.VanillaClicker;
import com.github.nyuppo.config.ClothConfigHotbarCycleConfig;
import com.github.nyuppo.config.DefaultHotbarCycleConfig;
import com.github.nyuppo.config.HotbarCycleConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class HotbarCycleClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("hotbarcycle");
    private static final HotbarCycleConfig CONFIG;
    private static final KeyBinding.Category HOTBARCYCLE_CATEGORY = KeyBinding.Category.MISC;
    private static final BooleanSupplier[] COLUMN_CHECKS;

    private static KeyBinding cycleKeyBinding;
    private static KeyBinding singleCycleKeyBinding;
    private static Clicker clicker;

    static {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            CONFIG = AutoConfig.register(ClothConfigHotbarCycleConfig.class, GsonConfigSerializer::new).getConfig();
        } else {
            LOGGER.warn("Cloth Config no encontrado. Usando configuración por defecto.");
            CONFIG = new DefaultHotbarCycleConfig();
        }

        COLUMN_CHECKS = new BooleanSupplier[]{
                CONFIG::getEnableColumn0, CONFIG::getEnableColumn1, CONFIG::getEnableColumn2,
                CONFIG::getEnableColumn3, CONFIG::getEnableColumn4, CONFIG::getEnableColumn5,
                CONFIG::getEnableColumn6, CONFIG::getEnableColumn7, CONFIG::getEnableColumn8
        };
    }

    @Override
    public void onInitializeClient() {
        clicker = getClicker();
        registerKeyBindings();
        registerTickHandler();
    }

    // --- Getters Públicos para Mixins y API ---
    public static HotbarCycleConfig getConfig() {
        return CONFIG;
    }

    public static KeyBinding getCycleKeyBinding() {
        return cycleKeyBinding;
    }

    public static KeyBinding getSingleCycleKeyBinding() {
        return singleCycleKeyBinding;
    }

    private void registerKeyBindings() {
        cycleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hotbarcycle.cycle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, HOTBARCYCLE_CATEGORY));
        singleCycleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hotbarcycle.single_cycle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, HOTBARCYCLE_CATEGORY));
    }

    private void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || CONFIG.getHoldAndScroll()) return;
            while (cycleKeyBinding.wasPressed()) shiftRows(client, Direction.DOWN);
            while (singleCycleKeyBinding.wasPressed()) shiftSingle(client, client.player.getInventory().getSelectedSlot(), Direction.DOWN);
        });
    }

    public enum Direction {
        UP, DOWN;
        public Direction reverse(boolean reversed) { return reversed ? (this == UP ? DOWN : UP) : this; }
    }

    public static void shiftRows(MinecraftClient client, Direction requestedDirection) {
        if (client.player == null) return;
        List<Integer> rowsToCycle = getRowsInCycleOrder(requestedDirection.reverse(CONFIG.getReverseCycleDirection()));
        if (rowsToCycle.isEmpty()) return;

        for (int rowStartSlot : rowsToCycle) {
            for (int i = 0; i < 9; i++) {
                if (isColumnEnabled(i)) {
                    clicker.swap(client, rowStartSlot + i, i);
                }
            }
        }
        playCycleSound(client, 1.5f);
    }

    public static void shiftSingle(MinecraftClient client, int hotbarSlot, Direction requestedDirection) {
        if (client.player == null) return;
        List<Integer> rowsToCycle = getRowsInCycleOrder(requestedDirection.reverse(CONFIG.getReverseCycleDirection()));
        if (rowsToCycle.isEmpty()) return;

        rowsToCycle.forEach(rowStartSlot -> clicker.swap(client, rowStartSlot + hotbarSlot, hotbarSlot));
        playCycleSound(client, 1.8f);
    }
    
    private static List<Integer> getRowsInCycleOrder(Direction direction) {
        List<Integer> rows = new ArrayList<>(3);
        if (direction == Direction.DOWN) {
            if (CONFIG.getEnableRow1()) rows.add(9);
            if (CONFIG.getEnableRow2()) rows.add(18);
            if (CONFIG.getEnableRow3()) rows.add(27);
        } else { // UP
            if (CONFIG.getEnableRow3()) rows.add(27);
            if (CONFIG.getEnableRow2()) rows.add(18);
            if (CONFIG.getEnableRow1()) rows.add(9);
        }
        return rows;
    }

    private static void playCycleSound(MinecraftClient client, float pitch) {
        if (CONFIG.getPlaySound() && client.world != null && client.player != null) {
            client.world.playSound(client.player, client.player.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 0.5f, pitch);
        }
    }

    private static Clicker getClicker() {
        return FabricLoader.getInstance().isModLoaded("inventoryprofilesnext") ? new IPNClicker() : new VanillaClicker();
    }

    private static boolean isColumnEnabled(int i) {
        return i >= 0 && i < 9 && COLUMN_CHECKS[i].getAsBoolean();
    }
}
