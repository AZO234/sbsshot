# SBSShot

> 🇬🇧 [English README is here](README.md)

**SBSShot** は、VR/HMD 向けに **Side-by-Side (SBS) ステレオスクリーンショット**を撮影する Minecraft mod です。

**F12** を押すだけで、左目・右目の視点を横並びにした `.pns`（PNG Stereo）ファイルを保存します。

---

## プレビュー

`.pns` ファイルは **SBS Stereo Viewer** で閲覧できます：

👉 **[https://azo234.github.io/sbs-stereo-viewer/](https://azo234.github.io/sbs-stereo-viewer/)**

---

## 機能

- F12 一発で SBS ステレオスクリーンショットを撮影
- `.pns`（PNG Side-by-Side Stereo）形式で出力
- 保存先：`.minecraft/screenshots/stereo/stereo_YYYY-MM-DD_HH.mm.ss.pns`
- 視差（デフォルト: 6.5 cm）と出力サブディレクトリを設定可能
- HUD なしのレンダリング — VR/HMD 向けに最適
- **Forge 1.20.1**・**NeoForge 26.1**・**Fabric 26.1** 対応

---

## インストール

mod の `.jar` ファイルを `.minecraft/mods/` フォルダに入れるだけです。依存 mod は不要です。

---

## 設定

| キー | デフォルト | 説明 |
|------|----------|------|
| `parallax_cm` | `6.5` | カメラ間隔（センチメートル） |
| `output_sub_dir` | `stereo` | `screenshots/` 内のサブディレクトリ名 |

設定は **Mod Menu**（Fabric）または **Config** 画面（NeoForge/Forge）からゲーム内で変更できます。

---

## キーバインド

| キー | 操作 |
|------|------|
| F12 | SBS ステレオスクリーンショットを撮影 |

---

## 技術的な注意点

左目・右目それぞれに視差分だけ水平にカメラをオフセットして 2 回レンダリングします。フレームバッファはメインレンダーターゲットの FBO から直接読み取ることで正確な画像を取得します。

### Minecraft 26.1 での変更対応

- `Camera.setup()` 廃止 → `Camera.update(DeltaTracker)` + `Camera.setPosition(Vec3)` で代替
- `renderLevel()` は画面バッファではなく `mainRenderTarget`（FBO）に描画 → `GlTexture#firstFboId` で FBO ID を取得
- `ResourceLocation` → `Identifier` に改名
- `KeyBindingHelper` → `KeyMappingHelper` に改名

---

## ライセンス

MIT
