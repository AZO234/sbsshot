package me.azo234.sbsshot;

import me.azo234.sbsshot.stereo.StereoConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Forge config — sbsshot-client.toml に parallax_cm と output_sub_dir を保存する。
 * Config 画面は ForgeConfigSpec を登録するだけで Mods 画面の「Config」ボタンから
 * Forge 組み込みの自動生成画面として開ける。
 */
public class ForgeConfigHelper {

    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<ClientConfig, ForgeConfigSpec> pair =
                new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT      = pair.getLeft();
        CLIENT_SPEC = pair.getRight();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static void applyTo(StereoConfig cfg) {
        cfg.parallaxCm   = CLIENT.parallaxCm.get().floatValue();
        cfg.outputSubDir = CLIENT.outputSubDir.get();
    }

    public static class ClientConfig {
        public final ForgeConfigSpec.DoubleValue parallaxCm;
        public final ForgeConfigSpec.ConfigValue<String> outputSubDir;

        ClientConfig(ForgeConfigSpec.Builder b) {
            b.comment("SBSShot settings").push("stereo");

            parallaxCm = b
                    .comment("Inter-ocular distance in cm (human average ~6.5)")
                    .translation("sbsshot.config.stereo.parallax_cm")
                    .defineInRange("parallax_cm", 6.5, 1.0, 30.0);

            outputSubDir = b
                    .comment("Sub-directory under screenshots/")
                    .translation("sbsshot.config.stereo.output_sub_dir")
                    .define("output_sub_dir", "stereo");

            b.pop();
        }
    }
}
