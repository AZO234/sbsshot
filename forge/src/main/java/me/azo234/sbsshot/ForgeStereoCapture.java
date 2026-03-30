package me.azo234.sbsshot;

import com.mojang.blaze3d.vertex.PoseStack;
import me.azo234.sbsshot.stereo.StereoCapture;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

/**
 * Forge 1.20.1 用 StereoCapture 実装。
 * Camera.setup() をリフレクションで呼び、エンティティ移動後にカメラ位置を確定する。
 */
public class ForgeStereoCapture extends StereoCapture {

    public static final ForgeStereoCapture INSTANCE = new ForgeStereoCapture();

    private static final Method CAMERA_SETUP;
    static {
        Method m = null;
        for (Method method : Camera.class.getDeclaredMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if (p.length == 5
                    && net.minecraft.world.level.BlockGetter.class.isAssignableFrom(p[0])
                    && net.minecraft.world.entity.Entity.class.isAssignableFrom(p[1])
                    && p[2] == boolean.class
                    && p[3] == boolean.class
                    && p[4] == float.class) {
                method.setAccessible(true);
                m = method;
                break;
            }
        }
        if (m == null) {
            System.err.println("[SBSShot] WARNING: Camera.setup() method not found via reflection!");
        }
        CAMERA_SETUP = m;
    }

    @Override
    protected BufferedImage renderWithOffset(Minecraft mc,
            double rightX, double rightZ, float offset) {
        var entity = mc.getCameraEntity();
        if (entity == null) return null;

        double ox = entity.getX(), oy = entity.getY(), oz = entity.getZ();
        float oyaw = entity.getYRot(), opitch = entity.getXRot();

        try {
            entity.setPos(ox + rightX * offset, oy, oz + rightZ * offset);
            entity.setYRot(oyaw);
            entity.setXRot(opitch);

            if (CAMERA_SETUP != null) {
                Camera camera = mc.gameRenderer.getMainCamera();
                CAMERA_SETUP.invoke(camera, mc.level, entity, false, false, 1.0f);
            }

            mc.gameRenderer.renderLevel(1.0f, System.nanoTime(), new PoseStack());
            return readFramebuffer(mc);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            entity.setPos(ox, oy, oz);
            entity.setYRot(oyaw);
            entity.setXRot(opitch);
        }
    }

    @Override
    protected void sendMessage(Minecraft mc, Component msg) {
        if (mc.player != null) mc.player.displayClientMessage(msg, false);
    }
}
