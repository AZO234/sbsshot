package me.azo234.sbsshot;

import me.azo234.sbsshot.stereo.StereoConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Fabric config — config/sbsshot.properties に parallax_cm と output_sub_dir を保存する。
 */
public class FabricConfigHelper {

    private static final String FILE_NAME = "sbsshot.properties";

    public static void load(StereoConfig cfg) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!path.toFile().exists()) {
            save(cfg);
            return;
        }
        Properties props = new Properties();
        try (Reader r = new FileReader(path.toFile())) {
            props.load(r);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try { cfg.parallaxCm = Float.parseFloat(
                props.getProperty("stereo.parallax_cm", String.valueOf(cfg.parallaxCm)));
        } catch (NumberFormatException ignored) {}
        cfg.outputSubDir = props.getProperty("stereo.output_sub_dir", cfg.outputSubDir);
    }

    public static void save(StereoConfig cfg) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties props = new Properties();
        props.setProperty("stereo.parallax_cm",   String.valueOf(cfg.parallaxCm));
        props.setProperty("stereo.output_sub_dir", cfg.outputSubDir);
        try (Writer w = new FileWriter(path.toFile())) {
            props.store(w, "SBSShot config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
