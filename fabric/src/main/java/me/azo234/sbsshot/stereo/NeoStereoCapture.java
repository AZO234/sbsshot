package me.azo234.sbsshot.stereo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.awt.image.BufferedImage;

public class NeoStereoCapture extends StereoCapture {

    public static final NeoStereoCapture INSTANCE = new NeoStereoCapture();

    @Override
    protected BufferedImage renderWithOffset(Minecraft mc,
            double rightX, double rightZ, float offset) {
        var entity = mc.getCameraEntity();
        if (entity == null) return null;

        double ox = entity.getX(), oy = entity.getY(), oz = entity.getZ();
        float oyaw = entity.getYRot(), opitch = entity.getXRot();

        try {
            entity.setPos(ox + rightX * offset, oy, oz + rightZ * offset);
            mc.gameRenderer.update(DeltaTracker.ONE, true);
            mc.gameRenderer.extract(DeltaTracker.ONE, true);
            mc.gameRenderer.renderLevel(DeltaTracker.ONE);
            return readFramebuffer(mc);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            entity.setPos(ox, oy, oz);
            entity.setYRot(oyaw);
            entity.setXRot(opitch);
            mc.gameRenderer.update(DeltaTracker.ONE, true);
            mc.gameRenderer.extract(DeltaTracker.ONE, true);
        }
    }

    @Override
    protected void sendMessage(Minecraft mc, Component msg) {
        if (mc.player != null) mc.player.sendSystemMessage(msg);
    }
}
