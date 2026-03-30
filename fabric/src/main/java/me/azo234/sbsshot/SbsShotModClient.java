package me.azo234.sbsshot;

import me.azo234.sbsshot.stereo.NeoStereoCapture;
import me.azo234.sbsshot.stereo.StereoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

public class SbsShotModClient implements ClientModInitializer {

    public static final StereoConfig STEREO_CONFIG = new StereoConfig();

    private static KeyMapping keyStereo;

    @Override
    public void onInitializeClient() {
        FabricConfigHelper.load(STEREO_CONFIG);

        // Fabric 26.1: KeyBindingHelper → KeyMappingHelper (keymapping.v1)
        // KeyMapping.Category 型を使う（1.21.9 以降）
        KeyMapping.Category category = new KeyMapping.Category(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("sbsshot", "category")
        );

        keyStereo = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.sbsshot.stereo_screenshot",
                InputConstants.KEY_F12,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyStereo.consumeClick()) {
                NeoStereoCapture.INSTANCE.capture(STEREO_CONFIG);
            }
        });
    }
}
