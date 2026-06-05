package dev.ripiters.create_frequency;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import dev.ripiters.create_frequency.common.*;
import dev.ripiters.create_frequency.common.CFCreativeTabEvents;
import dev.ripiters.create_frequency.common.network.CFPackets;
import dev.ripiters.create_frequency.common.network.FrequencyNetworkHandler;
import dev.ripiters.create_frequency.config.FrequencyConfig;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CreateFrequency.MODID)
public class CreateFrequency {
    public static final String MODID = "create_frequency";
    public static final String NAME = "Create Frequency";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final FrequencyNetworkHandler FREQUENCY_NETWORK_HANDLER = new FrequencyNetworkHandler();

    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID)
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            );

    public CreateFrequency() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        REGISTRATE.registerEventListeners(modEventBus);

        modEventBus.addListener(CFCreativeTabEvents::addCreativeContents);
        CFBlocks.register();
        CFItems.register();
        CFBlockEntityTypes.register();
        CFMenuTypes.register();

        FrequencyConfig.register(modLoadingContext);

        modEventBus.addListener(this::commonSetup);

        CreateFrequency.LOGGER.debug("Create Frequency Registration complete.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CFPackets::register);
        CreateFrequency.LOGGER.debug("Create Frequency Common Setup complete.");
    }

    public static CreateRegistrate getRegistrate() {
        return REGISTRATE;
    }

    public static ResourceLocation resourceLocation(String path) {
        return new ResourceLocation(MODID, path);
    }
}