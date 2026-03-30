package me.azo234.sbsshot;

import me.azo234.sbsshot.stereo.StereoConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SbsShotMod.MOD_ID)
public class SbsShotMod {
    public static final String MOD_ID = "sbsshot";
    public static final StereoConfig STEREO_CONFIG = new StereoConfig();

    public SbsShotMod(IEventBus modEventBus, ModContainer container) {
        NeoConfigHelper.register(container);

        // RegisterKeyMappingsEvent は mod バス（クライアント限定）
        modEventBus.addListener(StereoKeyHandler::onRegisterKeys);

        // ClientTickEvent.Post は NeoForge バス
        NeoForge.EVENT_BUS.addListener(StereoKeyHandler::onClientTick);

        modEventBus.addListener(this::clientSetup);

        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, parent) -> new ConfigurationScreen(container, parent));
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> NeoConfigHelper.applyTo(STEREO_CONFIG));
    }
}
