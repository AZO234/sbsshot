package me.azo234.sbsshot.stereo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class NeoStereoCapture extends StereoCapture {

    public static final NeoStereoCapture INSTANCE = new NeoStereoCapture();

    @Override
    protected void renderWithOffset(Minecraft mc,
            double rightX, double rightZ, float offset, Consumer<BufferedImage> consumer) {
        var entity = mc.getCameraEntity();
        if (entity == null) return;

        double ox = entity.getX(), oy = entity.getY(), oz = entity.getZ();
        float oyaw = entity.getYRot(), opitch = entity.getXRot();

        try {
            entity.setPos(ox + rightX * offset, oy, oz + rightZ * offset);
            McCompat.updateGameRenderer(mc, DeltaTracker.ONE);
            mc.gameRenderer.extract(DeltaTracker.ONE, true);
            mc.gameRenderer.renderLevel(DeltaTracker.ONE);
            // 読み取りは非同期。この時点のフレームバッファ内容が即 GPU バッファへコピーされる。
            captureTarget(mc, consumer);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            entity.setPos(ox, oy, oz);
            entity.setYRot(oyaw);
            entity.setXRot(opitch);
            McCompat.updateGameRenderer(mc, DeltaTracker.ONE);
            mc.gameRenderer.extract(DeltaTracker.ONE, true);
        }
    }

    @Override
    protected void sendMessage(Minecraft mc, Component msg) {
        if (mc.player != null) mc.player.sendSystemMessage(msg);
    }
}
