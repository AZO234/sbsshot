package me.azo234.sbsshot.stereo;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ステレオ撮影の共通ロジック。
 *
 * カメラオフセットは各ローダーのサブクラスで実装する。
 * このクラスは撮影フロー・FBO からのフレームバッファ読み取り・PNS 保存を担当する。
 *
 * 26.1 の変更点:
 *   - renderLevel() は mainRenderTarget (FBO) に描画する（画面バッファではない）
 *   - FBO ID は colorTexture (GlTexture)#firstFboId から取得する
 *   - Camera.setup() 廃止 → update(DeltaTracker) + setPosition() で代替
 */
public abstract class StereoCapture {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    /**
     * カメラを指定オフセットに移動してレンダリングし、
     * フレームバッファを読んで返す。
     *
     * @param mc     Minecraft インスタンス
     * @param rightX 右方向単位ベクトル X
     * @param rightZ 右方向単位ベクトル Z
     * @param offset カメラ横オフセット（ブロック単位、負=左目、正=右目）
     */
    protected abstract BufferedImage renderWithOffset(Minecraft mc,
            double rightX, double rightZ, float offset);

    protected abstract void sendMessage(Minecraft mc, Component msg);

    public void capture(StereoConfig config) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean wasPaused = mc.isPaused();
        if (!wasPaused) mc.pauseGame(false);

        try {
            // プレイヤーの向きから右方向ベクトルを計算
            float yaw = mc.player.getYRot();
            double[] right = yawToRight(yaw);

            float offsetL = config.cameraOffsetBlocks(0);  // 負（左目）
            float offsetR = config.cameraOffsetBlocks(1);  // 正（右目）

            BufferedImage left    = renderWithOffset(mc, right[0], right[2], offsetL);
            BufferedImage rightImg = renderWithOffset(mc, right[0], right[2], offsetR);

            if (left == null || rightImg == null) return;

            // SBS 結合（左 | 右）
            int w = left.getWidth(), h = left.getHeight();
            BufferedImage sbs = new BufferedImage(w * 2, h, BufferedImage.TYPE_INT_RGB);
            sbs.getGraphics().drawImage(left,      0, 0, null);
            sbs.getGraphics().drawImage(rightImg,  w, 0, null);

            // PNS 保存
            File outDir = new File(mc.gameDirectory, "screenshots/" + config.outputSubDir);
            //noinspection ResultOfMethodCallIgnored
            outDir.mkdirs();
            String ts  = LocalDateTime.now().format(DATE_FMT);
            File   out = new File(outDir, "stereo_" + ts + ".pns");

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

        } finally {
            if (!wasPaused) mc.pauseGame(false);
        }
    }

    protected static BufferedImage readFramebuffer(Minecraft mc) {
        // renderLevel() は mainRenderTarget (FBO) に描画する。
        // GL11.glReadPixels はデフォルト FBO を読むため、mainRenderTarget の FBO を bind する。
        Object rt = mc.getMainRenderTarget();
        int w = getRtInt(rt, "width",  mc.getWindow().getWidth());
        int h = getRtInt(rt, "height", mc.getWindow().getHeight());

        int fboId = getFboId(rt);
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, fboId);
        org.lwjgl.opengl.GL11.glFinish();

        ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4);
        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);

        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, 0);

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = buf.get() & 0xFF, g = buf.get() & 0xFF, b = buf.get() & 0xFF;
                buf.get();
                img.setRGB(x, h - 1 - y, (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    private static java.lang.reflect.Field findField(Class<?> cls, String name) {
        while (cls != null && cls != Object.class) {
            try {
                var f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Exception ignored) {}
            cls = cls.getSuperclass();
        }
        return null;
    }

    private static int getRtInt(Object rt, String fieldName, int fallback) {
        Class<?> cls = rt.getClass();
        while (cls != null) {
            try {
                var f = cls.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.getInt(rt);
            } catch (Exception ignored) {}
            cls = cls.getSuperclass();
        }
        return fallback;
    }

    /** RenderTarget の FBO ID を取得。
     *  26.1: colorTexture (GlTexture)#firstFboId が FBO ID。
     */
    private static int getFboId(Object rt) {
        try {
            var colorTexField = findField(rt.getClass(), "colorTexture");
            if (colorTexField != null) {
                Object colorTex = colorTexField.get(rt);
                var fboIdField = findField(colorTex.getClass(), "firstFboId");
                if (fboIdField != null) {
                    return fboIdField.getInt(colorTex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("[SBSShot] WARNING: Could not find FBO id");
        return 0;
    }

    protected static double[] yawToRight(float yaw) {
        double rad = Math.toRadians(yaw + 90.0);
        return new double[]{ -Math.sin(rad), 0.0, Math.cos(rad) };
    }
}
