package dev.ripiters.create_frequency.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

public final class CFCreativeTabEvents {

    private CFCreativeTabEvents() {
    }

    public static void addCreativeContents(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tabId = event.getTabKey().location();
        // Only inject into the main Create tab, not Building Blocks or any other Create tab.
        if (!"create".equals(tabId.getNamespace()) || !"base".equals(tabId.getPath())) {
            return;
        }

        event.accept(CFBlocks.FREQUENCY_TRANSMITTER.get());
        event.accept(CFItems.FREQUENCY_CONTROLLER.get());
    }
}
