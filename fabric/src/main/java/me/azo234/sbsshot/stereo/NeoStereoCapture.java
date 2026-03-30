package me.azo234.sbsshot.stereo;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

public class NeoStereoCapture extends StereoCapture {

    public static final NeoStereoCapture INSTANCE = new NeoStereoCapture();

    private static final Method CAMERA_UPDATE;
    private static final Method CAMERA_POSITION;
    private static final Method CAMERA_SET_POSITION;

    static {
        Method update = null, position = null, setPos = null;
        for (Method m : Camera.class.getDeclaredMethods()) {
            m.setAccessible(true);
            String n = m.getName();
            Class<?>[] p = m.getParameterTypes();
            if (n.equals("update") && p.length == 1
                    && p[0].getSimpleName().equals("DeltaTracker")) update = m;
            if (n.equals("position") && p.length == 0) position = m;
            if (n.equals("setPosition") && p.length == 1
                    && p[0] == Vec3.class) setPos = m;
        }
        CAMERA_UPDATE       = update;
        CAMERA_POSITION     = position;
        CAMERA_SET_POSITION = setPos;
    }

    @Override
    protected BufferedImage renderWithOffset(Minecraft mc,
            double rightX, double rightZ, float offset) {
        var entity = mc.getCameraEntity();
        if (entity == null) return null;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 basePos = getCameraPos(camera);
        if (basePos == null) basePos = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());

        double ox = entity.getX(), oy = entity.getY(), oz = entity.getZ();
        float oyaw = entity.getYRot(), opitch = entity.getXRot();

        try {
            entity.setPos(ox + rightX * offset, oy, oz + rightZ * offset);

            if (CAMERA_UPDATE != null) {
                CAMERA_UPDATE.invoke(camera, DeltaTracker.ONE);
            }

            Vec3 targetPos = basePos.add(rightX * offset, 0, rightZ * offset);
            setCameraPos(camera, targetPos);

            mc.gameRenderer.renderLevel(DeltaTracker.ONE);
            return readFramebuffer(mc);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            entity.setPos(ox, oy, oz);
            entity.setYRot(oyaw);
            entity.setXRot(opitch);
            setCameraPos(camera, basePos);
        }
    }

    private static Vec3 getCameraPos(Camera camera) {
        if (CAMERA_POSITION != null) {
            try { return (Vec3) CAMERA_POSITION.invoke(camera); } catch (Exception ignored) {}
        }
        return null;
    }

    private static void setCameraPos(Camera camera, Vec3 pos) {
        if (CAMERA_SET_POSITION != null) {
            try { CAMERA_SET_POSITION.invoke(camera, pos); return; } catch (Exception ignored) {}
        }
        // フォールバック: position フィールドに直接書き込み
        try {
            var f = Camera.class.getDeclaredField("position");
            f.setAccessible(true);
            f.set(camera, pos);
        } catch (Exception ignored) {}
    }

    @Override
    protected void sendMessage(Minecraft mc, Component msg) {
        if (mc.player != null) mc.player.sendSystemMessage(msg);
    }
}
