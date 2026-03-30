package me.azo234.sbsshot;

import me.azo234.sbsshot.stereo.NeoStereoCapture;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class StereoKeyHandler {

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("sbsshot", "category"));

    public static final KeyMapping KEY_STEREO = new KeyMapping(
            "key.sbsshot.stereo_screenshot",
            GLFW.GLFW_KEY_F12,
            CATEGORY
    );

    // mod バスに addListener(StereoKeyHandler::onRegisterKeys) で登録
    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(KEY_STEREO);
    }

    // NeoForge バスに addListener(StereoKeyHandler::onClientTick) で登録
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (KEY_STEREO.consumeClick()) {
            NeoStereoCapture.INSTANCE.capture(SbsShotMod.STEREO_CONFIG);
        }
    }
}
