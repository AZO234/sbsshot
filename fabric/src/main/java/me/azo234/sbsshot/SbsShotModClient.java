package me.azo234.sbsshot;

import me.azo234.sbsshot.stereo.NeoStereoCapture;
import me.azo234.sbsshot.stereo.StereoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

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
                InputConstants.KEY_PERIOD,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyStereo.consumeClick()) {
                NeoStereoCapture.INSTANCE.capture(STEREO_CONFIG);
            }
        });

        // /sbsshot shot で撮影
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommands.literal("sbsshot")
                                // 引数なし /sbsshot は使い方を表示
                                .executes(ctx -> {
                                    ctx.getSource().sendFeedback(
                                            net.minecraft.network.chat.Component.translatable("sbsshot.command.usage"));
                                    return 1;
                                })
                                .then(ClientCommands.literal("shot")
                                        .executes(ctx -> {
                                            // GL 操作はメインスレッドで実行する
                                            Minecraft.getInstance().execute(() ->
                                                    NeoStereoCapture.INSTANCE.capture(STEREO_CONFIG));
                                            return 1;
                                        }))
                ));
    }
}
