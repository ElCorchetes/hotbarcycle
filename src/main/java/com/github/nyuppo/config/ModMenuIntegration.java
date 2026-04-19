package com.github.nyuppo.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigManager;
import me.shedaniel.autoconfig.gui.ConfigScreenProvider;
import me.shedaniel.autoconfig.gui.DefaultGuiProviders;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    private static final GuiRegistry GUI_REGISTRY = DefaultGuiProviders.apply(new GuiRegistry());

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::createConfigScreen;
    }

    @SuppressWarnings("unchecked")
    private static Screen createConfigScreen(Screen parent) {
        if (!(AutoConfig.getConfigHolder(ClothConfigHotbarCycleConfig.class) instanceof ConfigManager<?> rawManager)) {
            return parent;
        }

        final ConfigManager<ClothConfigHotbarCycleConfig> manager =
                (ConfigManager<ClothConfigHotbarCycleConfig>) rawManager;
        return new ConfigScreenProvider<>(manager, GUI_REGISTRY, parent).get();
    }
}
