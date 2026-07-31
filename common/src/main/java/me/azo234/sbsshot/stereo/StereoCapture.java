package me.azo234.sbsshot.stereo;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * ステレオ撮影の共通ロジック。
 *
 * カメラオフセットは各ローダーのサブクラスで実装する。
 * このクラスは撮影フロー・レンダーターゲット読み取り・SBS PNG 保存を担当する。
 *
 * フレームバッファ読み取りは {@link Screenshot#takeScreenshot} を使う。
 * これは blaze3d の GPU 抽象（CommandEncoder/GpuBuffer）経由で読み取るため、
 * OpenGL・Vulkan 双方のバックエンドで安全に動作する。
 * （旧実装は生の GL glReadPixels を使っていたため、Vulkan では
 *   「No context is current」で JVM が abort していた。）
 *
 * 読み取りは非同期（GPU フェンス後にコールバックが発火）である点に注意。
 * このためレンダリングと結合・保存はコールバックで受け渡す。
 */
public abstract class StereoCapture {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    /**
     * カメラを指定オフセットに移動してレンダリングし、
     * 読み取り結果（BufferedImage）を非同期にコールバックへ渡す。
     *
     * @param mc       Minecraft インスタンス
     * @param rightX   右方向単位ベクトル X
     * @param rightZ   右方向単位ベクトル Z
     * @param offset   カメラ横オフセット（ブロック単位、負=左目、正=右目）
     * @param consumer 読み取り完了時に画像を受け取るコールバック
     */
    protected abstract void renderWithOffset(Minecraft mc,
            double rightX, double rightZ, float offset, Consumer<BufferedImage> consumer);

    protected abstract void sendMessage(Minecraft mc, Component msg);

    /**
     * 視差を一時的に上書きして撮影する（設定は保存しない）。
     * 撮影後に元の視差へ戻す。
     */
    public void capture(StereoConfig config, float parallaxCmOverride) {
        float prev = config.parallaxCm;
        config.parallaxCm = parallaxCmOverride;
        try {
            capture(config);
        } finally {
            config.parallaxCm = prev;
        }
    }

    public void capture(StereoConfig config) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 実カメラの向きから右方向ベクトルを計算する。
        // leftVector() を使うことで 1人称・3人称（背面/正面）・離れ視点
        // （スペクテイター等）すべてでカメラの実際の視線に追従する。
        double[] right = cameraRight(mc);

        float offsetL = config.cameraOffsetBlocks(0);  // 負（左目）
        float offsetR = config.cameraOffsetBlocks(1);  // 正（右目）

        // 左右それぞれレンダリング → GPU 読み取りは非同期に発火する。
        // 両目の画像が揃った時点で結合・保存する。
        //
        // 2 回の renderLevel と 2 回の読み取りコピーは同一コマンドバッファに
        // 記録順で積まれ、GPU 上でもその順に実行される
        // （左描画→左コピー→右描画→右コピー）ため、各目の画素は正しく分離される。
        final BufferedImage[] eyes = new BufferedImage[2];
        renderWithOffset(mc, right[0], right[2], offsetL, img -> {
            eyes[0] = img;
            finishIfReady(mc, config, eyes);
        });
        renderWithOffset(mc, right[0], right[2], offsetR, img -> {
            eyes[1] = img;
            finishIfReady(mc, config, eyes);
        });

        // フレーム外で積んだコマンドをサブミットしてフェンス境界を作る。
        // これをしないと 26.2 の Vulkan で次フレームが
        // 「Cannot wait on a fence for the current submit」で落ちる。
        McCompat.submitCommands();
    }

    /** 両目の画像が揃っていれば SBS 結合して PNG 保存する。 */
    private void finishIfReady(Minecraft mc, StereoConfig config, BufferedImage[] eyes) {
        BufferedImage left = eyes[0], rightImg = eyes[1];
        if (left == null || rightImg == null) return;

        // SBS 結合（左 | 右）
        int w = left.getWidth(), h = left.getHeight();
        BufferedImage sbs = new BufferedImage(w * 2, h, BufferedImage.TYPE_INT_RGB);
        var g = sbs.getGraphics();
        g.drawImage(left,     0, 0, null);
        g.drawImage(rightImg, w, 0, null);
        g.dispose();

        // PNG 保存
        File outDir = new File(mc.gameDirectory, "screenshots/" + config.outputSubDir);
        //noinspection ResultOfMethodCallIgnored
        outDir.mkdirs();
        String ts  = LocalDateTime.now().format(DATE_FMT);
        File   out = new File(outDir, "stereo_" + ts + ".png");

        try (FileOutputStream fos = new FileOutputStream(out)) {
            ImageIO.write(sbs, "png", fos);
        } catch (IOException e) {
            e.printStackTrace();
            out = null;
        }

        Component msg = out != null
                ? Component.translatable("sbsshot.stereo.saved", out.getName())
                : Component.translatable("sbsshot.stereo.failed");
        sendMessage(mc, msg);
    }

    /**
     * メインレンダーターゲットを読み取り、BufferedImage を非同期にコールバックへ渡す。
     *
     * {@link Screenshot#takeScreenshot} は copyTextureToBuffer で
     * 「呼び出し時点の」テクスチャ内容を GPU バッファへ即コピーするため、
     * 左右の目でターゲットを共有していても取得順序は保たれる。
     * バッファ→NativeImage 変換（＝コールバック発火）は GPU フェンス後、
     * レンダースレッド上で行われる。
     */
    protected static void captureTarget(Minecraft mc, Consumer<BufferedImage> consumer) {
        RenderTarget rt = (RenderTarget) McCompat.mainRenderTarget(mc);
        Screenshot.takeScreenshot(rt, nativeImage -> {
            // NativeImage の所有権はコールバックにある。使い終わったら閉じる。
            try {
                consumer.accept(toBufferedImage(nativeImage));
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                nativeImage.close();
            }
        });
    }

    /**
     * NativeImage を不透明 RGB の BufferedImage に変換する。
     * NativeImage#getPixel は ARGB を返し、原点は左上（vanilla スクショと同じ向き）。
     */
    private static BufferedImage toBufferedImage(NativeImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, src.getPixel(x, y) & 0xFFFFFF);
            }
        }
        return img;
    }

    protected static double[] yawToRight(float yaw) {
        double rad = Math.toRadians(yaw + 90.0);
        return new double[]{ -Math.sin(rad), 0.0, Math.cos(rad) };
    }

    /**
     * 実カメラの左右軸（水平成分）から右方向の単位ベクトルを返す。
     * カメラ右 = -leftVector()。視点モードに依存せず正しい視差方向になる。
     * カメラがほぼ真上/真下を向く場合はカメラ yaw でフォールバックする。
     */
    protected static double[] cameraRight(Minecraft mc) {
        net.minecraft.client.Camera cam = McCompat.mainCamera(mc);
        org.joml.Vector3fc left = cam.leftVector();
        double rx = -left.x();
        double rz = -left.z();
        double len = Math.sqrt(rx * rx + rz * rz);
        if (len < 1.0e-4) {
            return yawToRight(cam.yRot());
        }
        return new double[]{ rx / len, 0.0, rz / len };
    }
}
