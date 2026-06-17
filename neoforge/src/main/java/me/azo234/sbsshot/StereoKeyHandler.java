package me.azo234.sbsshot;

import com.mojang.brigadier.arguments.FloatArgumentType;
import me.azo234.sbsshot.stereo.NeoStereoCapture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class StereoKeyHandler {

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("sbsshot", "category"));

    public static final KeyMapping KEY_STEREO = new KeyMapping(
            "key.sbsshot.stereo_screenshot",
            GLFW.GLFW_KEY_PERIOD,
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

    // NeoForge バスに addListener(StereoKeyHandler::onRegisterClientCommands) で登録
    // /sbsshot shot で撮影
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("sbsshot")
                        // 引数なし /sbsshot は使い方を表示
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() ->
                                    Component.translatable("sbsshot.command.usage"), false);
                            return 1;
                        })
                        // /sbsshot shot [parallax]
                        .then(Commands.literal("shot")
                                .executes(ctx -> {
                                    // GL 操作はメインスレッドで実行する
                                    Minecraft.getInstance().execute(() ->
                                            NeoStereoCapture.INSTANCE.capture(SbsShotMod.STEREO_CONFIG));
                                    return 1;
                                })
                                .then(Commands.argument("parallax", FloatArgumentType.floatArg(0.0f, 30.0f))
                                        .executes(ctx -> {
                                            float p = FloatArgumentType.getFloat(ctx, "parallax");
                                            Minecraft.getInstance().execute(() ->
                                                    NeoStereoCapture.INSTANCE.capture(SbsShotMod.STEREO_CONFIG, p));
                                            return 1;
                                        })))
                        // /sbsshot get_parallax
                        .then(Commands.literal("get_parallax")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "sbsshot.command.get_parallax", SbsShotMod.STEREO_CONFIG.parallaxCm), false);
                                    return 1;
                                }))
                        // /sbsshot set_parallax <parallax>
                        .then(Commands.literal("set_parallax")
                                .then(Commands.argument("parallax", FloatArgumentType.floatArg(0.0f, 30.0f))
                                        .executes(ctx -> {
                                            float p = FloatArgumentType.getFloat(ctx, "parallax");
                                            SbsShotMod.STEREO_CONFIG.parallaxCm = p;
                                            NeoConfigHelper.saveParallax(p);
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    "sbsshot.command.set_parallax", p), false);
                                            return 1;
                                        })))
        );
    }
}
