package me.azo234.sbsshot;

import me.azo234.sbsshot.stereo.StereoConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SbsShotMod.MOD_ID)
public class SbsShotMod {
    public static final String MOD_ID = "sbsshot";
    public static final StereoConfig STEREO_CONFIG = new StereoConfig();

    public SbsShotMod() {
        ForgeConfigHelper.register();
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(StereoKeyHandler::onRegisterKeys);
        modBus.addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(new StereoKeyHandler());
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ForgeConfigHelper.applyTo(STEREO_CONFIG));
    }
}
