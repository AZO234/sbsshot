package me.azo234.sbsshot;

import me.azo234.sbsshot.stereo.StereoConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * NeoForge config — sbsshot-client.toml に parallax_cm と output_sub_dir を保存する。
 */
public class NeoConfigHelper {

    public static final ClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        Pair<ClientConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT      = pair.getLeft();
        CLIENT_SPEC = pair.getRight();
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static void applyTo(StereoConfig cfg) {
        cfg.parallaxCm   = CLIENT.parallaxCm.get().floatValue();
        cfg.outputSubDir = CLIENT.outputSubDir.get();
    }

    public static class ClientConfig {
        public final ModConfigSpec.DoubleValue parallaxCm;
        public final ModConfigSpec.ConfigValue<String> outputSubDir;

        ClientConfig(ModConfigSpec.Builder b) {
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
