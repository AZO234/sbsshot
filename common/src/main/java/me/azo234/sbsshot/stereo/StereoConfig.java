package me.azo234.sbsshot.stereo;

/**
 * ローダー非依存の撮影設定。
 * 出力形式は PNS（PNG Side-by-Side Stereo）固定。
 * 撮影は左右2フレーム固定。
 */
public class StereoConfig {

    /** 左右の視点間隔（cm）。人間の平均眼幅 ≈ 6.5cm */
    public float parallaxCm = 6.5f;

    /** 出力先サブディレクトリ名（screenshots/ 以下） */
    public String outputSubDir = "stereo";

    // ---- 単位換算 --------------------------------------------------------

    /**
     * Minecraft のブロック単位に換算した左右オフセット（1 block = 1 m）。
     * i=0 → -parallax/2（左目）、i=1 → +parallax/2（右目）。
     */
    public float cameraOffsetBlocks(int i) {
        float half = (parallaxCm / 100.0f) / 2.0f;
        return i == 0 ? -half : half;
    }
}
