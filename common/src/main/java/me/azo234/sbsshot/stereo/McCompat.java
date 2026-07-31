package me.azo234.sbsshot.stereo;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
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
 *   - CommandEncoder.submit() 追加（明示的なコマンドサブミット）
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

    /**
     * 記録済みの GPU コマンドをサブミットしてフェンス境界を作る（26.2+）。
     *
     * 26.2 では 1 フレーム＝1 コマンドサブミットで構築されるため、
     * フレーム外（クライアント tick 中）で renderLevel()/extract() を行うと、
     * ユニフォームのリングバッファがサブミットされないまま進み、次の実フレームが
     * 「現在のサブミットのフェンスを待てない」で落ちる（特に Vulkan）。
     * 手動レンダー後に submit() してコマンドバッファを閉じることで解消する。
     *
     * 26.1.x には CommandEncoder#submit が無い（フレーム末尾で暗黙サブミット）ため
     * リフレクションが失敗し no-op になる。
     */
    public static void submitCommands() {
        try {
            CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
            // submit() は 26.2 で追加。直接参照すると 26.1.x でコンパイル不能なため反射。
            enc.getClass().getMethod("submit").invoke(enc);
        } catch (ReflectiveOperationException ignored) {
            // 26.1.x: submit() 無し
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
