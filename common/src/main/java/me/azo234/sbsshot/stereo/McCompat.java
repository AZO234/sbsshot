package me.azo234.sbsshot.stereo;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Method;

/**
 * Minecraft 26.1.x と 26.2 の API 差異を吸収する互換レイヤ。
 *
 * 単一ソースを複数の MC バージョンでビルドするため、差異のあるメソッドは
 * （loom が Mojang 公式マッピングを使う前提で）リフレクションで呼ぶ。
 *
 * 26.2 での変更点:
 *   - Minecraft.getMainRenderTarget() 廃止 → GameRenderer.mainRenderTarget()
 *   - GameRenderer.getMainCamera()        → GameRenderer.mainCamera()
 *   - GameRenderer.update(DeltaTracker, boolean) → update(DeltaTracker)
 *   - Minecraft.setScreen(Screen)         → setScreenAndShow(Screen)
 */
public final class McCompat {

    private McCompat() {}

    /** メインの RenderTarget（FBO ラッパー）を取得する。 */
    public static Object mainRenderTarget(Minecraft mc) {
        // 26.1: Minecraft#getMainRenderTarget
        try {
            Method m = mc.getClass().getMethod("getMainRenderTarget");
            return m.invoke(mc);
        } catch (ReflectiveOperationException ignored) {}
        // 26.2: GameRenderer#mainRenderTarget
        try {
            Object gr = mc.gameRenderer;
            Method m = gr.getClass().getMethod("mainRenderTarget");
            return m.invoke(gr);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("[SBSShot] main RenderTarget not found", e);
        }
    }

    /** メインカメラを取得する。 */
    public static Camera mainCamera(Minecraft mc) {
        Object gr = mc.gameRenderer;
        // 26.1: getMainCamera, 26.2: mainCamera
        for (String name : new String[]{"getMainCamera", "mainCamera"}) {
            try {
                Method m = gr.getClass().getMethod(name);
                return (Camera) m.invoke(gr);
            } catch (ReflectiveOperationException ignored) {}
        }
        throw new IllegalStateException("[SBSShot] main Camera not found");
    }

    /** GameRenderer の状態を更新する。 */
    public static void updateGameRenderer(Minecraft mc, DeltaTracker delta) {
        Object gr = mc.gameRenderer;
        // 26.1: update(DeltaTracker, boolean)
        try {
            Method m = gr.getClass().getMethod("update", DeltaTracker.class, boolean.class);
            m.invoke(gr, delta, true);
            return;
        } catch (ReflectiveOperationException ignored) {}
        // 26.2: update(DeltaTracker)
        try {
            Method m = gr.getClass().getMethod("update", DeltaTracker.class);
            m.invoke(gr, delta);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("[SBSShot] GameRenderer.update not found", e);
        }
    }

    /** 画面を切り替える。 */
    public static void setScreen(Minecraft mc, Screen screen) {
        // 26.1: setScreen, 26.2: setScreenAndShow
        for (String name : new String[]{"setScreen", "setScreenAndShow"}) {
            try {
                Method m = mc.getClass().getMethod(name, Screen.class);
                m.invoke(mc, screen);
                return;
            } catch (ReflectiveOperationException ignored) {}
        }
        throw new IllegalStateException("[SBSShot] setScreen not found");
    }
}
