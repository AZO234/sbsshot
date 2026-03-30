package me.azo234.sbsshot;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.client.KeyMapping;

@OnlyIn(Dist.CLIENT)
public class StereoKeyHandler {

    public static final KeyMapping KEY_STEREO = new KeyMapping(
            "key.sbsshot.stereo_screenshot",   // i18n キー
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F12,                  // デフォルト: F12（configで変更可）
            "key.categories.sbsshot"
    );

    /** MOD バスに登録 */
    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY_STEREO);
    }

    /** FORGE イベントバスに登録 → クライアント tick ごとに押下チェック */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (KEY_STEREO.consumeClick()) {
            ForgeStereoCapture.INSTANCE.capture(SbsShotMod.STEREO_CONFIG);
        }
    }
}
