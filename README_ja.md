[<img src="images/minecraft.svg" width="64" alt="Fabric icon">](https://www.minecraft.net/en-us/store/minecraft-deluxe-collection-pc) [<img src="images/fabric.png" width="64" alt="Fabric icon">](https://fabricmc.net/) [<img src="images/neoforge.png" width="64" alt="NeoForge icon">](https://neoforged.net/) [<img src="images/modrinth.webp" width="64" alt="Modrinth icon">](https://modrinth.com/) [<img src="images/curseforge.png" width="64" alt="CurseForge icon">](https://www.curseforge.com/) <img src="images/java.svg" height="64" alt="Java icon"> 

# <img src="common/pack.png" height="64" alt="SBSShot icon"> SBSShot

![](https://img.shields.io/github/v/tag/azo234/sbsshot) ![](https://img.shields.io/github/actions/workflow/status/azo234/sbsshot/publish.yml)

> 🇬🇧 [English README is here](README.md)

<img src="images/stereo_2026-03-30_15.43.56.png" alt="PNS screenshot"> 

**SBSShot** は、VR/HMD 向けに **Side-by-Side (SBS) ステレオスクリーンショット**を撮影する Minecraft mod です。

**.** キーで、左目・右目の視点を横並びにした `.pns`（PNG Stereo）ファイルを保存します。

---

## プレビュー

`.pns` ファイルは **SBS Stereo Viewer** で閲覧できます：

👉 **[https://azo234.github.io/sbs-stereo-viewer/](https://azo234.github.io/sbs-stereo-viewer/)**

---

## 機能

- `.` キーで SBS ステレオスクリーンショットを撮影
- `.pns`（PNG Side-by-Side Stereo）形式で出力
- 保存先：`.minecraft/screenshots/stereo/stereo_YYYY-MM-DD_HH.mm.ss.pns`
- 視差（デフォルト: 6.5 cm）と出力サブディレクトリを設定可能
- HUD なしのレンダリング — VR/HMD 向けに最適
- **NeoForge**・**Fabric** 対応

---

## インストール

mod の `.jar` ファイルを `.minecraft/mods/` フォルダに入れます。

**Fabric** では、以下の依存 mod も必要です。

- **Fabric API**  
  https://modrinth.com/mod/fabric-api  
  https://www.curseforge.com/minecraft/mc-mods/fabric-api
- **Mod Menu**  
  ゲーム内設定画面を開くための推奨 mod です。  
  https://modrinth.com/mod/modmenu  
  https://www.curseforge.com/minecraft/mc-mods/modmenu

**NeoForge** では追加の依存 mod は不要です。

---

## 設定

| キー | デフォルト | 説明 |
|------|----------|------|
| `parallax_cm` | `6.5` | カメラ間隔（センチメートル） |
| `output_sub_dir` | `stereo` | `screenshots/` 内のサブディレクトリ名 |

設定は **Mod Menu**（Fabric）または **Config** 画面（NeoForge）からゲーム内で変更できます。

---

## キーバインド

| キー | 操作 |
|------|------|
| . | SBS ステレオスクリーンショットを撮影 |

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

## リンク

[Modrinth](https://modrinth.com/project/sbsshot)  
[CurseForge](https://legacy.curseforge.com/minecraft/mc-mods/sbsshot)
